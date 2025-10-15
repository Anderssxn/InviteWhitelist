package anderssxn.com.inviteList;

import anderssxn.com.inviteList.CommandExecutors.InviteExecutor;
import anderssxn.com.inviteList.CommandExecutors.InviteListExecutor;
import anderssxn.com.inviteList.Listeners.PlayerLoginListener;
import anderssxn.com.inviteList.Subroutines.WhitelistManager;
import anderssxn.com.inviteList.cache.WhitelistCache;
import anderssxn.com.inviteList.database.DatabaseType;
import anderssxn.com.inviteList.database.InviteDatabase;
import anderssxn.com.inviteList.tasks.CacheRefreshTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class InviteList extends JavaPlugin {
    
    private static InviteList instance;
    private InviteDatabase database;
    private WhitelistCache whitelistCache;
    private CacheRefreshTask refreshTask;
    private MessagesManager messagesManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize messages
        messagesManager = new MessagesManager(this);
        
        // Initialize database
        String dbTypeStr = getConfig().getString("database.type", "sqlite").toUpperCase();
        DatabaseType dbType;
        try {
            dbType = DatabaseType.valueOf(dbTypeStr);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid database type '" + dbTypeStr + "', defaulting to SQLite");
            dbType = DatabaseType.SQLITE;
        }
        
        database = new InviteDatabase(this, dbType);
        database.connect();
        
        // Initialize whitelist cache for high-performance lookups
        whitelistCache = new WhitelistCache(this, database);
        whitelistCache.loadCache();
        
        // Set database in WhitelistManager
        WhitelistManager.setDatabase(database);
        WhitelistManager.setCache(whitelistCache);
        
        // Start cache refresh task
        startCacheRefreshTask();
        
        // Register commands and listeners
        getCommand("invitelist").setExecutor(new InviteListExecutor());
        getCommand("invite").setExecutor(new InviteExecutor());
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(), this);
        
        getLogger().info("InviteList enabled with " + dbType + " database + in-memory cache!");
    }
    
    private void startCacheRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        
        if (!getConfig().getBoolean("cache.auto-refresh", true)) {
            getLogger().info("Cache auto-refresh is disabled");
            return;
        }
        
        long intervalSeconds = getConfig().getLong("cache.refresh-interval-seconds", 300);
        long intervalTicks = intervalSeconds * 20; // Convert seconds to ticks
        
        refreshTask = new CacheRefreshTask(this, whitelistCache);
        refreshTask.runTaskTimerAsynchronously(this, intervalTicks, intervalTicks);
        
        getLogger().info("Cache auto-refresh started (interval: " + intervalSeconds + " seconds)");
    }

    @Override
    public void onDisable() {
        // Cancel refresh task
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        
        // Disconnect database
        if (database != null) {
            database.disconnect();
        }
        
        getLogger().info("InviteList disabled!");
    }
    
    public static InviteList getInstance() {
        return instance;
    }
    
    public InviteDatabase getDatabase() {
        return database;
    }
    
    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
    
    public WhitelistCache getWhitelistCache() {
        return whitelistCache;
    }
    
    /**
     * Reload database connection and data
     */
    public void reloadDatabase() {
        getLogger().info("Reloading database...");
        
        // Disconnect old connection
        if (database != null) {
            database.disconnect();
        }
        
        // Reconnect with current config
        String dbTypeStr = getConfig().getString("database.type", "sqlite").toUpperCase();
        DatabaseType dbType;
        try {
            dbType = DatabaseType.valueOf(dbTypeStr);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid database type '" + dbTypeStr + "', defaulting to SQLite");
            dbType = DatabaseType.SQLITE;
        }
        
        database = new InviteDatabase(this, dbType);
        database.connect();
        WhitelistManager.setDatabase(database);
        
        getLogger().info("Database reloaded successfully!");
    }
}
