package madmike.skirmish.components;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import madmike.skirmish.VSSkirmish;
import net.minecraft.util.Identifier;

public class SkirmishComponents implements ScoreboardComponentInitializer {
    private static Identifier id(String path) {
        return new Identifier(VSSkirmish.MOD_ID, path);
    }

    public static final ComponentKey<NamesComponent> NAMES =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("names"), NamesComponent.class);
    public static final ComponentKey<ReturnPointComponent> RETURN_POINTS =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("return_points"), ReturnPointComponent.class);


    @Override
    public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry scoreboardComponentFactoryRegistry) {
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(NAMES, NamesComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(RETURN_POINTS, ReturnPointComponent::new);
    }
}
