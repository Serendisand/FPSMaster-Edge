package top.fpsmaster.modules.statistics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.io.FileUtils;

import java.util.UUID;

public class TelemetryIdentityStatistics {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "statistics/telemetry_identity.json";

    private static boolean loaded;
    private static String instanceId;

    public static synchronized String getInstanceId() {
        ensureLoaded();
        if (instanceId == null || instanceId.trim().isEmpty()) {
            instanceId = UUID.randomUUID().toString();
            save();
        }
        return instanceId;
    }

    public static synchronized void migrateLegacyInstanceId(String legacyInstanceId) {
        if (legacyInstanceId == null || legacyInstanceId.trim().isEmpty()) {
            return;
        }
        ensureLoaded();
        if (instanceId == null || instanceId.trim().isEmpty()) {
            instanceId = legacyInstanceId.trim();
            save();
        }
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
            if (root != null && root.has("telemetryInstanceId") && !root.get("telemetryInstanceId").isJsonNull()) {
                instanceId = root.get("telemetryInstanceId").getAsString();
            }
        } catch (FileException | JsonSyntaxException exception) {
            ClientLogger.error("Failed to load telemetry identity statistics");
        }
    }

    private static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("telemetryInstanceId", instanceId);
            FileUtils.saveFile(FILE_NAME, GSON.toJson(root));
        } catch (FileException exception) {
            ClientLogger.error("Failed to save telemetry identity statistics");
        }
    }
}
