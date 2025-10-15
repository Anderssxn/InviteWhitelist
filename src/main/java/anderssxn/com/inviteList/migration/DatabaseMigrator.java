package anderssxn.com.inviteList.migration;

import anderssxn.com.inviteList.InviteList;
import anderssxn.com.inviteList.database.DatabaseType;
import anderssxn.com.inviteList.database.InviteDatabase;
import org.bukkit.command.CommandSender;

import java.sql.*;
import java.util.concurrent.CompletableFuture;

/**
 * Handles database migration from SQLite to MySQL
 */
public class DatabaseMigrator {
    
    private final InviteList plugin;
    
    public DatabaseMigrator(InviteList plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Migrate data from SQLite to MySQL
     */
    public CompletableFuture<MigrationResult> migrateToMySQL(CommandSender sender) {
        return CompletableFuture.supplyAsync(() -> {
            MigrationResult result = new MigrationResult();
            
            try {
                sender.sendMessage("§7[Migration] Starting migration from SQLite to MySQL...");
                
                // Create source (SQLite) database connection
                InviteDatabase sourceDb = new InviteDatabase(plugin, DatabaseType.SQLITE);
                sourceDb.connect();
                sender.sendMessage("§7[Migration] Connected to source database (SQLite)");
                
                // Create target (MySQL) database connection
                InviteDatabase targetDb = new InviteDatabase(plugin, DatabaseType.MYSQL);
                targetDb.connect();
                sender.sendMessage("§7[Migration] Connected to target database (MySQL)");
                
                // Migrate whitelist table
                sender.sendMessage("§7[Migration] Migrating whitelist table...");
                result.whitelistCount = migrateWhitelist(sourceDb, targetDb);
                sender.sendMessage("§a[Migration] Migrated " + result.whitelistCount + " whitelist entries");
                
                // Migrate invitations table
                sender.sendMessage("§7[Migration] Migrating invitations history...");
                result.invitationsCount = migrateInvitations(sourceDb, targetDb);
                sender.sendMessage("§a[Migration] Migrated " + result.invitationsCount + " invitation records");
                
                // Cleanup
                sourceDb.disconnect();
                targetDb.disconnect();
                
                result.success = true;
                result.message = "Migration completed successfully!";
                
            } catch (Exception e) {
                result.success = false;
                result.message = "Migration failed: " + e.getMessage();
                plugin.getLogger().severe("Migration error: " + e.getMessage());
                e.printStackTrace();
            }
            
            return result;
        });
    }
    
    private int migrateWhitelist(InviteDatabase source, InviteDatabase target) throws SQLException {
        int count = 0;
        
        String selectSql = "SELECT uuid, name, invited_by, invited_at FROM whitelist";
        String insertSql = "INSERT INTO whitelist (uuid, name, invited_by, invited_at) VALUES (?, ?, ?, ?) " +
                          "ON DUPLICATE KEY UPDATE name=?, invited_by=?, invited_at=?";
        
        try (Connection sourceConn = source.getConnection();
             Connection targetConn = target.getConnection();
             Statement selectStmt = sourceConn.createStatement();
             PreparedStatement insertStmt = targetConn.prepareStatement(insertSql);
             ResultSet rs = selectStmt.executeQuery(selectSql)) {
            
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String name = rs.getString("name");
                String invitedBy = rs.getString("invited_by");
                long invitedAt = rs.getLong("invited_at");
                
                // Insert into MySQL
                insertStmt.setString(1, uuid);
                insertStmt.setString(2, name);
                insertStmt.setString(3, invitedBy);
                insertStmt.setLong(4, invitedAt);
                insertStmt.setString(5, name);
                insertStmt.setString(6, invitedBy);
                insertStmt.setLong(7, invitedAt);
                
                insertStmt.executeUpdate();
                count++;
                
                // Log progress every 1000 entries
                if (count % 1000 == 0) {
                    plugin.getLogger().info("Migrated " + count + " whitelist entries...");
                }
            }
        }
        
        return count;
    }
    
    private int migrateInvitations(InviteDatabase source, InviteDatabase target) throws SQLException {
        int count = 0;
        
        String selectSql = "SELECT inviter_uuid, inviter_name, invitee_uuid, invitee_name, invited_at FROM invitations";
        String insertSql = "INSERT INTO invitations (inviter_uuid, inviter_name, invitee_uuid, invitee_name, invited_at) " +
                          "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection sourceConn = source.getConnection();
             Connection targetConn = target.getConnection();
             Statement selectStmt = sourceConn.createStatement();
             PreparedStatement insertStmt = targetConn.prepareStatement(insertSql);
             ResultSet rs = selectStmt.executeQuery(selectSql)) {
            
            while (rs.next()) {
                String inviterUuid = rs.getString("inviter_uuid");
                String inviterName = rs.getString("inviter_name");
                String inviteeUuid = rs.getString("invitee_uuid");
                String inviteeName = rs.getString("invitee_name");
                long invitedAt = rs.getLong("invited_at");
                
                // Insert into MySQL
                insertStmt.setString(1, inviterUuid);
                insertStmt.setString(2, inviterName);
                insertStmt.setString(3, inviteeUuid);
                insertStmt.setString(4, inviteeName);
                insertStmt.setLong(5, invitedAt);
                
                insertStmt.executeUpdate();
                count++;
                
                // Log progress every 5000 entries
                if (count % 5000 == 0) {
                    plugin.getLogger().info("Migrated " + count + " invitation records...");
                }
            }
        }
        
        return count;
    }
    
    public static class MigrationResult {
        public boolean success;
        public String message;
        public int whitelistCount;
        public int invitationsCount;
    }
}

