package anderssxn.com.inviteList;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessagesManager {
    
    private final InviteList plugin;
    private File messagesFile;
    private FileConfiguration messages;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    public MessagesManager(InviteList plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Auto-merge missing keys from default messages
        mergeDefaults();
    }
    
    private void mergeDefaults() {
        // Get default messages from JAR
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream == null) return;
        
        FileConfiguration defaults = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defaultStream));
        boolean modified = false;
        
        // Check and add missing sections
        for (String key : defaults.getKeys(true)) {
            if (!messages.contains(key)) {
                messages.set(key, defaults.get(key));
                modified = true;
            }
        }
        
        if (modified) {
            try {
                messages.save(messagesFile);
                plugin.getLogger().info("Messages file auto-merged with new entries!");
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save merged messages: " + e.getMessage());
            }
        }
    }
    
    public void reloadMessages() {
        loadMessages();
    }
    
    public String getRaw(String path) {
        return messages.getString(path, "Missing message: " + path);
    }
    
    public Component get(String path, String... replacements) {
        String message = getRaw(path);
        
        // Apply replacements
        if (replacements.length > 0 && replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        
        return parseMessage(message);
    }
    
    public Component parseMessage(String message) {
        // Check if message contains legacy color codes (& or &#)
        boolean hasLegacyCodes = message.contains("&");
        boolean hasMiniMessage = message.contains("<") && message.contains(">");
        
        if (hasLegacyCodes && hasMiniMessage) {
            // Mix of both formats - convert legacy first, then parse MiniMessage
            message = convertLegacyToMiniMessage(message);
            return miniMessage.deserialize(message);
        } else if (hasLegacyCodes) {
            // Only legacy codes - use legacy serializer with hex support
            message = translateHexCodes(message);
            return legacySerializer.deserialize(message);
        } else {
            // Only MiniMessage format (or plain text)
            return miniMessage.deserialize(message);
        }
    }
    
    private String convertLegacyToMiniMessage(String message) {
        // Convert hex codes first (&#RRGGBB)
        message = translateHexCodes(message);
        
        // Convert legacy color codes to MiniMessage equivalents
        message = message.replace("&0", "<black>")
                        .replace("&1", "<dark_blue>")
                        .replace("&2", "<dark_green>")
                        .replace("&3", "<dark_aqua>")
                        .replace("&4", "<dark_red>")
                        .replace("&5", "<dark_purple>")
                        .replace("&6", "<gold>")
                        .replace("&7", "<gray>")
                        .replace("&8", "<dark_gray>")
                        .replace("&9", "<blue>")
                        .replace("&a", "<green>")
                        .replace("&b", "<aqua>")
                        .replace("&c", "<red>")
                        .replace("&d", "<light_purple>")
                        .replace("&e", "<yellow>")
                        .replace("&f", "<white>")
                        .replace("&k", "<obfuscated>")
                        .replace("&l", "<bold>")
                        .replace("&m", "<strikethrough>")
                        .replace("&n", "<underlined>")
                        .replace("&o", "<italic>")
                        .replace("&r", "<reset>");
        
        return message;
    }
    
    private String translateHexCodes(String message) {
        // Convert &#RRGGBB to &x&R&R&G&G&B&B for legacy serializer
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                replacement.append("&").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        
        return buffer.toString();
    }
}

