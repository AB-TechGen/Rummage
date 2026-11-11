package dev.rummage.mixin.client;
import dev.rummage.client.RummageClient;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class PauseMixin {
    @Inject(method="isPaused",at=@At("HEAD"),cancellable=true)
    private void rummage$keepMatchRunning(CallbackInfoReturnable<Boolean> cir) {
        if(RummageClient.INSTANCE!=null && RummageClient.INSTANCE.inMatchWorld())cir.setReturnValue(false);
    }
}
