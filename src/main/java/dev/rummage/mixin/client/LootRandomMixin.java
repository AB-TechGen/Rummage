package dev.rummage.mixin.client;
import dev.rummage.client.RummageClient;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootContext.Builder.class)
public class LootRandomMixin {
    @Shadow @Final private LootParams params;
    @Shadow private RandomSource random;
    @Inject(method="create",at=@At("HEAD"))
    private void rummage$sequence(Optional<Identifier> sequence,CallbackInfoReturnable<LootContext> cir){
        RummageClient mod=RummageClient.INSTANCE;
        var state=mod==null?null:mod.activeState(params.getLevel().getServer());
        if(state==null)return;
        String key=sequence.map(Identifier::toString).orElse("");
        var block=params.contextMap().get(LootContextParams.BLOCK_STATE);
        if(block!=null)key="blocks/"+BuiltInRegistries.BLOCK.getKey(block.getBlock()).getPath().replace("deepslate_","");
        else if(!(key.contains("entities/")||key.contains("piglin_bartering")))return;
        random=params.getLevel().getServer().getRandomSequence(Identifier.fromNamespaceAndPath("rummage",key.replace(':','/')));
    }
}
