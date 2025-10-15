package anderssxn.com.inviteList.CommandExecutors;

import anderssxn.com.inviteList.InviteList;
import anderssxn.com.inviteList.MessagesManager;
import anderssxn.com.inviteList.Subroutines.WhitelistManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InviteExecutor implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        InviteList plugin = InviteList.getInstance();
        MessagesManager msg = plugin.getMessagesManager();
        
        // Check if invites are enabled
        if (!plugin.getConfig().getBoolean("invites-enabled", true)) {
            sender.sendMessage(msg.get("invite.disabled"));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(msg.get("invite.usage"));
            return true;
        }
        
        String playerName = args[0];
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        
        Player inviter = sender instanceof Player ? (Player) sender : null;
        int response = WhitelistManager.addToWhitelist(inviter, player);
        handleResponse(sender, playerName, response);
        
        return true;
    }

    private void handleResponse(CommandSender sender, String playerName, int response) {
        MessagesManager msg = InviteList.getInstance().getMessagesManager();
        
        if (response == 0) {
            sender.sendMessage(msg.get("invite.success", "{player}", playerName));
        } else if (response == 1) {
            sender.sendMessage(msg.get("invite.already-whitelisted", "{player}", playerName));
        } else {
            sender.sendMessage(msg.get("invite.error", "{player}", playerName));
        }
    }
}
