package dev.rummage.client;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** One optional filter, with readable presets and a custom-ID escape hatch. */
final class FilterScreen extends Screen {
    private final Screen parent;
    private final String[] choices;
    private final Consumer<String> choose;
    private String custom;
    private int page;
    FilterScreen(Screen parent,boolean biome,String current,Consumer<String> choose){
        super(Component.literal(biome?"Required Biome":"Required Structure"));this.parent=parent;this.custom=current;this.choose=choose;
        choices=biome?new String[]{"","plains","forest","desert","jungle","badlands","snowy_plains","taiga","swamp","bamboo_jungle","lush_caves","ocean","savanna","cherry_grove"}:new String[]{"","village_plains","village_desert","village_savanna","village_snowy","village_taiga","desert_pyramid","shipwreck","pillager_outpost","ruined_portal","jungle_pyramid","ancient_city","trial_chambers","mansion","monument","igloo"};
    }
    static String label(String id){if(id.isBlank())return "Any";String s=id.replace("minecraft:","").replace('_',' ');return Character.toUpperCase(s.charAt(0))+s.substring(1);}
    private Button button(String s,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Component.literal(s),b->action.run()).bounds(x,y,w,20).build());}
    private int capacity(){return Math.max(1,(height-142)/24)*2;}
    @Override protected void init(){int w=Math.min(380,width-24),x=(width-w)/2,half=(w-4)/2;int count=capacity();page=Math.min(page,(choices.length-1)/count);
        var field=new EditBox(font,x,49,w-76,20,Component.literal("Custom ID"));field.setMaxLength(128);field.setValue(custom);field.setResponder(v->custom=v.strip());addRenderableWidget(field);
        button("Use ID",x+w-72,49,72,()->select(custom));
        for(int i=page*count;i<Math.min(choices.length,(page+1)*count);i++){String id=choices[i].isEmpty()?"":"minecraft:"+choices[i];int n=i-page*count;button((id.equals(custom)?"✓ ":"")+label(id),x+(n%2)*(half+4),83+(n/2)*24,half,()->select(id));}
        if(choices.length>count){button("Previous",x,height-52,half,()->{page--;rebuildWidgets();}).active=page>0;button("Next",x+half+4,height-52,half,()->{page++;rebuildWidgets();}).active=(page+1)*count<choices.length;}
        button("Cancel",x,height-28,w,this::onClose);
    }
    private void select(String id){choose.accept(id);onClose();}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){g.centeredText(font,title,width/2,14,Ui.GOLD);g.centeredText(font,"Choose one, or enter a custom Minecraft ID.",width/2,31,Ui.MUTED);super.extractRenderState(g,mx,my,delta);}
}
