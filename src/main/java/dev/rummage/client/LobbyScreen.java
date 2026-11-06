package dev.rummage.client;

import dev.rummage.net.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.security.SecureRandom;

public final class LobbyScreen extends Screen {
    private final RummageClient mod;
    private EditBox name, invite, seed, seconds, rounds, chat;
    private Button copyInvite;
    private String layout="";
    private int left, top, panel;
    public LobbyScreen(RummageClient mod) { super(Component.literal("Rummage")); this.mod=mod; }
    private String layoutKey() { Snapshot s=mod.connection==null?null:mod.connection.state; return s==null ? "join" : s.phase()+":"+s.leader().equals(mod.connection.id); }
    @Override protected void init() {
        clearWidgets(); copyInvite=null; layout=layoutKey(); panel=Math.min(520,width-24); left=(width-panel)/2; top=Math.max(40,(height-300)/2);
        Snapshot s=mod.connection==null?null:mod.connection.state;
        if(s==null) {
            name=field(left,top+20,panel,"Display name",mod.name,24);

            invite=field(left,top+60,panel,"Invite code","",512);
            button("Create lobby",left,top+86,panel/2-4,()->mod.host(name.getValue()));
            button("Join lobby",left+panel/2+4,top+86,panel/2-4,()->mod.join(invite.getValue(),name.getValue()));
            button("Recover session",left,top+112,panel,mod::recover).active=!mod.preferences.get("token","").isEmpty();
            button("Preferences",left,top+138,panel/3-4,()->minecraft.gui.setScreen(new SettingsScreen(mod,this)));
            button("Old worlds",left+panel/3,top+138,panel/3-4,()->minecraft.gui.setScreen(new WorldsScreen(mod,this)));
            button("Updates",left+2*panel/3,top+138,panel/3,()->minecraft.gui.setScreen(new UpdateScreen(mod,this)));
        } else {
            int half=panel/2;
            copyInvite=button("Copy invite",left,top+20,half/2-4,()->minecraft.keyboardHandler.setClipboard(mod.invite));copyInvite.active=!mod.invite.isBlank();
            button("Players",left+half/2,top+20,half/2-4,()->minecraft.gui.setScreen(new PlayersScreen(mod,this)));
            button("Leave lobby",left+half+4,top+20,half-4,()->{mod.leave();rebuildWidgets();});
            if(s.phase().equals("LOBBY")) {
                seed=field(left,top+64,half-4,"Seed",mod.preferences.flag("randomSeed",true)?"":Long.toString(s.seed()),24);
                seconds=field(left+half+4,top+64,half/2-6,"Seconds",Integer.toString(s.seconds()),4);
                rounds=field(left+half+half/2+4,top+64,half/2-4,"Rounds",Integer.toString(s.maxRounds()),3);
                Button apply=button("Apply settings",left,top+90,half-4,this::configure);
                Button start=button("Start match",left+half+4,top+90,half-4,()->{if(configure())mod.connection.send("START");});
                boolean leader=s.leader().equals(mod.connection.id); apply.active=leader; start.active=leader; seed.setEditable(leader); seconds.setEditable(leader); rounds.setEditable(leader);
                button("Rules / Preferences",left,top+116,panel,()->minecraft.gui.setScreen(new SettingsScreen(mod,this)));
            } else {
                button(minecraft.level==null?"Join match world":"Resume game",left,top+64,half-4,()->{if(minecraft.level==null)mod.createWorld();else onClose();});
                if(s.phase().equals("FINISHED")||s.phase().equals("LOADING")) {
                    Button reset=button("Return to lobby",left+half+4,top+64,half-4,()->mod.connection.send("RESET")); reset.active=s.leader().equals(mod.connection.id);
                } else button("Forfeit",left+half+4,top+64,half-4,()->mod.connection.send("FORFEIT"));
                button(s.drawVotes().contains(mod.connection.id)?"Cancel draw vote":"Offer / accept draw",left,top+90,half-4,()->mod.connection.send("DRAW"));
                button("Preferences",left+half+4,top+90,half-4,()->minecraft.gui.setScreen(new SettingsScreen(mod,this)));
            }
            chat=field(left,height-54,panel-60,"Message","",240);
            button("Send",left+panel-54,height-54,54,()->{if(!chat.getValue().isBlank()){mod.connection.send("CHAT",chat.getValue());chat.setValue("");}});
        }
        button("Back",left,height-28,panel,this::onClose);
    }
    private boolean configure() {
        try { boolean random=seed.getValue().isBlank();long value=random?new SecureRandom().nextLong():Long.parseLong(seed.getValue()); int duration=Integer.parseInt(seconds.getValue()), count=Integer.parseInt(rounds.getValue()); if(duration<30||duration>1800||count<1||count>100)throw new NumberFormatException();mod.preferences.put("seed",value);mod.preferences.put("randomSeed",random);mod.preferences.put("seconds",duration);mod.preferences.put("rounds",count);mod.savePreferences(); mod.connection.send("CONFIG",""+value,""+duration,""+count);mod.connection.send("RULES",(mod.pendingRules==null?mod.connection.state.rules():mod.pendingRules).encode(),""+random); mod.error="";return true; }
        catch(NumberFormatException e){mod.error="Use a numeric seed, 30–1800 seconds and 1–100 rounds.";return false;}
    }
    private EditBox field(int x,int y,int w,String hint,String value,int max) { EditBox e=new EditBox(font,x,y,w,20,Component.literal(hint));e.setMaxLength(max);e.setHint(Component.literal(hint));e.setValue(value);return addRenderableWidget(e); }
    private Button button(String text,int x,int y,int w,Runnable action) { return addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(x,y,w,20).build()); }
    @Override public void tick(){if(!layout.equals(layoutKey()))rebuildWidgets();if(copyInvite!=null)copyInvite.active=!mod.invite.isBlank();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xF00D171B);
        g.centeredText(font,"R U M M A G E  /  0.1.1",width/2,14,0xFF67E8A5);
        g.centeredText(font,"Separate worlds. Same seed. Beat the clock.",width/2,28,0xFFB1C5BE);
        Snapshot s=mod.connection==null?null:mod.connection.state;
        if(s==null) {
            label(g,"DISPLAY NAME",left,top+8);label(g,"INVITE CODE OR HOST:PORT",left,top+48);
            if(mod.connection!=null)label(g,mod.connection.status,left,height-40);
        } else {
            label(g,mod.connection.connected ? "LOBBY  /  "+s.phase()+"  /  "+s.players().size()+" PLAYERS" : mod.connection.status,left,top+6);
            if(mod.host!=null){label(g,mod.relayStatus,left,top+42);}
            if(s.phase().equals("LOBBY")){label(g,"SEED (BLANK = RANDOM)",left,top+52);label(g,"SECONDS       ROUNDS",left+panel/2+4,top+52);}
            else label(g,s.phase().equals("FINISHED")?s.result():RummageClient.displayBlock(s.block())+"  /  "+s.secondsLeft()+"s",left,top+50);
            int y=top+(s.phase().equals("LOBBY")?144:118);
            if(!s.drawVotes().isEmpty()){label(g,"Draw vote: "+s.drawVotes().size()+" accepted (30-second window)",left,y);y+=14;}
            for(Snapshot.Player p:s.players()) {
                if(y>height-90)break;
                String state=!p.connected()?"away":!p.alive()?"out":s.phase().equals("LOADING")?(p.ready()?"ready":"loading"):p.complete()?"found":"playing";
                label(g,(p.id().equals(s.leader())?"* ":"  ")+p.name()+"   "+p.score()+" pts   "+state,left,y);y+=12;
            }
            String[] messages=mod.messages.toArray(String[]::new);
            int available=Math.max(0,(height-76-y)/12),start=Math.max(0,messages.length-Math.min(4,available));
            for(int i=start;i<messages.length;i++){label(g,messages[i],left,y+6);y+=12;}
        }
        if(!mod.error.isBlank())g.text(font,font.plainSubstrByWidth(mod.error,panel),left,height-(s==null?40:68),0xFFFFB58D);
        super.extractRenderState(g,mx,my,delta);
    }
    private void label(GuiGraphicsExtractor g,String value,int x,int y){g.text(font,font.plainSubstrByWidth(value,panel),x,y,0xFFBDCEC8);}
}
