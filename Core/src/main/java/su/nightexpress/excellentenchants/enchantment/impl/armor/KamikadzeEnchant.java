package su.nightexpress.excellentenchants.enchantment.impl.armor;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.TradeType;
import su.nightexpress.excellentenchants.api.enchantment.meta.ChanceMeta;
import su.nightexpress.excellentenchants.api.enchantment.type.DeathEnchant;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDefinition;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDistribution;
import su.nightexpress.excellentenchants.enchantment.impl.GameEnchantment;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.rarity.EnchantRarity;
import su.nightexpress.excellentenchants.util.ItemCategories;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.manager.SimpeListener;
import su.nightexpress.nightcore.util.wrapper.UniParticle;

import java.io.File;

import static su.nightexpress.excellentenchants.Placeholders.ENCHANTMENT_CHANCE;

public class KamikadzeEnchant extends GameEnchantment implements ChanceMeta, DeathEnchant, SimpeListener {

    public static final String ID = "self_destruction";

    private Modifier explosionSize;
    private boolean  applyOnResurrect;

    private Entity exploder;

    public KamikadzeEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file) {
        super(plugin, file, definition(), EnchantDistribution.treasure(TradeType.JUNGLE_COMMON));
    }

    @NotNull
    private static EnchantDefinition definition() {
        return EnchantDefinition.create(
            ENCHANTMENT_CHANCE + "% chance to create an explosion on death.",
            EnchantRarity.RARE,
            3,
            ItemCategories.TORSO
        );
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.meta.setProbability(Probability.create(config, Modifier.add(0, 5, 1, 100)));

        this.applyOnResurrect = ConfigValue.create("Settings.Apply_On_Resurrect",
            true,
            "Sets whether or not enchantment will trigger on resurrect (when a totem is used)."
        ).read(config);

        this.explosionSize = Modifier.read(config, "Settings.Explosion.Size",
            Modifier.add(1, 1, 1, 5),
            "A size of the explosion. The more size - the bigger the damage."
        );
    }

    public boolean isApplyOnResurrect() {
        return this.applyOnResurrect;
    }

    public final double getExplosionSize(int level) {
        return this.explosionSize.getValue(level);
    }

    public boolean createExplosion(@NotNull LivingEntity entity, @NotNull ItemStack item, int level) {
        if (!this.checkTriggerChance(level)) return false;

        float size = (float) this.getExplosionSize(level);

        this.exploder = entity;
        boolean exploded = entity.getWorld().createExplosion(entity.getLocation(), size, false, false, entity);
        this.exploder = null;

        if (exploded && this.hasVisualEffects()) {
            UniParticle.of(Particle.SMOKE).play(entity.getEyeLocation(), 0.5, 0.1, 60);
            UniParticle.of(Particle.LAVA).play(entity.getEyeLocation(), 1.25, 0.1, 100);
        }

        return exploded;
    }

    @Override
    public boolean onDeath(@NotNull EntityDeathEvent event, @NotNull LivingEntity entity, ItemStack item, int level) {
        return this.createExplosion(entity, item, level);
    }

    @Override
    public boolean onResurrect(@NotNull EntityResurrectEvent event, @NotNull LivingEntity entity, @NotNull ItemStack item, int level) {
        return this.isApplyOnResurrect() && this.createExplosion(entity, item, level);
    }

    @Override
    public boolean onKill(@NotNull EntityDeathEvent event, @NotNull LivingEntity entity, @NotNull Player killer, ItemStack weapon, int level) {
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(EntityDamageByEntityEvent event) {
        if (this.exploder == null || event.getDamager() != this.exploder) return;

        if (event.getEntity() instanceof Item || event.getEntity() == this.exploder) {
            event.setCancelled(true);
        }
    }
}
