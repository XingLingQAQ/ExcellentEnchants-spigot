package su.nightexpress.excellentenchants.enchantment.impl.weapon;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.TradeType;
import su.nightexpress.excellentenchants.api.enchantment.type.GenericEnchant;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDefinition;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDistribution;
import su.nightexpress.excellentenchants.enchantment.impl.GameEnchantment;
import su.nightexpress.excellentenchants.rarity.EnchantRarity;
import su.nightexpress.excellentenchants.util.ItemCategories;
import su.nightexpress.excellentenchants.util.EnchantUtils;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.manager.SimpeListener;
import su.nightexpress.nightcore.util.NumberUtil;

import java.io.File;

import static su.nightexpress.excellentenchants.Placeholders.GENERIC_TIME;

public class InfernusEnchant extends GameEnchantment implements GenericEnchant, SimpeListener {

    public static final String ID = "infernus";

    private Modifier fireTicks;

    public InfernusEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file) {
        super(plugin, file, definition(), EnchantDistribution.regular(TradeType.DESERT_COMMON));
    }

    @NotNull
    private static EnchantDefinition definition() {
        return EnchantDefinition.create(
            "Launched trident will ignite the enemy for " + GENERIC_TIME + "s. on hit.",
            EnchantRarity.COMMON,
            3,
            ItemCategories.TRIDENT
        );
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.fireTicks = Modifier.read(config, "Settings.Fire_Ticks",
            Modifier.add(60, 20, 1, 60 * 20),
            "Sets for how long (in ticks) entity will be ignited on hit. 20 ticks = 1 second.");

        this.addPlaceholder(GENERIC_TIME, level -> NumberUtil.format((double) this.getFireTicks(level) / 20D));
    }

    public int getFireTicks(int level) {
        return (int) this.fireTicks.getValue(level);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInfernusTridentLaunch(ProjectileLaunchEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof LivingEntity shooter)) return;
        if (!this.isAvailableToUse(shooter)) return;

        ItemStack item = trident.getItem();

        int level = EnchantUtils.getLevel(item, this.getBukkitEnchantment());
        if (level <= 0) return;
        if (this.isOutOfCharges(item)) return;

        trident.setFireTicks(Integer.MAX_VALUE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInfernusDamageApply(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (!(entity instanceof Trident trident)) return;

        ItemStack item = trident.getItem();

        int level = EnchantUtils.getLevel(item, this.getBukkitEnchantment());
        if (level <= 0 || trident.getFireTicks() <= 0) return;

        int ticks = this.getFireTicks(level);
        event.getEntity().setFireTicks(ticks);
    }
}
