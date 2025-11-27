package madmike.skirmish.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import madmike.skirmish.VSSkirmish;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.*;

public class SkirmishConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("vs-skirmish.toml");

    public static int skirmishChallengeMaxTime;
    public static int skirmishMaxTime;

    public static final Set<String> ENTITY_WHITELIST = new HashSet<>();

    public static void load() {
        try {
            CommentedFileConfig config = CommentedFileConfig.builder(CONFIG_PATH)
                    .autosave()
                    .sync()
                    .writingMode(WritingMode.REPLACE)
                    .build();

            config.load();
            VSSkirmish.LOG.info("Loaded config from: {}", CONFIG_PATH);

            // Write defaults if missing
            if (!config.contains("skirmishChallengeMaxTime")) {
                config.setComment("skirmishChallengeMaxTime", "How many seconds should a skirmish challenge be acceptable for.");
                config.set("skirmishChallengeMaxTime", 60);
            }
            if (!config.contains("skirmishMaxTime")) {
                config.setComment("skirmishMaxTime", "How many seconds should a skirmish last before a draw.");
                config.set("skirmishMaxTime", 300);
            }

            // === Entity Whitelist ===
            if (!config.contains("entityWhitelist")) {
                config.setComment("entityWhitelist",
                        "Entities allowed to spawn in the skirmish dimension.\n" +
                                "Use full IDs like 'minecraft:armor_stand' or 'mymod:custom_minion'.");
                config.set("entityWhitelist", Collections.emptyList());
            }

            // Save and close
            config.save();
            config.close();

            // === Load values into memory ===
            skirmishChallengeMaxTime = config.getInt("skirmishChallengeMaxTime");
            skirmishMaxTime = config.getInt("skirmishMaxTime");

            VSSkirmish.LOG.info("Config values - Challenge Max Time: {}s, Max Time: {}s", 
                    skirmishChallengeMaxTime, skirmishMaxTime);

            // === Load entity whitelist ===
            ENTITY_WHITELIST.clear();
            List<String> whitelist = config.get("entityWhitelist");
            if (whitelist != null) {
                ENTITY_WHITELIST.addAll(whitelist);
                VSSkirmish.LOG.info("Loaded {} entities in whitelist", whitelist.size());
            }
        } catch (Exception e) {
            VSSkirmish.LOG.error("Failed to load config", e);
        }
    }
}
