package net.rainbowcreation.demoeco;

import net.rainbowcreation.storage.api.template.ADataManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

public class DataManager extends ADataManager {
    private final Logger logger;

    public DataManager(FileConfiguration config, Logger logger) {
        this.logger = logger;
        // Load Config
        this.dbName = config.getString("db", "main");
        this.token = config.getString("secret", "");

        // TCP Config
        this.tcpEnabled = config.getBoolean("tcp.enabled", false);
        this.tcpHost = config.getString("tcp.host", "127.0.0.1");
        this.tcpPort = config.getInt("tcp.port", 7071);

        // API Config
        this.apiBaseUrl = config.getString("api.url", "http://localhost:7070");
    }

    @Override
    public void registerClasses() {
        registerClass(Policy.class);
    }

    @Override
    protected void logInfo(String msg) { logger.info(msg); }
    @Override
    protected void logWarn(String msg) { logger.warning(msg); }
    @Override
    protected void logSevere(String msg) { logger.severe(msg); }
}