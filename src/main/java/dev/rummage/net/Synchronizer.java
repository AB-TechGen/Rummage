package dev.rummage.net;

import dev.rummage.core.Match;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

/** Only game events travel over this socket; no world data or account credentials. */
public final class Synchronizer implements AutoCloseable {
    private final ServerSocket server;
    private final Match match = new Match();
    private final Map<String, Peer> peers = new HashMap<>();
    private final Map<String, String> tokens = new HashMap<>();
    private final Set<Socket> sockets = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService clock = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "rummage-clock"); t.setDaemon(true); return t; });
    private final Semaphore capacity = new Semaphore(16);
    public final String secret = UUID.randomUUID().toString();
    public Synchronizer(int port) throws IOException {
        server = new ServerSocket(port);
        match.seed = new SecureRandom().nextLong();
        Thread.ofVirtual().name("rummage-accept").start(this::accept);
        match.graceMillis=300;
        clock.scheduleAtFixedRate(() -> { synchronized (match) { long now=Snapshot.now(); match.tick(now); for(Peer p:peers.values())if(now-p.pingSent>=2000){p.pingSent=now;p.nonce=UUID.randomUUID().toString();p.send("PROBE",p.nonce);} tokens.values().removeIf(id -> !match.players.containsKey(id)); broadcast(Snapshot.encode(match, now)); } }, 0, 100, TimeUnit.MILLISECONDS);
    }
    public int port() { return server.getLocalPort(); }
    public String invite(String host) { return "BS1-" + Wire.pack(host, ""+port(), secret); }
    private void accept() {
        while (!server.isClosed()) {
            try {
                Socket socket = server.accept();
                if (!capacity.tryAcquire()) { socket.close(); continue; }
                sockets.add(socket);
                Thread.ofVirtual().start(() -> serve(socket));
            } catch (IOException e) { if (!server.isClosed()) System.err.println("Accept failed: " + e.getMessage()); }
        }
    }
    private void serve(Socket socket) {
        Peer peer = null;
        try (socket) {
            socket.setSoTimeout(15000); socket.setTcpNoDelay(true);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            String[] hello = Wire.read(in);
            if (hello.length != 5 || !hello[0].equals("HELLO") || !hello[1].equals(Match.VERSION) || !hello[2].equals(secret)) { Wire.write(out,"ERROR","Wrong invite or Rummage version."); return; }
            synchronized (match) {
                String id;
                if (!hello[4].isEmpty()) {
                    id = tokens.get(hello[4]);
                    if (id == null) { Wire.write(out,"ERROR","Reconnect slot not found."); return; }
                    Peer old=peers.remove(id);
                    if(old!=null){old.socket.close();match.disconnect(id,Snapshot.now());}
                    try { match.reconnect(id, Snapshot.now()); } catch (IllegalArgumentException e) { Wire.write(out,"ERROR",e.getMessage()); return; }
                } else { try { id = match.join(hello[3]).id; } catch (IllegalArgumentException e) { Wire.write(out,"ERROR",e.getMessage()); return; } }
                String token = hello[4].isEmpty() ? UUID.randomUUID().toString() : hello[4];
                tokens.put(token, id);
                peer = new Peer(id, socket, out);
                peers.put(id,peer);
                peer.send("WELCOME",id,token);
                broadcast("CHAT","Lobby",match.players.get(id).name + " joined.");
            }
            Peer writer = peer;
            Thread.ofVirtual().start(writer::writeLoop);
            long window = Snapshot.now(); int messages = 0;
            while (!socket.isClosed()) {
                String[] f = Wire.read(in);
                long now = Snapshot.now();
                if (now-window >= 1000) { messages=0; window=now; }
                if (++messages > 30) throw new IOException("Too many messages");
                synchronized (match) {
                    try {
                        String id = peer.id;
                        switch (f[0]) {
                            case "PING" -> peer.send("PONG");
                            case "PROBE_REPLY" -> { if(f[1].equals(peer.nonce)){int rtt=(int)Math.min(2000,now-peer.pingSent);peer.lag=Math.min(300,rtt/2);match.players.get(id).ping=rtt;peer.nonce="";} }
                            case "RULES" -> match.rules(id,dev.rummage.core.MatchRules.decode(f[1]),Boolean.parseBoolean(f[2]));
                            case "WORLD" -> match.worldReport(id,f[1],f[2].isEmpty()?Set.of():Set.of(f[2].split(",")),Boolean.parseBoolean(f[3]),now);
                            case "DRAW" -> match.voteDraw(id,now);
                            case "DIMENSION" -> { if(f[1].matches("[a-z0-9_.-]+:[a-z0-9_./-]+")&&f[1].length()<100)match.players.get(id).dimension=f[1]; }
                            case "CONFIG" -> match.configure(id, Long.parseLong(f[1]),Integer.parseInt(f[2]),Integer.parseInt(f[3]));
                            case "START" -> { match.seedAttempts=0; match.start(id, now); }
                            case "READY" -> match.ready(id,f[1],Long.parseLong(f[2]));
                            case "CLAIM" -> match.claim(id,f[1],Integer.parseInt(f[2]),f[3],now-peer.lag);
                            case "FORFEIT" -> match.forfeit(id);
                            case "RESET" -> match.reset(id);
                            case "LEAVE" -> { match.leave(id,now); tokens.values().removeIf(id::equals); return; }
                            case "CHAT" -> { if (f[1].length() > 240) throw new IllegalArgumentException("Chat limit is 240 characters."); broadcast("CHAT",match.players.get(id).name,f[1].replaceAll("[\\p{Cntrl}§]", "")); }
                            default -> throw new IllegalArgumentException("Unknown message");
                        }
                    } catch (RuntimeException e) { peer.send("ERROR",e instanceof IllegalArgumentException && e.getMessage()!=null ? e.getMessage() : "Invalid message"); }
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // A broken connection keeps the player's token and slot for five minutes.
        } finally {
            synchronized (match) { if (peer != null && peers.get(peer.id) == peer) { peers.remove(peer.id); match.disconnect(peer.id, Snapshot.now()); } }
            sockets.remove(socket); capacity.release();
        }
    }
    private void broadcast(String... f) { peers.values().forEach(p -> p.send(f)); }
    @Override public void close() throws IOException { clock.shutdownNow(); server.close(); for (Socket s : sockets) try { s.close(); } catch (IOException ignored) {} }
    private static final class Peer {
        final String id; final Socket socket; final DataOutputStream out;
        final BlockingQueue<String[]> queue = new ArrayBlockingQueue<>(64);
        long pingSent; int lag; String nonce="";
        Peer(String id, Socket socket, DataOutputStream out) { this.id=id; this.socket=socket; this.out=out; }
        void send(String... f) { if (!queue.offer(f)) try { socket.close(); } catch (IOException ignored) {} }
        void writeLoop() {
            try { while (!socket.isClosed()) { String[] f = queue.poll(1, TimeUnit.SECONDS); if (f != null) Wire.write(out,f); } }
            catch (IOException | InterruptedException e) { try { socket.close(); } catch (IOException ignored) {} }
        }
    }
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 25570;
        Synchronizer s = new Synchronizer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { s.close(); } catch (IOException ignored) {} }));
        System.out.println("Rummage " + Match.VERSION + " listening on TCP " + s.port());
        System.out.println("Invite: " + s.invite(args.length>1?args[1]:InetAddress.getLocalHost().getHostAddress()));
        if(args.length>2){Relay.Host relay=new Relay.Host(args[2],s.port(),s.secret);Runtime.getRuntime().addShutdownHook(new Thread(relay::close));Thread.ofVirtual().start(()->{try{while(relay.code.isEmpty())Thread.sleep(200);System.out.println("Relay invite: RM-"+relay.code);}catch(InterruptedException ignored){}});}
        else System.out.println("Direct play needs a reachable address. Pass a TLS relay endpoint as the third argument to enable short invites.");
        new CountDownLatch(1).await();
    }
}
