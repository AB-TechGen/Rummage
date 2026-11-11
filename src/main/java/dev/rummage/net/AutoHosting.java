package dev.rummage.net;

import java.io.*;
import java.util.function.Consumer;

/** OpenDraft's sharing order: built-in Cloudflare relay, UPnP, then a clearly labelled LAN invite. */
public final class AutoHosting implements AutoCloseable {
    public record Reach(String code,boolean anywhere,String note,boolean reconnecting,int port) {}
    interface HostedRelay extends AutoCloseable { String code(); void close(); }
    interface Mapping { boolean opened();String local();String external();String problem();void close(); }
    interface Network {
        HostedRelay relay(int port,Consumer<String> log,CloudRelay.Watcher watcher)throws IOException;
        Mapping router(int port);
        String lan();String publicAddress();
    }
    private static final Network SYSTEM=new Network(){
        public HostedRelay relay(int port,Consumer<String> log,CloudRelay.Watcher watcher)throws IOException {
            var bridge=CloudRelay.host(port,log,watcher);
            return new HostedRelay(){public String code(){return bridge.code();}public void close(){bridge.close();}};
        }
        public Mapping router(int port){var r=PortOpener.open(port);return new Mapping(){
            public boolean opened(){return r.opened();}public String local(){return r.localIp;}public String external(){return r.externalIp;}
            public String problem(){return r.problem;}public void close(){r.close();}
        };}
        public String lan(){return Invite.lanAddress();}
        public String publicAddress(){return PortOpener.publicAddress();}
    };
    private final int port;
    private final Consumer<String> log;
    private volatile Reach reach;
    private volatile boolean closed;
    private HostedRelay relay;
    private Mapping router;
    private Thread cleanup;
    public AutoHosting(int port,Consumer<String> log){this(port,log,SYSTEM);}
    AutoHosting(int port,Consumer<String> log,Network network){
        this.port=port;this.log=log;reach=new Reach("",false,"Connecting so friends can join…",false,port);
        Thread.ofVirtual().name("rummage-auto-host").start(()->share(network));
    }
    public Reach reach(){return reach;}
    private synchronized void update(boolean anywhere,String note,boolean reconnecting){
        if(!closed)reach=new Reach(reach.code,anywhere,note,reconnecting,port);
    }
    private void share(Network network){
        try {
            HostedRelay connected=network.relay(port,log,new CloudRelay.Watcher(){
                public void reconnecting(){update(false,"Relay connection interrupted. Reconnection is in progress.",true);}
                public void back(){update(true,"Internet lobby connection available.",false);}
                public void lost(){update(false,"This invite has expired. Leave the lobby and create it again.",false);}
            });
            synchronized(this){if(closed){connected.close();return;}relay=connected;reach=new Reach(connected.code(),true,"Internet lobby connection available.",false,port);}
            return;
        }catch(IOException|RuntimeException e){log.accept("Online connection unavailable; trying the router automatically.");}
        if(closed)return;
        update(false,"Asking your router to allow friends to connect…",false);
        Mapping mapping=network.router(port);
        synchronized(this){
            if(closed){mapping.close();return;}
            if(mapping.opened()){
                router=mapping;cleanup=new Thread(mapping::close,"Rummage router cleanup");Runtime.getRuntime().addShutdownHook(cleanup);
            }
        }
        String lan=mapping.local()!=null?mapping.local():network.lan();
        String internet=Invite.isPublic(mapping.external())?mapping.external():null;
        Reach result;
        if(mapping.opened()&&internet!=null){
            result=new Reach(Invite.encode(lan,internet,port),true,"Internet lobby connection available.",false,port);
        }else if(mapping.opened()){
            result=new Reach(Invite.encode(lan,null,port),false,"Only people on your network can join (port "+port+"). Your provider uses another private network (CGNAT).",false,port);
        }else{
            if(internet==null)internet=network.publicAddress();
            result=new Reach(Invite.encode(lan,internet,port),false,"Only people on your network can join (port "+port+"). The router did not open it automatically. A VPN or manual forwarding can help.",false,port);
            log.accept("Router fallback: "+mapping.problem());
        }
        if(result.code==null)result=new Reach("",false,"No usable IPv4 network address found. Connect to a network and create the lobby again.",false,port);
        synchronized(this){if(!closed)reach=result;}
    }
    public void close(){
        HostedRelay connection;Mapping mapping;Thread hook;
        synchronized(this){if(closed)return;closed=true;connection=relay;relay=null;mapping=router;router=null;hook=cleanup;cleanup=null;}
        if(connection!=null)connection.close();
        if(mapping!=null){try{Runtime.getRuntime().removeShutdownHook(hook);}catch(IllegalStateException ignored){}Thread.ofVirtual().start(mapping::close);}
    }
}
