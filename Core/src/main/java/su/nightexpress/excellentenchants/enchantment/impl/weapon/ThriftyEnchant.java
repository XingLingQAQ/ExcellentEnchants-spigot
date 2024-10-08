package su.nightexpress.excellentenchants.enchantment.impl.weapon;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
import su.nightexpress.excellentenchants.util.EnchantUtils;
import su.nightexpress.excellentenchants.hook.HookPlugin;
import su.nightexpress.excellentenchants.hook.impl.MythicMobsHook;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.BukkitThing;
import su.nightexpress.nightcore.util.Plugins;
import su.nightexpress.nightcore.util.StringUtil;
import su.nightexpress.nightcore.util.Version;

import java.io.File;
import java.util.Set;

import static org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import static su.nightexpress.excellentenchants.Placeholders.ENCHANTMENT_CHANCE;

public class ThriftyEnchant extends GameEnchantment implements ChanceMeta, DeathEnchant {

    public static final String ID = "thrifty";

    private Set<EntityType>  ignoredEntityTypes;
    private Set<SpawnReason> ignoredSpawnReasons;
    private boolean          ignoreMythicMobs;

    public ThriftyEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file) {
        super(plugin, file, definition(), EnchantDistribution.treasure(TradeType.SWAMP_SPECIAL));
    }

    @NotNull
    private static EnchantDefinition definition() {
        return EnchantDefinition.create(
            ENCHANTMENT_CHANCE + "% chance for mobs to drop spawn egg.",
            EnchantRarity.LEGENDARY,
            3,
            ItemCategories.WEAPON
        );
    }

    @Override
    public boolean checkServerRequirements() {
        if (!Version.isAtLeast(Version.V1_20_R2)) {
            this.warn("Enchantment available only for version " + Version.V1_20_R2.getLocalized() + " or above!");
            return false;
        }
        return true;
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.meta.setProbability(Probability.create(config, Modifier.add(1, 1, 1, 100)));

        this.ignoredEntityTypes = ConfigValue.forSet("Settings.Ignored_Mobs",
            BukkitThing::getEntityType,
            (cfg, path, set) -> cfg.set(path, set.stream().map(Enum::name).toList()),
            Set.of(EntityType.WITHER, EntityType.ENDER_DRAGON),
            "List of mobs exempted from this enchantment."
        ).read(config);

        this.ignoredSpawnReasons = ConfigValue.forSet("Settings.Ignored_Spawn_Reasons",
            id -> StringUtil.getEnum(id, SpawnReason.class).orElse(null),
            (cfg, path, set) -> cfg.set(path, set.stream().map(Enum::name).toList()),
            Set.of(
                SpawnReason.SPAWNER_EGG,
                SpawnReason.SPAWNER,
                SpawnReason.DISPENSE_EGG
            ),
            "List of mobs exempted from this enchantment when spawned by specified reasons.",
            "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html"
        ).read(config);

        this.ignoreMythicMobs = ConfigValue.create("Settings.Ignored_MythicMobs",
            true,
            "Sets whether or not MythicMobs should be exempted from this enchantment."
        ).read(config);
    }

    @Override
    public boolean onKill(@NotNull EntityDeathEvent event, @NotNull LivingEntity entity, @NotNull Player killer, ItemStack weapon, int level) {
        if (this.ignoredEntityTypes.contains(entity.getType())) return false;
        if (this.ignoreMythicMobs && Plugins.isLoaded(HookPlugin.MYTHIC_MOBS) && MythicMobsHook.isMythicMob(entity)) return false;

        SpawnReason spawnReason = EnchantUtils.getSpawnReason(entity);
        if (spawnReason != null && this.ignoredSpawnReasons.contains(spawnReason)) return false;

        if (!this.checkTriggerChance(level)) return false;

        Material material = plugin.getServer().getItemFactory().getSpawnEgg(entity.getType());
        if (material == null) return false;

        ItemStack eggItem = new ItemStack(material);
        event.getDrops().add(eggItem);
        return true;
    }

    @Override
    public boolean onDeath(@NotNull EntityDeathEvent event, @NotNull LivingEntity entity, ItemStack item, int level) {
        return false;
    }

    @Override
    public boolean onResurrect(@NotNull EntityResurrectEvent event, @NotNull LivingEntity entity, @NotNull ItemStack item, int level) {
        return false;
    }
}
