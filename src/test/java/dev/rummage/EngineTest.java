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
        m.configure(m.leader,42,30,rounds);m.start(m.leader,0);m.worldReport(m.leader,m.matchId,Set.of(),true,0);
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
        features(); relay(); relayTls(); recovery(); storageAndUpdates();
        checks+=dev.rummage.net.ConnectionTest.run();
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
            leader.send("WORLD",leader.state.matchId(),"","true");a.send("READY",a.state.matchId(),"123");b.send("READY",b.state.matchId(),"123");
            await(()->a.state.phase().equals("COUNTDOWN"));
            a.send("FORFEIT");await(()->b.state.phase().equals("FINISHED"));check(b.state.result().contains("Bob"),"Network forfeit declares survivor");
            leader.send("RESET");await(()->a.state.phase().equals("LOBBY"));
        }
        try(Synchronizer server=new Synchronizer(0);Socket s=new Socket("127.0.0.1",server.port())){
            s.setSoTimeout(2000);Wire.write(new DataOutputStream(s.getOutputStream()),"HELLO","bad-version",server.secret,"name","");check(Wire.read(new DataInputStream(s.getInputStream()))[0].equals("ERROR"),"Version mismatch rejected");
        }
    }
    static void features(){
        MatchRules rules=new MatchRules(true,true,15,true,true,true,true,"plains","",400);
        check(MatchRules.decode(rules.encode()).equals(rules),"Rules round trip");
        rejected(()->MatchRules.decode("true"));rejected(()->new MatchRules(true,false,-1,true,false,true,true,"","",400));
        Match sudden=running(2,1);sudden.rules=rules;
        for(var p:sudden.players.values())sudden.claim(p.id,sudden.matchId,1,sudden.target,5002);
        sudden.tick(5003);check(sudden.phase==Match.Phase.INTERMISSION,"Sudden death continues past cap");sudden.tick(sudden.deadline);
        check(sudden.round==2&&sudden.deadline==25003,"Sudden death halves round timer");
        sudden.claim(sudden.leader,sudden.matchId,2,sudden.target,10004);sudden.tick(sudden.deadline);check(sudden.phase==Match.Phase.FINISHED,"Sudden death produces survivor");
        Match draw=running(3,3);var ids=new ArrayList<>(draw.players.keySet());draw.voteDraw(ids.get(0),5002);draw.voteDraw(ids.get(1),5003);check(draw.phase==Match.Phase.ROUND,"Partial draw does not end game");draw.voteDraw(ids.get(2),5004);check(draw.phase==Match.Phase.FINISHED&&draw.result.contains("agreed"),"Unanimous draw ends game");
        Match expired=running(2,3);expired.voteDraw(expired.leader,5002);expired.tick(35002);check(expired.drawVotes.isEmpty(),"Draw expires after 30 seconds");
        Match cancel=running(2,3);cancel.voteDraw(cancel.leader,5002);cancel.voteDraw(cancel.leader,5003);check(cancel.drawVotes.isEmpty(),"Draw vote can be cancelled");
        cancel.voteDraw(cancel.leader,5004);cancel.claim(cancel.leader,cancel.matchId,1,cancel.target,5005);check(cancel.drawVotes.isEmpty(),"Completion cancels draw vote");
        Match grace=running(2,3);grace.graceMillis=300;long deadline=grace.deadline;grace.tick(deadline);check(grace.phase==Match.Phase.ROUND,"Grace keeps deadline claims open");grace.claim(grace.leader,grace.matchId,1,grace.target,deadline-1);grace.tick(deadline+300);check(grace.result.contains("Player 0"),"Compensated claim accepted before grace closes");
        Match look=new Match();look.join("a");look.rules=rules;look.start(look.leader,0);look.worldReport(look.leader,look.matchId,Set.of(),true,0);look.ready(look.leader,look.matchId,0);look.tick(1);check(look.deadline==20001,"Lookout extends countdown");
        String previous="";for(int i=1;i<80;i++){String target=BlockPool.draw(12,i,previous,Set.of("minecraft:oak_planks","minecraft:dried_kelp_block"));check(!target.equals("minecraft:oak_planks")&&!target.equals("minecraft:dried_kelp_block"),"Excluded targets never drawn");previous=target;}
        Match filter=new Match();filter.join("a");filter.rules(filter.leader,rules,true);filter.start(filter.leader,0);long seed=filter.seed;String match=filter.matchId;filter.worldReport(filter.leader,match,Set.of(),false,1);check(filter.seed!=seed&&!filter.matchId.equals(match)&&filter.phase==Match.Phase.LOADING,"Rejected random seed retries with new match");
        for(int i=0;i<7;i++)filter.worldReport(filter.leader,filter.matchId,Set.of(),false,2+i);check(filter.phase==Match.Phase.FINISHED,"Seed search is bounded");
        Match fixed=new Match();fixed.join("a");fixed.rules(fixed.leader,rules,false);fixed.start(fixed.leader,0);fixed.worldReport(fixed.leader,fixed.matchId,Set.of(),false,1);check(fixed.phase==Match.Phase.FINISHED,"Typed seed is not silently replaced");
        Snapshot snapshot=Snapshot.decode(Snapshot.encode(look,1));check(snapshot.rules().equals(rules),"Rules included in snapshots");
    }
    static void relay()throws Exception{
        try(Relay relay=new Relay(0,true);Synchronizer server=new Synchronizer(0);Relay.Host host=new Relay.Host("tcp://127.0.0.1:"+relay.port(),server.port(),server.secret)){
            await(()->!host.code.isEmpty());String endpoint="tcp://127.0.0.1:"+relay.port();
            try(LobbyConnection a=new LobbyConnection("RM-"+host.code,"Relay Alice",s->{},"",t->{},endpoint);LobbyConnection b=new LobbyConnection("RM-"+host.code,"Relay Bob",s->{},"",t->{},endpoint)){
                await(()->a.state!=null&&b.state!=null&&a.state.players().size()==2);check(!a.id.equals(b.id),"Relay has independent client tunnels");
                LobbyConnection leader=a.state.leader().equals(a.id)?a:b;
                leader.send("RULES",new MatchRules(true,true,10,true,true,true,true,"","",400).encode(),"true");await(()->a.state.rules().suddenDeath()&&b.state.rules().race());
                a.send("DIMENSION","minecraft:the_nether");await(()->b.state.player(a.id).dimension().equals("minecraft:the_nether"));
            }
            try(Socket socket=Relay.connect(endpoint)){socket.setSoTimeout(2000);Wire.write(new DataOutputStream(socket.getOutputStream()),"HOST",host.code,"wrong-secret","wrong-owner");try{Wire.read(new DataInputStream(socket.getInputStream()));throw new AssertionError("Relay owner takeover accepted");}catch(IOException expected){checks++;}}
        }
        try{Relay.connect("tcp://example.com:25571");throw new AssertionError("Plain remote relay accepted");}catch(IOException expected){checks++;}
    }
    static void recovery()throws Exception{
        try(Synchronizer server=new Synchronizer(0)){
            String id,token;
            try(Socket socket=new Socket("127.0.0.1",server.port())){socket.setSoTimeout(2000);Wire.write(new DataOutputStream(socket.getOutputStream()),"HELLO",Match.VERSION,server.secret,"Recover","");String[] welcome=Wire.read(new DataInputStream(socket.getInputStream()));id=welcome[1];token=welcome[2];}
            try(LobbyConnection recovered=new LobbyConnection(server.invite("127.0.0.1"),"Recover",s->{},token,t->{},"")){await(()->recovered.state!=null);check(recovered.id.equals(id)&&recovered.state.players().size()==1,"Crash token restores same slot");}
        }
    }
    static void relayTls()throws Exception{
        var dir=java.nio.file.Files.createTempDirectory(java.nio.file.Path.of("build"),"tls-tests-");var keyFile=dir.resolve("relay.p12");
        String keytool=java.nio.file.Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")?"keytool.exe":"keytool").toString();
        Process process=new ProcessBuilder(keytool,"-genkeypair","-alias","relay","-keyalg","RSA","-keysize","2048","-validity","2","-dname","CN=localhost","-ext","SAN=dns:localhost","-storetype","PKCS12","-keystore",keyFile.toString(),"-storepass","test-only-password","-keypass","test-only-password","-noprompt").redirectErrorStream(true).redirectOutput(dir.resolve("keytool.log").toFile()).start();
        if(!process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS)){process.destroyForcibly();throw new AssertionError("TLS test key generation timed out");}check(process.exitValue()==0,"TLS test certificate created");
        var keyStore=java.security.KeyStore.getInstance("PKCS12");try(var in=java.nio.file.Files.newInputStream(keyFile)){keyStore.load(in,"test-only-password".toCharArray());}
        var km=javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());km.init(keyStore,"test-only-password".toCharArray());var tm=javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());tm.init(keyStore);
        var original=javax.net.ssl.SSLContext.getDefault();var context=javax.net.ssl.SSLContext.getInstance("TLS");context.init(km.getKeyManagers(),tm.getTrustManagers(),null);javax.net.ssl.SSLContext.setDefault(context);
        try(Relay relay=new Relay(0,false);Synchronizer server=new Synchronizer(0);Relay.Host host=new Relay.Host("tls://localhost:"+relay.port(),server.port(),server.secret)){
            await(()->!host.code.isEmpty());try(LobbyConnection client=new LobbyConnection("RM-"+host.code,"TLS player",s->{},"",t->{},"tls://localhost:"+relay.port())){await(()->client.state!=null);check(client.connected,"Verified TLS relay carries game traffic");}
            try(Socket bad=Relay.connect("tls://127.0.0.1:"+relay.port())){throw new AssertionError("Mismatched TLS hostname accepted");}catch(javax.net.ssl.SSLHandshakeException expected){checks++;}
        }finally{javax.net.ssl.SSLContext.setDefault(original);}
    }
    static void storageAndUpdates()throws Exception{
        java.nio.file.Path root=java.nio.file.Files.createTempDirectory(java.nio.file.Path.of("build"),"feature-tests-");
        LocalState prefs=new LocalState(root.resolve("settings.properties"));prefs.put("name","Alex");prefs.put("token","private");prefs.save();check(new LocalState(root.resolve("settings.properties")).get("token","").equals("private"),"Recovery preferences persist");
        WorldStore store=new WorldStore(root.resolve("saves"));java.nio.file.Files.createDirectories(root.resolve("saves/Ordinary world"));
        String protectedWorld="Rummage "+UUID.randomUUID(),old="Rummage "+UUID.randomUUID(),recent="Rummage "+UUID.randomUUID();int age=0;
        for(String folder:List.of(protectedWorld,old,recent)){var p=root.resolve("saves").resolve(folder);java.nio.file.Files.createDirectories(p);java.nio.file.Files.writeString(p.resolve("level.dat"),"test");java.nio.file.Files.setLastModifiedTime(p.resolve("level.dat"),java.nio.file.attribute.FileTime.fromMillis(1000L*(++age)));store.mark(folder);}
        store.favorite(protectedWorld,true);check(store.prune(0,"")==0,"Retention disabled keeps all");
        try(var lockFile=java.nio.channels.FileChannel.open(root.resolve("saves").resolve(old).resolve("session.lock"),java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.WRITE);var lock=lockFile.lock()){check(store.prune(1,"")==0,"Open worlds protected from cleanup");}
        check(store.prune(1,"")==1,"Old unprotected world removed");check(store.list().size()==2,"Newest and favorite retained");check(java.nio.file.Files.isDirectory(root.resolve("saves/Ordinary world")),"Ordinary saves untouched");
        var key=java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();Properties manifest=new Properties();manifest.setProperty("version","0.1.2");manifest.setProperty("url","https://example.com/rummage.jar");manifest.setProperty("sha256","a".repeat(64));var signer=java.security.Signature.getInstance("Ed25519");signer.initSign(key.getPrivate());signer.update(Updates.payload("0.1.2",manifest.getProperty("url"),manifest.getProperty("sha256")));manifest.setProperty("signature",Base64.getEncoder().encodeToString(signer.sign()));String publicKey=Base64.getEncoder().encodeToString(key.getPublic().getEncoded());
        check(Updates.verify(manifest,publicKey).version().equals("0.1.2"),"Signed update accepted");manifest.setProperty("version","9.0.0");try{Updates.verify(manifest,publicKey);throw new AssertionError("Tampered release accepted");}catch(java.security.SignatureException expected){checks++;}
        check(Updates.newer("0.1.10","0.1.2")&&!Updates.newer("0.1.1","0.1.1")&&!Updates.newer("0.1.0","0.1.1"),"Update ordering blocks downgrades");
        var mods=root.resolve("mods");var staged=root.resolve("update");java.nio.file.Files.createDirectories(mods);java.nio.file.Files.createDirectories(staged);
        var oldJar=mods.resolve("rummage-0.1.1.jar");var download=staged.resolve("rummage-0.1.2.jar");java.nio.file.Files.writeString(oldJar,"old");java.nio.file.Files.writeString(download,"new");
        String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(download)));
        try{UpdateInstaller.main(new String[]{"9223372036854775807",oldJar.toString(),download.toString(),"wrong"});throw new AssertionError("Changed update installed");}catch(SecurityException expected){checks++;}
        check(java.nio.file.Files.readString(oldJar).equals("old"),"Failed verification preserves installed jar");
        UpdateInstaller.main(new String[]{"9223372036854775807",oldJar.toString(),download.toString(),hash});
        check(java.nio.file.Files.readString(mods.resolve("rummage-0.1.2.jar")).equals("new")&&java.nio.file.Files.readString(staged.resolve("previous.jar.backup")).equals("old"),"Exit installer preserves rollback and installs new bytes");
    }
}
