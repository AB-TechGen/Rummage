package dev.rummage.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Optional persistent standings view; Tab provides the quick in-world view. */
final class PlayersScreen extends Screen {
    private final RummageClient mod;
    private final Screen parent;
    PlayersScreen(RummageClient mod,Screen parent){super(Component.literal("Players"));this.mod=mod;this.parent=parent;}
    @Override protected void init(){addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(width/2-100,height-28,200,20).build());}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        var c=mod.connection;var s=c==null?null:c.state;int w=Math.min(420,width-24),left=(width-w)/2;
        Ui.panel(g,left,10,w,26+(s==null?0:s.players().size()*22));g.centeredText(font,"Player Standings",width/2,18,Ui.GOLD);
        int row=37;if(s!=null)for(var p:s.players()){
            Ui.text(g,font,(p.id().equals(s.leader())?"★ ":"")+p.name()+(p.id().equals(c.id)?" (you)":""),left+8,row,w/2-12,Ui.TEXT);
            Ui.text(g,font,p.score()+" pts · "+Ui.status(s,p)+" · "+p.ping()+"ms",left+w/2,row,w/2-8,p.complete()?Ui.GREEN:Ui.TEXT);
            Ui.text(g,font,p.dimension().replace("minecraft:",""),left+8,row+10,w-16,Ui.MUTED);row+=22;
        }
        super.extractRenderState(g,x,y,delta);
    }
}
