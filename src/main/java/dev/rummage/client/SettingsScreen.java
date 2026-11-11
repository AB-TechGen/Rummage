package dev.rummage.client;

import java.util.*;
import java.security.SecureRandom;
import dev.rummage.core.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A working copy of settings; navigation and resizing never discard edits. */
final class SettingsScreen extends Screen {
    private final RummageClient mod;
    private final Screen parent;
    private final Map<String,String> values=new LinkedHashMap<>();
    private int tab,page;
    private String error="";
    private static final String[][] KEYS={{"seconds","rounds","sudden","lookout","race","drops","allowBright","fair"},{"seed","biome","biomeFilter","structureFilter","radius"},{"hud","hudScale","fullbright","playerChat","keepWorlds"},{"updateUrl","updateKey"}};
    private static final String[][] LABELS={{"Round time (seconds)","Round limit","Sudden death","Lookout (seconds)","Race preset","Standard loot RNG","Allow fullbright","Fair-play controls"},{"Seed (blank = random)","Biome-aware targets","Required biome","Required structure","Search radius (blocks)"},{"Show target HUD","HUD scale (%)","Fullbright enabled","Player chat in world","Keep worlds (0 = all)"},{"Update manifest URL","Trusted public key"}};
    private static final String[][] HELP={{"30–1800 seconds. The clock keeps running in menus.","1–100 rounds before shared victory or sudden death.","After the cap, shorten rounds until one player remains or all fail.","0–120 seconds above spawn, then a five-second countdown.","Starting supplies, reduced Nether fog and guaranteed pearl trades.","Use world-seeded sequences for supported drops and bartering.","Allow each player to toggle brighter lighting with their key bind.","Lock difficulty, commands, LAN and game rules in match worlds."},{"A numeric seed, or blank for a fresh random seed each match.","Exclude supported biome-dependent targets absent near spawn.","Choose one required biome. Any accepts all biomes.","Choose one required structure. Any accepts all structures.","100–2000 blocks. Random seeds retry up to eight generated worlds."},{"Hide the compact target card; countdown and results still show.","50–200%. Automatically capped to fit the screen.","Your local toggle, subject to the leader allowing it.","Send world chat to the lobby and show lobby messages in world.","0 keeps everything. Protected worlds are always kept."},{"HTTPS address supplied by your release maintainer.","Trusted Ed25519 public key supplied by your release maintainer."}};
    private static final Set<String> TOGGLES=Set.of("biome","sudden","drops","race","allowBright","fair","hud","fullbright","playerChat");
    SettingsScreen(RummageClient mod,Screen parent){
        super(Component.literal("Rummage Settings"));this.mod=mod;this.parent=parent;
        var s=mod.connection==null?null:mod.connection.state;var r=s==null?mod.savedRules():s.rules();
        values.put("seconds",""+(s==null?mod.preferences.number("seconds",300):s.seconds()));values.put("rounds",""+(s==null?mod.preferences.number("rounds",20):s.maxRounds()));
        values.put("seed",(s==null||s.leader().equals(mod.connection.id))&&mod.preferences.flag("randomSeed",true)?"":s==null?mod.preferences.get("seed",""):""+s.seed());
        String[] keys={"biome","sudden","lookout","drops","race","allowBright","fair","biomeFilter","structureFilter","radius"};
        String[] v={""+r.biomeTargets(),""+r.suddenDeath(),""+r.lookoutSeconds(),""+r.standardDrops(),""+r.race(),""+r.fullbright(),""+r.fairPlay(),r.biomeFilter(),r.structureFilter(),""+r.radius()};for(int i=0;i<keys.length;i++)values.put(keys[i],v[i]);
        for(int t=2;t<4;t++)for(String key:KEYS[t])values.put(key,mod.preferences.get(key,(key.equals("hud")||key.equals("playerChat"))?"true":key.equals("hudScale")?"100":key.equals("fullbright")?"false":key.equals("keepWorlds")?"0":""));
    }
    private boolean editable(){var c=mod.connection;return c==null||c.state==null||(c.connected&&c.state.phase().equals("LOBBY")&&c.state.leader().equals(c.id));}
    private int rows(){return Math.max(1,(height-124)/26);}
    private Button button(String label,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());}
    private void tip(AbstractWidget widget,String text){widget.setTooltip(Tooltip.create(Component.literal(text)));}
    @Override protected void init(){
        int w=Math.min(480,width-24),left=(width-w)/2,tw=(w-12)/4;String[] tabs={"Match","World","Display","Updates"};
        for(int i=0;i<4;i++){final int t=i;button(tabs[i],left+i*(tw+4),8,tw,()->{tab=t;page=0;error="";rebuildWidgets();}).active=tab!=i;}
        int count=rows(),from=page*count;boolean enabled=tab>=2||editable();
        for(int i=from;i<Math.min(KEYS[tab].length,from+count);i++){
            String key=KEYS[tab][i];int y=60+(i-from)*26,x=left+w/2,cw=w/2;String help=HELP[tab][i];
            if(TOGGLES.contains(key)){
                var b=button(Boolean.parseBoolean(values.get(key))?"On":"Off",x,y,cw,()->{values.put(key,""+!Boolean.parseBoolean(values.get(key)));rebuildWidgets();});b.active=enabled;tip(b,help);
            }else if(key.equals("hudScale")){
                var slider=new AbstractSliderButton(x,y,cw,20,Component.empty(),(Integer.parseInt(values.get(key))-50)/150.0){
                    {updateMessage();}
                    @Override protected void applyValue(){values.put(key,""+(50+10*(int)Math.round(value*15)));}
                    @Override protected void updateMessage(){setMessage(Component.literal((50+10*(int)Math.round(value*15))+"%"));}
                };addRenderableWidget(slider);tip(slider,help);
            }else if(Set.of("seconds","rounds","lookout","radius","keepWorlds").contains(key)){
                int min=key.equals("seconds")?30:key.equals("rounds")?1:key.equals("radius")?100:key.equals("hudScale")?50:0;
                int max=key.equals("seconds")?1800:key.equals("rounds")?100:key.equals("lookout")?120:key.equals("radius")?2000:key.equals("hudScale")?200:1000;
                int step=key.equals("seconds")?30:key.equals("lookout")?5:key.equals("radius")?50:key.equals("hudScale")?10:1;
                for(int sign:new int[]{-1,1}){var b=button(sign<0?"−":"+",sign<0?x:x+cw-20,y,20,()->{values.put(key,""+Math.clamp(Integer.parseInt(values.get(key))+sign*step,min,max));rebuildWidgets();});b.active=enabled;tip(b,help);}
            }else if(key.endsWith("Filter")){
                var b=button(FilterScreen.label(values.get(key))+"…",x,y,cw,()->minecraft.gui.setScreen(new FilterScreen(this,key.equals("biomeFilter"),values.get(key),v->values.put(key,v))));b.active=enabled;tip(b,help);
            }else{
                var f=new EditBox(font,x,y,cw,20,Component.literal(LABELS[tab][i]));f.setMaxLength(key.equals("seed")?24:1024);f.setValue(values.get(key));f.setResponder(v->values.put(key,v.strip()));f.setEditable(enabled);tip(f,help);addRenderableWidget(f);
            }
        }
        int pages=(KEYS[tab].length+count-1)/count;
        if(pages>1){button("Previous",left,height-76,90,()->{page--;rebuildWidgets();}).active=page>0;button("Next",left+w-90,height-76,90,()->{page++;rebuildWidgets();}).active=page<pages-1;}
        button("Cancel",left,height-28,w/2-2,this::onClose);button("Save Settings",left+w/2+2,height-28,w/2-2,this::save);
    }
    @Override public void resize(int w,int h){page=0;super.resize(w,h);}
    private boolean flag(String key){return Boolean.parseBoolean(values.get(key));}
    private int number(String key){return Integer.parseInt(values.get(key));}
    private void save(){try{
        var rules=new MatchRules(flag("biome"),flag("sudden"),number("lookout"),flag("drops"),flag("race"),flag("allowBright"),flag("fair"),values.get("biomeFilter"),values.get("structureFilter"),number("radius"));
        int seconds=number("seconds"),rounds=number("rounds");if(seconds<30||seconds>1800||rounds<1||rounds>100)throw new IllegalArgumentException("Round time: 30–1800; rounds: 1–100.");
        if(number("hudScale")<50||number("hudScale")>200||number("keepWorlds")<0||number("keepWorlds")>1000)throw new IllegalArgumentException("HUD: 50–200%; worlds: 0–1000.");
        boolean random=values.get("seed").isBlank();long seed=random?new SecureRandom().nextLong():Long.parseLong(values.get("seed"));
        if(editable()){mod.pendingRules=rules;mod.preferences.put("rules",rules.encode());mod.preferences.put("seconds",seconds);mod.preferences.put("rounds",rounds);mod.preferences.put("seed",seed);mod.preferences.put("randomSeed",random);var c=mod.connection;if(c!=null&&c.state!=null){mod.pendingConfig=new String[]{""+seed,""+seconds,""+rounds};c.send("CONFIG",mod.pendingConfig[0],mod.pendingConfig[1],mod.pendingConfig[2]);c.send("RULES",rules.encode(),""+random);}}
        for(int t=2;t<4;t++)for(String key:KEYS[t])mod.preferences.put(key,values.get(key));mod.savePreferences();onClose();
    }catch(IllegalArgumentException e){error="Invalid setting: "+e.getMessage();}}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        int w=Math.min(480,width-24),left=(width-w)/2,count=rows(),from=page*count;
        g.centeredText(font,tab<2&&!editable()?"Match rules are read-only; display settings stay editable.":"Changes apply when you save. Hover a control for details.",width/2,37,Ui.MUTED);
        for(int i=from;i<Math.min(KEYS[tab].length,from+count);i++){int y=60+(i-from)*26;String key=KEYS[tab][i];Ui.panel(g,left,y,w/2-6,20);Ui.text(g,font,LABELS[tab][i],left+4,y+6,w/2-14,Ui.TEXT);if(Set.of("seconds","rounds","lookout","radius","keepWorlds").contains(key))g.centeredText(font,values.get(key),left+w*3/4,y+6,Ui.TEXT);}
        if(!error.isBlank())Ui.text(g,font,error,left,height-44,w,0xFFEF5350);
        int pages=(KEYS[tab].length+count-1)/count;if(pages>1)g.centeredText(font,(page+1)+" / "+pages,width/2,height-70,Ui.MUTED);
        super.extractRenderState(g,mx,my,delta);
    }
}
