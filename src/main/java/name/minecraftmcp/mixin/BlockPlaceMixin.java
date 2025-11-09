package name.minecraftmcp.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import name.minecraftmcp.Minecraftmcp;

@Mixin(BlockItem.class)
public class BlockPlaceMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("minecraft-mcp");
    @Inject(
        method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
        at = @At("TAIL")
    )
    private void onBlockPlaced(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        // Check if the block placement was successful (isAccepted() means it wasn't cancelled)
        if (cir.getReturnValue().isAccepted()) {
            
            World world = context.getWorld();

            // We only want to run this on the server side
            // Block placement logic is handled by the server
            if (!world.isClient()) {
                
                BlockPos pos = context.getBlockPos();
                BlockState state = world.getBlockState(pos);
                PlayerEntity player = context.getPlayer();
                
                // Get the translatable name of the block
                String blockName = state.getBlock().getName().getString();

                if (player != null) {
                    // Log the information
                    String playerName = player.getName().getString();
                    LOGGER.info("[BlockPlaceMixin] Player '{}' placed block '{}' at [{}, {}, {}]", 
                                playerName, blockName, pos.getX(), pos.getY(), pos.getZ());

                    // Send the event to the MCP server
                    Minecraftmcp.sendEventToMCP("block_placed", blockName);
                } else {
                    // Should rarely happen in normal gameplay, but good to check
                    LOGGER.info("[BlockPlaceMixin] A non-player placed block '{}' at [{}, {}, {}]",
                                blockName, pos.getX(), pos.getY(), pos.getZ());
                }
            }
        }
    }
}
