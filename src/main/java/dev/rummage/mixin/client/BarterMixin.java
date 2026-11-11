package dev.rummage.mixin.client;
import dev.rummage.client.RummageClient;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LootTable.class)
public class BarterMixin {
    @Shadow @Final private Optional<Identifier> randomSequence;
    @Inject(method="getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V",at=@At("HEAD"),cancellable=true)
    private void rummage$pearls(LootContext context,Consumer<ItemStack> output,CallbackInfo ci){
        var mod=RummageClient.INSTANCE;
        var state=mod==null?null:mod.activeState(context.getLevel().getServer());
        if(state==null||randomSequence.isEmpty()||!randomSequence.get().getPath().equals("gameplay/piglin_bartering"))return;
        String key="barters."+state.matchId();int count=mod.preferences.number(key,0)+1;mod.preferences.put(key,count);mod.savePreferences();
        if(count<=144&&count%36==0){output.accept(new ItemStack(Items.ENDER_PEARL,3));ci.cancel();}
    }
}
