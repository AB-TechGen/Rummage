package dev.rummage.client;

import dev.rummage.net.Snapshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Round actions stay separate from the social lobby, with the world visible behind them. */
final class MatchScreen extends Screen {
    private final RummageClient mod;
    private Button forfeit,draw,lobby,join;
    private long armedAt;
    private final int scaleWhenOpened;
    MatchScreen(RummageClient mod){super(Component.literal("Rummage Match"));this.mod=mod;scaleWhenOpened=mod.preferences.number("hudScale",100);}
    private Button button(String title,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Component.literal(title),b->action.run()).bounds(x,y,w,20).build());}
    private int panelWidth(){return Math.min(2*Ui.BUTTON_WIDTH+20,width-24);}
    private int panelHeight(){var s=mod.connection==null?null:mod.connection.state;return 150+(s==null?0:s.players().size()*rowHeight());}
    private int rowHeight(){return height<270?11:13;}
    private int top(){return Math.max(1,(height-panelHeight())/2);}
    @Override protected void init(){int w=panelWidth(),x=(width-w)/2,y=top()+panelHeight()-50,half=(w-20)/2;
        var scale=addRenderableWidget(new AbstractSliderButton(width/2-half/2,y-26,half,20,Component.empty(),(Math.clamp(mod.preferences.number("hudScale",100),50,200)-50)/150.0){
            {updateMessage();}
            @Override protected void applyValue(){mod.preferences.put("hudScale",50+10*(int)Math.round(value*15));}
            @Override protected void updateMessage(){setMessage(Component.literal("HUD size: "+(50+10*(int)Math.round(value*15))+"%"));}
        });
        scale.setTooltip(Tooltip.create(Component.literal("Your target HUD size. Automatically capped to fit the screen. HUD visibility and fullbright are hotkeys in Minecraft Controls.")));
        forfeit=button("Forfeit",x+8,y,half,()->{if(armedAt!=0&&System.currentTimeMillis()-armedAt<4000){mod.connection.send("FORFEIT");armedAt=0;}else armedAt=System.currentTimeMillis();});
        draw=button("Offer Draw",x+12+half,y,half,()->mod.connection.send("DRAW"));
        lobby=button("Return to Lobby",x+8,y,half,()->mod.connection.send("RESET"));
        join=button("Join Match World",x+12+half,y,half,()->{if(minecraft.level==null)mod.createWorld();else onClose();});
        button("Lobby / Chat",x+8,y+26,half,()->minecraft.gui.setScreen(new LobbyScreen(mod)));
        button("Close",x+12+half,y+26,half,this::onClose);tick();
    }
    @Override public void tick(){var c=mod.connection;var s=c==null?null:c.state;if(s==null){minecraft.gui.setScreen(new LobbyScreen(mod));return;}
        var self=s.player(c.id);boolean active=!s.phase().equals("LOBBY")&&!s.phase().equals("FINISHED")&&!s.phase().equals("LOADING");
        forfeit.visible=active;forfeit.active=c.connected&&self!=null&&self.alive();
        if(System.currentTimeMillis()-armedAt>=4000)armedAt=0;forfeit.setMessage(Component.literal(armedAt==0?"Forfeit":"Really forfeit?"));
        draw.visible=active&&minecraft.level!=null;draw.active=forfeit.active;draw.setMessage(Component.literal(s.drawVotes().contains(c.id)?"Withdraw Draw":s.drawVotes().isEmpty()?"Offer Draw":"Accept Draw"));
        lobby.visible=!active;lobby.active=c.connected&&s.leader().equals(c.id)&&(s.phase().equals("FINISHED")||s.phase().equals("LOADING"));
        join.visible=!active||minecraft.level==null;join.active=!s.phase().equals("LOBBY");join.setMessage(Component.literal(minecraft.level==null?"Join Match World":"Resume Game"));
    }
    @Override public void removed(){super.removed();if(mod.preferences.number("hudScale",100)!=scaleWhenOpened)mod.savePreferences();}
    @Override public boolean keyPressed(KeyEvent e){if(mod.matchKey(e)){onClose();return true;}return super.keyPressed(e);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        var c=mod.connection;var s=c==null?null:c.state;if(s==null)return;int w=panelWidth(),x=(width-w)/2,y=top();Ui.panel(g,x,y,w,panelHeight());
        g.centeredText(font,"Round "+s.round()+" / "+s.maxRounds(),width/2,y+8,Ui.GOLD);
        String title=s.phase().equals("FINISHED")?s.result():s.phase().equals("LOADING")?"Waiting for match worlds…":s.phase().equals("COUNTDOWN")?"Get ready — "+s.secondsLeft()+"s":RummageClient.displayBlock(s.block());
        if(!s.block().isEmpty()&&(s.phase().equals("ROUND")||s.phase().equals("INTERMISSION"))){
            String block=font.plainSubstrByWidth(RummageClient.displayBlock(s.block()),w-48);
            int targetX=x+(w-font.width(block)-22)/2;
            g.item(new ItemStack(BuiltInRegistries.BLOCK.getValue(Identifier.parse(s.block()))),targetX,y+20);
            g.text(font,block,targetX+22,y+25,Ui.TEXT);
        }else Ui.text(g,font,title,x+10,y+25,w-20,Ui.TEXT);
        g.centeredText(font,MatchHud.phaseText(s),width/2,y+40,Ui.MUTED);
        int row=y+57;for(var p:s.players()){Ui.text(g,font,(p.id().equals(c.id)?"You":p.name())+(p.id().equals(s.leader())?" ★":""),x+10,row,w/2-10,Ui.TEXT);Ui.text(g,font,p.score()+" pts · "+Ui.status(s,p),x+w/2,row,w/2-10,p.complete()?Ui.GREEN:Ui.MUTED);row+=rowHeight();}
        String note=!c.connected?c.status:!s.drawVotes().isEmpty()?"Draw: "+s.drawVotes().size()+" votes · "+Math.max(0,(s.drawRemaining()-(Snapshot.now()-s.receivedAt())+999)/1000)+"s left":"Stand on the target before time runs out.";
        Ui.text(g,font,note,x+10,row+3,w-20,Ui.MUTED);super.extractRenderState(g,mx,my,delta);
    }
}
