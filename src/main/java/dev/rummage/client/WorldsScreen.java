package dev.rummage.client;
import java.util.*;
import dev.rummage.core.WorldStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
final class WorldsScreen extends Screen {
    private final RummageClient mod;private final Screen parent;private List<WorldStore.World> worlds=List.of();private int page;private String status="";private boolean confirm;
    WorldsScreen(RummageClient m,Screen parent){super(Component.literal("Match worlds"));mod=m;this.parent=parent;}
    @Override protected void init(){try{worlds=mod.worlds.list();}catch(Exception e){status=e.getMessage();}int count=Math.max(1,(height-115)/28);int start=page*count;if(start>=worlds.size()&&page>0){page=0;start=0;}
        for(int i=start;i<Math.min(start+count,worlds.size());i++){var w=worlds.get(i);addRenderableWidget(Button.builder(Component.literal(w.favorite()?"Unprotect":"Protect"),b->{try{mod.worlds.favorite(w.folder(),!w.favorite());rebuildWidgets();}catch(Exception e){status=e.getMessage();}}).bounds(width-94,48+(i-start)*28,84,20).build());}
        addRenderableWidget(Button.builder(Component.literal("Next page"),b->{page++;rebuildWidgets();}).bounds(10,height-52,100,20).build());
        Button clean=addRenderableWidget(Button.builder(Component.literal(confirm?"Confirm cleanup":"Clean old worlds"),b->{if(!confirm){confirm=true;status="Keep newest "+mod.preferences.number("keepWorlds",0)+" unprotected worlds; delete older ones.";rebuildWidgets();return;}try{status="Removed "+mod.worlds.prune(mod.preferences.number("keepWorlds",0),"")+" worlds.";}catch(Exception e){status=e.getMessage();}confirm=false;rebuildWidgets();}).bounds(116,height-52,Math.max(100,width-126),20).build());clean.active=minecraft.getSingleplayerServer()==null&&mod.preferences.number("keepWorlds",0)>0;
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(10,height-28,width-20,20).build());
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float d){Ui.panel(g,4,4,width-8,height-8);g.centeredText(font,"RUMMAGE MATCH WORLDS",width/2,16,Ui.GOLD);g.centeredText(font,"Set retention in the lobby. Protected worlds are kept.",width/2,30,0xFFBDCEC8);int count=Math.max(1,(height-115)/28),start=page*count;for(int i=start;i<Math.min(start+count,worlds.size());i++){var w=worlds.get(i);String label=(w.favorite()?"★ ":"")+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(w.modified()))+"  "+w.folder().substring(8,16);g.text(font,font.plainSubstrByWidth(label,width-110),10,54+(i-start)*28,0xFFFFFFFF);}g.text(font,font.plainSubstrByWidth(status,width-20),10,height-66,0xFFFFB58D);super.extractRenderState(g,x,y,d);}
}
