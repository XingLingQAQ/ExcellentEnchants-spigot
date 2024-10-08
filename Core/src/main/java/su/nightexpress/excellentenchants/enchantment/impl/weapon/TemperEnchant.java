package su.nightexpress.excellentenchants.enchantment.impl.weapon;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.TradeType;
import su.nightexpress.excellentenchants.api.enchantment.type.CombatEnchant;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDefinition;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDistribution;
import su.nightexpress.excellentenchants.enchantment.impl.GameEnchantment;
import su.nightexpress.excellentenchants.rarity.EnchantRarity;
import su.nightexpress.excellentenchants.util.ItemCategories;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.EntityUtil;
import su.nightexpress.nightcore.util.NumberUtil;

import java.io.File;

import static su.nightexpress.excellentenchants.Placeholders.GENERIC_AMOUNT;
import static su.nightexpress.excellentenchants.Placeholders.GENERIC_RADIUS;

public class TemperEnchant extends GameEnchantment implements CombatEnchant {

    public static final String ID = "temper";

    private Modifier damageAmount;
    private Modifier damageStep;

    public TemperEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file) {
        super(plugin, file, definition(), EnchantDistribution.regular(TradeType.SNOW_SPECIAL));
    }

    @NotNull
    private static EnchantDefinition definition() {
        return EnchantDefinition.create(
            "Inflicts " + GENERIC_AMOUNT + "% more damage for each " + GENERIC_RADIUS + "❤ missing.",
            EnchantRarity.MYTHIC,
            5,
            ItemCategories.WEAPON
        );
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.damageAmount = Modifier.read(config, "Settings.Damage.Amount",
            Modifier.add(0, 5, 1, 100),
            "Extra damage (in %)"
        );

        this.damageStep = Modifier.read(config, "Settings.Damage.Step",
            Modifier.add(0.5, 0, 0),
            "Damage will be increased for every X entity's health points missing. Where X is this value.",
            "By default increases damage by 5% for every 0.5 HP missing."
        );

        this.addPlaceholder(GENERIC_AMOUNT, level -> NumberUtil.format(this.getDamageAmount(level)));
        this.addPlaceholder(GENERIC_RADIUS, level -> NumberUtil.format(this.getDamageStep(level)));
    }

    public double getDamageAmount(int level) {
        return this.damageAmount.getValue(level);
    }

    public double getDamageStep(int level) {
        return this.damageStep.getValue(level);
    }

    @Override
    public boolean onAttack(@NotNull EntityDamageByEntityEvent event, @NotNull LivingEntity damager, @NotNull LivingEntity victim, @NotNull ItemStack weapon, int level) {
        double health = damager.getHealth();
        double maxHealth = EntityUtil.getAttribute(damager, Attribute.GENERIC_MAX_HEALTH);
        if (health >= maxHealth) return false;

        double missingHealth = maxHealth - health;
        double step = this.getDamageStep(level);
        if (step == 0 || missingHealth < step) return false;

        double steps = Math.floor(missingHealth / step);
        if (steps == 0) return false;

        double percent = 1D + (this.getDamageAmount(level) * steps / 100D);

        event.setDamage(event.getDamage() * percent);
        return true;
    }

    @Override
    public boolean onProtect(@NotNull EntityDamageByEntityEvent event, @NotNull LivingEntity damager, @NotNull LivingEntity victim, @NotNull ItemStack weapon, int level) {
        return false;
    }
}
