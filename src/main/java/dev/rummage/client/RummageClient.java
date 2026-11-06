package dev.rummage.client;

import dev.rummage.net.*;
import dev.rummage.core.*;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import com.mojang.blaze3d.platform.InputConstants;

public final class RummageClient implements ClientModInitializer {
    public static RummageClient INSTANCE;
    public volatile LobbyConnection connection;
    public Synchronizer host;
    public String invite = "", error = "", name = "Player";
    public final Deque<String> messages = new ConcurrentLinkedDeque<>();
    private volatile String worldMatch = "", worldFolder = "";
    private volatile Vec3 spawn;
    private volatile boolean announcedReady;
    private volatile String reportedWorld="";
    private double lookoutY;
    private long lastClaim;
    private KeyMapping lobbyKey;
    private KeyMapping brightKey,hudKey,playersKey;
    public final LocalState preferences=new LocalState(FabricLoader.getInstance().getConfigDir().resolve("rummage.properties"));
    public final WorldStore worlds=new WorldStore(FabricLoader.getInstance().getGameDir().resolve("saves"));
    public volatile String updateStatus="",relayStatus="";
    public volatile Updates.Release update;
    private Relay.Host relayHost;
    private volatile boolean worldMarked;
    private long lastDimension;
    private boolean recoveringWorld;
    public MatchRules pendingRules;
    public MatchRules savedRules(){try{return MatchRules.decode(preferences.get("rules",MatchRules.DEFAULT.encode()));}catch(IllegalArgumentException e){return MatchRules.DEFAULT;}}
    public void savePreferences(){try{preferences.save();}catch(RuntimeException e){error="Could not save preferences: "+e.getMessage();}}
    @Override public void onInitializeClient() {
        INSTANCE = this;
        name=preferences.get("name","Player");worldMatch=preferences.get("worldMatch","");worldFolder=preferences.get("worldFolder","");
        var category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rummage","extras"));
        brightKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.rummage.fullbright",InputConstants.KEY_G,category));
        hudKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.rummage.hud",InputConstants.KEY_H,category));
        playersKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.rummage.players",InputConstants.KEY_P,category));
        if(!preferences.get("updateUrl","").isBlank())checkUpdates();
        lobbyKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.rummage.lobby",InputConstants.KEY_B,KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rummage","main"))));
        ScreenEvents.AFTER_INIT.register((client,screen,w,h) -> {
            if (screen instanceof TitleScreen) {
                Screens.getWidgets(screen).add(Button.builder(Component.literal("Rummage"),b -> client.gui.setScreen(new LobbyScreen(this))).bounds(w-114,8,106,20).build());
                if(client.getSingleplayerServer()==null&&preferences.number("keepWorlds",0)>0)Thread.ofVirtual().start(()->{try{worlds.prune(preferences.number("keepWorlds",0),worldFolder);}catch(Exception e){message("World cleanup skipped: "+e.getMessage());}});
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            LobbyConnection c=connection; Snapshot s=c==null?null:c.state;
            if (s==null || !s.matchId().equals(worldMatch) || !server.getWorldData().getLevelName().equals(worldFolder)) return;
            var players=server.getPlayerList().getPlayers();
            if (players.isEmpty()) return;
            var p=players.getFirst();
            if (spawn==null) {spawn=p.position();lookoutY=spawn.y;for(int x=-16;x<=16;x+=16)for(int z=-16;z<=16;z+=16)lookoutY=Math.max(lookoutY,p.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,(int)spawn.x+x,(int)spawn.z+z));lookoutY+=20;}
            if(!worldMarked){try{worlds.mark(worldFolder);worldMarked=true;}catch(Exception e){message("Could not mark match world: "+e.getMessage());}}
            if(s.rules().fairPlay()){
                server.getWorldData().setAllowCommands(false);server.setDifficulty(Difficulty.NORMAL,true);server.setDifficultyLocked(true);
                if(server instanceof net.minecraft.client.server.IntegratedServer integrated){integrated.setWorldAllowCommands(false);if(integrated.isPublished())integrated.unpublishServer();}
            }
            if(Snapshot.now()-lastDimension>2000){c.send("DIMENSION",p.level().dimension().identifier().toString());lastDimension=Snapshot.now();}
            if (s.phase().equals("LOADING") || s.phase().equals("COUNTDOWN")) {
                p.setGameMode(GameType.SPECTATOR);
                p.teleportTo(spawn.x,s.phase().equals("COUNTDOWN")&&s.secondsLeft()>5?lookoutY:spawn.y,spawn.z);
                p.setDeltaMovement(Vec3.ZERO);
                if (c.connected && s.phase().equals("LOADING")) {
                    if(c.id.equals(s.leader())&&!reportedWorld.equals(s.matchId())){
                        try{var report=WorldChecks.inspect(server.overworld(),BlockPos.containing(spawn),s.rules());c.send("WORLD",s.matchId(),String.join(",",report.excluded()),""+report.accepted());}
                        catch(RuntimeException e){c.send("WORLD",s.matchId(),"","false");message("World check failed: "+e.getMessage());}
                        reportedWorld=s.matchId();
                    }
                    if(!announcedReady){c.send("READY",s.matchId(),""+server.overworld().getSeed()); announcedReady=true;}
                }
            } else {
                Snapshot.Player self=s.player(c.id);
                p.setGameMode(self!=null && self.alive() ? GameType.SURVIVAL : GameType.SPECTATOR);
                if(s.rules().race()&&self!=null&&self.alive()&&s.phase().equals("ROUND")&&!preferences.flag("kit."+s.matchId(),false)){
                    for(var stack:List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LAVA_BUCKET),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FLINT_AND_STEEL),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD,16)))if(!p.getInventory().add(stack))p.spawnAtLocation(p.level(),stack);
                    preferences.put("kit."+s.matchId(),true);savePreferences();
                }
                if (s.phase().equals("ROUND") && self!=null && self.alive() && !self.complete() && p.onGround() && Snapshot.now()-lastClaim>250) {
                    String block=BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(BlockPos.containing(p.getX(),p.getY()-0.05,p.getZ())).getBlock()).toString();
                    if (block.equals(s.block())) { c.send("CLAIM",s.matchId(),""+s.round(),block); lastClaim=Snapshot.now(); }
                }
            }
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("rummage","status"),(g,delta) -> {
            Minecraft mc=Minecraft.getInstance(); LobbyConnection c=connection; Snapshot s=c==null?null:c.state;
            if (s==null || s.phase().equals("LOBBY") || !inMatchWorld() || !preferences.flag("hud",true)) return;
            float scale=Math.max(0.5f,Math.min(2f,preferences.number("hudScale",100)/100f));
            scale=Math.min(scale,Math.min((mc.getWindow().getGuiScaledWidth()-16)/234f,(mc.getWindow().getGuiScaledHeight()-16)/70f));
            g.pose().pushMatrix();g.pose().scale(scale,scale);
            int x=8,y=8;
            g.fill(x,y,x+234,y+70,0xDE102024);
            g.fill(x,y,x+3,y+70,0xFF67E8A5);
            g.text(mc.font,"RUMMAGE  /  ROUND "+s.round(),x+10,y+8,0xFF67E8A5);
            if(!s.block().isEmpty())g.item(new net.minecraft.world.item.ItemStack(BuiltInRegistries.BLOCK.getValue(Identifier.parse(s.block()))),x+10,y+21);
            g.text(mc.font,mc.font.plainSubstrByWidth(displayBlock(s.block()),190),x+30,y+24,0xFFFFFFFF);
            Snapshot.Player self=s.player(c.id);
            String status=!c.connected ? c.status : s.phase().equals("FINISHED") ? s.result() : self!=null && !self.alive() ? "Eliminated" : self!=null && self.complete() ? "Block found!" : s.phase()+"  "+s.secondsLeft()+"s";
            g.text(mc.font,mc.font.plainSubstrByWidth(status,210),x+10,y+40,self!=null&&self.complete()?0xFF67E8A5:s.secondsLeft()<=10?0xFFFF7755:0xFFCFD9D6);
            g.text(mc.font,"B: lobby / standings",x+10,y+55,0xFF8FA49D);
            g.pose().popMatrix();
        });
    }
    private void tick(Minecraft mc) {
        while(brightKey.consumeClick()){preferences.put("fullbright",!preferences.flag("fullbright",false));savePreferences();}
        while(hudKey.consumeClick()){preferences.put("hud",!preferences.flag("hud",true));savePreferences();}
        while(playersKey.consumeClick())mc.gui.setScreen(mc.gui.screen() instanceof PlayersScreen?null:new PlayersScreen(this,mc.gui.screen()));
        if(relayHost!=null){relayStatus=relayHost.status;if(!relayHost.code.isEmpty())invite="RM-"+relayHost.code;}
        while (lobbyKey.consumeClick()) mc.gui.setScreen(mc.gui.screen() instanceof LobbyScreen ? null : new LobbyScreen(this));
        LobbyConnection c=connection; Snapshot s=c==null?null:c.state;
        if (s==null) return;
        if(recoveringWorld&&mc.level==null&&mc.gui.screen() instanceof LobbyScreen&&!s.phase().equals("LOBBY")){recoveringWorld=false;createWorld();}
        if(s.phase().equals("LOADING")&&!s.matchId().equals(worldMatch)&&mc.getSingleplayerServer()!=null&&mc.getSingleplayerServer().getWorldData().getLevelName().equals(worldFolder)){mc.disconnect(new LobbyScreen(this),false);return;}
        if (s.phase().equals("LOBBY") && mc.getSingleplayerServer()!=null && mc.getSingleplayerServer().getWorldData().getLevelName().equals(worldFolder)) { mc.disconnect(new LobbyScreen(this),false); return; }
        if (s.phase().equals("LOADING") && !s.matchId().equals(worldMatch) && mc.level==null && mc.gui.screen() instanceof LobbyScreen) createWorld();
        if (s.phase().equals("LOADING") && !c.connected) {announcedReady=false;reportedWorld="";}
    }
    public void createWorld() {
        Minecraft mc=Minecraft.getInstance(); Snapshot s=connection==null?null:connection.state;
        if (s==null || mc.level!=null) { error="Save and quit your current world before creating the match world."; return; }
        if (s.matchId().equals(worldMatch)) { mc.createWorldOpenFlows().openWorld(worldFolder, () -> mc.gui.setScreen(new LobbyScreen(this))); return; }
        worldMatch=s.matchId(); worldFolder="Rummage "+s.matchId(); spawn=null; announcedReady=false;
        worldMarked=false;preferences.put("worldMatch",worldMatch);preferences.put("worldFolder",worldFolder);savePreferences();
        try {
            LevelSettings settings=new LevelSettings(worldFolder,GameType.SURVIVAL,new LevelSettings.DifficultySettings(Difficulty.NORMAL,false,false),false,WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(worldFolder,settings,new WorldOptions(s.seed(),true,false),WorldPresets::createNormalWorldDimensions,new LobbyScreen(this));
        } catch (RuntimeException e) { worldMatch=""; error="World creation failed: "+e.getMessage(); mc.gui.setScreen(new LobbyScreen(this)); }
    }
    public boolean inMatchWorld() { Minecraft mc=Minecraft.getInstance(); return connection!=null && connection.state!=null && !connection.state.phase().equals("LOBBY") && mc.getSingleplayerServer()!=null && mc.getSingleplayerServer().getWorldData().getLevelName().equals(worldFolder); }
    public Snapshot activeState(net.minecraft.server.MinecraftServer server){var c=connection;var s=c==null?null:c.state;return s!=null&&!s.phase().equals("LOBBY")&&s.matchId().equals(worldMatch)&&server.getWorldData().getLevelName().equals(worldFolder)?s:null;}
    public boolean applies(net.minecraft.server.MinecraftServer server){return activeState(server)!=null;}
    public boolean fullbright(){return inMatchWorld()&&connection.state.rules().fullbright()&&preferences.flag("fullbright",false);}
    public void host(String playerName, String advertisedAddress) {
        try { leave(); host=new Synchronizer(25570); invite=host.invite(advertisedAddress.isBlank()?InetAddress.getLocalHost().getHostAddress():advertisedAddress.strip()); name=playerName;rememberName();connect(host.invite("127.0.0.1"),false);String relay=preferences.get("relay","");if(!relay.isBlank())relayHost=new Relay.Host(relay,host.port(),host.secret);error="";
            LobbyConnection joined=connection;Thread.ofVirtual().start(()->{for(int i=0;i<100&&connection==joined;i++){if(joined.state!=null){joined.send("CONFIG",preferences.flag("randomSeed",true)?""+joined.state.seed():preferences.get("seed",""+joined.state.seed()),preferences.get("seconds","300"),preferences.get("rounds","20"));joined.send("RULES",savedRules().encode(),""+preferences.flag("randomSeed",true));return;}try{Thread.sleep(100);}catch(InterruptedException e){return;}}});
        }
        catch (Exception e) { error="Could not host: "+e.getMessage(); }
    }
    public void join(String code,String playerName) { try { leave(); name=playerName;rememberName(); invite=code.strip();connect(invite,false);error=""; } catch (RuntimeException e) { error="Could not join: "+e.getMessage(); } }
    private void rememberName(){preferences.put("name",name);savePreferences();}
    private void connect(String code,boolean recovery){pendingRules=null;String token=recovery?preferences.get("token",""):"";preferences.put("recoveryInvite",code);savePreferences();connection=new LobbyConnection(code,name,this::message,token,t->{preferences.put("token",t);savePreferences();},preferences.get("relay",""));}
    public void recover(){try{if(connection!=null&&connection.connected){error="Already connected.";return;}if(connection!=null)connection.close();invite=preferences.get("recoveryInvite","");connect(invite,true);recoveringWorld=true;error="";}catch(RuntimeException e){error="Recovery failed: "+e.getMessage();}}
    public void leave() { if(connection!=null) connection.close(); connection=null; if(relayHost!=null)relayHost.close();relayHost=null;if(host!=null)try {host.close();}catch(Exception ignored){} host=null; invite=""; messages.clear();preferences.put("token","");preferences.put("recoveryInvite","");savePreferences(); }
    public void checkUpdates(){Thread.ofVirtual().start(()->{String url=preferences.get("updateUrl",""),key=preferences.get("updateKey","");if(url.isBlank()||key.isBlank()){updateStatus="Set an update manifest URL and trusted key in Preferences.";return;}try{updateStatus="Checking updates…";update=Updates.check(url,key);updateStatus=update==null?"You are up to date.":"Verified update available: "+update.version();}catch(Exception e){updateStatus="Update check failed: "+e.getMessage();}});}
    public void downloadUpdate(){var release=update;if(release==null)return;Thread.ofVirtual().start(()->{try{Path jar=Updates.download(release,FabricLoader.getInstance().getConfigDir().resolve("rummage-update"));updateStatus="Verified download: "+jar.getFileName()+". Open update folder to install.";}catch(Exception e){updateStatus="Download failed: "+e.getMessage();}});}
    public void message(String text) { messages.addLast(text); while(messages.size()>50)messages.pollFirst(); }
    public static String displayBlock(String id) { if(id.isEmpty())return "Waiting for the next block…"; String text=id.replace("minecraft:","").replace('_',' '); return Character.toUpperCase(text.charAt(0))+text.substring(1); }
}
