package dev.rummage.net;

import dev.rummage.core.Match;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class LobbyConnection implements AutoCloseable {
    public volatile Snapshot state;
    public volatile String id = "", status = "Connecting…";
    public volatile boolean connected;
    private volatile boolean closed;
    private volatile Socket socket;
    private volatile DataOutputStream out;
    private String token = "";
    private final String host, secret, name;
    private final int port;
    private final Consumer<String> messages;
    private Consumer<String> saveToken = t -> {};
    private String relayEndpoint="", relayCode="";
    private final BlockingQueue<String[]> outgoing = new ArrayBlockingQueue<>(64);
    public LobbyConnection(String invite, String name, Consumer<String> messages) {
        this(invite,name,messages,"",t -> {},"");
    }
    public LobbyConnection(String invite, String name, Consumer<String> messages, String savedToken, Consumer<String> saveToken, String relay) {
        if(invite.startsWith("RM-")) { relayEndpoint=relay;relayCode=invite.substring(3);if(relay.isBlank())throw new IllegalArgumentException("Set the relay address in Preferences first.");invite="BS1-"+Wire.pack("relay","1",""); }
        if (!invite.startsWith("BS1-")) throw new IllegalArgumentException("Paste a Rummage invite starting BS1-.");
        String[] f = Wire.unpack(invite.substring(4).strip());
        if (f.length != 3 || f[0].isBlank()) throw new IllegalArgumentException("Invalid invite");
        host = f[0]; port = Integer.parseInt(f[1]); secret = f[2]; this.name = name; this.messages = messages;
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid port");
        this.token=savedToken;this.saveToken=saveToken;
        Thread.ofVirtual().name("rummage-client").start(this::run);
    }
    private void run() {
        long retryUntil = Snapshot.now()+Match.RECONNECT_MS;
        while (!closed && Snapshot.now()<retryUntil) {
            Thread sender=null;
            try (Socket s = relayEndpoint.isEmpty()?new Socket():Relay.connect(relayEndpoint)) {
                socket=s; if(relayEndpoint.isEmpty())s.connect(new InetSocketAddress(host,port),5000); s.setSoTimeout(10000); s.setTcpNoDelay(true);
                DataInputStream in = new DataInputStream(s.getInputStream());
                out = new DataOutputStream(s.getOutputStream());
                String lobbySecret=secret;
                if(!relayEndpoint.isEmpty()){Wire.write(out,"JOIN",relayCode);String[] response=Wire.read(in);if(!response[0].equals("JOINED"))throw new IOException("Relay lobby unavailable");lobbySecret=response[1];}
                Wire.write(out,"HELLO",Match.VERSION,lobbySecret,name,token);
                String[] welcome = Wire.read(in);
                if (!welcome[0].equals("WELCOME")) { status = welcome.length>1 ? welcome[1] : "Connection refused"; messages.accept(status); closed = true; break; }
                id=welcome[1]; token=welcome[2]; connected=true; status="Connected"; outgoing.clear();
                saveToken.accept(token);
                DataOutputStream output = out;
                sender=Thread.ofVirtual().start(() -> {
                    try { while (!closed && !s.isClosed()) { String[] f = outgoing.poll(2,TimeUnit.SECONDS); Wire.write(output, f==null ? new String[]{"PING"} : f); } }
                    catch (IOException | InterruptedException e) { try { s.close(); } catch (IOException ignored) {} }
                });
                while (!closed) {
                    String[] f=Wire.read(in);
                    retryUntil=Snapshot.now()+Match.RECONNECT_MS;
                    if (f[0].equals("STATE")) state=Snapshot.decode(f);
                    else if(f[0].equals("PROBE"))send("PROBE_REPLY",f[1]);
                    else if (f[0].equals("CHAT")) messages.accept("<"+f[1]+"> "+f[2]);
                    else if (f[0].equals("ERROR")) messages.accept(f[1]);
                }
            } catch (IOException | RuntimeException e) { if (!closed) status="Disconnected — reconnecting…"; }
            finally { connected=false; out=null;if(sender!=null)sender.interrupt(); }
            if (!closed) try { Thread.sleep(2000); } catch (InterruptedException e) { break; }
        }
        if (!closed) status="Connection expired. Join again.";
    }
    public void send(String... f) { if (connected && !outgoing.offer(f)) messages.accept("Connection busy; try again."); }
    @Override public void close() {
        closed=true; connected=false;
        DataOutputStream output=out;
        Socket s=socket;
        Thread.ofVirtual().start(() -> { try { if (output!=null) Wire.write(output,"LEAVE"); } catch(IOException ignored) {} finally { try { if(s!=null) s.close(); } catch(IOException ignored) {} } });
    }
}
