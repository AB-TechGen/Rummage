package dev.rummage.mixin.client;
import dev.rummage.client.RummageClient;
import net.minecraft.client.renderer.fog.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FogRenderer.class)
public class FogMixin {
    @Inject(method="setupFog",at=@At("RETURN"))
    private void rummage$fog(Camera camera,int distance,DeltaTracker delta,float partial,ClientLevel level,CallbackInfoReturnable<FogData> cir){
        var mod=RummageClient.INSTANCE;
        var player=Minecraft.getInstance().player;
        if(player!=null&&(player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)||player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)))return;
        if(mod!=null&&mod.inMatchWorld()&&mod.connection.state.rules().race()&&level.dimension().equals(Level.NETHER)&&camera.getFluidInCamera()==FogType.NONE){
            FogData fog=cir.getReturnValue();fog.environmentalStart=Math.max(fog.environmentalStart,distance*8);fog.environmentalEnd=Math.max(fog.environmentalEnd,distance*16);
        }
    }
}
