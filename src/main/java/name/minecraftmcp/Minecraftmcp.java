package name.minecraftmcp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture; // For async networking

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Http MCP server stuff
import com.google.gson.Gson;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
// Event imports
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

public class Minecraftmcp implements ModInitializer {
	public static final String MOD_ID = "minecraft-mcp";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final HttpClient httpClient = HttpClient.newHttpClient();
	private static final Gson gson = new Gson();
	private static final String MCP_SERVER_URL = "http://localhost:8080/mcp";

    private static final Map<UUID, RegistryKey<Biome>> playerBiomeMap = new HashMap<>();
    private static boolean isCurrentlyDay;

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

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerWorld overworld = server.getOverworld();
            if (overworld != null) {
                long initialTime = overworld.getTimeOfDay() % 24000;
                isCurrentlyDay = initialTime >= 0 && initialTime < 13000;
            }
        });

        registerBiomeChangeDetector();
        registerDayNightDetector();
	}
    private void registerBiomeChangeDetector() {
    
    // 2. Register a listener for the END_SERVER_TICK event
    // This event fires once per server tick
    ServerTickEvents.END_SERVER_TICK.register(server -> {
        
        // 3. Loop through every player on the server
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            
            // --- The rest of the logic is the same, just moved inside this loop ---
            
            // Get the player's UUID
            UUID playerUuid = player.getUuid();
            
            // Get the player's current position and world
            BlockPos playerPos = player.getBlockPos();
            var world = player.getEntityWorld();
            
            // Get the RegistryKey of the current biome
            RegistryKey<Biome> currentBiomeKey = world.getBiome(playerPos).getKey().orElse(null);

            // If we couldn't get a biome key, skip this player and check the next one
            if (currentBiomeKey == null) {
                continue; 
            }

            // 4. Get the player's previous biome from our map
            RegistryKey<Biome> previousBiomeKey = playerBiomeMap.get(playerUuid);

            if (previousBiomeKey == null) {
                // This is the first time we're checking this player
                // Just store their current biome and move to the next player
                playerBiomeMap.put(playerUuid, currentBiomeKey);
                continue; 
            }

            // 5. Compare the current biome to the previous one
            if (!currentBiomeKey.equals(previousBiomeKey)) {
                // --- A BIOME CHANGE HAS HAPPENED! ---
                
                // The Identifier (e.g., "minecraft:plains")
                String newBiomeId = currentBiomeKey.getValue().toString(); 
                String oldBiomeId = previousBiomeKey.getValue().toString();

                LOGGER.info("Player " + player.getName().getString() + " moved from biome " + oldBiomeId + " to " + newBiomeId);
                
                // --- Put your custom action here! ---
                Minecraftmcp.sendEventToMCP("biome_change", newBiomeId);
                
                // 6. CRITICAL: Update the map with the new biome
                playerBiomeMap.put(playerUuid, currentBiomeKey);
            }
            }
        });


    
        // 7. (Optional but Recommended) Clean up the map when a player disconnects
        //    (This part is the same as before and is correct)
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            playerBiomeMap.remove(handler.player.getUuid());
        });
    }

    private void registerDayNightDetector() {
    
    // We use the same server-wide tick event
    ServerTickEvents.END_SERVER_TICK.register(server -> {
        
        // Get the Overworld (where the main day-night cycle happens)
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) {
            return; // Not ready yet
        }

        // Get the time of day, wrapping around at 24000 ticks
        long timeOfDay = overworld.getTimeOfDay() % 24000;

        // 
        // In Minecraft:
        // 0 = Sunrise
        // 13000 = Sunset
        // So, 0-12999 is "day" and 13000-23999 is "night".
        boolean nowIsDay = timeOfDay >= 0 && timeOfDay < 13000;

        // 1. Check if the current state is DIFFERENT from our stored state
        if (nowIsDay != isCurrentlyDay) {
            
            // --- A TIME CHANGE HAS HAPPENED! ---
            
            if (nowIsDay) {
                // It just turned Day
                LOGGER.info("Time changed to Day");
                Minecraftmcp.sendEventToMCP("time_change", "day");
            } else {
                // It just turned Night
                LOGGER.info("Time changed to Night");
                Minecraftmcp.sendEventToMCP("time_change", "night");
            }
            
            // 2. CRITICAL: Update our stored state to the new state
            isCurrentlyDay = nowIsDay;
        }
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