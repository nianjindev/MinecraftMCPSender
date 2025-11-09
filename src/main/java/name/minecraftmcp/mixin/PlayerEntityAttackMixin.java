package name.minecraftmcp.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import name.minecraftmcp.Minecraftmcp;

@Mixin(PlayerEntity.class)
public class PlayerEntityAttackMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("minecraft-mcp");

    /**
     * This injects code at the HEAD (start) of the 'attack' method.
     * This fires when a player tries to attack an entity.
     *
     * @param target The Entity being attacked
     * @param ci     The callback info object
     */
    @Inject(
        method = "attack(Lnet/minecraft/entity/Entity;)V",
        at = @At("HEAD")
    )
    private void onPlayerAttack(Entity target, CallbackInfo ci) {
        // 'this' in a non-static mixin method refers to the instance of the class being mixed into
        // So, 'this' is the PlayerEntity that is doing the attacking.
        PlayerEntity player = (PlayerEntity) (Object) this;

        // We only want to log this on the server side
        if (!player.getEntityWorld().isClient()) {
            String playerName = player.getName().getString();
            
            // Get the name of the entity being hit
            Text entityName = target.getName();
            String entityNameString = entityName.getString();

            // Get the "type" of entity (e.g., "minecraft:zombie")
            String entityType = target.getType().getTranslationKey();

            LOGGER.info("[AttackMixin] Player '{}' attacked entity '{}' (Type: {})", 
                        playerName, entityNameString, entityType);
            Minecraftmcp.sendEventToMCP("player_attacked", target.getName().toString());
        }
    }
}