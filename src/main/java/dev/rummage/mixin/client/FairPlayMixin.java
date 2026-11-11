package dev.rummage.mixin.client;
import dev.rummage.client.RummageClient;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(IntegratedServer.class)
public class FairPlayMixin {
    private boolean rummage$locked(){var m=RummageClient.INSTANCE;var s=m==null?null:m.activeState((MinecraftServer)(Object)this);return s!=null;}
    @Inject(method={"publishServer(Lnet/minecraft/server/MinecraftServer$MultiplayerScope;ZI)Z","publishServer(Lnet/minecraft/server/MinecraftServer$MultiplayerScope;I)Z"},at=@At("HEAD"),cancellable=true)
    private void rummage$noLan(CallbackInfoReturnable<Boolean> cir){if(rummage$locked())cir.setReturnValue(false);}
    @Inject(method="setWorldAllowCommands",at=@At("HEAD"),cancellable=true)
    private void rummage$noCommands(boolean allowed,CallbackInfo ci){if(rummage$locked()&&(allowed||!((MinecraftServer)(Object)this).getWorldData().isAllowCommands()))ci.cancel();}
    // Permission refreshes normally restore the world's default mode. Match phases own that choice.
    @Inject(method="updateGameModeBasedOnPermission",at=@At("HEAD"),cancellable=true)
    private void rummage$keepPhaseMode(CallbackInfoReturnable<Integer> cir){if(rummage$locked())cir.setReturnValue(0);}
    @Inject(method={"setPersonalGameType","setWorldGameType"},at=@At("HEAD"),cancellable=true)
    private void rummage$noCreative(CallbackInfo ci){if(rummage$locked())ci.cancel();}
}
