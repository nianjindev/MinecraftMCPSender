// In mixin/EatDetectMixin.java
package name.minecraftmcp.mixin;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import name.minecraftmcp.Minecraftmcp;

@Mixin(ItemStack.class)
public abstract class EatDetectMixin {
    // This runs BEFORE the stack gets mutated/consumed
    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void onFinishUsingHead(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = (ItemStack)(Object)this; // 'this' is the ItemStack being used

        if (!world.isClient() && user instanceof PlayerEntity) {
            Item item = stack.getItem();
            Minecraftmcp.sendEventToMCP("player_used_item", item.getName().toString());
        }
    }
}