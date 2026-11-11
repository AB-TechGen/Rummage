package dev.rummage.client;

import dev.rummage.net.Snapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class Ui {
    static final int BUTTON_WIDTH=120;
    static final int PANEL=0xD0101014,TEXT=0xFFDDDDDD,MUTED=0xFFAAAAAA,GOLD=0xFFFFD54F,GREEN=0xFF66BB6A;
    static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h){g.fill(x,y,x+w,y+h,PANEL);}
    static void text(GuiGraphicsExtractor g,Font f,String s,int x,int y,int w,int color){g.text(f,f.plainSubstrByWidth(s,Math.max(0,w)),x,y,color);}
    static int wrap(GuiGraphicsExtractor g,Font f,String s,int x,int y,int w,int color){for(var line:f.split(Component.literal(s),Math.max(1,w))){g.text(f,line,x,y,color);y+=11;}return y;}
    static String status(Snapshot s,Snapshot.Player p){if(!p.connected())return "Away";if(!p.alive())return "Eliminated";return switch(s.phase()){case "LOBBY"->"In lobby";case "LOADING"->p.ready()?"Ready":"Loading";case "COUNTDOWN"->"Ready";case "FINISHED"->"Finished";default->p.complete()?"Found":"Finding";};}
    static String clock(long s){return "%d:%02d".formatted(s/60,s%60);}
    private Ui(){}
}
