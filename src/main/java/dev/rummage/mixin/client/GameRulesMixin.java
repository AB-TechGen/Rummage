package dev.rummage.mixin.client;
import dev.rummage.client.RummageClient;
import net.minecraft.world.level.gamerules.*;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRules.class)
public class GameRulesMixin {
    @Inject(method="set",at=@At("HEAD"),cancellable=true)
    private <T> void rummage$lockRules(GameRule<T> rule,T value,MinecraftServer server,CallbackInfo ci){
        var mod=RummageClient.INSTANCE;var state=server==null||mod==null?null:mod.activeState(server);if(state!=null&&state.rules().fairPlay())ci.cancel();
    }
    @Inject(method="setAll",at=@At("HEAD"),cancellable=true)
    private void rummage$lockBulk(CallbackInfo ci){var mod=RummageClient.INSTANCE;if(mod!=null&&mod.inMatchWorld()&&mod.connection.state.rules().fairPlay())ci.cancel();}
}
