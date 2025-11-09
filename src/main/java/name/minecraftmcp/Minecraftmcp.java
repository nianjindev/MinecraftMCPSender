package name.minecraftmcp;

import net.fabricmc.api.ModInitializer;

// Event imports
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.message.MessageType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Http MCP server stuff
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map; // For building the JSON object
import java.util.concurrent.CompletableFuture; // For async networking

public class Minecraftmcp implements ModInitializer {
	public static final String MOD_ID = "minecraft-mcp";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final HttpClient httpClient = HttpClient.newHttpClient();
	private static final Gson gson = new Gson();
	private static final String MCP_SERVER_URL = "http://localhost:8080/mcp";

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			String blockName = state.getBlock().toString();
			LOGGER.info("Player broke block: " + blockName);
			sendEventToMCP("block_broken", blockName);
			return true;
		});

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, typeKey) -> {
            String chatContent = message.getContent().toString();
            sendEventToMCP("chat_message", chatContent + " from " + sender.getName().getString());
        });

        ServerMessageEvents.GAME_MESSAGE.register((message, sender, typeKey) -> {
            String gameContent = message.toString();
            sendEventToMCP("game_message", gameContent + " from " + sender.getString());
        });

	}

	public static void sendEventToMCP(String event, String source) {
    CompletableFuture.runAsync(() -> {
        try {
            Map<String, Object> parameters = Map.of(
                "event", event,
                "source", source
            );

            Map<String, Object> mcpPayload = Map.of(
                "mcp_version", "1.0",
                "call_id", "mc-call-" + System.currentTimeMillis(), // Simple unique ID
                "type", "tool_call",
                "tool_name", "report_event",
                "parameters", parameters
            );

            String jsonPayload = gson.toJson(mcpPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MCP_SERVER_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            LOGGER.info("MCP Server Response: " + response.body());
        } catch (Exception e) {
            // If the Python server isn't running, this will catch the error
            LOGGER.warn("Failed to send event to MCP server. Is it running?");
            LOGGER.warn(e.getMessage());
        }
    });
	}
}