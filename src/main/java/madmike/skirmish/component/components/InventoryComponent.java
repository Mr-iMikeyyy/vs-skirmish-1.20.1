package madmike.skirmish.component.components;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import madmike.skirmish.VSSkirmish;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryComponent implements ComponentV3 {

    // ============================================================
    // STORED INVENTORY DATA CLASS
    // ============================================================
    private static class StoredInventory {
        // VANILLA
        private final DefaultedList<ItemStack> storedMain =
                DefaultedList.ofSize(36, ItemStack.EMPTY);
        private final DefaultedList<ItemStack> storedArmor =
                DefaultedList.ofSize(4, ItemStack.EMPTY);
        private final DefaultedList<ItemStack> storedOffhand =
                DefaultedList.ofSize(1, ItemStack.EMPTY);

        // TRINKETS: group -> slot -> list
        private final Map<String, Map<String, DefaultedList<ItemStack>>> storedTrinkets =
                new HashMap<>();
    }

    // All stored inventories by player UUID
    private final Map<UUID, StoredInventory> storedInventories = new HashMap<>();

    // Needed for constructing with Scoreboard component provider
    private final Scoreboard provider;
    private final MinecraftServer server;

    public InventoryComponent(Scoreboard provider, MinecraftServer server) {
        this.provider = provider;
        this.server = server;
    }

    // ============================================================
    // SAVE INVENTORY
    // ============================================================
    public void saveInventory(ServerPlayerEntity player) {

        StoredInventory data = new StoredInventory();
        PlayerInventory inv = player.getInventory();

        // --- VANILLA ---
        for (int i = 0; i < 36; i++)
            data.storedMain.set(i, inv.getStack(i).copy());

        for (int i = 0; i < 4; i++)
            data.storedArmor.set(i, inv.armor.get(i).copy());

        data.storedOffhand.set(0, inv.offHand.get(0).copy());

        // --- TRINKETS ---

        TrinketComponent trinkets =
                TrinketsApi.getTrinketComponent(player).orElse(null);

        if (trinkets != null) {
            for (var groupEntry : trinkets.getInventory().entrySet()) {
                String group = groupEntry.getKey();
                data.storedTrinkets.put(group, new HashMap<>());

                for (var slotEntry : groupEntry.getValue().entrySet()) {
                    String slot = slotEntry.getKey();
                    TrinketInventory originalList = slotEntry.getValue();

                    // ================== DEBUG LOGS (SAVE) ==================
                    VSSkirmish.LOGGER.info(
                            "[INV] Saving trinket slot '{}:{}' size={}",
                            group, slot, originalList.size()
                    );

                    for (int i = 0; i < originalList.size(); i++) {
                        ItemStack s = originalList.getStack(i);
                        if (!s.isEmpty()) {
                            VSSkirmish.LOGGER.info(
                                    "[INV] -> slot {} contains {}",
                                    i, s.getItem()
                            );
                        }
                    }
                    // =======================================================

                    DefaultedList<ItemStack> copyList =
                            DefaultedList.ofSize(originalList.size(), ItemStack.EMPTY);

                    for (int i = 0; i < originalList.size(); i++)
                        copyList.set(i, originalList.getStack(i).copy());

                    data.storedTrinkets.get(group).put(slot, copyList);
                }
            }
        }
        storedInventories.put(player.getUuid(), data);
    }

    // ============================================================
    // RESTORE INVENTORY
    // ============================================================
    public void restoreInventory(ServerPlayerEntity player) {

        StoredInventory data = storedInventories.remove(player.getUuid());
        if (data == null) {
            return; // nothing to restore
        }

        PlayerInventory inv = player.getInventory();

        inv.clear();

        // --- VANILLA ---
        for (int i = 0; i < 36; i++)
            inv.setStack(i, data.storedMain.get(i).copy());

        for (int i = 0; i < 4; i++)
            inv.armor.set(i, data.storedArmor.get(i).copy());

        inv.offHand.set(0, data.storedOffhand.get(0).copy());

        // --- TRINKETS ---
        TrinketComponent trinkets =
                TrinketsApi.getTrinketComponent(player).orElse(null);

        if (trinkets == null) {
            VSSkirmish.LOGGER.warn("[INV] No TrinketComponent for {}", player.getName().getString());
            return;
        }

        VSSkirmish.LOGGER.info("[INV] Restoring trinkets for {}", player.getName().getString());

        for (var groupEntry : data.storedTrinkets.entrySet()) {
            String group = groupEntry.getKey();

            Map<String, TrinketInventory> liveGroup =
                    trinkets.getInventory().get(group);

            if (liveGroup == null) {
                VSSkirmish.LOGGER.warn("[INV] Missing trinket group '{}' on restore", group);
                continue;
            }

            for (var slotEntry : groupEntry.getValue().entrySet()) {
                String slot = slotEntry.getKey();
                DefaultedList<ItemStack> savedList = slotEntry.getValue();

                TrinketInventory target = liveGroup.get(slot);
                if (target == null) {
                    VSSkirmish.LOGGER.warn("[INV] Missing trinket slot '{}:{}' on restore", group, slot);
                    continue;
                }

                VSSkirmish.LOGGER.info(
                        "[INV] Restoring trinket slot '{}:{}' savedSize={} targetSize={}",
                        group, slot, savedList.size(), target.size()
                );

                int limit = Math.min(savedList.size(), target.size());

                // CLEAR FIRST
                for (int i = 0; i < target.size(); i++) {
                    target.setStack(i, ItemStack.EMPTY);
                }

                for (int i = 0; i < limit; i++) {
                    ItemStack stack = savedList.get(i);
                    if (!stack.isEmpty()) {
                        VSSkirmish.LOGGER.info(
                                "[INV] -> slot {} inserting {}",
                                i,
                                stack.getItem()
                        );
                        target.setStack(i, stack.copy());

                    }
                }
                target.markUpdate();
            }
        }

        trinkets.update();
        inv.markDirty();
        player.currentScreenHandler.sendContentUpdates();
    }


    // ============================================================
    // WRITE NBT (Full Map)
    // ============================================================
    @Override
    public void writeToNbt(@NotNull NbtCompound tag) {

        NbtCompound all = new NbtCompound();

        for (var entry : storedInventories.entrySet()) {
            UUID uuid = entry.getKey();
            StoredInventory data = entry.getValue();

            NbtCompound playerTag = new NbtCompound();

            // vanilla
            Inventories.writeNbt(playerTag, data.storedMain);
            playerTag.put("Armor", Inventories.writeNbt(new NbtCompound(), data.storedArmor));
            playerTag.put("Offhand", Inventories.writeNbt(new NbtCompound(), data.storedOffhand));

            // trinkets
            NbtCompound trinketsTag = new NbtCompound();
            for (var groupEntry : data.storedTrinkets.entrySet()) {
                NbtCompound groupTag = new NbtCompound();

                for (var slotEntry : groupEntry.getValue().entrySet()) {
                    NbtCompound slotTag = Inventories.writeNbt(new NbtCompound(), slotEntry.getValue());
                    groupTag.put(slotEntry.getKey(), slotTag);
                }

                trinketsTag.put(groupEntry.getKey(), groupTag);
            }

            playerTag.put("Trinkets", trinketsTag);

            all.put(uuid.toString(), playerTag);
        }

        tag.put("Inventories", all);
    }


    // ============================================================
    // READ NBT (Full Map)
    // ============================================================
    @Override
    public void readFromNbt(NbtCompound tag) {

        storedInventories.clear();

        NbtCompound all = tag.getCompound("Inventories");

        for (String uuidStr : all.getKeys()) {
            UUID uuid = UUID.fromString(uuidStr);
            NbtCompound playerTag = all.getCompound(uuidStr);

            StoredInventory data = new StoredInventory();

            Inventories.readNbt(playerTag, data.storedMain);
            Inventories.readNbt(playerTag.getCompound("Armor"), data.storedArmor);
            Inventories.readNbt(playerTag.getCompound("Offhand"), data.storedOffhand);

            // Trinkets
            data.storedTrinkets.clear();
            NbtCompound trinketsTag = playerTag.getCompound("Trinkets");

            for (String group : trinketsTag.getKeys()) {
                NbtCompound groupTag = trinketsTag.getCompound(group);
                data.storedTrinkets.put(group, new HashMap<>());

                for (String slot : groupTag.getKeys()) {
                    NbtCompound slotTag = groupTag.getCompound(slot);
                    int size = slotTag.getList("Items", NbtElement.COMPOUND_TYPE).size();

                    DefaultedList<ItemStack> list =
                            DefaultedList.ofSize(size, ItemStack.EMPTY);

                    Inventories.readNbt(slotTag, list);

                    data.storedTrinkets.get(group).put(slot, list);
                }
            }

            storedInventories.put(uuid, data);
        }
    }
}
