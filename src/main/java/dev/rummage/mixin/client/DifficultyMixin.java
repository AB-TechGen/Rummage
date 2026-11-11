package dev.rummage.mixin.client;

import dev.rummage.client.RummageClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Replace Peaceful before vanilla applies it, so hostile mobs are never cleared. */
@Mixin(MinecraftServer.class)
public class DifficultyMixin {
    @ModifyVariable(method="setDifficulty",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private Difficulty rummage$noPeaceful(Difficulty wanted){
        var mod=RummageClient.INSTANCE;
        return wanted==Difficulty.PEACEFUL&&mod!=null&&mod.applies((MinecraftServer)(Object)this)?Difficulty.EASY:wanted;
    }
}
