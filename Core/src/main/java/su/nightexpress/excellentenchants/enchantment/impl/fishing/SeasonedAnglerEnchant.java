package su.nightexpress.excellentenchants.enchantment.impl.fishing;

import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.TradeType;
import su.nightexpress.excellentenchants.api.enchantment.type.FishingEnchant;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDefinition;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDistribution;
import su.nightexpress.excellentenchants.enchantment.impl.GameEnchantment;
import su.nightexpress.excellentenchants.rarity.EnchantRarity;
import su.nightexpress.excellentenchants.util.ItemCategories;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.NumberUtil;

import java.io.File;

import static su.nightexpress.excellentenchants.Placeholders.GENERIC_AMOUNT;

public class SeasonedAnglerEnchant extends GameEnchantment implements FishingEnchant {

    public static final String ID = "seasoned_angler";

    private Modifier xpModifier;

    public SeasonedAnglerEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file) {
        super(plugin, file, definition(), EnchantDistribution.regular(TradeType.TAIGA_COMMON));
    }

    @NotNull
    private static EnchantDefinition definition() {
        return EnchantDefinition.create(
            "Increases amount of XP gained from fishing by " + GENERIC_AMOUNT + "%.",
            EnchantRarity.RARE,
            4,
            ItemCategories.FISHING_ROD
        );
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.xpModifier = Modifier.read(config, "Settings.XP_Modifier",
            Modifier.add(0, 25, 1, 300),
            "Amount (in percent) of additional XP from fishing.");

        this.addPlaceholder(GENERIC_AMOUNT, level -> NumberUtil.format(this.getXPPercent(level)));
    }

    public int getXPPercent(int level) {
        return (int) this.xpModifier.getValue(level);
    }

    @Override
    public boolean onFishing(@NotNull PlayerFishEvent event, @NotNull ItemStack item, int level) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return false;
        if (event.getExpToDrop() == 0) return false;

        int expDrop = event.getExpToDrop();
        int expPercent = this.getXPPercent(level);
        int expModified = (int) Math.ceil(expDrop * (1D + expPercent / 100D));

        event.setExpToDrop(expModified);
        return true;
    }
}
