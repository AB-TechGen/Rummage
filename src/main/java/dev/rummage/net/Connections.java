package dev.rummage.net;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Opens OpenDraft-style short codes, dual-address invites, and ordinary host:port addresses. */
final class Connections {
    record Opened(Lines channel,String secret) {}

    static Opened open(String invite,String legacyRelay)throws IOException {
        String code=CloudRelay.normalize(invite);
        if(code!=null)return discover(CloudRelay.join(code));
        if(invite.startsWith("RM-")){
            Socket socket=Relay.connect(legacyRelay);
            try {
                DataOutputStream out=new DataOutputStream(socket.getOutputStream());
                Wire.write(out,"JOIN",invite.substring(3));
                String[] response=Wire.read(new DataInputStream(socket.getInputStream()));
                if(!response[0].equals("JOINED"))throw new IOException("Relay lobby unavailable");
                return new Opened(new FrameLines(socket),response[1]);
            }catch(Exception e){socket.close();throw e;}
        }
        if(invite.startsWith("BS1-")){
            String[] fields=Wire.unpack(invite.substring(4).strip());
            if(fields.length!=3||fields[0].isBlank())throw new IOException("Invalid invite");
            String address=fields[0].contains(":")&&!fields[0].startsWith("[")?"["+fields[0]+"]":fields[0];
            return new Opened(new FrameLines(race(List.of(parseAddress(address+":"+fields[1])),8000)),fields[2]);
        }
        List<InetSocketAddress> targets=Invite.decode(invite);
        return discover(new FrameLines(race(targets==null?List.of(parseAddress(invite)):targets,8000)));
    }

    private static Opened discover(Lines channel)throws IOException {
        try {
            channel.send(Wire.pack("INVITE"));
            String line=channel.readLine();
            if(line==null||line.equals("CLOSED"))throw new IOException("The lobby closed");
            String[] fields=Wire.unpack(line);
            if(fields.length!=3||!fields[0].equals("INVITE"))throw new IOException("Not a Rummage lobby");
            return new Opened(channel,fields[1]);
        }catch(IOException|RuntimeException e){channel.close();throw e;}
    }

    static InetSocketAddress parseAddress(String raw) {
        String text=raw.strip(),host=text;int port=25570;
        if(text.isEmpty())throw new IllegalArgumentException("Paste an invite or enter host:port.");
        if(text.startsWith("[")){
            int end=text.indexOf(']');if(end<0)throw new IllegalArgumentException("Invalid IPv6 address");
            host=text.substring(1,end);
            if(end+1<text.length()){if(text.charAt(end+1)!=':')throw new IllegalArgumentException("Invalid address");port=Integer.parseInt(text.substring(end+2));}
        }else if(text.indexOf(':')>=0&&text.indexOf(':')==text.lastIndexOf(':')){
            int colon=text.indexOf(':');host=text.substring(0,colon);port=Integer.parseInt(text.substring(colon+1));
        }
        if(host.isBlank()||port<1||port>65535||host.contains("/")||host.contains(" "))throw new IllegalArgumentException("Invalid host or port");
        return InetSocketAddress.createUnresolved(host,port);
    }

    /** Race LAN/public addresses like OpenDraft; always close losing sockets. */
    static Socket race(List<InetSocketAddress> targets,int timeout)throws IOException {
        if(targets.isEmpty())throw new IOException("No addresses in invite");
        CompletableFuture<Socket> first=new CompletableFuture<>();AtomicInteger left=new AtomicInteger(targets.size());
        Set<Socket> pending=ConcurrentHashMap.newKeySet();
        for(var target:targets)Thread.ofVirtual().start(()->{
            Socket socket=new Socket();pending.add(socket);
            try {
                socket.connect(new InetSocketAddress(target.getHostString(),target.getPort()),timeout);
                if(!first.complete(socket))socket.close();
            }catch(IOException|RuntimeException e){try{socket.close();}catch(IOException ignored){}if(left.decrementAndGet()==0)first.completeExceptionally(e);}
            finally{pending.remove(socket);}
        });
        Socket winner=null;
        try{winner=first.get(timeout+1000L,TimeUnit.MILLISECONDS);return winner;}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Connection interrupted",e);}
        catch(ExecutionException|TimeoutException e){throw new IOException("Could not reach the lobby",e);}
        finally{
            Socket selected=winner;
            first.whenComplete((socket,error)->{if(socket!=null&&socket!=selected)try{socket.close();}catch(IOException ignored){}});
            first.completeExceptionally(new IOException("Connection attempt ended"));
            for(Socket socket:pending)if(socket!=winner)try{socket.close();}catch(IOException ignored){}
        }
    }
    private Connections(){}
}
