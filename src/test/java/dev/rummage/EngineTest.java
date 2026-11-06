package dev.rummage;

import dev.rummage.core.*;
import dev.rummage.net.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.BooleanSupplier;

public final class EngineTest {
    private static int checks;
    static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    static void rejected(Runnable r){try{r.run();throw new AssertionError("Expected rejection");}catch(IllegalArgumentException expected){checks++;}}
    static Match running(int count,int rounds){
        Match m=new Match();for(int i=0;i<count;i++)m.join("Player "+i);
        m.configure(m.leader,42,30,rounds);m.start(m.leader,0);
        for(var p:m.players.values())m.ready(p.id,m.matchId,42);
        m.tick(1);check(m.phase==Match.Phase.COUNTDOWN,"Ready barrier");m.tick(5001);check(m.phase==Match.Phase.ROUND,"Countdown starts round");return m;
    }
    public static void main(String[] args)throws Exception{
        Match m=running(2,10);var ids=new ArrayList<>(m.players.keySet());String a=ids.get(0),b=ids.get(1);
        m.claim(a,m.matchId,m.round,"minecraft:bedrock",5002);check(!m.players.get(a).complete,"Wrong block rejected");
        m.claim(a,"old-match",m.round,m.target,5002);check(!m.players.get(a).complete,"Stale match rejected");
        m.claim(a,m.matchId,0,m.target,5002);check(!m.players.get(a).complete,"Stale round rejected");
        m.claim(a,m.matchId,m.round,m.target,5002);m.claim(a,m.matchId,m.round,m.target,5003);check(m.players.get(a).score==1,"Idempotent claims");
        m.tick(m.deadline);check(m.phase==Match.Phase.FINISHED&&m.result.contains("Player 0"),"Deadline eliminates failure and declares winner");
        Match tie=running(2,1);for(var p:tie.players.values())tie.claim(p.id,tie.matchId,1,tie.target,5002);tie.tick(5003);check(tie.phase==Match.Phase.FINISHED&&tie.result.contains("Player 0")&&tie.result.contains("Player 1"),"Round cap shares victory");
        Match late=running(2,3);late.claim(late.leader,late.matchId,1,late.target,late.deadline);check(late.players.get(late.leader).score==0,"Deadline boundary rejected");late.tick(late.deadline);check(late.result.startsWith("Draw"),"Simultaneous elimination is draw");
        Match solo=running(1,2);solo.claim(solo.leader,solo.matchId,1,solo.target,5002);solo.tick(5003);check(solo.phase==Match.Phase.INTERMISSION,"Solo does not win immediately");solo.tick(10003);check(solo.round==2,"Next round");solo.claim(solo.leader,solo.matchId,2,solo.target,10004);solo.tick(10005);check(solo.phase==Match.Phase.FINISHED,"Solo completes max rounds");
        Match loading=new Match();var p=loading.join("Alex");var q=loading.join("Alex");check(!p.name.equals(q.name),"Duplicate names disambiguated");rejected(()->loading.configure(q.id,1,30,5));loading.start(p.id,0);rejected(()->loading.ready(p.id,loading.matchId,123));loading.ready(p.id,loading.matchId,0);loading.tick(100);check(loading.phase==Match.Phase.LOADING,"Slow player holds barrier");loading.tick(300000);check(loading.phase==Match.Phase.FINISHED,"Loading timeout");
        Match reconnect=running(2,10);String rid=reconnect.leader;reconnect.disconnect(rid,6000);reconnect.tick(6001);check(reconnect.players.get(rid).alive,"Away is not immediate elimination");reconnect.reconnect(rid,6002);check(reconnect.players.get(rid).connected,"Resume works");rejected(()->reconnect.reconnect(rid,6003));
        reconnect.disconnect(rid,6004);reconnect.tick(306004);check(!reconnect.players.get(rid).alive,"Expired slot eliminated");
        Match ff=running(2,3);String fid=ff.leader;ff.forfeit(fid);check(ff.phase==Match.Phase.FINISHED&&!ff.players.get(fid).alive,"Forfeit ends 1v1");
        String prev="";for(int i=1;i<100;i++){String block=BlockPool.draw(123,i,prev);check(!block.equals(prev),"No consecutive duplicate");check(block.equals(BlockPool.draw(123,i,prev)),"Deterministic target");prev=block;}
        String[] fields={"hello","","Unicode: café 草","tabs\tnewlines\n.dots"};check(Arrays.equals(fields,Wire.unpack(Wire.pack(fields))),"Wire round trip");
        ByteArrayOutputStream buf=new ByteArrayOutputStream();Wire.write(new DataOutputStream(buf),fields);check(Arrays.equals(fields,Wire.read(new DataInputStream(new ByteArrayInputStream(buf.toByteArray())))),"Framed round trip");
        try{Wire.read(new DataInputStream(new ByteArrayInputStream(new byte[]{0,2,0,0})));throw new AssertionError("Oversized frame accepted");}catch(IOException expected){checks++;}
        Snapshot snapshot=Snapshot.decode(Snapshot.encode(m,5004));check(snapshot.players().size()==2&&snapshot.round()==1,"Snapshot serialization");
        network();
        System.out.println("PASS: "+checks+" assertions, including real TCP lobby integration.");
    }
    static void await(BooleanSupplier condition)throws Exception{long end=System.nanoTime()+5_000_000_000L;while(!condition.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(20);check(condition.getAsBoolean(),"Network condition timed out");}
    static void network()throws Exception{
        try(Synchronizer server=new Synchronizer(0);
            LobbyConnection a=new LobbyConnection(server.invite("127.0.0.1"),"Alice",s->{});
            LobbyConnection b=new LobbyConnection(server.invite("127.0.0.1"),"Bob",s->{})){
            await(()->a.state!=null&&b.state!=null&&a.state.players().size()==2&&b.state.players().size()==2);
            LobbyConnection leader=a.state.leader().equals(a.id)?a:b;
            leader.send("CONFIG","123","30","2");leader.send("START");
            await(()->a.state.phase().equals("LOADING")&&b.state.phase().equals("LOADING"));
            check(a.state.seed()==123&&b.state.seed()==123,"Both clients share seed");
            a.send("READY",a.state.matchId(),"123");b.send("READY",b.state.matchId(),"123");
            await(()->a.state.phase().equals("COUNTDOWN"));
            a.send("FORFEIT");await(()->b.state.phase().equals("FINISHED"));check(b.state.result().contains("Bob"),"Network forfeit declares survivor");
            leader.send("RESET");await(()->a.state.phase().equals("LOBBY"));
        }
        try(Synchronizer server=new Synchronizer(0);Socket s=new Socket("127.0.0.1",server.port())){
            s.setSoTimeout(2000);Wire.write(new DataOutputStream(s.getOutputStream()),"HELLO","bad-version",server.secret,"name","");check(Wire.read(new DataInputStream(s.getInputStream()))[0].equals("ERROR"),"Version mismatch rejected");
        }
    }
}
