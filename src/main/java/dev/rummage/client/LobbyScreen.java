package dev.rummage.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.security.SecureRandom;

public final class LobbyScreen extends Screen {
    private final RummageClient mod;
    private EditBox name,invite,chat;
    private String layout="",draftChat="",draftInvite="";
    private boolean showCode;
    private Button copy,start;
    private int side;
    public LobbyScreen(RummageClient mod){super(Component.literal("Rummage Lobby"));this.mod=mod;}
    private String layoutKey(){var c=mod.connection;var s=c==null?null:c.state;return s==null?"join":s.phase()+":"+s.leader().equals(c.id);}
    private Button button(String label,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());}
    private EditBox field(String label,int x,int y,int w,String value,int max){var f=new EditBox(font,x,y,w,18,Component.literal(label));f.setMaxLength(max);f.setValue(value);return addRenderableWidget(f);}
    @Override protected void init(){
        layout=layoutKey();copy=null;start=null;chat=null;int cx=width/2,w=Math.min(220,width-24);
        var s=mod.connection==null?null:mod.connection.state;
        if(s==null){
            name=field("Your name",cx-w/2,49,w,mod.name,24);name.setResponder(v->mod.name=v);
            button("Create Lobby",cx-w/2,74,w,()->mod.host(name.getValue()));
            invite=field("Invite code",cx-w/2,119,w-60,draftInvite,512);invite.setResponder(v->draftInvite=v);
            button("Join",cx+w/2-56,118,56,this::join);
            button("Recover Session",cx-w/2,144,w,mod::recover).active=!mod.preferences.get("token","").isBlank();
            int small=(w-8)/3;
            button("Settings",cx-w/2,height-52,small,()->minecraft.gui.setScreen(new SettingsScreen(mod,this)));
            button("Worlds",cx-w/2+small+4,height-52,small,()->minecraft.gui.setScreen(new WorldsScreen(mod,this)));
            button("Updates",cx-w/2+2*(small+4),height-52,w-2*(small+4),()->minecraft.gui.setScreen(new UpdateScreen(mod,this)));
            button("Back",cx-w/2,height-28,w,this::onClose);
        }else{
            side=Math.min(210,(width-24)/2);
            button(showCode?"Hide":"Show",8,43,side/2-2,()->{showCode=!showCode;rebuildWidgets();});
            copy=button("Copy",10+side/2,43,side/2-2,()->minecraft.keyboardHandler.setClipboard(mod.invite));
            button(s.leader().equals(mod.connection.id)&&s.phase().equals("LOBBY")?"Edit Settings":"View Settings",width-8-side,137,side,()->minecraft.gui.setScreen(new SettingsScreen(mod,this)));
            button("Leave Lobby",width-8-side,height-28,side/2-2,()->{mod.leave();rebuildWidgets();});
            start=button(s.phase().equals("LOBBY")?"Start":"Match",width-6-side/2,height-28,side/2-2,()->{if(s.phase().equals("LOBBY"))startMatch();else minecraft.gui.setScreen(new MatchScreen(mod));});
            int chatW=Math.max(80,width-side-28);
            chat=field("Chat",8,height-28,chatW,draftChat,240);chat.setHint(Component.literal("Say something… (Enter to send)"));chat.setResponder(v->draftChat=v);
        }
        tick();
    }
    private void join(){if(invite.getValue().isBlank()){mod.error="Paste an invite code first.";return;}mod.join(invite.getValue(),name.getValue());}
    private void startMatch(){
        var c=mod.connection;if(c==null||c.state==null||!c.connected)return;
        String[] config=mod.pendingConfig;
        if(config!=null)c.send("CONFIG",mod.preferences.flag("randomSeed",true)?""+new SecureRandom().nextLong():config[0],config[1],config[2]);
        else if(mod.preferences.flag("randomSeed",true))c.send("CONFIG",""+new SecureRandom().nextLong(),""+c.state.seconds(),""+c.state.maxRounds());
        c.send("START");
    }
    @Override public void tick(){if(!layout.equals(layoutKey())){rebuildWidgets();return;}if(copy!=null)copy.active=!mod.invite.isBlank();if(start!=null){var c=mod.connection;start.active=c!=null&&c.connected&&c.state!=null&&(!c.state.phase().equals("LOBBY")||c.state.leader().equals(c.id));}}
    @Override public boolean keyPressed(KeyEvent e){if(e.key()==257||e.key()==335){if(chat!=null&&chat.isFocused()){if(mod.connection.connected){if(!chat.getValue().isBlank())mod.connection.send("CHAT",chat.getValue());chat.setValue("");}return true;}if(layoutKey().equals("join")&&invite!=null&&invite.isFocused()){join();return true;}}return super.keyPressed(e);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        var s=mod.connection==null?null:mod.connection.state;
        if(s==null){g.centeredText(font,"Rummage Lobby",width/2,16,Ui.GOLD);int x=width/2-Math.min(220,width-24)/2;g.text(font,"Your name",x,37,Ui.MUTED);g.text(font,"Join with an invite code",x,107,Ui.MUTED);String note=!mod.error.isBlank()?mod.error:mod.connection==null?"":mod.connection.status;Ui.text(g,font,note,x,170,Math.min(220,width-24),0xFFEF5350);}
        else{
            Ui.panel(g,8,8,side,62+s.players().size()*13);g.text(font,"Lobby ("+s.players().size()+"/8)",13,13,Ui.GOLD);
            String code=mod.invite.isBlank()?"Preparing invite…":showCode?mod.invite:"XXXX-XXXX";Ui.text(g,font,code,13,29,side-10,Ui.TEXT);
            int y=72;for(var p:s.players()){Ui.text(g,font,(p.id().equals(s.leader())?"★ ":"")+p.name()+(p.id().equals(mod.connection.id)?" (you)":"")+(!p.connected()?" (away)":""),13,y,side-10,p.connected()?Ui.TEXT:Ui.MUTED);y+=13;}
            int x=width-8-side;Ui.panel(g,x,8,side,125);g.text(font,"Match Settings",x+5,13,Ui.GOLD);
            String[] lines={s.seconds()+" seconds per round",s.maxRounds()+" rounds"+(s.rules().suddenDeath()?" + sudden death":""),"Lookout: "+s.rules().lookoutSeconds()+"s","Targets: "+(s.rules().biomeTargets()?"biome-aware":"all"),"Race preset: "+(s.rules().race()?"on":"off"),"Loot RNG: "+(s.rules().standardDrops()?"shared":"vanilla"),s.leader().equals(mod.connection.id)?"You are the leader.":"The leader chooses the rules."};
            y=29;for(String line:lines){Ui.text(g,font,line,x+5,y,side-10,Ui.TEXT);y+=14;}
            int bottom=height-48,chatTop=Math.max(78+s.players().size()*13,164),chatW=Math.max(80,width-side-28);String[] messages=mod.messages.toArray(String[]::new);int count=Math.max(0,(bottom-chatTop)/11);for(int i=Math.max(0,messages.length-count);i<messages.length;i++){Ui.text(g,font,messages[i],10,chatTop,chatW,Ui.TEXT);chatTop+=11;}
            String status=!mod.error.isBlank()?mod.error:!mod.connection.connected?mod.connection.status:!mod.relayStatus.isBlank()?mod.relayStatus:s.phase().equals("LOBBY")?"Start when everyone is here.":"Match in progress — open Match to return.";
            if(width>=650)Ui.wrap(g,font,status,side+20,16,width-2*side-40,Ui.GOLD);else Ui.text(g,font,status,8,height-42,width-16,Ui.GOLD);
        }
        super.extractRenderState(g,mx,my,delta);
    }
}
