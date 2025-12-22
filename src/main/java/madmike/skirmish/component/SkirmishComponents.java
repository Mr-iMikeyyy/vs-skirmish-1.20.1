package madmike.skirmish.component;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import madmike.skirmish.VSSkirmish;
import madmike.skirmish.component.components.*;
import net.minecraft.util.Identifier;

public class SkirmishComponents implements ScoreboardComponentInitializer{
    private static Identifier id(String path) {
        return new Identifier(VSSkirmish.MOD_ID, path);
    }

    public static final ComponentKey<InventoryComponent> INVENTORY =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("inventory"), InventoryComponent.class);

    public static final ComponentKey<RefundComponent> REFUNDS =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("refunds"), RefundComponent.class);

    public static final ComponentKey<ReturnPointComponent> RETURN_POINTS =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("return_points"), ReturnPointComponent.class);

    public static final ComponentKey<StatsComponent> STATS =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("stats"), StatsComponent.class);

    public static final ComponentKey<ToggleComponent> TOGGLE =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("toggle"), ToggleComponent.class);


    @Override
    public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry scoreboardComponentFactoryRegistry) {
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(INVENTORY, InventoryComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(REFUNDS, RefundComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(RETURN_POINTS, ReturnPointComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(STATS, StatsComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(TOGGLE, ToggleComponent::new);
    }

}
