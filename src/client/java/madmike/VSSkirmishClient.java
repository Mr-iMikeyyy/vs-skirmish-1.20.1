package madmike;

import madmike.skirmish.VSSkirmish;
import net.fabricmc.api.ClientModInitializer;

public class VSSkirmishClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		VSSkirmish.LOG.info("VS Skirmish client init");
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
	}
}