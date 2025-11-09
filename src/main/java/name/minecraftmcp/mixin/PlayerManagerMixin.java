package name.minecraftmcp.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import name.minecraftmcp.Minecraftmcp;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("minecraft-mcp");

    /**
     * This injects code at the HEAD (start) of the 'broadcast' method.
     * This method is used for player chat, death messages, server announcements, etc.
     *
     * @param message The Text object being broadcast
     * @param ci      The callback info object
     */
    @Inject(
        method = "broadcast(Lnet/minecraft/text/Text;Z)V",
        at = @At("HEAD")
    )
    private void onBroadcastMessage(Text message, boolean overlay, CallbackInfo ci) {
        // We get the raw string content of the message
        String messageContent = message.getString();

        // Log the broadcasted message
        // Note: This will catch A LOT of messages, including "Player joined" / "Player left"
        LOGGER.info("[BroadcastMixin] Server broadcasted message: {}", messageContent);
        Minecraftmcp.sendEventToMCP("chat_message", message.toString());
    }
}
