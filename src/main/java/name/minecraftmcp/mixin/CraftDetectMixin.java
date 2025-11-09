package name.minecraftmcp.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import name.minecraftmcp.Minecraftmcp;

@Mixin(CraftingResultSlot.class)
public class CraftDetectMixin {
    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void onCraft(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (!player.getEntityWorld().isClient()) {
            String itemName = stack.getName().getString();

            Minecraftmcp.sendEventToMCP("item_crafted", itemName);
        }
    }
}