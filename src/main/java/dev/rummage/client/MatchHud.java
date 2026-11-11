package dev.rummage.client;

import dev.rummage.net.Snapshot;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

final class MatchHud {
    private final RummageClient mod;
    private String phase="",match="";
    private long changedAt;
    MatchHud(RummageClient mod){this.mod=mod;}
    static String phaseText(Snapshot s){return switch(s.phase()){case "LOBBY"->"Waiting in lobby";case "LOADING"->"Loading worlds";case "COUNTDOWN"->s.secondsLeft()>5?"Lookout · "+(s.secondsLeft()-5)+"s":"Starting in "+s.secondsLeft()+"s";case "INTERMISSION"->"Next round in "+s.secondsLeft()+"s";case "FINISHED"->"Lobby in "+s.secondsLeft()+"s";default->Ui.clock(s.secondsLeft())+" remaining";};}
    void render(GuiGraphicsExtractor g){
        Minecraft mc=Minecraft.getInstance();var c=mod.connection;var s=c==null?null:c.state;if(s==null||!mod.inMatchWorld()||mc.player==null)return;
        long now=Snapshot.now();if(!phase.equals(s.phase())||!match.equals(s.matchId())){phase=s.phase();match=s.matchId();changedAt=now;}
        int sw=mc.getWindow().getGuiScaledWidth(),sh=mc.getWindow().getGuiScaledHeight();
        if(s.phase().equals("COUNTDOWN")&&s.secondsLeft()<=5)banner(g,""+s.secondsLeft(),Ui.GOLD,sw,sh);
        else if(s.phase().equals("ROUND")&&now-changedAt<1200)banner(g,"GO!",Ui.GREEN,sw,sh);
        else if(s.phase().equals("FINISHED")&&now-changedAt<7000){var self=s.player(c.id);boolean draw=s.result().startsWith("Draw");boolean decided=s.result().startsWith("Winner:")||s.result().startsWith("Completed all rounds:");boolean won=self!=null&&self.alive();banner(g,draw?"DRAW":decided?(won?"VICTORY":"DEFEAT"):"MATCH OVER",draw?Ui.TEXT:decided&&!won?0xFFEF5350:Ui.GOLD,sw,sh);Ui.wrap(g,mc.font,s.result(),sw/2-Math.min(180,sw/2-8),sh/2+6,Math.min(360,sw-16),Ui.TEXT);}
        if(!mod.preferences.flag("hud",true)||mc.gui.screen() instanceof MatchScreen)return;
        String heading="Round "+s.round()+" / "+s.maxRounds();
        String target=s.block().isEmpty()?phaseText(s):RummageClient.displayBlock(s.block());
        var self=s.player(c.id);
        String status=!c.connected?c.status:self!=null&&!self.alive()?"Eliminated — spectating":self!=null&&self.complete()?"Block found!":phaseText(s);
        long alive=s.players().stream().filter(Snapshot.Player::alive).count();
        String footer=alive+" alive · "+mod.matchKeyName()+": match";
        int targetPadding=s.block().isEmpty()?12:32;
        int panelWidth=Math.min(160,Math.max(mc.font.width(heading)+12,Math.max(mc.font.width(target)+targetPadding,Math.max(mc.font.width(status)+12,mc.font.width(footer)+12))));
        int top=mc.player.getActiveEffects().isEmpty()?4:54;
        float scale=Math.min(mod.preferences.number("hudScale",100)/100f,Math.min((sw-8)/(float)panelWidth,(sh-top-4)/76f));scale=Math.max(0.1f,scale);
        g.pose().pushMatrix();g.pose().translate(sw-4-panelWidth*scale,top);g.pose().scale(scale,scale);Ui.panel(g,0,0,panelWidth,76);
        Ui.text(g,mc.font,heading,6,5,panelWidth-12,Ui.GOLD);
        int targetX=6;
        if(!s.block().isEmpty()){g.item(new ItemStack(BuiltInRegistries.BLOCK.getValue(Identifier.parse(s.block()))),6,19);targetX=26;}
        Ui.text(g,mc.font,target,targetX,23,panelWidth-targetX-6,Ui.TEXT);
        Ui.text(g,mc.font,status,6,41,panelWidth-12,self!=null&&self.complete()?Ui.GREEN:s.phase().equals("ROUND")&&s.secondsLeft()<=10?0xFFEF5350:Ui.TEXT);
        Ui.text(g,mc.font,footer,6,59,panelWidth-12,Ui.MUTED);g.pose().popMatrix();
    }
    void waiting(GuiGraphicsExtractor g){var c=mod.connection;var s=c==null?null:c.state;Minecraft mc=Minecraft.getInstance();if(s==null||!s.phase().equals("LOADING")||!mod.inMatchWorld()||mc.player==null)return;int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();g.fill(0,0,w,h,0xFF101014);g.centeredText(mc.font,"Loading match worlds…",w/2,h/2-16,Ui.GOLD);String names=String.join(", ",s.players().stream().filter(p->p.connected()&&!p.ready()).map(Snapshot.Player::name).toList());g.centeredText(mc.font,mc.font.plainSubstrByWidth(names.isBlank()?"Checking the seed…":"Waiting for "+names,w-24),w/2,h/2+2,Ui.TEXT);}
    private void banner(GuiGraphicsExtractor g,String text,int color,int w,int h){var f=Minecraft.getInstance().font;float scale=Math.min(4f,(w-16)/(float)Math.max(1,f.width(text)));g.pose().pushMatrix();g.pose().translate(w/2f,h/2f-30);g.pose().scale(scale,scale);g.text(f,text,-f.width(text)/2,-f.lineHeight/2,color);g.pose().popMatrix();}
    HudElement playerList(HudElement vanilla){return(g,delta)->{var c=mod.connection;var s=c==null?null:c.state;var mc=Minecraft.getInstance();if(s==null||!mod.inMatchWorld()||mc.player==null||!mc.options.keyPlayerList.isDown()){vanilla.extractRenderState(g,delta);return;}int w=Math.min(380,mc.getWindow().getGuiScaledWidth()-16),x=(mc.getWindow().getGuiScaledWidth()-w)/2,y=10;Ui.panel(g,x,y,w,22+s.players().size()*20);g.centeredText(mc.font,"Rummage · Player Standings",x+w/2,y+5,Ui.GOLD);y+=20;for(var p:s.players()){Item item=switch(p.dimension()){case "minecraft:the_nether"->Items.NETHERRACK;case "minecraft:the_end"->Items.END_STONE;default->Items.GRASS_BLOCK;};g.item(new ItemStack(item),x+4,y);Ui.text(g,mc.font,p.name()+(p.id().equals(c.id)?" (you)":""),x+24,y+4,w/2-26,p.alive()?mod.color(p.id()):Ui.MUTED);Ui.text(g,mc.font,p.score()+" · "+Ui.status(s,p)+" · "+p.ping()+"ms",x+w/2,y+4,w/2-4,p.complete()?Ui.GREEN:Ui.MUTED);y+=20;}};}
}
