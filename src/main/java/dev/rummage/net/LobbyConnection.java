package dev.rummage.net;

import dev.rummage.core.Match;
import java.io.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class LobbyConnection implements AutoCloseable {
    public volatile Snapshot state;
    public volatile String id = "", status = "Connecting…";
    public volatile boolean connected;
    private volatile boolean closed;
    private volatile Lines channel;
    private String token = "";
    private final String invite, name;
    private final Consumer<String> messages;
    private final Consumer<ChatEvent> events;
    private Consumer<String> saveToken = t -> {};
    private final String legacyRelay;
    private final BlockingQueue<String[]> outgoing = new ArrayBlockingQueue<>(64);
    public LobbyConnection(String invite, String name, Consumer<String> messages) {
        this(invite,name,messages,"",t -> {},"");
    }
    public LobbyConnection(String invite, String name, Consumer<String> messages, String savedToken, Consumer<String> saveToken, String relay) {
        this(invite,name,messages,savedToken,saveToken,relay,event->messages.accept(event.plainText()));
    }
    public LobbyConnection(String invite,String name,Consumer<String> messages,String savedToken,Consumer<String> saveToken,String relay,Consumer<ChatEvent> events){
        this.events=events;
        this.invite=invite.strip();this.name=name;this.messages=messages;this.legacyRelay=relay;
        if(this.invite.startsWith("RM-")&&relay.isBlank())throw new IllegalArgumentException("This older RM invite needs its original relay address. Ask the host for a new automatic invite.");
        if(CloudRelay.normalize(this.invite)==null&&!this.invite.startsWith("RM-")&&!this.invite.startsWith("BS1-")&&Invite.decode(this.invite)==null)Connections.parseAddress(this.invite);
        this.token=savedToken;this.saveToken=saveToken;
        Thread.ofVirtual().name("rummage-client").start(this::run);
    }
    private void run() {
        boolean interrupted=false;
        long retryUntil = Snapshot.now()+Match.RECONNECT_MS;
        while (!closed && Snapshot.now()<retryUntil) {
            Thread sender=null;
            Lines opened=null;
            try {
                var transport=Connections.open(invite,legacyRelay);
                Lines active=transport.channel();opened=active;channel=active;
                if(closed){active.close();break;}
                active.send(Wire.pack("HELLO",Match.VERSION,transport.secret(),name,token));
                String[] welcome=read(active);

                if (!welcome[0].equals("WELCOME")) { status = welcome.length>1 ? welcome[1] : "Connection refused"; messages.accept(status); closed = true; break; }
                id=welcome[1]; token=welcome[2]; connected=true; status="Connected"; outgoing.clear();
                saveToken.accept(token);
                if(interrupted){events.accept(ChatEvent.notice("Lobby connection restored."));interrupted=false;}

                sender=Thread.ofVirtual().start(() -> {
                    try { while (!closed && !active.isClosed()) { String[] f = outgoing.poll(2,TimeUnit.SECONDS); active.send(Wire.pack(f==null ? new String[]{"PING"} : f)); } }
                    catch (InterruptedException e) { active.close(); }
                });
                while (!closed) {
                    String[] f=read(active);
                    retryUntil=Snapshot.now()+Match.RECONNECT_MS;
                    if (f[0].equals("STATE")) state=Snapshot.decode(f);
                    else if(f[0].equals("PROBE"))send("PROBE_REPLY",f[1]);
                    else if (f[0].equals("CHAT")) events.accept(ChatEvent.chat(f[1],f[2],f[3]));
                    else if(f[0].equals("ADV")) events.accept(ChatEvent.advancement(f[1],f[2],f[3],f[4]));
                    else if(f[0].equals("NOTICE")) events.accept(ChatEvent.notice(f[1]));
                    else if (f[0].equals("ERROR")) messages.accept(f[1]);
                }
            } catch (LobbyClosed e) { closed=true;status="The lobby closed. Create or join another lobby.";messages.accept(status); }
            catch (IOException | RuntimeException e) { if (!closed) {status="Lobby connection interrupted. Reconnecting…";if(!interrupted){interrupted=true;events.accept(ChatEvent.notice(status));}} }
            finally { connected=false;if(opened!=null)opened.close();channel=null;if(sender!=null)sender.interrupt(); }
            if (!closed) try { Thread.sleep(2000); } catch (InterruptedException e) { break; }
        }
        if (!closed){status="The reconnection period has expired. Please rejoin the lobby.";events.accept(ChatEvent.notice(status));}
    }
    public void send(String... f) { if (connected && !outgoing.offer(f)) messages.accept("Connection busy; try again."); }
    private static final class LobbyClosed extends IOException {}
    private static String[] read(Lines lines)throws IOException {
        String line=lines.readLine();if(line==null)throw new EOFException();if(line.equals("CLOSED"))throw new LobbyClosed();return Wire.unpack(line);
    }
    @Override public void close() {
        closed=true;connected=false;
        Lines active=channel;
        if(active!=null)Thread.ofVirtual().start(()->{active.send(Wire.pack("LEAVE"));active.close();});
    }
}
