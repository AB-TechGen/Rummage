package dev.rummage.mixin.client;

import dev.rummage.client.RummageClient;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adjust lighting while it is extracted, never after it has been handed to the renderer. */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapMixin {
    @Shadow private boolean needsUpdate;
    @Unique private boolean rummage$previous;

    @Inject(method="extract",at=@At("HEAD"))
    private void rummage$refresh(LightmapRenderState state,float partialTick,CallbackInfo ci){
        var mod=RummageClient.INSTANCE;
        boolean enabled=mod!=null&&mod.fullbright();
        // Let vanilla repopulate every lighting field when toggling or leaving a match.
        if(enabled!=rummage$previous)needsUpdate=true;
        rummage$previous=enabled;
    }

    @Inject(method="extract",at=@At("RETURN"))
    private void rummage$bright(LightmapRenderState state,float partialTick,CallbackInfo ci){
        // A skipped extraction is not a valid new lightmap. Keep its render update skipped too.
        if(!state.needsUpdate||!rummage$previous)return;
        state.brightness=15.0f;
    }
}
