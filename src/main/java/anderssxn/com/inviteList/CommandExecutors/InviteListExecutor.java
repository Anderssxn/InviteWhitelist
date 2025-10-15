package anderssxn.com.inviteList.CommandExecutors;

import anderssxn.com.inviteList.InviteList;
import anderssxn.com.inviteList.MessagesManager;
import anderssxn.com.inviteList.migration.DatabaseMigrator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.UUID;

public class InviteListExecutor implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        InviteList plugin = InviteList.getInstance();
        MessagesManager msg = plugin.getMessagesManager();
        
        if (args.length == 0) {
            sender.sendMessage(msg.get("general.unknown-subcommand"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                sender.sendMessage(msg.get("help.header"));
                sender.sendMessage(msg.get("help.invite"));
                sender.sendMessage(msg.get("help.help-command"));
                sender.sendMessage(msg.get("help.list"));
                sender.sendMessage(msg.get("help.invites"));
                sender.sendMessage(msg.get("help.invitedby"));
                if (sender.hasPermission("invitelist.admin")) {
                    sender.sendMessage(msg.get("help.remove"));
                }
                if (!(sender instanceof org.bukkit.entity.Player)) {
                    sender.sendMessage(msg.get("help.reload"));
                }
                return true;
                
            case "list":
                return handleListCommand(sender);
                
            case "invites":
                if (args.length < 2) {
                    sender.sendMessage(msg.get("invites.usage"));
                    return true;
                }
                
                return handleInvitesCommand(sender, args[1]);
                
            case "invitedby":
                if (args.length < 2) {
                    sender.sendMessage(msg.get("invitedby.usage"));
                    return true;
                }
                
                return handleInvitedByCommand(sender, args[1]);
                
            case "remove":
                // Only console or ops can remove
                if (sender instanceof org.bukkit.entity.Player && !sender.hasPermission("invitelist.admin")) {
                    sender.sendMessage(msg.get("remove.no-permission"));
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage(msg.get("remove.usage"));
                    return true;
                }
                
                return handleRemoveCommand(sender, args[1]);
                
            case "reload":
                // Only console can reload
                if (sender instanceof org.bukkit.entity.Player) {
                    sender.sendMessage(msg.get("reload.console-only"));
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage(msg.get("reload.usage"));
                    return true;
                }
                
                return handleReloadCommand(sender, args[1]);
                
            case "cache":
                // Cache info command
                return handleCacheCommand(sender);
                
            case "migrate":
                // Only console can migrate
                if (sender instanceof org.bukkit.entity.Player) {
                    sender.sendMessage(msg.get("migrate.console-only"));
                    return true;
                }
                
                return handleMigrateCommand(sender);
                
            case "toggle":
                // Only console can toggle
                if (sender instanceof org.bukkit.entity.Player) {
                    sender.sendMessage(msg.get("toggle.console-only"));
                    return true;
                }
                
                return handleToggleCommand(sender);
                
            default:
                sender.sendMessage(msg.get("general.unknown-subcommand"));
                return true;
        }
    }
    
    private boolean handleInvitesCommand(CommandSender sender, String playerName) {
        InviteList plugin = InviteList.getInstance();
        MessagesManager msg = plugin.getMessagesManager();
        
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);
        if (target == null) {
            target = Bukkit.getPlayerExact(playerName);
        }
        
        if (target == null) {
            sender.sendMessage(msg.get("invites.player-not-found", "{player}", playerName));
            return true;
        }
        
        final OfflinePlayer finalTarget = target;
        
        // Run async query, then send messages on main thread
        plugin.getDatabase().getPlayersInvitedBy(finalTarget.getUniqueId()).thenAccept(invited -> {
            // Schedule messages to run on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (invited.isEmpty()) {
                    sender.sendMessage(msg.get("invites.no-invites", "{player}", finalTarget.getName()));
                } else {
                    sender.sendMessage(msg.get("invites.header", "{player}", finalTarget.getName()));
                    for (String name : invited) {
                        sender.sendMessage(msg.get("invites.list-entry", "{player}", name));
                    }
                    sender.sendMessage(msg.get("invites.footer", "{count}", String.valueOf(invited.size())));
                }
            });
        });
        
        return true;
    }
    
    private boolean handleInvitedByCommand(CommandSender sender, String playerName) {
        InviteList plugin = InviteList.getInstance();
        MessagesManager msg = plugin.getMessagesManager();
        
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);
        if (target == null) {
            target = Bukkit.getPlayerExact(playerName);
        }
        
        if (target == null) {
            sender.sendMessage(msg.get("invitedby.player-not-found", "{player}", playerName));
            return true;
        }
        
        final OfflinePlayer finalTarget = target;
        
        // Run async query, then send messages on main thread
        plugin.getDatabase().getInvitedBy(finalTarget.getUniqueId()).thenAccept(inviterUuid -> {
            // Schedule messages to run on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (inviterUuid.equals("UNKNOWN")) {
                    sender.sendMessage(msg.get("invitedby.no-data", "{player}", finalTarget.getName()));
                } else if (inviterUuid.equals("CONSOLE")) {
                    sender.sendMessage(msg.get("invitedby.console", "{player}", finalTarget.getName()));
                } else {
                    try {
                        UUID uuid = UUID.fromString(inviterUuid);
                        OfflinePlayer inviter = Bukkit.getOfflinePlayer(uuid);
                        sender.sendMessage(msg.get("invitedby.invited-by", 
                            "{player}", finalTarget.getName(),
                            "{inviter}", inviter.getName()));
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(msg.get("invitedby.unknown", 
                            "{player}", finalTarget.getName(),
                            "{inviter}", inviterUuid));
                    }
                }
            });
        });
        
        return true;
    }
    
    private boolean handleRemoveCommand(CommandSender sender, String playerName) {
        InviteList plugin = InviteList.getInstance();
        MessagesManager msg = plugin.getMessagesManager();
        
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);
        if (target == null) {
            target = Bukkit.getPlayerExact(playerName);
        }
        
        if (target == null) {
            sender.sendMessage(msg.get("remove.player-not-found", "{player}", playerName));
            return true;
        }
        
        final OfflinePlayer finalTarget = target;
        
        // Run async removal
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean removed = anderssxn.com.inviteList.Subroutines.WhitelistManager.removeFromWhitelist(finalTarget.getUniqueId());
            
            // Send result on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (removed) {
                    sender.sendMessage(msg.get("remove.success", "{player}", finalTarget.getName()));
                } else {
                    sender.sendMessage(msg.get("remove.not-whitelisted", "{player}", finalTarget.getName()));
                }
            });
        });
        
        return true;
    }
    
    private boolean handleListCommand(CommandSender sender) {
        InviteList plugin = InviteList.getInstance();
        MessagesManager msg = plugin.getMessagesManager();
        
        sender.sendMessage(msg.get("list.loading"));
        
        // Get whitelist with inviters
        plugin.getDatabase().getAllWhitelistedWithInviters().thenAccept(whitelisted -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (whitelisted.isEmpty()) {
                    sender.sendMessage(msg.get("list.empty"));
                    return;
                }
                
                sender.sendMessage(msg.get("list.header", "{count}", String.valueOf(whitelisted.size())));
                
                for (Map.Entry<String, String> entry : whitelisted.entrySet()) {
                    String playerName = entry.getKey();
                    String inviterUuid = entry.getValue();
                    
                    String inviterDisplay;
                    if (inviterUuid.equals("CONSOLE")) {
                        inviterDisplay = "Console";
                    } else {
                        try {
                            UUID uuid = UUID.fromString(inviterUuid);
                            OfflinePlayer inviter = Bukkit.getOfflinePlayer(uuid);
                            inviterDisplay = inviter.getName();
                        } catch (IllegalArgumentException e) {
                            inviterDisplay = "Unknown";
                        }
                    }
                    
                    sender.sendMessage(msg.get("list.entry", 
                        "{player}", playerName,
                        "{inviter}", inviterDisplay));
                }
                
                sender.sendMessage(msg.get("list.footer", "{count}", String.valueOf(whitelisted.size())));
            });
        });
        
        return true;
    }
    
    private boolean handleReloadCommand(CommandSender sender, String type) {
        InviteList plugin = InviteList.getInstance();
        MessagesManager msg = plugin.getMessagesManager();
        
        switch (type.toLowerCase()) {
            case "config":
                plugin.reloadConfig();
                sender.sendMessage(msg.get("reload.config-success"));
                return true;
                
            case "messages":
                plugin.getMessagesManager().reloadMessages();
                sender.sendMessage(msg.get("reload.messages-success"));
                return true;
                
            case "database":
                sender.sendMessage(msg.get("reload.database-loading"));
                plugin.reloadDatabase();
                plugin.getWhitelistCache().refresh();
                sender.sendMessage(msg.get("reload.database-success"));
                return true;
                
            default:
                sender.sendMessage(msg.get("reload.usage"));
                return true;
        }
    }
    
    private boolean handleCacheCommand(CommandSender sender) {
        InviteList plugin = InviteList.getInstance();
        MessagesManager msg = plugin.getMessagesManager();
        
        int cacheSize = plugin.getWhitelistCache().getCacheSize();
        boolean loaded = plugin.getWhitelistCache().isLoaded();
        boolean loading = plugin.getWhitelistCache().isLoading();
        
        sender.sendMessage(msg.get("cache.info-header"));
        sender.sendMessage(msg.get("cache.size", "{count}", String.valueOf(cacheSize)));
        sender.sendMessage(msg.get("cache.status", "{status}", 
            loading ? "Loading..." : (loaded ? "Loaded" : "Not Loaded")));
        
        long estimatedMemory = cacheSize * 40L / 1024; // KB
        sender.sendMessage(msg.get("cache.memory", "{memory}", estimatedMemory + " KB"));
        
        return true;
    }
    
    private boolean handleMigrateCommand(CommandSender sender) {
        InviteList plugin = InviteList.getInstance();
        MessagesManager msg = plugin.getMessagesManager();
        
        // Check current database type
        String currentType = plugin.getConfig().getString("database.type", "sqlite").toUpperCase();
        if (currentType.equals("MYSQL")) {
            sender.sendMessage(msg.get("migrate.already-mysql"));
            return true;
        }
        
        // Warn about backup
        sender.sendMessage(msg.get("migrate.warning-1"));
        sender.sendMessage(msg.get("migrate.warning-2"));
        sender.sendMessage(msg.get("migrate.warning-3"));
        sender.sendMessage(msg.get("migrate.confirm"));
        sender.sendMessage("");
        sender.sendMessage(msg.get("migrate.starting"));
        
        // Start migration
        DatabaseMigrator migrator = new DatabaseMigrator(plugin);
        migrator.migrateToMySQL(sender).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.success) {
                    sender.sendMessage(msg.get("migrate.success"));
                    sender.sendMessage(msg.get("migrate.whitelist-count", "{count}", String.valueOf(result.whitelistCount)));
                    sender.sendMessage(msg.get("migrate.invitations-count", "{count}", String.valueOf(result.invitationsCount)));
                    sender.sendMessage(msg.get("migrate.next-step-1"));
                    sender.sendMessage(msg.get("migrate.next-step-2"));
                    sender.sendMessage(msg.get("migrate.next-step-3"));
                } else {
                    sender.sendMessage(msg.get("migrate.failed", "{error}", result.message));
                }
            });
        });
        
        return true;
    }
    
    private boolean handleToggleCommand(CommandSender sender) {
        InviteList plugin = InviteList.getInstance();
        MessagesManager msg = plugin.getMessagesManager();
        
        // Toggle the setting
        boolean currentState = plugin.getConfig().getBoolean("invites-enabled", true);
        boolean newState = !currentState;
        
        plugin.getConfig().set("invites-enabled", newState);
        plugin.saveConfig();
        
        if (newState) {
            sender.sendMessage(msg.get("toggle.enabled"));
        } else {
            sender.sendMessage(msg.get("toggle.disabled"));
        }
        
        return true;
    }
}

