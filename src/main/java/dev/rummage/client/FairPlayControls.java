package dev.rummage.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Disable restricted vanilla controls without disabling the player's difficulty choice. */
final class FairPlayControls {
    private static final Set<String> LOCKED=Set.of(
        "menu.shareToLan","menu.multiplayerOptions.button","lanServer.start",
        "menu.multiplayerOptions.lan","menu.multiplayerOptions.network",
        "selectWorld.allowCommands","selectWorld.allowCommands.new",
        "options.worldOptions.game_mode","options.worldOptions.personal_game_mode",
        "options.worldOptions.guest.command_access","options.worldOptions.guest.force_game_mode",
        "editGamerule.inGame.button"
    );
    private static final Tooltip WHY=Tooltip.create(Component.literal("Unavailable during a Rummage match."));
    static void apply(Screen screen){
        var mod=RummageClient.INSTANCE;var server=Minecraft.getInstance().getSingleplayerServer();
        if(mod==null||server==null||!mod.applies(server))return;
        lock(screen.children());
    }
    private static void lock(List<? extends GuiEventListener> children){
        for(var child:children){
            if(child instanceof AbstractWidget widget){
                if(mentions(widget.getMessage(),LOCKED)||widget instanceof LockIconButton){widget.active=false;widget.setTooltip(WHY);}
                else if(widget instanceof CycleButton<?> && mentions(widget.getMessage(),Set.of("options.difficulty"))){widget.active=true;widget.setTooltip(null);}
            }
            if(child instanceof ContainerEventHandler container)lock(container.children());
        }
    }
    private static boolean mentions(Component component,Set<String> keys){
        if(component.getContents() instanceof TranslatableContents translated){
            if(keys.contains(translated.getKey()))return true;
            for(Object arg:translated.getArgs())if(arg instanceof Component c&&mentions(c,keys))return true;
        }
        for(var sibling:component.getSiblings())if(mentions(sibling,keys))return true;
        return false;
    }
    private FairPlayControls(){}
}
