package dev.rummage.client;

import java.security.SecureRandom;
import java.util.*;
import dev.rummage.core.MatchRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Only the lobby leader edits this working copy of match-specific settings. */
final class SettingsScreen extends Screen {
    private final RummageClient mod;
    private final Screen parent;
    private final Map<String,String> values=new LinkedHashMap<>();
    private int tab;
    private String error="";
    private Button saveButton;
    private static final String[][] KEYS={{"seconds","rounds","sudden","lookout"},{"seed","biomeFilter","structureFilter","radius"}};
    private static final String[][] LABELS={{"Round time (seconds)","Round limit","Sudden death","Lookout (seconds)"},{"Seed (blank = random)","Required biome","Required structure","Search radius (blocks)"}};
    private static final String[][] HELP={{"30–1800 seconds. The clock keeps running in menus.","1–100 rounds before shared victory or sudden death.","After the cap, shorten rounds until one player remains or all fail.","0–120 seconds above spawn, then a five-second countdown."},{"A numeric seed, or blank for a fresh random seed each match.","Choose one required biome. Any accepts all biomes. Targets are always biome-aware.","Choose one required structure. Any accepts all structures.","100–2000 blocks. Random seeds retry up to eight generated worlds."}};
    SettingsScreen(RummageClient mod,Screen parent){
        super(Component.literal("Match Settings"));this.mod=mod;this.parent=parent;
        var c=mod.connection;var s=c==null?null:c.state;var r=s==null?mod.savedRules():s.rules();
        values.put("seconds",""+(s==null?mod.preferences.number("seconds",300):s.seconds()));
        values.put("rounds",""+(s==null?mod.preferences.number("rounds",20):s.maxRounds()));
        values.put("seed",mod.preferences.flag("randomSeed",true)?"":s==null?mod.preferences.get("seed",""):""+s.seed());
        values.put("sudden",""+r.suddenDeath());values.put("lookout",""+r.lookoutSeconds());
        values.put("biomeFilter",r.biomeFilter());values.put("structureFilter",r.structureFilter());values.put("radius",""+r.radius());
    }
    private boolean editable(){var c=mod.connection;var s=c==null?null:c.state;return c!=null&&c.connected&&s!=null&&s.phase().equals("LOBBY")&&s.leader().equals(c.id);}
    private Button button(String label,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());}
    @Override protected void init(){
        int w=Math.min(328,width-24),left=(width-w)/2,cw=Math.min(Ui.BUTTON_WIDTH,(w-8)/2),buttonsLeft=width/2-cw-2;
        for(int i=0;i<2;i++){final int t=i;button(i==0?"Match":"World",buttonsLeft+i*(cw+4),8,cw,()->{tab=t;error="";rebuildWidgets();}).active=tab!=i;}
        for(int i=0;i<KEYS[tab].length;i++){
            String key=KEYS[tab][i];int y=60+i*26,x=left+w-cw;String help=HELP[tab][i];
            if(key.equals("sudden")){
                var b=button(Boolean.parseBoolean(values.get(key))?"On":"Off",x,y,cw,()->{values.put(key,""+!Boolean.parseBoolean(values.get(key)));rebuildWidgets();});b.active=editable();b.setTooltip(Tooltip.create(Component.literal(help)));
            }else if(Set.of("seconds","rounds","lookout","radius").contains(key)){
                int min=key.equals("seconds")?30:key.equals("rounds")?1:key.equals("radius")?100:0;
                int max=key.equals("seconds")?1800:key.equals("rounds")?100:key.equals("lookout")?120:2000;
                int step=key.equals("seconds")?30:key.equals("lookout")?5:key.equals("radius")?50:1;
                for(int sign:new int[]{-1,1}){var b=button(sign<0?"−":"+",sign<0?x:x+cw-20,y,20,()->{values.put(key,""+Math.clamp(number(key)+sign*step,min,max));rebuildWidgets();});b.active=editable();b.setTooltip(Tooltip.create(Component.literal(help)));}
            }else if(key.endsWith("Filter")){
                var b=button(FilterScreen.label(values.get(key))+"…",x,y,cw,()->minecraft.gui.setScreen(new FilterScreen(this,key.equals("biomeFilter"),values.get(key),v->values.put(key,v))));b.active=editable();b.setTooltip(Tooltip.create(Component.literal(help)));
            }else{
                var f=new EditBox(font,x,y,cw,20,Component.literal(LABELS[tab][i]));f.setMaxLength(24);f.setValue(values.get(key));f.setResponder(v->values.put(key,v.strip()));f.setEditable(editable());f.setTooltip(Tooltip.create(Component.literal(help)));addRenderableWidget(f);
            }
        }
        button("Cancel",buttonsLeft,height-28,cw,this::onClose);saveButton=button("Save Settings",buttonsLeft+cw+4,height-28,cw,this::save);saveButton.active=editable();
    }
    @Override public void tick(){saveButton.active=editable();}
    private int number(String key){return Integer.parseInt(values.get(key));}
    private void save(){
        if(!editable()){error="Only the leader can change settings before a match.";return;}
        try{
            var rules=new MatchRules(Boolean.parseBoolean(values.get("sudden")),number("lookout"),values.get("biomeFilter"),values.get("structureFilter"),number("radius"));
            int seconds=number("seconds"),rounds=number("rounds");if(seconds<30||seconds>1800||rounds<1||rounds>100)throw new IllegalArgumentException("Round time: 30–1800; rounds: 1–100.");
            boolean random=values.get("seed").isBlank();long seed=random?new SecureRandom().nextLong():Long.parseLong(values.get("seed"));
            mod.pendingRules=rules;mod.preferences.put("rules",rules.encode());mod.preferences.put("seconds",seconds);mod.preferences.put("rounds",rounds);mod.preferences.put("seed",seed);mod.preferences.put("randomSeed",random);
            mod.pendingConfig=new String[]{""+seed,""+seconds,""+rounds};var c=mod.connection;
            c.send("CONFIG",mod.pendingConfig[0],mod.pendingConfig[1],mod.pendingConfig[2]);c.send("RULES",rules.encode(),""+random);mod.savePreferences();onClose();
        }catch(IllegalArgumentException e){error="Invalid setting: "+e.getMessage();}
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        int w=Math.min(328,width-24),left=(width-w)/2,cw=Math.min(Ui.BUTTON_WIDTH,(w-8)/2),labelWidth=w-cw-8;
        Ui.text(g,font,editable()?"Match choices for everyone. Hover for details.":"Only the lobby leader can save match settings.",left,37,w,Ui.MUTED);
        for(int i=0;i<KEYS[tab].length;i++){int y=60+i*26;String key=KEYS[tab][i];Ui.panel(g,left,y,labelWidth,20);Ui.text(g,font,LABELS[tab][i],left+4,y+6,labelWidth-8,Ui.TEXT);if(Set.of("seconds","rounds","lookout","radius").contains(key))g.centeredText(font,values.get(key),left+w-cw/2,y+6,Ui.TEXT);}
        if(!error.isBlank())Ui.text(g,font,error,left,height-44,w,0xFFEF5350);super.extractRenderState(g,mx,my,delta);
    }
}
