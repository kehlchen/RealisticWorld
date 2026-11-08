package dev.realisticworld;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Writes a small data pack into the main world folder that overrides the overworld dimension type
 * with a different min_y / height. The vanilla JSON is read from the server files and only the height
 * fields are changed, so the pack stays valid when Mojang changes the format.
 */
final class HeightPack {
    private HeightPack() {
    }

    static void install(JavaPlugin plugin, Settings s) {
        try {
            Path override = plugin.getDataFolder().toPath().resolve("vanilla");
            byte[] dimBytes = VanillaData.find(override, "data/minecraft/dimension_type/overworld.json", plugin.getLogger());
            byte[] metaBytes = VanillaData.find(override, "pack.mcmeta", plugin.getLogger());
            if (dimBytes == null || metaBytes == null) {
                plugin.getLogger().warning("Vanilla " + (dimBytes == null ? "dimension_type/overworld.json" : "pack.mcmeta")
                        + " not found in the server files. Height override skipped, terrain uses the default world height.");
                plugin.getLogger().warning("Fix: copy data/minecraft/dimension_type/overworld.json and pack.mcmeta from a "
                        + "vanilla server jar (or the output of the vanilla data generator) to "
                        + override + " keeping the same relative paths, then restart.");
                return;
            }
            JsonObject dim = JsonParser.parseString(new String(dimBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject meta = JsonParser.parseString(new String(metaBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            dim.addProperty("min_y", s.minY());
            dim.addProperty("height", s.height());
            dim.addProperty("logical_height", s.height());
            if (meta.has("pack") && meta.get("pack").isJsonObject()) {
                meta.getAsJsonObject("pack").addProperty("description", "RealisticWorld height override");
            }

            Path pack = Path.of(Bukkit.getWorldContainer().getPath(), levelName(), "datapacks", "realisticworld-height");
            Path dimDir = pack.resolve("data/minecraft/dimension_type");
            Files.createDirectories(dimDir);
            Files.writeString(dimDir.resolve("overworld.json"), dim.toString(), StandardCharsets.UTF_8);
            Files.writeString(pack.resolve("pack.mcmeta"), meta.toString(), StandardCharsets.UTF_8);
            plugin.getLogger().info("Height pack written: y " + s.minY() + " to " + (s.minY() + s.height() - 1)
                    + ". It takes effect for new overworld worlds after a restart.");
        } catch (Exception e) {
            plugin.getLogger().warning("Could not write height pack: " + e.getMessage());
        }
    }

    static String levelName() {
        Path props = Path.of("server.properties");
        if (Files.isRegularFile(props)) {
            try (var in = Files.newInputStream(props)) {
                Properties p = new Properties();
                p.load(in);
                return p.getProperty("level-name", "world");
            } catch (IOException ignored) {
            }
        }
        return "world";
    }
}
