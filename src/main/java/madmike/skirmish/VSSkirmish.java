package madmike.skirmish;

import madmike.skirmish.command.SkirmishCommand;
import madmike.skirmish.config.SkirmishConfig;
import madmike.skirmish.event.SkirmishEvents;
import madmike.skirmish.feature.SkirmishBlocks;
import madmike.skirmish.util.LoggerUtil;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VSSkirmish implements ModInitializer {
	public static final String MOD_ID = "vs-skirmish";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final LoggerUtil LOG = new LoggerUtil(LOGGER);

	@Override
	public void onInitialize() {

		LOGGER.info("VS Skirmish init");

		SkirmishConfig.load();

		SkirmishCommand.register();

		SkirmishEvents.register();

		SkirmishBlocks.init();

		LOGGER.info("VS Skirmish done");
	}
}