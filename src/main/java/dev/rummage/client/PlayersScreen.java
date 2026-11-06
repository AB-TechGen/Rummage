package dev.rummage.client;

import dev.rummage.net.Snapshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class PlayersScreen extends Screen {
    private final RummageClient mod;
    private final Screen parent;
    PlayersScreen(RummageClient mod,Screen parent){super(Component.literal("Players"));this.mod=mod;this.parent=parent;}
    @Override protected void init(){addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(width/2-100,height-28,200,20).build());}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        g.fill(0,0,width,height,0xF00D171B);g.centeredText(font,"PLAYERS / STANDINGS",width/2,20,0xFF67E8A5);
        Snapshot s=mod.connection==null?null:mod.connection.state;int row=48;
        if(s!=null)for(var p:s.players()){
            String status=!p.connected()?"away":!p.alive()?"eliminated":p.complete()?"found":p.ready()?"ready":"waiting";
            g.text(font,font.plainSubstrByWidth((p.id().equals(s.leader())?"* ":"  ")+p.name(),width/2-25),16,row,0xFFFFFFFF);
            g.text(font,font.plainSubstrByWidth(p.score()+" pts / "+status+" / "+p.ping()+"ms",width/2-16),width/2,row,0xFFBDCEC8);
            g.text(font,p.dimension().replace("minecraft:",""),20,row+11,0xFF8FA49D);row+=28;
        }
        super.extractRenderState(g,x,y,delta);
    }
}
