package anderssxn.com.inviteList.cache;

import anderssxn.com.inviteList.InviteList;
import anderssxn.com.inviteList.database.InviteDatabase;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-performance in-memory whitelist cache
 * Optimized for instant lookups even with millions of players
 */
public class WhitelistCache {
    
    private final InviteList plugin;
    private final InviteDatabase database;
    
    // HashSet for O(1) lookup performance
    private final Set<UUID> whitelistedUUIDs = ConcurrentHashMap.newKeySet();
    
    private final AtomicBoolean isLoaded = new AtomicBoolean(false);
    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    
    public WhitelistCache(InviteList plugin, InviteDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }
    
    /**
     * Load whitelist into cache from database
     * This is async and won't block the main thread
     */
    public void loadCache() {
        if (isLoading.get()) {
            plugin.getLogger().warning("Cache is already loading!");
            return;
        }
        
        isLoading.set(true);
        long startTime = System.currentTimeMillis();
        
        plugin.getLogger().info("Loading whitelist cache from database...");
        
        database.getAllWhitelisted().thenAccept(uuids -> {
            whitelistedUUIDs.clear();
            whitelistedUUIDs.addAll(uuids);
            
            isLoaded.set(true);
            isLoading.set(false);
            
            long loadTime = System.currentTimeMillis() - startTime;
            plugin.getLogger().info("Loaded " + whitelistedUUIDs.size() + " UUIDs into cache (" + loadTime + "ms)");
            
            // Log memory usage
            long estimatedMemory = whitelistedUUIDs.size() * 40L; // ~40 bytes per UUID in HashSet
            plugin.getLogger().info("Estimated cache memory usage: " + (estimatedMemory / 1024) + " KB");
            
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Failed to load whitelist cache: " + ex.getMessage());
            isLoading.set(false);
            return null;
        });
    }
    
    /**
     * Check if a player is whitelisted (instant O(1) lookup)
     * @param uuid Player UUID
     * @return true if whitelisted
     */
    public boolean isWhitelisted(UUID uuid) {
        if (!isLoaded.get()) {
            plugin.getLogger().warning("Cache not loaded yet! Falling back to database query...");
            // Fallback to database if cache not ready
            return database.isWhitelisted(uuid).join();
        }
        
        return whitelistedUUIDs.contains(uuid);
    }
    
    /**
     * Add player to cache (call after database insert)
     */
    public void addToCache(UUID uuid) {
        whitelistedUUIDs.add(uuid);
        plugin.getLogger().fine("Added " + uuid + " to cache");
    }
    
    /**
     * Remove player from cache (call after database delete)
     */
    public void removeFromCache(UUID uuid) {
        whitelistedUUIDs.remove(uuid);
        plugin.getLogger().fine("Removed " + uuid + " from cache");
    }
    
    /**
     * Get cache size
     */
    public int getCacheSize() {
        return whitelistedUUIDs.size();
    }
    
    /**
     * Check if cache is loaded and ready
     */
    public boolean isLoaded() {
        return isLoaded.get();
    }
    
    /**
     * Check if cache is currently loading
     */
    public boolean isLoading() {
        return isLoading.get();
    }
    
    /**
     * Clear cache (useful for troubleshooting)
     */
    public void clearCache() {
        whitelistedUUIDs.clear();
        isLoaded.set(false);
        plugin.getLogger().info("Cache cleared");
    }
    
    /**
     * Refresh cache from database
     */
    public void refresh() {
        loadCache();
    }
}

