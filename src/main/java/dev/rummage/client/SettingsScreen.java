package dev.rummage.client;

import java.util.*;
import dev.rummage.core.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class SettingsScreen extends Screen {
    private final RummageClient mod;private final Screen parent;private int page;
    private final Map<String,String> values=new LinkedHashMap<>();private final Map<String,EditBox> fields=new HashMap<>();
    private String error="";
    private static final String[][] KEYS={{"biome","sudden","lookout","drops","race","allowBright"},{"fair","biomeFilter","structureFilter","radius"},{"hud","hudScale","fullbright","keepWorlds"},{"relay","updateUrl","updateKey"}};
    private static final String[][] LABELS={{"Biome-aware targets","Sudden death","Lookout seconds","Standard loot RNG","Race preset","Allow fullbright"},{"Fair-play controls","Required biome ID","Required structure ID","Search radius"},{"Show HUD","HUD scale (%)","Fullbright enabled","Keep worlds (0 = all)"},{"Relay address","Update manifest URL","Trusted public key"}};
    private static final Set<String> TOGGLES=Set.of("biome","sudden","drops","race","allowBright","fair","hud","fullbright");
    SettingsScreen(RummageClient mod,Screen parent){super(Component.literal("Rummage settings"));this.mod=mod;this.parent=parent;
        var s=mod.connection==null?null:mod.connection.state;MatchRules r=s==null?mod.savedRules():s.rules();
        String[] v={""+r.biomeTargets(),""+r.suddenDeath(),""+r.lookoutSeconds(),""+r.standardDrops(),""+r.race(),""+r.fullbright(),""+r.fairPlay(),r.biomeFilter(),r.structureFilter(),""+r.radius()};int n=0;
        for(int p=0;p<2;p++)for(String key:KEYS[p])values.put(key,v[n++]);
        for(int p=2;p<4;p++)for(String key:KEYS[p])values.put(key,mod.preferences.get(key,key.equals("hud")?"true":key.equals("hudScale")?"100":key.equals("fullbright")?"false":key.equals("keepWorlds")?"0":""));
    }
    private void collect(){fields.forEach((k,f)->values.put(k,f.getValue().strip()));}
    private boolean editable(){var c=mod.connection;return c==null||c.state==null||(c.state.phase().equals("LOBBY")&&c.state.leader().equals(c.id));}
    @Override protected void init(){fields.clear();int left=Math.max(10,width/2-250),w=Math.min(500,width-20),step=Math.min(32,Math.max(22,(height-125)/6));
        for(int i=0;i<KEYS[page].length;i++){String key=KEYS[page][i];int y=54+i*step;int x=left+w/2;boolean enabled=page>=2||editable();
            if(TOGGLES.contains(key)){Button b=addRenderableWidget(Button.builder(Component.literal(Boolean.parseBoolean(values.get(key))?"On":"Off"),button->{boolean next=!Boolean.parseBoolean(values.get(key));values.put(key,""+next);button.setMessage(Component.literal(next?"On":"Off"));}).bounds(x,y,w/2,20).build());b.active=enabled;}
            else{EditBox f=new EditBox(font,x,y,w/2,20,Component.literal(LABELS[page][i]));f.setMaxLength(1024);f.setValue(values.get(key));f.setEditable(enabled);fields.put(key,addRenderableWidget(f));}
        }
        addRenderableWidget(Button.builder(Component.literal("Previous"),b->{collect();page=(page+3)%4;rebuildWidgets();}).bounds(left,height-52,w/2-3,20).build());
        addRenderableWidget(Button.builder(Component.literal("Next"),b->{collect();page=(page+1)%4;rebuildWidgets();}).bounds(left+w/2+3,height-52,w/2-3,20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"),b->save()).bounds(left,height-28,w/2-3,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(left+w/2+3,height-28,w/2-3,20).build());
    }
    private boolean flag(String key){return Boolean.parseBoolean(values.get(key));}
    private int number(String key){return Integer.parseInt(values.get(key));}
    private void save(){collect();try{
        MatchRules rules=new MatchRules(flag("biome"),flag("sudden"),number("lookout"),flag("drops"),flag("race"),flag("allowBright"),flag("fair"),values.get("biomeFilter"),values.get("structureFilter"),number("radius"));
        if(number("hudScale")<50||number("hudScale")>200||number("keepWorlds")<0||number("keepWorlds")>1000)throw new IllegalArgumentException("HUD scale: 50–200; retained worlds: 0–1000.");
        if(editable()){mod.pendingRules=rules;mod.preferences.put("rules",rules.encode());if(mod.connection!=null&&mod.connection.state!=null)mod.connection.send("RULES",rules.encode(),""+mod.preferences.flag("randomSeed",true));}
        for(int p=2;p<4;p++)for(String key:KEYS[p])mod.preferences.put(key,values.get(key));mod.savePreferences();onClose();
    }catch(IllegalArgumentException e){error="Invalid setting: "+e.getMessage();}}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){g.fill(0,0,width,height,0xF00D171B);g.centeredText(font,new String[]{"MATCH RULES","WORLD FILTERS","PREFERENCES","NETWORK / UPDATES"}[page],width/2,16,0xFF67E8A5);
        String hint=page==0?"Race: portal supplies, food, reduced Nether fog.":page==1?"Blank filters accept any seed; random searches try up to 8 worlds.":page==2?"Cleanup is opt-in; favourites are always protected.":"Relay changes apply to the next connection.";
        g.centeredText(font,font.plainSubstrByWidth(hint,width-20),width/2,32,0xFFBDCEC8);
        int left=Math.max(10,width/2-250),w=Math.min(500,width-20),step=Math.min(32,Math.max(22,(height-125)/6));for(int i=0;i<LABELS[page].length;i++)g.text(font,font.plainSubstrByWidth(LABELS[page][i],w/2-6),left,60+i*step,0xFFFFFFFF);
        if(!error.isEmpty())g.text(font,font.plainSubstrByWidth(error,width-20),10,height-66,0xFFFF7755);super.extractRenderState(g,x,y,delta);
    }
}
