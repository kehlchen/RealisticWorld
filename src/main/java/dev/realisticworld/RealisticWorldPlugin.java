package dev.realisticworld;

import dev.realisticworld.gen.Column;
import dev.realisticworld.gen.EndGenerator;
import dev.realisticworld.gen.NetherGenerator;
import dev.realisticworld.gen.RealisticChunkGenerator;
import dev.realisticworld.gen.TerrainSampler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import dev.realisticworld.tools.Preview;

public final class RealisticWorldPlugin extends JavaPlugin {
    private static final int CONFIG_VERSION = 8;
    private Settings settings;
    private RealisticChunkGenerator generator;
    private NetherGenerator nether;
    private EndGenerator end;
    private MountainManager mountains;
    private final Set<String> busy = ConcurrentHashMap.newKeySet();

    @Override
    public void onLoad() {
        saveDefaultConfig();
        migrateConfig();
        FileConfiguration cfg = getConfig();
        validateConfig(cfg);
        settings = load(cfg);
        if (settings.tallWorld()) HeightPack.install(this, settings);
        generator = new RealisticChunkGenerator(settings);
        nether = new NetherGenerator(settings);
        end = new EndGenerator(settings);
        mountains = new MountainManager(this, generator, settings);
        if (cfg.getBoolean("default-world.enabled", true)) bindDefaultWorlds(cfg);

    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(mountains, this);
        mountains.attachLoadedWorlds();
        List<World> worlds = getServer().getWorlds();
        if (!worlds.isEmpty() && cfgBound() && worlds.get(0).getGenerator() == null && worlds.get(0).getEnvironment() == World.Environment.NORMAL) {
            getLogger().warning("The default world was already loaded without the RealisticWorld generator. "
                    + "bukkit.yml has been updated; stop the server, delete the world folder and start again.");
        }

        int pluginId = 35206;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(
                new Metrics.SimplePie("chart_id", () -> "My value")
        );
    }

    private boolean cfgBound() {
        return getConfig().getBoolean("default-world.enabled", true);
    }

    /**
     * Makes the main overworld, nether and end use this plugin's generators. Sets bukkit.yml (for every later
     * start) and also tries to patch the already loaded server configuration so the first start works too.
     */
    private void bindDefaultWorlds(FileConfiguration cfg) {
        String level = HeightPack.levelName();
        boolean existing = Files.exists(Path.of(Bukkit.getWorldContainer().getPath(), level, "level.dat"));
        bind(level, getName());
        if (cfg.getBoolean("default-world.nether", true) && settings.netherEnabled()) bind(level + "_nether", getName() + ":nether");
        if (cfg.getBoolean("default-world.end", true) && settings.endEnabled()) bind(level + "_the_end", getName() + ":end");
        if (existing) {
            getLogger().warning("World '" + level + "' already exists. Chunks that are already generated keep their "
                    + "old terrain; only new chunks use RealisticWorld, which creates visible seams. "
                    + "Use a fresh world for clean results.");
        }
    }

    private void bind(String world, String generatorName) {
        String path = "worlds." + world + ".generator";
        boolean memory = false;
        try {
            Field f = getServer().getClass().getDeclaredField("configuration");
            f.setAccessible(true);
            Object cfg = f.get(getServer());
            if (cfg instanceof ConfigurationSection section) {
                if (section.getString(path) == null) section.set(path, generatorName);
                memory = true;
            }
        } catch (Throwable ignored) {
            // not available on this server build; bukkit.yml below is the fallback
        }

        boolean disk = false;
        try {
            File file = new File("bukkit.yml");
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            if (yml.getString(path) == null) {
                yml.set(path, generatorName);
                yml.save(file);
            }
            disk = true;
        } catch (Exception e) {
            getLogger().warning("Could not write bukkit.yml: " + e.getMessage());
        }
        getLogger().info("World '" + world + "' bound to " + generatorName + " (in-memory: " + memory
                + ", bukkit.yml: " + disk + ").");
    }

    /**
     * Older config files are backed up and replaced by the current commented default. All values that still
     * exist in the new layout are carried over; values of the pre-4 layout are moved to their new paths.
     */
    private void migrateConfig() {
        // read the file itself: getConfig() would fall back to the defaults from the jar
        YamlConfiguration old = YamlConfiguration.loadConfiguration(getDataFolder().toPath().resolve("config.yml").toFile());
        int ver = old.getInt("config-version", 1);
        if (ver >= CONFIG_VERSION) return;

        String[][] moves = {
                {"sea-level", "overworld.sea-level"},
                {"terrain.horizontal-scale", "overworld.terrain.horizontal-scale"},
                {"terrain.mountain-height", "overworld.terrain.mountain-height"},
                {"terrain.mountain-intensity", "overworld.terrain.mountain-intensity"},
                {"terrain.river-width", "overworld.terrain.river-width"},
                {"features.dunes", "overworld.desert.dunes"},
                {"features.boulders", "overworld.features.boulders"},
                {"features.hoodoos", "overworld.features.hoodoos"},
                {"features.biome-extras", "overworld.features.biome-extras"},
                {"features.remove-vanilla-lava", "overworld.features.remove-vanilla-lava"},
        };

        try {
            Path file = getDataFolder().toPath().resolve("config.yml");
            Path backup = getDataFolder().toPath().resolve("config-backup-v" + ver + ".yml");
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            saveResource("config.yml", true);
            reloadConfig();
            FileConfiguration cfg = getConfig();

            Map<String, Object> keep = new LinkedHashMap<>();
            for (String key : old.getKeys(true)) {
                if (old.isConfigurationSection(key) || key.equals("config-version")) continue;
                if (cfg.contains(key) && !cfg.isConfigurationSection(key)) keep.put(key, old.get(key));
            }
            if (ver < 4) {
                for (String[] m : moves) {
                    if (old.contains(m[0])) keep.put(m[1], old.get(m[0]));
                }
            }
            // configs before version 3 had structures disabled by default
            if (ver < 3) keep.put("structures", true);

            keep.forEach(cfg::set);
            cfg.set("config-version", CONFIG_VERSION);
            saveConfig();
            getLogger().info("Config migrated from version " + ver + " to " + CONFIG_VERSION
                    + " (" + keep.size() + " values kept). The old file was saved as " + backup.getFileName() + ".");
        } catch (Exception e) {
            getLogger().warning("Config migration failed: " + e.getMessage());
        }
    }


    /** Logs a warning for every config entry that has no effect (typos) or that was clamped. */
    private void validateConfig(FileConfiguration cfg) {
        for (String dim : new String[]{"overworld", "nether", "end"}) {
            ConfigurationSection sec = cfg.getConfigurationSection(dim + ".biomes.weights");
            if (sec == null) continue;
            List<String> known = Settings.biomeKeys(dim);
            double sum = 0;
            for (String key : sec.getKeys(false)) {
                if (!known.contains(key)) {
                    getLogger().warning("config.yml: '" + dim + ".biomes.weights." + key + "' is not a biome of this dimension and is ignored.");
                    continue;
                }
                double v = sec.getDouble(key, 1.0);
                sum += Math.max(0, Math.min(10, v));
                if (v < 0 || v > 10) {
                    getLogger().warning("config.yml: '" + dim + ".biomes.weights." + key + "' = " + v + " is outside 0 to 10 and will be clamped.");
                }
            }
            if (sum <= 0 && !sec.getKeys(false).isEmpty()) {
                getLogger().warning("config.yml: every biome weight in '" + dim + "' is 0. The generator falls back to its default biomes.");
            }
        }
        for (String path : new String[]{"overworld.climate.biome-size", "nether.biome-size", "end.biome-size"}) {
            String v = cfg.getString(path);
            if (v != null && !Settings.validSize(v)) {
                getLogger().warning("config.yml: '" + path + "' = '" + v + "' is not one of small, medium, large, extreme; medium is used.");
            }
        }
        int pMin = cfg.getInt("overworld.stronghold-mountains.peak-min-y", 100), pMax = cfg.getInt("overworld.stronghold-mountains.peak-max-y", 150);
        if (pMax < pMin) {
            getLogger().warning("config.yml: overworld.stronghold-mountains.peak-max-y is below peak-min-y; peak-min-y is used for both.");
        }
        double chance = cfg.getDouble("overworld.stronghold-mountains.chance-percent", 30);
        if (chance < 0 || chance > 100) {
            getLogger().warning("config.yml: overworld.stronghold-mountains.chance-percent = " + chance + " is outside 0 to 100 and will be clamped.");
        }
        int minY = cfg.getInt("world-height.min-y", -64), height = cfg.getInt("world-height.height", 576);
        if (cfg.getBoolean("world-height.enabled", true) && (minY % 16 != 0 || height % 16 != 0)) {
            getLogger().warning("config.yml: world-height.min-y and world-height.height must be multiples of 16; the values are rounded.");
        }
    }

    private Settings load(FileConfiguration c) {
        return Settings.from(new Settings.Source() {
            @Override
            public double number(String path, double def) {
                return c.getDouble(path, def);
            }

            @Override
            public boolean bool(String path, boolean def) {
                return c.getBoolean(path, def);
            }

            @Override
            public String text(String path, String def) {
                String v = c.getString(path);
                return v == null ? def : v;
            }
        });
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        if (id != null) {
            switch (id.toLowerCase(Locale.ROOT)) {
                case "nether" -> {
                    return settings.netherEnabled() ? nether : null;
                }
                case "end", "the_end" -> {
                    return settings.endEnabled() ? end : null;
                }
                default -> {
                }
            }
        }
        return generator;
    }

    private static final String USAGE = "/rw create <name> [normal|nether|end] [seed] | /rw info | /rw tp <world> | "
            + "/rw locate <biome> [radius] | /rw preview [blocks] [seed|world] | /rw mountain <list|here|at|stronghold|scan|remove|tp> | /rw version";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(USAGE);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "tp" -> {
                if (!(sender instanceof Player p) || args.length < 2) {
                    sender.sendMessage("/rw tp <world> (players only)");
                    return true;
                }
                World w = getServer().getWorld(args[1]);
                if (w == null) {
                    sender.sendMessage("Unknown world.");
                    return true;
                }
                p.teleport(w.getSpawnLocation());
            }
            case "info" -> info(sender);
            case "locate" -> locate(sender, args);
            case "preview" -> preview(sender, args);
            case "mountain" -> mountains.command(sender, args);
            case "version" -> sender.sendMessage("RealisticWorld " + getPluginMeta().getVersion());
            default -> sender.sendMessage(USAGE);
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/rw create <name> [normal|nether|end] [seed]");
            return;
        }
        World.Environment env = World.Environment.NORMAL;
        ChunkGenerator gen = generator;
        int next = 2;
        if (args.length > 2) {
            String e = args[2].toLowerCase(Locale.ROOT);
            if (e.equals("nether")) {
                env = World.Environment.NETHER;
                gen = nether;
                next = 3;
            } else if (e.equals("end")) {
                env = World.Environment.THE_END;
                gen = end;
                next = 3;
            } else if (e.equals("normal")) {
                next = 3;
            }
        }
        WorldCreator wc = new WorldCreator(args[1]).environment(env).generator(gen);
        if (args.length > next) {
            try {
                wc.seed(Long.parseLong(args[next]));
            } catch (NumberFormatException e) {
                wc.seed(args[next].hashCode());
            }
        }
        sender.sendMessage("Creating world " + args[1] + " ...");
        World w = wc.createWorld();
        sender.sendMessage(w == null ? "World could not be created." : "World created: " + w.getName());
    }

    private void info(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return;
        }
        Location l = p.getLocation();
        World w = l.getWorld();
        if (w.getEnvironment() == World.Environment.THE_END) {
            sender.sendMessage(w.getGenerator() == end
                    ? end.describe(w, l.getBlockX(), l.getBlockZ())
                    : "This end world does not use the RealisticWorld generator.");
            return;
        }
        if (w.getEnvironment() == World.Environment.NETHER) {
            sender.sendMessage(w.getGenerator() == nether
                    ? "biome=" + nether.biomeKeyAt(w, l.getBlockX(), l.getBlockZ())
                    : "This nether world does not use the RealisticWorld generator.");
            return;
        }
        if (w.getGenerator() != generator) {
            sender.sendMessage("This world does not use the RealisticWorld generator.");
            return;
        }
        TerrainSampler s = generator.sampler(w);
        Column c = s.sample(l.getBlockX(), l.getBlockZ());
        sender.sendMessage(String.format(Locale.ROOT,
                "biome=%s height=%.1f continentalness=%.2f mountain=%.2f river/lake=%.2f temp=%.2f humidity=%.2f mountain-site=%.2f",
                TerrainSampler.keyOf(c.biome()), c.height(), c.cont(), c.mountain(), c.river(), c.temp(), c.humid(), c.site()));
    }

    /** The world a command refers to: the player's world, otherwise the first world that uses one of the generators. */
    private World contextWorld(CommandSender sender) {
        if (sender instanceof Player p) return p.getWorld();
        for (World w : getServer().getWorlds()) {
            if (w.getGenerator() == generator) return w;
        }
        return getServer().getWorlds().isEmpty() ? null : getServer().getWorlds().get(0);
    }

    private void locate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/rw locate <biome> [radius]  (overworld biome, nether biome or end zone of your current world)");
            return;
        }
        World w = contextWorld(sender);
        if (w == null) {
            sender.sendMessage("No world available.");
            return;
        }
        int cx = sender instanceof Player p ? p.getLocation().getBlockX() : 0;
        int cz = sender instanceof Player p ? p.getLocation().getBlockZ() : 0;
        String key = args[1].toLowerCase(Locale.ROOT).replace("minecraft:", "").replace(' ', '_').replace('-', '_');
        int radius;
        int step;
        java.util.function.BiFunction<Integer, Integer, String> at;
        if (w.getGenerator() == generator) {
            Biome target = TerrainSampler.biomeByKey(key);
            if (target == null) {
                sender.sendMessage("Unknown biome '" + args[1] + "'. Overworld biomes: " + String.join(", ", TerrainSampler.biomeKeys()));
                return;
            }
            if (key.equals("lush_caves") || key.equals("dripstone_caves")) {
                sender.sendMessage("Cave biomes are underground and cannot be located with this command.");
                return;
            }
            TerrainSampler s = generator.sampler(w);
            radius = 6000;
            step = key.contains("river") || key.contains("beach") || key.equals("stony_shore") ? 12 : 32;
            at = (x, z) -> s.biomeAt(x, z) == target ? key : "";
        } else if (w.getGenerator() == nether) {
            if (!NetherGenerator.biomeKeys().contains(key)) {
                sender.sendMessage("Unknown nether biome. Options: " + String.join(", ", NetherGenerator.biomeKeys()));
                return;
            }
            radius = 3000;
            step = 16;
            at = (x, z) -> nether.biomeKeyAt(w, x, z).equals(key) ? key : "";
        } else if (w.getGenerator() == end) {
            if (!EndGenerator.zoneKeys().contains(key)) {
                sender.sendMessage("Unknown end zone. Options: " + String.join(", ", EndGenerator.zoneKeys()));
                return;
            }
            radius = 6000;
            step = 32;
            at = (x, z) -> end.zoneKeyAt(w, x, z).equals(key) ? key : "";
        } else {
            sender.sendMessage("This world does not use the RealisticWorld generator.");
            return;
        }
        if (args.length > 2) {
            try {
                radius = Math.max(step, Math.min(20000, Integer.parseInt(args[2])));
            } catch (NumberFormatException e) {
                sender.sendMessage("Radius must be a number.");
                return;
            }
        }
        String who = sender.getName();
        if (!busy.add(who)) {
            sender.sendMessage("A search or preview of yours is still running.");
            return;
        }
        final int maxR = radius, st = step;
        sender.sendMessage("Searching for " + key + " within " + maxR + " blocks ...");
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            String result;
            try {
                result = spiral(at, cx, cz, maxR, st, key);
            } catch (Throwable t) {
                result = "Search failed: " + t;
            } finally {
                busy.remove(who);
            }
            final String msg = result;
            getServer().getScheduler().runTask(this, () -> sender.sendMessage(msg));
        });
    }

    /** Square rings around the start, nearest first; the first hit is reported. */
    private static String spiral(java.util.function.BiFunction<Integer, Integer, String> at, int cx, int cz, int maxR, int step, String key) {
        if (!at.apply(cx, cz).isEmpty()) return key + " is at your position.";
        for (int r = step; r <= maxR; r += step) {
            for (int i = -r; i <= r; i += step) {
                int[][] pts = {{cx + i, cz - r}, {cx + i, cz + r}, {cx - r, cz + i}, {cx + r, cz + i}};
                for (int[] p : pts) {
                    if (!at.apply(p[0], p[1]).isEmpty()) {
                        return String.format(Locale.ROOT, "Nearest %s: x=%d z=%d (about %d blocks away). Use /tp %d ~ %d.",
                                key, p[0], p[1], (int) Math.hypot(p[0] - cx, p[1] - cz), p[0], p[1]);
                    }
                }
            }
        }
        return "No " + key + " found within " + maxR + " blocks (searched in steps of " + step + ").";
    }

    private void preview(CommandSender sender, String[] args) {
        int blocks = 2048;
        if (args.length > 1) {
            try {
                blocks = Math.max(256, Math.min(16384, Integer.parseInt(args[1])));
            } catch (NumberFormatException e) {
                sender.sendMessage("/rw preview [blocks 256-16384] [seed|world]");
                return;
            }
        }
        World w = null;
        long seed = 0;
        String name;
        if (args.length > 2) {
            w = getServer().getWorld(args[2]);
            if (w == null) {
                try {
                    seed = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    seed = args[2].hashCode();
                }
            }
        } else {
            World ctx = contextWorld(sender);
            if (ctx != null && ctx.getGenerator() == generator) w = ctx;
            else if (ctx != null) seed = ctx.getSeed();
        }
        name = w != null ? w.getName() : "seed" + seed;
        TerrainSampler sampler = w != null && w.getGenerator() == generator ? generator.sampler(w)
                : new TerrainSampler(w != null ? w.getSeed() : seed, settings, settings.minY(), settings.minY() + settings.height());
        int cx = sender instanceof Player p ? p.getLocation().getBlockX() : 0;
        int cz = sender instanceof Player p ? p.getLocation().getBlockZ() : 0;
        String who = sender.getName();
        if (!busy.add(who)) {
            sender.sendMessage("A search or preview of yours is still running.");
            return;
        }
        final int size = blocks;
        File out = new File(getDataFolder(), "previews/preview-" + name.replaceAll("[^A-Za-z0-9_.-]", "_") + "-" + size + ".png");
        sender.sendMessage("Rendering " + size + "x" + size + " blocks around x=" + cx + " z=" + cz + " (overworld, "
                + name + ") ...");
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            String result;
            try {
                Preview.render(sampler, settings.seaLevel(), cx, cz, size, 1024, out);
                result = "Preview saved: " + out.getPath();
            } catch (Throwable t) {
                result = "Preview failed: " + t;
            } finally {
                busy.remove(who);
            }
            final String msg = result;
            getServer().getScheduler().runTask(this, () -> sender.sendMessage(msg));
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filter(List.of("create", "info", "tp", "locate", "preview", "mountain", "version"), args[0]);
        if (args[0].equalsIgnoreCase("mountain")) return mountains.complete(args);
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && sub.equals("tp")) return filter(worldNames(), args[1]);
        if (args.length == 3 && sub.equals("create")) return filter(List.of("normal", "nether", "end"), args[2]);
        if (args.length == 3 && sub.equals("preview")) return filter(worldNames(), args[2]);
        if (args.length == 2 && sub.equals("locate")) {
            World w = contextWorld(sender);
            java.util.List<String> keys = new java.util.ArrayList<>();
            if (w != null && w.getGenerator() == nether) keys.addAll(NetherGenerator.biomeKeys());
            else if (w != null && w.getGenerator() == end) keys.addAll(EndGenerator.zoneKeys());
            else keys.addAll(TerrainSampler.biomeKeys());
            return filter(keys, args[1]);
        }
        return List.of();
    }

    private List<String> worldNames() {
        return getServer().getWorlds().stream().map(World::getName).toList();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }
}
