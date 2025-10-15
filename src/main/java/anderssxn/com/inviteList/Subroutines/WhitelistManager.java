package anderssxn.com.inviteList.Subroutines;

import anderssxn.com.inviteList.InviteList;
import anderssxn.com.inviteList.cache.WhitelistCache;
import anderssxn.com.inviteList.database.InviteDatabase;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WhitelistManager {
    
    private static InviteDatabase database;
    private static WhitelistCache cache;
    
    public static void setDatabase(InviteDatabase db) {
        database = db;
    }
    
    public static void setCache(WhitelistCache c) {
        cache = c;
    }

    public static int addToWhitelist(Player inviter, OfflinePlayer invitee) {
        UUID inviteeUUID = invitee.getUniqueId();
        UUID inviterUUID = inviter != null ? inviter.getUniqueId() : null;
        String inviterName = inviter != null ? inviter.getName() : "Console";
        
        try {
            // Check if already whitelisted
            if (database.isWhitelisted(inviteeUUID).join()) {
                return 1; // Already whitelisted
            }
            
            // Add to whitelist
            boolean success = database.addToWhitelist(
                inviteeUUID, 
                invitee.getName(), 
                inviterUUID, 
                inviterName
            ).join();
            
            // Update cache immediately
            if (success && cache != null) {
                cache.addToCache(inviteeUUID);
            }
            
            return success ? 0 : 3; // 0 = success, 3 = error
            
        } catch (Exception e) {
            e.printStackTrace();
            return 3;
        }
    }

    public static int isWhitelisted(UUID playerUUID) {
        // Use cache for instant lookup if available
        if (cache != null && cache.isLoaded()) {
            boolean whitelisted = cache.isWhitelisted(playerUUID);
            return whitelisted ? 0 : 1; // 0 = whitelisted, 1 = not whitelisted
        }
        
        // Fallback to database if cache not ready
        try {
            boolean whitelisted = database.isWhitelisted(playerUUID).join();
            return whitelisted ? 0 : 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 2; // Error
        }
    }
    
    public static boolean removeFromWhitelist(UUID playerUUID) {
        try {
            boolean removed = database.removeFromWhitelist(playerUUID).join();
            
            // Update cache immediately
            if (removed && cache != null) {
                cache.removeFromCache(playerUUID);
            }
            
            return removed;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
