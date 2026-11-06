package dev.rummage.client;

import dev.rummage.net.*;
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
    private long lastClaim;
    private KeyMapping lobbyKey;
    @Override public void onInitializeClient() {
        INSTANCE = this;
        lobbyKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.rummage.lobby",InputConstants.KEY_B,KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rummage","main"))));
        ScreenEvents.AFTER_INIT.register((client,screen,w,h) -> {
            if (screen instanceof TitleScreen) Screens.getWidgets(screen).add(Button.builder(Component.literal("Rummage"),b -> client.gui.setScreen(new LobbyScreen(this))).bounds(w-114,8,106,20).build());
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            LobbyConnection c=connection; Snapshot s=c==null?null:c.state;
            if (s==null || !s.matchId().equals(worldMatch) || !server.getWorldData().getLevelName().equals(worldFolder)) return;
            var players=server.getPlayerList().getPlayers();
            if (players.isEmpty()) return;
            var p=players.getFirst();
            if (spawn==null) spawn=p.position();
            if (s.phase().equals("LOADING") || s.phase().equals("COUNTDOWN")) {
                p.setGameMode(GameType.SPECTATOR);
                p.teleportTo(spawn.x,spawn.y,spawn.z);
                p.setDeltaMovement(Vec3.ZERO);
                if (!announcedReady && c.connected && s.phase().equals("LOADING")) { c.send("READY",s.matchId(),""+server.overworld().getSeed()); announcedReady=true; }
            } else {
                Snapshot.Player self=s.player(c.id);
                p.setGameMode(self!=null && self.alive() ? GameType.SURVIVAL : GameType.SPECTATOR);
                if (s.phase().equals("ROUND") && self!=null && self.alive() && !self.complete() && p.onGround() && Snapshot.now()-lastClaim>250) {
                    String block=BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(BlockPos.containing(p.getX(),p.getY()-0.05,p.getZ())).getBlock()).toString();
                    if (block.equals(s.block())) { c.send("CLAIM",s.matchId(),""+s.round(),block); lastClaim=Snapshot.now(); }
                }
            }
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("rummage","status"),(g,delta) -> {
            Minecraft mc=Minecraft.getInstance(); LobbyConnection c=connection; Snapshot s=c==null?null:c.state;
            if (s==null || s.phase().equals("LOBBY")) return;
            int x=8,y=8;
            g.fill(x,y,x+234,y+70,0xDE102024);
            g.fill(x,y,x+3,y+70,0xFF67E8A5);
            g.text(mc.font,"RUMMAGE  /  ROUND "+s.round(),x+10,y+8,0xFF67E8A5);
            g.text(mc.font,displayBlock(s.block()),x+10,y+24,0xFFFFFFFF);
            Snapshot.Player self=s.player(c.id);
            String status=!c.connected ? c.status : s.phase().equals("FINISHED") ? s.result() : self!=null && !self.alive() ? "Eliminated" : self!=null && self.complete() ? "Block found!" : s.phase()+"  "+s.secondsLeft()+"s";
            g.text(mc.font,mc.font.plainSubstrByWidth(status,210),x+10,y+40,0xFFCFD9D6);
            g.text(mc.font,"B: lobby / standings",x+10,y+55,0xFF8FA49D);
        });
    }
    private void tick(Minecraft mc) {
        while (lobbyKey.consumeClick()) mc.gui.setScreen(mc.gui.screen() instanceof LobbyScreen ? null : new LobbyScreen(this));
        LobbyConnection c=connection; Snapshot s=c==null?null:c.state;
        if (s==null) return;
        if (s.phase().equals("LOBBY") && mc.getSingleplayerServer()!=null && mc.getSingleplayerServer().getWorldData().getLevelName().equals(worldFolder)) { mc.disconnect(new LobbyScreen(this),false); return; }
        if (s.phase().equals("LOADING") && !s.matchId().equals(worldMatch) && mc.level==null && mc.gui.screen() instanceof LobbyScreen) createWorld();
        if (s.phase().equals("LOADING") && !c.connected) announcedReady=false;
    }
    public void createWorld() {
        Minecraft mc=Minecraft.getInstance(); Snapshot s=connection==null?null:connection.state;
        if (s==null || mc.level!=null) { error="Save and quit your current world before creating the match world."; return; }
        if (s.matchId().equals(worldMatch)) { mc.createWorldOpenFlows().openWorld(worldFolder, () -> mc.gui.setScreen(new LobbyScreen(this))); return; }
        worldMatch=s.matchId(); worldFolder="Rummage "+s.matchId(); spawn=null; announcedReady=false;
        try {
            LevelSettings settings=new LevelSettings(worldFolder,GameType.SURVIVAL,new LevelSettings.DifficultySettings(Difficulty.NORMAL,false,false),false,WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(worldFolder,settings,new WorldOptions(s.seed(),true,false),WorldPresets::createNormalWorldDimensions,new LobbyScreen(this));
        } catch (RuntimeException e) { worldMatch=""; error="World creation failed: "+e.getMessage(); mc.gui.setScreen(new LobbyScreen(this)); }
    }
    public boolean inMatchWorld() { Minecraft mc=Minecraft.getInstance(); return connection!=null && connection.state!=null && !connection.state.phase().equals("LOBBY") && mc.getSingleplayerServer()!=null && mc.getSingleplayerServer().getWorldData().getLevelName().equals(worldFolder); }
    public void host(String playerName, String advertisedAddress) {
        try { leave(); host=new Synchronizer(25570); invite=host.invite(advertisedAddress.isBlank()?InetAddress.getLocalHost().getHostAddress():advertisedAddress.strip()); name=playerName; connection=new LobbyConnection(host.invite("127.0.0.1"),name,this::message); error=""; }
        catch (Exception e) { error="Could not host: "+e.getMessage(); }
    }
    public void join(String code,String playerName) { try { leave(); name=playerName; invite=code.strip(); connection=new LobbyConnection(invite,name,this::message); error=""; } catch (RuntimeException e) { error="Could not join: "+e.getMessage(); } }
    public void leave() { if(connection!=null) connection.close(); connection=null; if(host!=null)try {host.close();}catch(Exception ignored){} host=null; invite=""; messages.clear(); }
    public void message(String text) { messages.addLast(text); while(messages.size()>50)messages.pollFirst(); }
    public static String displayBlock(String id) { if(id.isEmpty())return "Waiting for the next block…"; String text=id.replace("minecraft:","").replace('_',' '); return Character.toUpperCase(text.charAt(0))+text.substring(1); }
}
