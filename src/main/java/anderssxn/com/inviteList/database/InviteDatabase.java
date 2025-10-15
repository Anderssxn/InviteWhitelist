package anderssxn.com.inviteList.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import anderssxn.com.inviteList.InviteList;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class InviteDatabase {
    
    private final InviteList plugin;
    private final DatabaseType type;
    private HikariDataSource dataSource;
    
    public InviteDatabase(InviteList plugin, DatabaseType type) {
        this.plugin = plugin;
        this.type = type;
    }
    
    public void connect() {
        try {
            HikariConfig config = new HikariConfig();
            
            if (type == DatabaseType.SQLITE) {
                config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/invitelist.db");
                config.setDriverClassName("org.sqlite.JDBC");
                config.setMaximumPoolSize(1);
            } else {
                String host = plugin.getConfig().getString("database.mysql.host", "localhost");
                int port = plugin.getConfig().getInt("database.mysql.port", 3306);
                String database = plugin.getConfig().getString("database.mysql.database", "invitelist");
                String username = plugin.getConfig().getString("database.mysql.username", "root");
                String password = plugin.getConfig().getString("database.mysql.password", "");
                
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setMaximumPoolSize(10);
            }
            
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("InviteList-Pool");
            
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Connected to " + type + " database!");
            
            createTables();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database!", e);
        }
    }
    
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Disconnected from database");
        }
    }
    
    private void createTables() {
        try (Connection conn = getConnection()) {
            
            // Whitelist table
            String whitelistTable = type == DatabaseType.SQLITE ?
                "CREATE TABLE IF NOT EXISTS whitelist (" +
                "uuid TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "invited_by TEXT, " +
                "invited_at INTEGER NOT NULL" +
                ")" :
                "CREATE TABLE IF NOT EXISTS whitelist (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(16) NOT NULL, " +
                "invited_by VARCHAR(36), " +
                "invited_at BIGINT NOT NULL, " +
                "INDEX idx_inviter (invited_by)" +
                ")";
            
            // Invitations table (tracks who invited whom)
            String invitationsTable = type == DatabaseType.SQLITE ?
                "CREATE TABLE IF NOT EXISTS invitations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "inviter_uuid TEXT NOT NULL, " +
                "inviter_name TEXT NOT NULL, " +
                "invitee_uuid TEXT NOT NULL, " +
                "invitee_name TEXT NOT NULL, " +
                "invited_at INTEGER NOT NULL" +
                ")" :
                "CREATE TABLE IF NOT EXISTS invitations (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "inviter_uuid VARCHAR(36) NOT NULL, " +
                "inviter_name VARCHAR(16) NOT NULL, " +
                "invitee_uuid VARCHAR(36) NOT NULL, " +
                "invitee_name VARCHAR(16) NOT NULL, " +
                "invited_at BIGINT NOT NULL, " +
                "INDEX idx_inviter (inviter_uuid), " +
                "INDEX idx_invitee (invitee_uuid)" +
                ")";
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(whitelistTable);
                stmt.execute(invitationsTable);
                plugin.getLogger().info("Database tables created/verified");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create tables!", e);
        }
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    // ========== WHITELIST OPERATIONS ==========
    
    public CompletableFuture<Boolean> isWhitelisted(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid FROM whitelist WHERE uuid = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check whitelist", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> addToWhitelist(UUID inviteeUuid, String inviteeName, UUID inviterUuid, String inviterName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                
                // Check if already whitelisted
                String checkSql = "SELECT uuid FROM whitelist WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                    stmt.setString(1, inviteeUuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return false; // Already whitelisted
                        }
                    }
                }
                
                // Add to whitelist
                String insertSql = type == DatabaseType.SQLITE ?
                    "INSERT OR REPLACE INTO whitelist (uuid, name, invited_by, invited_at) VALUES (?, ?, ?, ?)" :
                    "INSERT INTO whitelist (uuid, name, invited_by, invited_at) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE name=?, invited_by=?, invited_at=?";
                
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    long now = System.currentTimeMillis();
                    stmt.setString(1, inviteeUuid.toString());
                    stmt.setString(2, inviteeName);
                    stmt.setString(3, inviterUuid != null ? inviterUuid.toString() : "CONSOLE");
                    stmt.setLong(4, now);
                    
                    if (type == DatabaseType.MYSQL) {
                        stmt.setString(5, inviteeName);
                        stmt.setString(6, inviterUuid != null ? inviterUuid.toString() : "CONSOLE");
                        stmt.setLong(7, now);
                    }
                    
                    stmt.executeUpdate();
                }
                
                // Record invitation
                if (inviterUuid != null) {
                    recordInvitation(conn, inviterUuid, inviterName, inviteeUuid, inviteeName, System.currentTimeMillis());
                }
                
                return true;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add to whitelist", e);
                return false;
            }
        });
    }
    
    private void recordInvitation(Connection conn, UUID inviterUuid, String inviterName, UUID inviteeUuid, String inviteeName, long timestamp) throws SQLException {
        String sql = "INSERT INTO invitations (inviter_uuid, inviter_name, invitee_uuid, invitee_name, invited_at) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, inviterUuid.toString());
            stmt.setString(2, inviterName);
            stmt.setString(3, inviteeUuid.toString());
            stmt.setString(4, inviteeName);
            stmt.setLong(5, timestamp);
            stmt.executeUpdate();
        }
    }
    
    public CompletableFuture<List<UUID>> getAllWhitelisted() {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> whitelisted = new ArrayList<>();
            String sql = "SELECT uuid FROM whitelist";
            
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    try {
                        whitelisted.add(UUID.fromString(rs.getString("uuid")));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in whitelist: " + rs.getString("uuid"));
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load whitelist", e);
            }
            
            return whitelisted;
        });
    }
    
    public CompletableFuture<Map<String, String>> getAllWhitelistedWithInviters() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> whitelisted = new LinkedHashMap<>();
            String sql = "SELECT name, invited_by FROM whitelist ORDER BY name ASC";
            
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    String name = rs.getString("name");
                    String invitedBy = rs.getString("invited_by");
                    whitelisted.put(name, invitedBy != null ? invitedBy : "CONSOLE");
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load whitelist", e);
            }
            
            return whitelisted;
        });
    }
    
    public CompletableFuture<List<String>> getPlayersInvitedBy(UUID inviterUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> invited = new ArrayList<>();
            String sql = "SELECT invitee_name FROM invitations WHERE inviter_uuid = ? ORDER BY invited_at DESC";
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, inviterUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        invited.add(rs.getString("invitee_name"));
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get invited players", e);
            }
            
            return invited;
        });
    }
    
    public CompletableFuture<String> getInvitedBy(UUID inviteeUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT invited_by FROM whitelist WHERE uuid = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, inviteeUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String inviterUuid = rs.getString("invited_by");
                        return inviterUuid != null ? inviterUuid : "CONSOLE";
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get inviter", e);
            }
            
            return "UNKNOWN";
        });
    }
    
    public CompletableFuture<Boolean> removeFromWhitelist(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                
                // Check if player is whitelisted
                String checkSql = "SELECT uuid FROM whitelist WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            return false; // Not whitelisted
                        }
                    }
                }
                
                // Remove from whitelist
                String deleteSql = "DELETE FROM whitelist WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                    stmt.setString(1, uuid.toString());
                    int affected = stmt.executeUpdate();
                    return affected > 0;
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove from whitelist", e);
                return false;
            }
        });
    }
}

