package io.shantek.functions;

import io.shantek.PostOffice;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LogManager {

    private final PostOffice postOffice;
    private Connection connection;
    
    // Log type constants
    public static final String LOG_BOX_REGISTERED = "box_registered";
    public static final String LOG_BOX_CLAIMED = "box_claimed";
    public static final String LOG_BOX_REMOVED = "box_removed";
    public static final String LOG_SECONDARY_REGISTERED = "secondary_registered";
    public static final String LOG_SECONDARY_REMOVED = "secondary_removed";
    public static final String LOG_ITEM_DEPOSITED = "item_deposited";
    public static final String LOG_ITEM_WITHDRAWN = "item_withdrawn";

    public LogManager(PostOffice postOffice) {
        this.postOffice = postOffice;
        initializeDatabase();
        purgeOldLogs(); // Run purge on startup
    }

    private void initializeDatabase() {
        try {
            File dbFile = new File(postOffice.getDataFolder(), "logs.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            
            connection = DriverManager.getConnection(url);
            
            // Create logs table if it doesn't exist
            String createTableSQL = "CREATE TABLE IF NOT EXISTS logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "type TEXT NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "actor TEXT NOT NULL, " +
                    "owner TEXT, " +
                    "location TEXT, " +
                    "items TEXT, " +
                    "is_admin INTEGER DEFAULT 0" +
                    ")";
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
                
                // Create indexes for better query performance
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON logs(timestamp)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_type ON logs(type)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_actor ON logs(actor)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_owner ON logs(owner)");
                
                postOffice.getLogger().info("Log database initialized successfully");
            }
            
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.SEVERE, "Failed to initialize log database", e);
        }
    }

    /**
     * Log a box registration event
     */
    public void logBoxRegistered(Location location, UUID playerUUID) {
        String sql = "INSERT INTO logs (type, timestamp, actor, location) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, LOG_BOX_REGISTERED);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, playerUUID.toString());
            pstmt.setString(4, serializeLocation(location));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to log box registration", e);
        }
    }

    /**
     * Log a box claim event
     */
    public void logBoxClaimed(Location location, UUID ownerUUID, UUID claimedByUUID) {
        String sql = "INSERT INTO logs (type, timestamp, actor, owner, location) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, LOG_BOX_CLAIMED);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, claimedByUUID.toString());
            pstmt.setString(4, ownerUUID.toString());
            pstmt.setString(5, serializeLocation(location));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to log box claim", e);
        }
    }

    /**
     * Log a box removal event
     */
    public void logBoxRemoved(Location location, UUID ownerUUID, UUID removedByUUID) {
        String sql = "INSERT INTO logs (type, timestamp, actor, owner, location) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, LOG_BOX_REMOVED);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, removedByUUID.toString());
            pstmt.setString(4, ownerUUID.toString());
            pstmt.setString(5, serializeLocation(location));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to log box removal", e);
        }
    }

    /**
     * Log a secondary box registration
     */
    public void logSecondaryRegistered(Location location, UUID ownerUUID) {
        String sql = "INSERT INTO logs (type, timestamp, actor, owner, location) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, LOG_SECONDARY_REGISTERED);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, ownerUUID.toString());
            pstmt.setString(4, ownerUUID.toString());
            pstmt.setString(5, serializeLocation(location));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to log secondary box registration", e);
        }
    }

    /**
     * Log a secondary box removal
     */
    public void logSecondaryRemoved(Location location, UUID ownerUUID, UUID removedByUUID) {
        String sql = "INSERT INTO logs (type, timestamp, actor, owner, location) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, LOG_SECONDARY_REMOVED);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, removedByUUID.toString());
            pstmt.setString(4, ownerUUID.toString());
            pstmt.setString(5, serializeLocation(location));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to log secondary box removal", e);
        }
    }

    /**
     * Log items being deposited into a post box
     */
    public void logItemsDeposited(UUID boxOwnerUUID, UUID depositorUUID, List<ItemStack> items) {
        String sql = "INSERT INTO logs (type, timestamp, actor, owner, items) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, LOG_ITEM_DEPOSITED);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, depositorUUID.toString());
            pstmt.setString(4, boxOwnerUUID.toString());
            pstmt.setString(5, serializeItemsToString(items));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to log item deposit", e);
        }
    }

    /**
     * Log items being withdrawn from a post box
     */
    public void logItemsWithdrawn(UUID boxOwnerUUID, UUID withdrawerUUID, List<ItemStack> items, boolean isAdmin) {
        String sql = "INSERT INTO logs (type, timestamp, actor, owner, items, is_admin) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, LOG_ITEM_WITHDRAWN);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, withdrawerUUID.toString());
            pstmt.setString(4, boxOwnerUUID.toString());
            pstmt.setString(5, serializeItemsToString(items));
            pstmt.setInt(6, isAdmin ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to log item withdrawal", e);
        }
    }

    /**
     * Get admin logs with optional time filter, type filter, player filter, and pagination
     */
    public List<LogEntry> getAdminLogs(int days, int page, int entriesPerPage, String typeFilter, String playerFilter) {
        List<LogEntry> logs = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder("SELECT * FROM logs WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        // Add time filter
        if (days > 0) {
            long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
            sql.append(" AND timestamp >= ?");
            params.add(cutoffTime);
        }
        
        // Add type filter
        if (typeFilter != null) {
            sql.append(" AND type = ?");
            params.add(typeFilter);
        }
        
        // Add player filter (check both actor and owner)
        if (playerFilter != null) {
            // We need to get the UUID for the player name
            UUID playerUUID = getPlayerUUIDByName(playerFilter);
            if (playerUUID != null) {
                sql.append(" AND (actor = ? OR owner = ?)");
                params.add(playerUUID.toString());
                params.add(playerUUID.toString());
            } else {
                // Player not found, return empty
                return logs;
            }
        }
        
        // Add ordering and pagination
        sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
        params.add(entriesPerPage);
        params.add((page - 1) * entriesPerPage);
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(createLogEntryFromResultSet(rs));
            }
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to retrieve admin logs", e);
        }
        
        return logs;
    }

    /**
     * Get total count of admin logs with optional time, type, and player filters
     */
    public int getAdminLogsCount(int days, String typeFilter, String playerFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM logs WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        // Add time filter
        if (days > 0) {
            long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
            sql.append(" AND timestamp >= ?");
            params.add(cutoffTime);
        }
        
        // Add type filter
        if (typeFilter != null) {
            sql.append(" AND type = ?");
            params.add(typeFilter);
        }
        
        // Add player filter
        if (playerFilter != null) {
            UUID playerUUID = getPlayerUUIDByName(playerFilter);
            if (playerUUID != null) {
                sql.append(" AND (actor = ? OR owner = ?)");
                params.add(playerUUID.toString());
                params.add(playerUUID.toString());
            } else {
                return 0;
            }
        }
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to count admin logs", e);
        }
        
        return 0;
    }

    /**
     * Get player history (items deposited into their box)
     */
    public List<LogEntry> getPlayerHistory(UUID playerUUID, int days, int page, int entriesPerPage) {
        List<LogEntry> logs = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM logs WHERE type = ? AND owner = ?"
        );
        
        if (days > 0) {
            long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
            sql.append(" AND timestamp >= ?");
        }
        
        sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            pstmt.setString(paramIndex++, LOG_ITEM_DEPOSITED);
            pstmt.setString(paramIndex++, playerUUID.toString());
            
            if (days > 0) {
                long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
                pstmt.setLong(paramIndex++, cutoffTime);
            }
            
            pstmt.setInt(paramIndex++, entriesPerPage);
            pstmt.setInt(paramIndex++, (page - 1) * entriesPerPage);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(createLogEntryFromResultSet(rs));
            }
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to retrieve player history", e);
        }
        
        return logs;
    }

    /**
     * Get total count of player history logs with optional time filter
     */
    public int getPlayerHistoryCount(UUID playerUUID, int days) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM logs WHERE type = ? AND owner = ?"
        );
        
        if (days > 0) {
            long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
            sql.append(" AND timestamp >= ?");
        }
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            pstmt.setString(1, LOG_ITEM_DEPOSITED);
            pstmt.setString(2, playerUUID.toString());
            
            if (days > 0) {
                long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
                pstmt.setLong(3, cutoffTime);
            }
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to count player history", e);
        }
        
        return 0;
    }

    /**
     * Purge logs older than the configured retention period
     */
    public void purgeOldLogs() {
        int retentionDays = postOffice.getConfig().getInt("log-retention-days", 90);
        
        // If set to -1, never purge
        if (retentionDays == -1) {
            return;
        }

        long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000);
        String sql = "DELETE FROM logs WHERE timestamp < ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, cutoffTime);
            int deleted = pstmt.executeUpdate();
            
            if (deleted > 0) {
                postOffice.getLogger().info("Purged " + deleted + " old log entries older than " + retentionDays + " days");
                
                // Vacuum the database to reclaim space
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("VACUUM");
                }
            }
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to purge old logs", e);
        }
    }

    /**
     * Close the database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            postOffice.getLogger().log(Level.WARNING, "Failed to close log database connection", e);
        }
    }

    // Helper methods

    private LogEntry createLogEntryFromResultSet(ResultSet rs) throws SQLException {
        LogEntry entry = new LogEntry();
        entry.id = String.valueOf(rs.getInt("id"));
        entry.type = rs.getString("type");
        entry.timestamp = rs.getLong("timestamp");
        entry.actor = rs.getString("actor");
        entry.owner = rs.getString("owner");
        entry.location = rs.getString("location");
        
        String itemsStr = rs.getString("items");
        if (itemsStr != null && !itemsStr.isEmpty()) {
            entry.items = Arrays.asList(itemsStr.split(","));
        } else {
            entry.items = new ArrayList<>();
        }
        
        entry.isAdmin = rs.getInt("is_admin") == 1;
        return entry;
    }

    private String serializeLocation(Location location) {
        return location.getWorld().getName() + "," + 
               location.getBlockX() + "," + 
               location.getBlockY() + "," + 
               location.getBlockZ();
    }

    private String serializeItemsToString(List<ItemStack> items) {
        List<String> serialized = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                serialized.add(item.getAmount() + "x " + item.getType().name());
            }
        }
        return String.join(",", serialized);
    }

    private UUID getPlayerUUIDByName(String playerName) {
        // Try online players first
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                return player.getUniqueId();
            }
        }
        
        // Try offline players (this checks the cached player data)
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore()) {
            return offlinePlayer.getUniqueId();
        }
        
        return null;
    }

    public static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

    public static String formatTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " d";
        } else if (hours > 0) {
            return hours + " h";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }

    public static String getPlayerName(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            return player.getName() != null ? player.getName() : "Unknown";
        } catch (IllegalArgumentException e) {
            return "Unknown";
        }
    }

    /**
     * LogEntry class to hold log data
     */
    public static class LogEntry {
        public String id;
        public String type;
        public long timestamp;
        public String actor;
        public String owner;
        public String location;
        public List<String> items;
        public boolean isAdmin;

        public String getTypeDisplay() {
            switch (type) {
                case LOG_BOX_REGISTERED:
                    return "Box Registered";
                case LOG_BOX_CLAIMED:
                    return "Box Claimed";
                case LOG_BOX_REMOVED:
                    return "Box Removed";
                case LOG_SECONDARY_REGISTERED:
                    return "Secondary Box Registered";
                case LOG_SECONDARY_REMOVED:
                    return "Secondary Box Removed";
                case LOG_ITEM_DEPOSITED:
                    return "Items Deposited";
                case LOG_ITEM_WITHDRAWN:
                    return "Items Withdrawn";
                default:
                    return "Unknown";
            }
        }
    }

    public static String formatItemList(List<String> items) {
        return items.stream()
                .map(item -> formatItemName(item))
                .collect(Collectors.joining(", "));
    }

    public static String formatItemName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }

        String amountPrefix = "";
        String itemPart = raw;

        // Detect leading "Nx " (e.g. "3x birch_log")
        if (raw.matches("^\\d+x\\s+.*")) {
            int spaceIndex = raw.indexOf(' ');
            amountPrefix = raw.substring(0, spaceIndex + 1); // "3x "
            itemPart = raw.substring(spaceIndex + 1);        // "birch_log"
        }

        String[] parts = itemPart.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;

            formatted.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(" ");
        }

        return amountPrefix + formatted.toString().trim();
    }

}
