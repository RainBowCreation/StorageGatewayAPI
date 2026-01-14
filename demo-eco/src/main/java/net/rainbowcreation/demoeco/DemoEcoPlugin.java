package net.rainbowcreation.demoeco;

import net.rainbowcreation.demoeco.hooks.IGLangExtHook;

import me.icegames.iglanguages.IGLanguages;
import me.icegames.iglanguages.api.IGLanguagesAPI;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public final class DemoEcoPlugin extends JavaPlugin implements CommandExecutor {

    // Create a Manager class that extends ADataManager
    private DataManager dataManager;
    private String ns;
    private String keyPrefix;
    private IGLanguagesAPI langAPI;
    private boolean isAPIEnable;
    private IGLangExtHook policyHook;

    /* ========= Model Definition ========= */


    // Cache
    private final Map<UUID, Policy> perPlayer = new ConcurrentHashMap<>();
    private Policy globalDefault = new Policy();


    /* ================== Lifecycle ================== */

    @Override public void onEnable() {
        saveDefaultConfig();

        // Config setup
        ns = getConfig().getString("namespace", "players");
        keyPrefix = getConfig().getString("currencyKeyPrefix", "coins:");

        // Default policy setup
        try { globalDefault.mode = Policy.Mode.valueOf(getConfig().getString("translationPolicy.default.mode", "ALL")); }
        catch (Exception e) { globalDefault.mode = Policy.Mode.ALL; }
        globalDefault.cats.addAll(getConfig().getStringList("translationPolicy.default.categories"));

        // --- Initialize Data Manager ---
        dataManager = new DataManager(getConfig(), getLogger());
        try {
            dataManager.initialize(); // Auto-selects Direct/TCP/API
        } catch (Exception e) {
            getLogger().severe("Failed to initialize storage: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // --- IGLanguages detection ---
        if (Bukkit.getPluginManager().isPluginEnabled("IGLanguages")) {
            this.langAPI = IGLanguages.getInstance().getAPI();
            isAPIEnable = (langAPI != null);
            if (isAPIEnable) {
                policyHook = new IGLangExtHook(this);
                policyHook.register();
            }
        }

        Optional.ofNullable(getCommand("trcat")).ifPresent(c -> c.setExecutor(this));
        Optional.ofNullable(getCommand("eco")).ifPresent(c -> c.setExecutor(this));
    }

    @Override public void onDisable() {
        if (policyHook != null) policyHook.unregister();
        if (dataManager != null) dataManager.shutdown();
    }

    /* ================== Command routing ================== */

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String l, String[] a) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        if ("trcat".equals(name)) return handleTrcat(s, a);
        if ("eco".equals(name))   return handleEco(s, a);
        return false;
    }

    /* ================== Usage Example ================== */

    private boolean handleEco(CommandSender s, String[] a) {
        Player sp = (s instanceof Player) ? (Player) s : null;
        OfflinePlayer p = Bukkit.getOfflinePlayer(a[1]);
        String key = keyPrefix + p.getUniqueId();

        // GET Example, If this run on minecraft backend CLIENT that connect to proxy server using PLUGIN_MESSAGING and connect via DIRECT_LIBRARY
        // you must call get via getAsync because minecraft messaging channel block the main thread when calling
        if ("get".equalsIgnoreCase(a[0])) {
            dataManager.getIntAsync(ns, key).thenAccept(v -> {
                int val = (v != null) ? v : 0;
                // Run on main thread to send message safely
                Bukkit.getScheduler().runTask(this, () -> {
                    s.sendMessage("Balance: " + val);
                });
            });
            return true;
        }

        // SET Example
        if ("set".equalsIgnoreCase(a[0])) {
            int amount = Integer.parseInt(a[2]);
            dataManager.setAsync(ns, key, amount).thenRun(() -> {
                Bukkit.getScheduler().runTask(this, () -> {
                    s.sendMessage("Set balance to " + amount);
                });
            });
            return true;
        }
        return true;
    }

    private boolean handleTrcat(CommandSender sender, String[] args) {
        if (args[0].equals("find")) {
            String targetMode = args[1].toUpperCase();

            Map<String, String> filters = new HashMap<>();
            filters.put("mode", targetMode);

            sender.sendMessage("§7Searching...");

            // SEARCH Example
            dataManager.getAsync("trpolicy", filters, Policy.class).thenAccept(results -> {
                // Run on main thread
                Bukkit.getScheduler().runTask(this, () -> {
                    sender.sendMessage("Found " + results.size() + " matches.");
                });
            });
            return true;
        }
        return false;
    }

    private CompletableFuture<Policy> getPolicy(UUID id) {
        Policy cached = perPlayer.get(id);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return dataManager.getAsync("trpolicy", id.toString(), Policy.class).thenApply(p -> {
            if (p == null) {
                p = copyOf(globalDefault);
                p.ownerId = id.toString();
            }
            perPlayer.put(id, p);
            return p;
        });
    }

    private Policy copyOf(Policy in) {
        Policy p = new Policy();
        p.ownerId = in.ownerId;
        p.mode = in.mode;
        p.cats.addAll(in.cats);
        return p;
    }

    public boolean decideGate(Player player, String category) {
        if (player == null) return true;
        if (category == null || category.isEmpty()) category = "uncategorized";
        category = category.toLowerCase(java.util.Locale.ROOT);
        UUID id = player.getUniqueId();

        Policy pol = perPlayer.get(id);

        if (pol == null) { getPolicy(id); pol = globalDefault; }

        switch (pol.mode) {
            case ALL: return true;
            case ONLY: return pol.cats.contains(category);
            case EXCEPT: return !pol.cats.contains(category);
            default: return true;
        }
    }
}