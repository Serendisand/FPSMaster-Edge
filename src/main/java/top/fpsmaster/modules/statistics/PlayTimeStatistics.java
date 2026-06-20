package top.fpsmaster.modules.statistics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.io.FileUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PlayTimeStatistics {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "statistics/play_time.json";
    private static final long SAVE_INTERVAL_MS = 5000L;

    private static final Map<String, Entry> entries = new HashMap<>();
    private static boolean loaded;
    private static boolean dirty;
    private static String currentKey;
    private static long lastTickMillis;
    private static long sessionMillis;
    private static long lastSaveMillis;

    public static synchronized void update(Minecraft mc) {
        ensureLoaded();
        long now = Minecraft.getSystemTime();
        String key = resolveContextKey(mc);
        if (key == null) {
            leaveContext(now);
            return;
        }

        if (!key.equals(currentKey)) {
            leaveContext(now);
            currentKey = key;
            sessionMillis = 0L;
            lastTickMillis = now;
            return;
        }

        if (lastTickMillis <= 0L) {
            lastTickMillis = now;
            return;
        }

        long delta = now - lastTickMillis;
        lastTickMillis = now;
        if (delta <= 0L) {
            return;
        }

        sessionMillis += delta;
        Entry entry = entries.computeIfAbsent(currentKey, ignored -> new Entry());
        entry.totalMillis += delta;
        String day = getDayKey();
        entry.dailyMillis.put(day, entry.dailyMillis.getOrDefault(day, 0L) + delta);
        dirty = true;

        if (now - lastSaveMillis >= SAVE_INTERVAL_MS) {
            save();
        }
    }

    public static synchronized long getDisplayMillis(Minecraft mc, int mode) {
        update(mc);
        if (currentKey == null) {
            return 0L;
        }
        Entry entry = entries.get(currentKey);
        if (mode == 1) {
            return entry == null ? 0L : entry.dailyMillis.getOrDefault(getDayKey(), 0L);
        }
        if (mode == 2) {
            return entry == null ? 0L : entry.totalMillis;
        }
        return sessionMillis;
    }

    public static synchronized void flush() {
        if (loaded) {
            save();
        }
    }

    private static void leaveContext(long now) {
        if (currentKey != null && dirty && now - lastSaveMillis >= 1000L) {
            save();
        }
        currentKey = null;
        lastTickMillis = 0L;
        sessionMillis = 0L;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            String content = FileUtils.readFile(FILE_NAME);
            if (content.trim().isEmpty()) {
                return;
            }
            JsonObject root = GSON.fromJson(content, JsonObject.class);
            if (root == null || !root.has("entries") || !root.get("entries").isJsonObject()) {
                return;
            }
            JsonObject entriesJson = root.getAsJsonObject("entries");
            for (Map.Entry<String, com.google.gson.JsonElement> element : entriesJson.entrySet()) {
                Entry entry = GSON.fromJson(element.getValue(), Entry.class);
                if (entry != null) {
                    if (entry.dailyMillis == null) {
                        entry.dailyMillis = new HashMap<>();
                    }
                    entries.put(element.getKey(), entry);
                }
            }
        } catch (FileException | JsonSyntaxException exception) {
            ClientLogger.error("Failed to load play time statistics");
        }
    }

    private static void save() {
        if (!dirty) {
            lastSaveMillis = Minecraft.getSystemTime();
            return;
        }
        try {
            JsonObject root = new JsonObject();
            JsonObject entriesJson = new JsonObject();
            for (Map.Entry<String, Entry> entry : entries.entrySet()) {
                entriesJson.add(entry.getKey(), GSON.toJsonTree(entry.getValue()));
            }
            root.add("entries", entriesJson);
            FileUtils.saveFile(FILE_NAME, GSON.toJson(root));
            dirty = false;
            lastSaveMillis = Minecraft.getSystemTime();
        } catch (FileException exception) {
            ClientLogger.error("Failed to save play time statistics");
        }
    }

    private static String resolveContextKey(Minecraft mc) {
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
            return null;
        }

        ServerData serverData = mc.getCurrentServerData();
        if (serverData != null && serverData.serverIP != null && !serverData.serverIP.trim().isEmpty()) {
            return "server:" + serverData.serverIP.trim().toLowerCase(Locale.ROOT);
        }

        String worldName = null;
        try {
            if (mc.getIntegratedServer() != null) {
                worldName = mc.getIntegratedServer().getFolderName();
            }
        } catch (Throwable ignored) {
        }
        try {
            if ((worldName == null || worldName.trim().isEmpty()) && mc.theWorld.getWorldInfo() != null) {
                worldName = mc.theWorld.getWorldInfo().getWorldName();
            }
        } catch (Throwable ignored) {
        }

        if (worldName == null || worldName.trim().isEmpty()) {
            worldName = "unknown";
        }
        return "world:" + worldName.trim().toLowerCase(Locale.ROOT);
    }

    private static String getDayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(Minecraft.getSystemTime()));
    }

    private static class Entry {
        private long totalMillis;
        private Map<String, Long> dailyMillis = new HashMap<>();
    }
}
