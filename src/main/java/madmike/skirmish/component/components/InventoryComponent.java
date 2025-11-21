package madmike.skirmish.component.components;

import com.tiviacz.travelersbackpack.component.ComponentUtils;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

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

        // TRAVELERS BACKPACK
        private ItemStack storedBackpack = ItemStack.EMPTY;

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
    // GET / CREATE STORED INVENTORY ENTRY
    // ============================================================
    private StoredInventory getOrCreate(UUID uuid) {
        return storedInventories.computeIfAbsent(uuid, id -> new StoredInventory());
    }


    // ============================================================
    // SAVE INVENTORY
    // ============================================================
    public void saveInventory(ServerPlayerEntity player) {

        StoredInventory data = getOrCreate(player.getUuid());
        PlayerInventory inv = player.getInventory();

        // --- VANILLA ---
        for (int i = 0; i < 36; i++)
            data.storedMain.set(i, inv.getStack(i).copy());

        for (int i = 0; i < 4; i++)
            data.storedArmor.set(i, inv.armor.get(i).copy());

        data.storedOffhand.set(0, inv.offHand.get(0).copy());

        // --- BACKPACK ---
        ItemStack bc = ComponentUtils.getWearingBackpack(player);
        data.storedBackpack = bc != null ? bc.copy() : ItemStack.EMPTY;

        // --- TRINKETS ---
        data.storedTrinkets.clear();

        TrinketComponent trinkets =
                TrinketsApi.getTrinketComponent(player).orElse(null);

        if (trinkets != null) {
            for (var groupEntry : trinkets.getInventory().entrySet()) {
                String group = groupEntry.getKey();
                data.storedTrinkets.put(group, new HashMap<>());

                for (var slotEntry : groupEntry.getValue().entrySet()) {
                    String slot = slotEntry.getKey();
                    TrinketInventory originalList = slotEntry.getValue();

                    DefaultedList<ItemStack> copyList =
                            DefaultedList.ofSize(originalList.size(), ItemStack.EMPTY);

                    for (int i = 0; i < originalList.size(); i++)
                        copyList.set(i, originalList.getStack(i).copy());

                    data.storedTrinkets.get(group).put(slot, copyList);
                }
            }
        }
    }


    // ============================================================
    // RESTORE INVENTORY
    // ============================================================
    public void restoreInventory(ServerPlayerEntity player) {

        StoredInventory data = storedInventories.get(player.getUuid());
        if (data == null)
            return; // nothing to restore

        PlayerInventory inv = player.getInventory();

        // --- VANILLA ---
        for (int i = 0; i < 36; i++)
            inv.setStack(i, data.storedMain.get(i).copy());

        for (int i = 0; i < 4; i++)
            inv.armor.set(i, data.storedArmor.get(i).copy());

        inv.offHand.set(0, data.storedOffhand.get(0).copy());

        // --- BACKPACK ---
        if (!data.storedBackpack.isEmpty()) {
            ComponentUtils.equipBackpack(player, data.storedBackpack);
        }

        // --- TRINKETS ---
        TrinketComponent trinkets =
                TrinketsApi.getTrinketComponent(player).orElse(null);

        if (trinkets != null) {
            for (var groupEntry : data.storedTrinkets.entrySet()) {
                String group = groupEntry.getKey();

                for (var slotEntry : groupEntry.getValue().entrySet()) {
                    String slot = slotEntry.getKey();
                    DefaultedList<ItemStack> list = slotEntry.getValue();

                    TrinketInventory target = trinkets.getInventory().get(group).get(slot);

                    for (int i = 0; i < list.size(); i++)
                        target.setStack(i, list.get(i).copy());
                }
            }
            trinkets.update();
        }

        inv.markDirty();
        player.currentScreenHandler.sendContentUpdates();
    }


    // ============================================================
    // WRITE NBT (Full Map)
    // ============================================================
    @Override
    public void writeToNbt(NbtCompound tag) {

        NbtCompound all = new NbtCompound();

        for (var entry : storedInventories.entrySet()) {
            UUID uuid = entry.getKey();
            StoredInventory data = entry.getValue();

            NbtCompound playerTag = new NbtCompound();

            // vanilla
            Inventories.writeNbt(playerTag, data.storedMain);
            playerTag.put("Armor", Inventories.writeNbt(new NbtCompound(), data.storedArmor));
            playerTag.put("Offhand", Inventories.writeNbt(new NbtCompound(), data.storedOffhand));

            // backpack
            if (!data.storedBackpack.isEmpty())
                playerTag.put("Backpack", data.storedBackpack.writeNbt(new NbtCompound()));

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

            if (playerTag.contains("Backpack"))
                data.storedBackpack = ItemStack.fromNbt(playerTag.getCompound("Backpack"));

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
