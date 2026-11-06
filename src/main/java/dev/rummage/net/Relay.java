package dev.rummage.net;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;

/** TLS rendezvous relay. Plain TCP is permitted only on loopback for local tests. */
public final class Relay implements AutoCloseable {
    private final ServerSocket listener;
    private final Map<String,Room> rooms=new ConcurrentHashMap<>();
    private final Map<String,Long> retired=new ConcurrentHashMap<>();
    private final Map<String,Socket> pending=new ConcurrentHashMap<>();
    private final Set<Socket> sockets=ConcurrentHashMap.newKeySet();
    private final Semaphore capacity=new Semaphore(128);
    private record Room(Socket socket,DataOutputStream out,String secret,String owner){}
    public Relay(int port,boolean local)throws IOException {
        listener=local?new ServerSocket(port,50,InetAddress.getLoopbackAddress()):SSLServerSocketFactory.getDefault().createServerSocket(port);
        Thread.ofVirtual().start(()->{while(!listener.isClosed())try{Socket s=listener.accept();if(!capacity.tryAcquire()){s.close();continue;}sockets.add(s);Thread.ofVirtual().start(()->serve(s));}catch(IOException e){if(!listener.isClosed())System.err.println("Relay accept failed");}});
    }
    public int port(){return listener.getLocalPort();}
    public static Socket connect(String endpoint)throws IOException {
        URI uri=URI.create(endpoint.contains("://")?endpoint:"tls://"+endpoint);
        String host=uri.getHost();int port=uri.getPort();if(host==null||port<1)throw new IOException("Relay address must be tls://host:port");
        boolean local=uri.getScheme().equals("tcp") && (host.equals("127.0.0.1")||host.equals("localhost")||host.equals("[::1]"));
        if(!local&&!uri.getScheme().equals("tls"))throw new IOException("Remote relays require TLS");
        Socket s=local?new Socket():SSLSocketFactory.getDefault().createSocket();
        try{s.connect(new InetSocketAddress(host,port),5000);s.setSoTimeout(15000);
            if(s instanceof SSLSocket ssl){SSLParameters params=ssl.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");ssl.setSSLParameters(params);ssl.startHandshake();}return s;
        }catch(IOException e){s.close();throw e;}
    }
    private void serve(Socket s){
        String roomCode="";
        try(s){s.setSoTimeout(15000);DataInputStream in=new DataInputStream(s.getInputStream());DataOutputStream out=new DataOutputStream(s.getOutputStream());String[] f=Wire.read(in);
            retired.forEach((code,time)->{if(System.currentTimeMillis()-time>300_000){rooms.computeIfPresent(code,(k,r)->r.socket.isClosed()?null:r);retired.remove(code,time);}});
            switch(f[0]){
                case "HOST" -> {
                    if(f.length!=4)throw new IOException("Invalid host");
                    if(f[1].isEmpty()&&rooms.size()>=128)throw new IOException("Relay full");
                    if(!f[1].isEmpty()&&!rooms.containsKey(f[1]))throw new IOException("Lobby recovery expired");
                    roomCode=f[1].isEmpty()?UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase(Locale.ROOT):f[1];
                    Room room=new Room(s,out,f[2],f[3]);Room old=rooms.putIfAbsent(roomCode,room);
                    if(old!=null){if(!old.owner.equals(f[3]))throw new IOException("Owner mismatch");rooms.replace(roomCode,old,room);old.socket.close();}
                    retired.remove(roomCode);
                    Wire.write(out,"HOSTED",roomCode);while(!s.isClosed()){Wire.read(in);Wire.write(out,"ALIVE");}
                }
                case "JOIN" -> {
                    Room room=rooms.get(f[1]);if(room==null)throw new IOException("Unknown lobby");
                    String nonce=UUID.randomUUID().toString();pending.put(nonce,s);
                    try{Wire.write(out,"JOINED",room.secret);Wire.write(room.out,"CONNECT",nonce);
                        synchronized(s){if(pending.containsKey(nonce))s.wait(10000);}
                        if(pending.containsKey(nonce))throw new IOException("Host did not connect");
                        synchronized(s){while(!s.isClosed())s.wait(1000);}
                    }finally{pending.remove(nonce);}
                }
                case "TUNNEL" -> {
                    Room room=rooms.get(f[1]);if(room==null||!room.owner.equals(f[2]))throw new IOException("Owner mismatch");
                    Socket client=pending.remove(f[3]);if(client==null)throw new IOException("Expired rendezvous");
                    synchronized(client){client.notifyAll();}pipe(s,client);
                }
                default -> throw new IOException("Unknown relay operation");
            }
        }catch(IOException|InterruptedException|RuntimeException ignored){}
        finally{if(!roomCode.isEmpty()){Room room=rooms.get(roomCode);if(room!=null&&room.socket==s)retired.put(roomCode,System.currentTimeMillis());}sockets.remove(s);capacity.release();}
    }
    public static void pipe(Socket a,Socket b)throws IOException {
        a.setSoTimeout(15000);b.setSoTimeout(15000);
        Thread.ofVirtual().start(()->{try{a.getInputStream().transferTo(b.getOutputStream());}catch(IOException ignored){}finally{closeSocket(a);closeSocket(b);}});
        try{b.getInputStream().transferTo(a.getOutputStream());}finally{closeSocket(a);closeSocket(b);}
    }
    private static void closeSocket(Socket s){try{s.close();}catch(IOException ignored){}}
    public void close()throws IOException{listener.close();for(Socket s:sockets)closeSocket(s);}
    public static final class Host implements AutoCloseable {
        private volatile boolean closed;private volatile Socket control;
        public volatile String code="",status="Connecting to relay";
        private final String owner=UUID.randomUUID().toString();
        public Host(String endpoint,int localPort,String secret){Thread.ofVirtual().start(()->{
            while(!closed){try(Socket s=connect(endpoint)){control=s;DataOutputStream out=new DataOutputStream(s.getOutputStream());DataInputStream in=new DataInputStream(s.getInputStream());Wire.write(out,"HOST",code,secret,owner);String[] reply=Wire.read(in);if(!reply[0].equals("HOSTED"))throw new IOException();code=reply[1];status="Relay connected";
                Thread.ofVirtual().start(()->{try{while(!closed&&!s.isClosed()){Wire.write(out,"KEEP");Thread.sleep(3000);}}catch(Exception ignored){closeSocket(s);}});
                while(!closed){String[] f=Wire.read(in);if(f[0].equals("CONNECT"))Thread.ofVirtual().start(()->{try(Socket tunnel=connect(endpoint);Socket local=new Socket(InetAddress.getLoopbackAddress(),localPort)){Wire.write(new DataOutputStream(tunnel.getOutputStream()),"TUNNEL",code,owner,f[1]);pipe(tunnel,local);}catch(IOException ignored){}});}
            }catch(Exception e){status="Relay unavailable; retrying";}if(!closed)try{Thread.sleep(2000);}catch(InterruptedException e){return;}}
        });}
        public void close(){closed=true;if(control!=null)closeSocket(control);}
    }
    public static void main(String[] args)throws Exception{try(Relay r=new Relay(args.length==0?25571:Integer.parseInt(args[0]),false)){System.out.println("Rummage TLS relay listening on "+r.port());new CountDownLatch(1).await();}}
}
