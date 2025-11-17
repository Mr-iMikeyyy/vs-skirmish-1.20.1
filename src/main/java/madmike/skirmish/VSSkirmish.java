package madmike.skirmish;

import madmike.skirmish.command.SkirmishCommand;
import madmike.skirmish.event.SkirmishEvents;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VSSkirmish implements ModInitializer {
	public static final String MOD_ID = "vs-skirmish";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("VS Skirmish init");

		SkirmishCommand.register();

		SkirmishEvents.register();

		LOGGER.info("VS Skirmish done");
	}
}