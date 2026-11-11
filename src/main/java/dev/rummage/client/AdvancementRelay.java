package dev.rummage.client;

import java.util.*;
import dev.rummage.net.LobbyConnection;
import dev.rummage.net.Snapshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** OpenDraft-style advancement sharing: baseline each world/session, then send new announced completions. */
final class AdvancementRelay {
    private MinecraftServer lastServer;
    private LobbyConnection lastConnection;
    private String match="";
    private long nextPoll;
    private final Set<String> seen=new HashSet<>();
    void tick(MinecraftServer server,ServerPlayer player,LobbyConnection c,Snapshot state){
        if(!c.connected){lastConnection=null;return;}
        boolean baseline=server!=lastServer||c!=lastConnection||!match.equals(state.matchId());
        long now=Snapshot.now();if(!baseline&&now<nextPoll)return;nextPoll=now+1000;
        if(baseline){seen.clear();lastServer=server;lastConnection=c;match=state.matchId();}
        for(var holder:server.getAdvancements().getAllAdvancements()){
            var display=holder.value().display();
            if(display.isEmpty()||!display.get().announceToChat()||!player.getAdvancements().getOrStartProgress(holder).isDone())continue;
            if(!seen.add(holder.id().toString())||baseline||state.phase().equals("LOBBY")||state.phase().equals("FINISHED"))continue;
            String title=display.get().title().getString().replaceAll("[\\p{Cntrl}§]", "");
            if(!title.isBlank())c.send("ADV",match,holder.id().toString(),display.get().type().getSerializedName(),title.substring(0,Math.min(160,title.length())));
        }
    }
}
