package dev.realisticworld;

import org.bukkit.Bukkit;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Finds vanilla data files (dimension type, pack.mcmeta) in several places. */
final class VanillaData {
    private VanillaData() {
    }

    /**
     * Order: 1. plugins/RealisticWorld/vanilla/<entry>, 2. class loaders of the server,
     * 3. jar files in cache/, versions/ and the server folder (including jars nested in bundler jars).
     */
    static byte[] find(Path overrideDir, String entry, Logger log) {
        try {
            Path o = overrideDir.resolve(entry);
            if (Files.isRegularFile(o)) {
                log.info("Using " + o + " as " + entry);
                return Files.readAllBytes(o);
            }
        } catch (IOException ignored) {
        }

        List<ClassLoader> loaders = new ArrayList<>();
        try {
            loaders.add(Bukkit.getServer().getClass().getClassLoader());
        } catch (Throwable ignored) {
        }
        loaders.add(Thread.currentThread().getContextClassLoader());
        loaders.add(ClassLoader.getSystemClassLoader());
        for (ClassLoader cl : loaders) {
            if (cl == null) continue;
            try (InputStream in = cl.getResourceAsStream(entry)) {
                if (in != null) return in.readAllBytes();
            } catch (IOException ignored) {
            }
        }

        Path root = Path.of(Bukkit.getWorldContainer().getPath()).toAbsolutePath().normalize();
        List<Path> jars = new ArrayList<>();
        collect(root.resolve("cache"), 2, jars);
        collect(root.resolve("versions"), 4, jars);
        collect(root, 1, jars);
        for (Path jar : jars) {
            try (InputStream in = Files.newInputStream(jar)) {
                byte[] r = searchZip(in, entry, 2);
                if (r != null) {
                    log.info("Found " + entry + " in " + root.relativize(jar));
                    return r;
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static void collect(Path dir, int depth, List<Path> out) {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> s = Files.walk(dir, depth)) {
            s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jar"))
                    .filter(p -> !p.toString().contains("plugins"))
                    .sorted((a, b) -> Integer.compare(rank(a), rank(b)))
                    .forEach(out::add);
        } catch (IOException ignored) {
        }
    }

    /** Vanilla/Mojang jars first, they are the most likely to hold the data. */
    private static int rank(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        if (n.startsWith("mojang")) return 0;
        if (n.startsWith("server")) return 1;
        if (n.startsWith("paper")) return 2;
        return 3;
    }

    private static byte[] searchZip(InputStream raw, String name, int depth) throws IOException {
        ZipInputStream zis = new ZipInputStream(raw);
        ZipEntry e;
        while ((e = zis.getNextEntry()) != null) {
            String n = e.getName();
            if (n.equals(name)) return zis.readAllBytes();
            if (depth > 0 && n.startsWith("META-INF/versions/") && n.endsWith(".jar")) {
                byte[] r = searchZip(new FilterInputStream(zis) {
                    @Override
                    public void close() {
                    }
                }, name, depth - 1);
                if (r != null) return r;
            }
        }
        return null;
    }
}
