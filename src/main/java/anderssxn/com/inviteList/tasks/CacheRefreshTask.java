package anderssxn.com.inviteList.tasks;

import anderssxn.com.inviteList.InviteList;
import anderssxn.com.inviteList.cache.WhitelistCache;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodically refreshes the whitelist cache from database
 * Ensures cache stays in sync across server restarts and manual database edits
 */
public class CacheRefreshTask extends BukkitRunnable {
    
    private final InviteList plugin;
    private final WhitelistCache cache;
    
    public CacheRefreshTask(InviteList plugin, WhitelistCache cache) {
        this.plugin = plugin;
        this.cache = cache;
    }
    
    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("cache.auto-refresh", true)) {
            return;
        }
        
        plugin.getLogger().info("Auto-refreshing whitelist cache...");
        cache.refresh();
    }
}

