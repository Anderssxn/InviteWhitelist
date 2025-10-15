package anderssxn.com.inviteList.Listeners;

import anderssxn.com.inviteList.InviteList;
import anderssxn.com.inviteList.MessagesManager;
import anderssxn.com.inviteList.Subroutines.WhitelistManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class PlayerLoginListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID playerUUID = event.getUniqueId();
        
        // Note: Can't check isOp() here since player isn't loaded yet
        // Ops will be allowed by Minecraft's own whitelist system
        
        // Check whitelist using ultra-fast cache
        if (WhitelistManager.isWhitelisted(playerUUID) != 0) {
            // Not whitelisted - kick player
            MessagesManager msg = InviteList.getInstance().getMessagesManager();
            
            // Use modern Adventure API Component for kick message
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, 
                msg.parseMessage(msg.getRaw("login.not-whitelisted")));
        }
        // If whitelisted, event.allow() is called automatically
    }
}
