package dev.rummage.client;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import dev.rummage.core.UpdateInstaller;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
final class UpdateScreen extends Screen {
    private final RummageClient mod;private final Screen parent;
    UpdateScreen(RummageClient m,Screen p){super(Component.literal("Updates"));mod=m;parent=p;}
    @Override protected void init(){int x=width/2-110;
        addRenderableWidget(Button.builder(Component.literal("Check for updates"),b->mod.checkUpdates()).bounds(x,60,220,20).build());
        addRenderableWidget(Button.builder(Component.literal("Download verified update"),b->mod.downloadUpdate()).bounds(x,86,220,20).build());
        addRenderableWidget(Button.builder(Component.literal("Install on next game exit"),b->{try{var r=mod.update;if(r==null)throw new IllegalStateException("Check and download an update first.");Path downloaded=FabricLoader.getInstance().getConfigDir().resolve("rummage-update/rummage-"+r.version()+".jar");if(!java.nio.file.Files.isRegularFile(downloaded))throw new IllegalStateException("Download the verified update first.");UpdateInstaller.launch(Path.of(RummageClient.class.getProtectionDomain().getCodeSource().getLocation().toURI()),downloaded,r.sha256());mod.updateStatus="Installer ready. Save and quit Minecraft, then restart it.";b.active=false;}catch(Exception e){mod.updateStatus=e.getMessage();}}).bounds(x,112,220,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(x,height-28,220,20).build());
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float d){g.fill(0,0,width,height,0xF00D171B);g.centeredText(font,"SIGNED UPDATES",width/2,20,0xFF67E8A5);g.centeredText(font,font.plainSubstrByWidth(mod.updateStatus,width-20),width/2,150,0xFFFFFFFF);super.extractRenderState(g,x,y,d);}
}
