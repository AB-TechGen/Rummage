package dev.rummage.mixin.client;
import dev.rummage.client.RummageClient;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Lightmap.class)
public class LightmapMixin {
    @Unique private boolean rummage$previous;
    @Inject(method="render",at=@At("HEAD"))
    private void rummage$bright(LightmapRenderState state,CallbackInfo ci){
        var mod=RummageClient.INSTANCE;
        boolean enabled=mod!=null&&mod.fullbright();if(enabled!=rummage$previous)state.needsUpdate=true;rummage$previous=enabled;
        if(enabled){state.brightness=1;state.nightVisionEffectIntensity=1;state.nightVisionColor=new org.joml.Vector3f(1,1,1);state.needsUpdate=true;}
    }
}
