package dev.rummage.net;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/** Exercises the ported OpenDraft connection contract using real local WebSockets and a mock UPnP router. */
public final class ConnectionTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void await(BooleanSupplier condition)throws Exception{long end=System.nanoTime()+10_000_000_000L;while(!condition.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(20);check(condition.getAsBoolean(),"Connection condition timed out");}
    public static int run()throws Exception{
        invites();router();fallbacks();cloud();return checks;
    }
    private static void invites()throws Exception{
        String code=Invite.encode("192.168.1.8","8.8.8.8",25570);
        var addresses=Invite.decode(code);check(addresses.size()==2&&addresses.get(0).getHostString().equals("8.8.8.8"),"Public and LAN addresses survive invite encoding");
        check(Invite.decode(Invite.encode("192.168.1.8",null,25570)).size()==1,"LAN-only invite");
        check(Invite.decode(code.substring(0,code.length()-1)+"!")==null,"Corrupt invite rejected");
        check(!Invite.isPublic("100.64.1.1")&&!Invite.isPublic("10.0.0.1")&&!Invite.isPublic("192.168.1.1")&&Invite.isPublic("8.8.8.8"),"CGNAT and private addresses classified");
        check(CloudRelay.normalize(" abcl-2345 ").equals("ABC12345"),"Short invite normalizes ambiguous letters");
        check(CloudRelay.normalize("127.0.0.1:25570")==null,"Address is not mistaken for short invite");
        check(Connections.parseAddress("[::1]:25572").getPort()==25572,"IPv6 address parsing");
        try(Synchronizer server=new Synchronizer(0);LobbyConnection direct=new LobbyConnection("127.0.0.1:"+server.port(),"Direct",s->{})){
            await(()->direct.state!=null);check(direct.connected,"Bare address discovers Rummage handshake");
            String local=Invite.encode("127.0.0.1","127.0.0.2",server.port());
            try(LobbyConnection joined=new LobbyConnection(local,"Invite",s->{})){await(()->joined.state!=null&&joined.state.players().size()==2);check(joined.connected,"Raced public/LAN invite connects");}
            try(Synchronizer alternate=Synchronizer.openAnyPort(server.port())){check(alternate.port()!=server.port(),"Occupied port falls back to another free port");}
        }
        try(Synchronizer server=new Synchronizer(0);Socket socket=new Socket("127.0.0.1",server.port())){
            socket.setSoTimeout(2500);DataInputStream in=new DataInputStream(socket.getInputStream());
            Wire.write(new DataOutputStream(socket.getOutputStream()),"HELLO",dev.rummage.core.Match.VERSION,server.secret,"Rate","");
            long until=System.nanoTime()+1_100_000_000L;int states=0;
            while(System.nanoTime()<until){if(Wire.read(in)[0].equals("STATE"))states++;}
            check(states<=4,"Unchanged snapshots do not flood the shared Worker");
        }
    }
    private static void router()throws Exception{
        List<String> actions=new CopyOnWriteArrayList<>();AtomicBoolean failZero=new AtomicBoolean(true);AtomicBoolean deny=new AtomicBoolean();
        HttpServer gateway=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);
        gateway.createContext("/device",exchange->{
            String xml="<root><serviceList><service><serviceType>urn:schemas-upnp-org:service:WANIPConnection:2</serviceType><controlURL>/control</controlURL></service></serviceList></root>";
            byte[] bytes=xml.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });
        gateway.createContext("/control",exchange->{
            String action=exchange.getRequestHeaders().getFirst("SOAPAction"),body=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);actions.add(action+body);
            int status=200;String response="<ok/>";
            if(action.contains("GetExternalIPAddress"))response="<NewExternalIPAddress>8.8.8.8</NewExternalIPAddress>";
            if(action.contains("AddPortMapping")&&(deny.get()||body.contains("<NewLeaseDuration>0</NewLeaseDuration>")&&failZero.get())){status=500;response="<errorCode>"+(deny.get()?"718":"725")+"</errorCode>";}
            byte[] bytes=response.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });gateway.start();
        try{
            String location="http://127.0.0.1:"+gateway.getAddress().getPort()+"/device";
            PortOpener mapping=PortOpener.openAt(25570,location);check(mapping.opened()&&mapping.externalIp.equals("8.8.8.8"),"UPnP mapping obtains router address");
            check(actions.stream().anyMatch(a->a.contains("<NewLeaseDuration>86400</NewLeaseDuration>")),"UPnP retries with fixed lease");
            check(actions.stream().anyMatch(a->a.contains("<NewPortMappingDescription>Rummage</NewPortMappingDescription>")),"Router mapping branded Rummage");
            mapping.close();check(actions.stream().anyMatch(a->a.contains("DeletePortMapping")),"Router mapping removed on close");
            deny.set(true);PortOpener denied=PortOpener.openAt(25570,location);check(!denied.opened()&&denied.problem.contains("another device"),"UPnP conflict reported correctly");
            check(PortOpener.header("HTTP/1.1 200 OK\r\nlocation: http://router/device\r\n","LOCATION").equals("http://router/device"),"SSDP headers case insensitive");
        }finally{gateway.stop(0);}
    }
    private static void fallbacks()throws Exception{
        class FakeNetwork implements AutoHosting.Network {
            boolean cloud;String external="8.8.8.8";boolean mapped=true;AtomicInteger routerCalls=new AtomicInteger(),closed=new AtomicInteger(),publicCalls=new AtomicInteger();
            public AutoHosting.HostedRelay relay(int port,Consumer<String> log,CloudRelay.Watcher watcher)throws IOException{if(!cloud)throw new IOException("offline");return new AutoHosting.HostedRelay(){public String code(){return "ABC1-2345";}public void close(){closed.incrementAndGet();}};}
            public AutoHosting.Mapping router(int port){routerCalls.incrementAndGet();return new AutoHosting.Mapping(){public boolean opened(){return mapped;}public String local(){return "192.168.1.3";}public String external(){return external;}public String problem(){return "denied";}public void close(){closed.incrementAndGet();}};}
            public String lan(){return "192.168.1.3";}public String publicAddress(){publicCalls.incrementAndGet();return "8.8.4.4";}
        }
        FakeNetwork relay=new FakeNetwork();relay.cloud=true;try(AutoHosting auto=new AutoHosting(25570,s->{},relay)){await(()->!auto.reach().code().isBlank());check(auto.reach().anywhere()&&relay.routerCalls.get()==0,"Relay success skips UPnP");}await(()->relay.closed.get()==1);
        FakeNetwork upnp=new FakeNetwork();try(AutoHosting auto=new AutoHosting(25570,s->{},upnp)){await(()->!auto.reach().code().isBlank());check(auto.reach().anywhere()&&Invite.decode(auto.reach().code()).size()==2,"Relay failure uses UPnP dual-address invite");}await(()->upnp.closed.get()==1);
        FakeNetwork cgnat=new FakeNetwork();cgnat.external="100.64.1.1";try(AutoHosting auto=new AutoHosting(25570,s->{},cgnat)){await(()->!auto.reach().code().isBlank());check(!auto.reach().anywhere()&&Invite.decode(auto.reach().code()).size()==1&&cgnat.publicCalls.get()==0,"CGNAT mapping remains LAN-only");}
        FakeNetwork lan=new FakeNetwork();lan.mapped=false;lan.external=null;try(AutoHosting auto=new AutoHosting(25570,s->{},lan)){await(()->!auto.reach().code().isBlank());check(!auto.reach().anywhere()&&lan.publicCalls.get()==1&&Invite.decode(auto.reach().code()).size()==2,"Failed UPnP looks up public IP but labels LAN-only");}
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicBoolean cleaned=new AtomicBoolean();
        AutoHosting.Network delayed=new AutoHosting.Network(){
            public AutoHosting.HostedRelay relay(int port,Consumer<String> log,CloudRelay.Watcher watcher)throws IOException{entered.countDown();try{release.await();}catch(InterruptedException e){throw new IOException(e);}return new AutoHosting.HostedRelay(){public String code(){return "ABC1-2345";}public void close(){cleaned.set(true);}};}
            public AutoHosting.Mapping router(int port){throw new AssertionError();}public String lan(){throw new AssertionError();}public String publicAddress(){throw new AssertionError();}
        };
        AutoHosting closing=new AutoHosting(25570,s->{},delayed);check(entered.await(2,TimeUnit.SECONDS),"Host connection begins asynchronously");closing.close();release.countDown();await(cleaned::get);check(closing.reach().code().isBlank(),"Closing during setup cannot publish a stale invite");
    }
    private static void cloud()throws Exception{
        String original=CloudRelay.url;
        try(WebSocketRelay fixture=new WebSocketRelay();Synchronizer server=new Synchronizer(0)){
            // First endpoint refuses /host. Hosting must continue; joining must race both endpoints.
            CloudRelay.url="http://127.0.0.1:"+fixture.port()+"/missing,http://127.0.0.1:"+fixture.port();
            AtomicInteger reconnecting=new AtomicInteger(),back=new AtomicInteger();
            var host=CloudRelay.host(server.port(),s->{},new CloudRelay.Watcher(){public void reconnecting(){reconnecting.incrementAndGet();}public void back(){back.incrementAndGet();}public void lost(){}});
            try{
                check(host.code().equals("ABC1-2345"),"Cloudflare-compatible host receives short code");
                try(LobbyConnection alice=new LobbyConnection(host.code(),"Cloud Alice",s->{});LobbyConnection bob=new LobbyConnection(host.code(),"Cloud Bob",s->{})){
                    await(()->alice.state!=null&&bob.state!=null&&alice.state.players().size()==2);String id=alice.id;
                    alice.send("CHAT","WebSocket integration");await(()->fixture.forwarded.get()>5);check(alice.connected&&bob.connected,"Rummage frames cross real WebSockets in separate tunnels");
                    try{CloudRelay.WebSocketLines.connect("ws://127.0.0.1:"+fixture.port()+"/host/ABC12345",Map.of("X-OpenDraft-Key","wrong"));throw new AssertionError("Host takeover accepted");}catch(CloudRelay.Refused expected){checks++;}
                    fixture.dropHost();await(()->back.get()>0&&alice.connected&&alice.id.equals(id));check(reconnecting.get()>0,"Host reclaims code using secret key");
                    await(()->alice.state.players().size()==2);check(alice.id.equals(id),"Players recover the same identity through relay reconnect");
                    host.close();await(()->!alice.connected&&alice.status.contains("closed"));check(!bob.connected||bob.status.contains("closed"),"Permanent relay closure stops reconnecting");
                }
            }finally{host.close();}
        }finally{CloudRelay.url=original;}
    }

    /** Minimal wire-level fixture for the exact /host, /join, ROOM/OPEN/DATA/CLOSE/BYE Worker contract. */
    private static final class WebSocketRelay implements AutoCloseable {
        final ServerSocket server=new ServerSocket(0,50,InetAddress.getLoopbackAddress());
        final Set<Peer> all=ConcurrentHashMap.newKeySet();final Map<String,Peer> players=new ConcurrentHashMap<>();
        final AtomicInteger forwarded=new AtomicInteger();volatile Peer host;volatile boolean bye;
        WebSocketRelay()throws IOException{Thread.ofVirtual().start(()->{while(!server.isClosed())try{Socket s=server.accept();Thread.ofVirtual().start(()->serve(s));}catch(IOException ignored){}});}
        int port(){return server.getLocalPort();}
        void dropHost(){Peer old=host;if(old!=null)old.close();}
        void serve(Socket socket){Peer peer=null;boolean hosting=false;String id="";
            try(socket){socket.setSoTimeout(20000);InputStream in=socket.getInputStream();StringBuilder headers=new StringBuilder();while(!headers.toString().endsWith("\r\n\r\n")){int b=in.read();if(b<0||headers.length()>8192)throw new IOException();headers.append((char)b);}
                String raw=headers.toString(),path=raw.split(" ")[1];OutputStream out=socket.getOutputStream();int status=0;
                if(path.startsWith("/missing"))status=404;
                else if(path.equals("/host/ABC12345")&&!raw.toLowerCase(Locale.ROOT).contains("x-opendraft-key: owner-secret"))status=403;
                else if(path.startsWith("/join/")&&(host==null||host.socket.isClosed()))status=404;
                else if(!path.startsWith("/host")&&!path.startsWith("/join/"))status=404;
                if(status!=0){out.write(("HTTP/1.1 "+status+" Refused\r\nContent-Length: 0\r\n\r\n").getBytes(StandardCharsets.US_ASCII));return;}
                String key=PortOpener.header(raw,"Sec-WebSocket-Key");String accept=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII)));
                out.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: "+accept+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.flush();peer=new Peer(socket,in,out);all.add(peer);
                hosting=path.startsWith("/host");
                if(hosting){host=peer;bye=false;peer.text("ROOM ABC12345 owner-secret");}else{id=UUID.randomUUID().toString().substring(0,8);players.put(id,peer);host.text("OPEN "+id);}
                for(String text;(text=peer.read())!=null;){
                    if(hosting){if(text.equals("BYE")){bye=true;continue;}String[] p=text.split(" ",3);if(p.length>=2){Peer target=players.get(p[1]);if(target!=null){if(p[0].equals("DATA")&&p.length==3){forwarded.incrementAndGet();target.text(p[2]);}else if(p[0].equals("CLOSE"))target.close();}}}
                    else{Peer active=host;if(active!=null){forwarded.incrementAndGet();active.text("DATA "+id+" "+text);}}
                }
            }catch(Exception ignored){}finally{
                if(peer!=null){all.remove(peer);if(hosting&&host==peer){host=null;for(Peer player:players.values()){player.closeCode(bye?4000:4003);player.close();}players.clear();}else if(!hosting){players.remove(id,peer);Peer active=host;if(active!=null)active.text("CLOSE "+id);}}
            }
        }
        public void close()throws IOException{server.close();for(Peer peer:all)peer.close();}
        private static final class Peer {
            final Socket socket;final DataInputStream in;final DataOutputStream out;
            Peer(Socket s,InputStream in,OutputStream out){socket=s;this.in=new DataInputStream(in);this.out=new DataOutputStream(out);}
            synchronized void frame(int opcode,byte[] data){try{out.writeByte(0x80|opcode);if(data.length<126)out.writeByte(data.length);else{out.writeByte(126);out.writeShort(data.length);}out.write(data);out.flush();}catch(IOException ignored){}}
            void text(String text){frame(1,text.getBytes(StandardCharsets.UTF_8));}
            void closeCode(int code){frame(8,new byte[]{(byte)(code>>8),(byte)code});}
            String read()throws IOException{while(true){int opcode=in.read();if(opcode<0)return null;int len=in.readUnsignedByte();boolean masked=(len&128)!=0;len&=127;if(len==126)len=in.readUnsignedShort();else if(len==127){long n=in.readLong();if(n>65536)throw new IOException();len=(int)n;}byte[] mask=masked?in.readNBytes(4):null;byte[] data=in.readNBytes(len);if(data.length!=len)throw new EOFException();if(masked)for(int i=0;i<len;i++)data[i]^=mask[i%4];if((opcode&15)==8)return null;if((opcode&15)==9){frame(10,data);continue;}if((opcode&15)==1)return new String(data,StandardCharsets.UTF_8);}}
            void close(){try{socket.close();}catch(IOException ignored){}}
        }
    }
}
