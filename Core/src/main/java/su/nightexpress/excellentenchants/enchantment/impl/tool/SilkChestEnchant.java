package su.nightexpress.excellentenchants.enchantment.impl.tool;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.Placeholders;
import su.nightexpress.excellentenchants.api.enchantment.TradeType;
import su.nightexpress.excellentenchants.api.enchantment.type.BlockDropEnchant;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDefinition;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDistribution;
import su.nightexpress.excellentenchants.enchantment.impl.GameEnchantment;
import su.nightexpress.excellentenchants.rarity.EnchantRarity;
import su.nightexpress.excellentenchants.util.ItemCategories;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.manager.SimpeListener;
import su.nightexpress.nightcore.util.ItemReplacer;
import su.nightexpress.nightcore.util.PDCUtil;
import su.nightexpress.nightcore.util.Plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class SilkChestEnchant extends GameEnchantment implements BlockDropEnchant, SimpeListener {

    public static final String ID = "silk_chest";

    private       String        chestName;
    private       List<String>  chestLore;
    private final NamespacedKey keyChest;

    public SilkChestEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file) {
        super(plugin, file, definition(), EnchantDistribution.regular(TradeType.PLAINS_SPECIAL));

        this.keyChest = new NamespacedKey(plugin, ID + ".item");
    }

    @NotNull
    private static EnchantDefinition definition() {
        return EnchantDefinition.create(
            "Drop chests and saves all its content.",
            EnchantRarity.MYTHIC,
            1,
            ItemCategories.TOOL,
            ItemCategories.AXE
        );
    }

    @Override
    public boolean checkServerRequirements() {
        if (Plugins.isSpigot()) {
            this.warn("Enchantment can only be used in PaperMC or Paper based forks.");
            return false;
        }
        return true;
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.chestName = ConfigValue.create("Settings.Chest_Item.Name", "Chest (" + Placeholders.GENERIC_AMOUNT + " items)",
            "Chest item display name.",
            "Use '" + Placeholders.GENERIC_AMOUNT + "' for items amount."
        ).read(config);

        this.chestLore = ConfigValue.create("Settings.Chest_Item.Lore", new ArrayList<>(),
            "Chest item lore.",
            "Use '" + Placeholders.GENERIC_AMOUNT + "' for items amount."
        ).read(config);
    }

    public boolean isSilkChest(@NotNull ItemStack item) {
        return PDCUtil.getBoolean(item, this.keyChest).isPresent();
    }

    @NotNull
    public ItemStack getSilkChest(@NotNull Chest chest) {
        ItemStack chestStack = new ItemStack(chest.getType());

        BlockStateMeta stateMeta = (BlockStateMeta) chestStack.getItemMeta();
        if (stateMeta == null) return chestStack;

        Chest chestItem = (Chest) stateMeta.getBlockState();
        chestItem.getBlockInventory().setContents(chest.getBlockInventory().getContents());
        chestItem.update(true);

        int amount = (int) Stream.of(chestItem.getBlockInventory().getContents()).filter(i -> i != null && !i.getType().isAir()).count();

        stateMeta.setBlockState(chestItem);
        stateMeta.setDisplayName(this.chestName);
        stateMeta.setLore(this.chestLore);
        chestStack.setItemMeta(stateMeta);

        ItemReplacer.replace(chestStack, str -> str.replace(Placeholders.GENERIC_AMOUNT, String.valueOf(amount)));
        PDCUtil.set(chestStack, this.keyChest, true);
        return chestStack;
    }

    @Override
    public boolean onDrop(@NotNull BlockDropItemEvent event,
                          @NotNull LivingEntity player, @NotNull ItemStack item, int level) {
        BlockState state = event.getBlockState();

        if (!(state instanceof Chest chest)) return false;

        // Remove original chest from drops to prevent duplication.
        AtomicBoolean originRemoved = new AtomicBoolean(false);
        event.getItems().removeIf(drop -> drop.getItemStack().getType() == state.getType() && drop.getItemStack().getAmount() == 1 && !originRemoved.getAndSet(true));
        // Add chest content back to the chest.
        chest.getBlockInventory().addItem(event.getItems().stream().map(Item::getItemStack).toList().toArray(new ItemStack[0]));
        // Drop nothing of chest content.
        event.getItems().clear();

        if (chest.getBlockInventory().isEmpty()) {
            this.plugin.populateResource(event, new ItemStack(chest.getType()));
            return false;
        }

        this.plugin.populateResource(event, this.getSilkChest(chest));

        chest.getBlockInventory().clear();

        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSilkChestPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType().isAir()) return;

        Block block = event.getBlockPlaced();
        BlockState state = block.getState();
        if (!(state instanceof Chest chest)) return;

        chest.setCustomName(null);
        chest.update(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSilkChestStore(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.CRAFTING) {
            int hotkey = event.getHotbarButton();
            if (hotkey >= 0) {
                Player player = (Player) event.getWhoClicked();
                ItemStack hotItem = player.getInventory().getItem(hotkey);
                if (hotItem != null && this.isSilkChest(hotItem)) {
                    event.setCancelled(true);
                    return;
                }
            }

            ItemStack item = event.getCurrentItem();
            if (item == null) return;

            if (this.isSilkChest(item)) {
                event.setCancelled(true);
                return;
            }

            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        boolean isRightClick = (event.isRightClick() && !event.isShiftClick()) || event.getClick() == ClickType.CREATIVE;
        if (item.getType() == Material.BUNDLE && isRightClick) {
            ItemStack cursor = event.getView().getCursor(); // Creative is shit, undetectable.
            if (cursor != null && this.isSilkChest(cursor)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSilkChestHopper(InventoryPickupItemEvent event) {
        event.setCancelled(this.isSilkChest(event.getItem().getItemStack()));
    }
}
