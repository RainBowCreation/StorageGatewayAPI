package net.rainbowcreation.demoeco.hooks;

import net.rainbowcreation.demoeco.DemoEcoPlugin;

import me.icegames.iglanguages.api.TranslationExtension;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class IGLangExtHook implements TranslationExtension {
    private final DemoEcoPlugin plugin;
    public IGLangExtHook(DemoEcoPlugin plugin) { this.plugin = plugin; }

    @Override
    public String name() {
        return "DemoEcoExt";
    }

    @Override
    public Plugin plugin() {
        return plugin;
    }

    @Override public Gate gate(Player player, String lang, String category, String key) {
        return plugin.decideGate(player, category) ? Gate.ALWAYS_ALLOW : Gate.ALWAYS_DENY;
    }
}
