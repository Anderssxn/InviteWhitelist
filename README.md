# InviteList

**Maintain security and an active community by allowing pre-existing server members to invite other players to the whitelist.**

InviteList is a modern, high-performance whitelist management plugin for Paper/Spigot servers that empowers your community to grow organically. Instead of manually managing a whitelist, let your trusted players invite new members while maintaining full control and tracking.

[![License: CC BY-SA 4.0](https://img.shields.io/badge/License-CC%20BY--SA%204.0-lightgrey.svg)](https://creativecommons.org/licenses/by-sa/4.0/)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21+-green.svg)
![Paper](https://img.shields.io/badge/Server-Paper%2FSpigot-blue.svg)

---

## 📋 Features

### Core Features

- **Player-Driven Invites**: Allow your community to invite friends with a simple `/invite <player>` command
- **Full Invite Tracking**: See who invited whom and manage invitation chains
- **Flexible Database Support**: Choose between SQLite (zero setup) or MySQL (multi-server support)
- **High-Performance Caching**: In-memory cache for instant whitelist lookups, even with 100K+ players
- **Multi-Server Ready**: Shared MySQL database for synchronized whitelists across servers
- **Customizable Messages**: Full MiniMessage and legacy color code support for all messages

### Admin Features

- **Console Management**: Full control over the invite system from console
- **Database Migration**: One-command SQLite → MySQL migration tool
- **Toggle System**: Enable/disable invites without restarting
- **Hot Reload**: Reload config, messages, or database without server restart
- **Cache Statistics**: Monitor cache performance in real-time
- **Invitation History**: Track and audit all invitations

---

## 🚀 Installation

1. **Download** the latest `InviteList-1.0.0.jar` from releases
2. **Place** the jar file in your server's `plugins/` folder
3. **Restart** your server to generate config files
4. **Configure** `config.yml` and `messages.yml` to your liking
5. **Reload** with `/invitelist reload config` or restart again

### Requirements

- Paper 1.21+ or Spigot 1.21+
- Java 21 or higher
- (Optional) MySQL 5.7+ or MariaDB 10.3+ for multi-server setups

---

## ⚙️ Configuration

### config.yml

```yaml
# Enable or disable the invite system
invites-enabled: true

# Database Configuration
database:
  # Database type: "sqlite" or "mysql"
  type: "sqlite"

  # MySQL settings (only used if type is "mysql")
  mysql:
    host: "localhost"
    port: 3306
    database: "invitelist"
    username: "root"
    password: "password"
    max-pool-size: 10

# Cache Configuration
cache:
  # Enable auto-refresh (recommended)
  auto-refresh: true

  # How often to refresh cache from database (in seconds)
  # Recommended values:
  #   Single server: 300 (5 minutes)
  #   Multi-server (MySQL): 30-60 (30-60 seconds)
  #   High-traffic multi-server: 10-30 (10-30 seconds)
  refresh-interval-seconds: 300
```

### messages.yml

All messages are fully customizable with support for:

- **MiniMessage format**: `<red>Text</red>`, `<gradient:green:blue>Text</gradient>`, `<#FF5555>Hex colors</#FF5555>`
- **Legacy color codes**: `&c`, `&a`, `&#FF5555`, etc.
- **Mix both formats** in the same message!

**Placeholders:**

- `{player}` - Player name
- `{inviter}` - Inviter name
- `{count}` - Number count

See the generated `messages.yml` for all customization options.

---

## 📖 Commands

### Player Commands

| Command             | Description                      | Permission          | Default     |
| ------------------- | -------------------------------- | ------------------- | ----------- |
| `/invite <player>`  | Invite a player to the whitelist | `invitelist.invite` | All players |
| `/invitelist help`  | Show help menu                   | `invitelist.invite` | All players |
| `/invitelist cache` | View cache statistics            | `invitelist.invite` | All players |

### Admin Commands

| Command                          | Description                  | Permission         | Access |
| -------------------------------- | ---------------------------- | ------------------ | ------ |
| `/invitelist list`               | Show all whitelisted players | `invitelist.admin` | Ops    |
| `/invitelist invites <player>`   | See who a player has invited | `invitelist.admin` | Ops    |
| `/invitelist invitedby <player>` | See who invited a player     | `invitelist.admin` | Ops    |
| `/invitelist remove <player>`    | Remove player from whitelist | `invitelist.admin` | Ops    |

### Console-Only Commands

| Command                                           | Description                  | Access       |
| ------------------------------------------------- | ---------------------------- | ------------ |
| `/invitelist reload <config\|messages\|database>` | Hot-reload plugin data       | Console only |
| `/invitelist migrate`                             | Migrate SQLite → MySQL       | Console only |
| `/invitelist toggle`                              | Enable/disable invite system | Console only |

---

## 🔧 Usage Examples

### Basic Usage

```
# Player invites a friend
/invite Notch
> Successfully invited Notch to the whitelist!

# Check who invited a player
/invitelist invitedby Notch
> Notch was invited by steve123

# See all of someone's invites
/invitelist invites steve123
> Players invited by steve123:
>   - Notch
>   - Herobrine
> Total: 2 players
```

### Admin Management

```
# View all whitelisted players
/invitelist list

# Remove a player from whitelist
/invitelist remove BadPlayer
> Removed BadPlayer from the whitelist.

# Check cache status
/invitelist cache
> Cached Players: 1,234
> Status: Healthy
```

### Console Management

```
# Reload configuration
/invitelist reload config
> Configuration reloaded successfully!

# Disable invites temporarily
/invitelist toggle
> Invites are now DISABLED. Players cannot use /invite.

# Migrate to MySQL
/invitelist migrate
> Migration completed successfully!
> Migrated 1,234 whitelisted players
```

---

## 🌐 Multi-Server Setup

### Setting Up Shared Whitelist

1. **Install MySQL/MariaDB** on a dedicated server or hosting service

2. **Create database and user:**

   ```sql
   CREATE DATABASE invitelist;
   CREATE USER 'invitelist'@'%' IDENTIFIED BY 'your_secure_password';
   GRANT ALL PRIVILEGES ON invitelist.* TO 'invitelist'@'%';
   FLUSH PRIVILEGES;
   ```

3. **Update config.yml** on ALL servers:

   ```yaml
   database:
     type: "mysql"
     mysql:
       host: "your.mysql.server"
       port: 3306
       database: "invitelist"
       username: "invitelist"
       password: "your_secure_password"
       max-pool-size: 10

   cache:
     auto-refresh: true
     refresh-interval-seconds: 30 # Faster sync for multi-server
   ```

4. **Migrate existing data** (if needed):

   ```
   # On one server with existing SQLite data:
   /invitelist migrate
   ```

5. **Restart all servers** or reload database:
   ```
   /invitelist reload database
   ```

---

## 🎯 Performance

InviteList is designed for high-performance servers:

- **In-Memory Caching**: Whitelist lookups happen in O(1) time from RAM
- **Async Operations**: Database writes never block the main thread
- **Connection Pooling**: Efficient HikariCP database connections
- **Optimized Queries**: Prepared statements and batch operations
- **Scalable Architecture**: Tested with 100K+ whitelisted players

### Performance Tips

- **Single Server**: Use SQLite with 300s cache refresh (default)
- **Multi-Server**: Use MySQL with 30-60s cache refresh
- **High-Traffic**: Use MySQL with 10-30s cache refresh and higher pool size

---

## 🛠️ Building from Source

### Prerequisites

- JDK 21 or higher
- Git

### Build Steps

```bash
# Clone the repository
git clone https://github.com/your-repo/InviteList.git
cd InviteList

# Build with Gradle (Windows)
gradlew.bat build

# Build with Gradle (Linux/Mac)
./gradlew build

# Output jar will be in: build/libs/InviteList-1.0.0.jar
```

---

## 🐛 Troubleshooting

### Players can't join after being invited

- **Check cache status**: `/invitelist cache`
- **Verify player is whitelisted**: `/invitelist list`
- **Force cache refresh**: `/invitelist reload database`
- **Check cache interval**: Lower `refresh-interval-seconds` in config

### MySQL connection issues

- Verify MySQL is running and accessible
- Check firewall rules (port 3306)
- Test credentials manually
- Check MySQL user permissions
- Review server logs for detailed error messages

### Migration failed

- Ensure MySQL is configured correctly in `config.yml`
- Verify MySQL connection before migrating
- Check console for detailed error messages
- Make sure MySQL user has CREATE/INSERT permissions

### Cache not updating across servers

- Reduce `refresh-interval-seconds` (try 30-60s)
- Verify all servers use the same MySQL database
- Check network latency between servers and database
- Ensure system clocks are synchronized (NTP)

---

## 📊 Database Schema

InviteList automatically creates and manages these tables:

### `whitelist` Table

```sql
CREATE TABLE whitelist (
    uuid VARCHAR(36) PRIMARY KEY,
    username VARCHAR(16) NOT NULL,
    invited_by VARCHAR(36),
    invited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### `invitations` Table

```sql
CREATE TABLE invitations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    inviter_uuid VARCHAR(36),
    invited_uuid VARCHAR(36),
    invited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests, report bugs, or suggest features.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **Creative Commons Attribution-ShareAlike 4.0 International License**.

You are free to:

- **Share**: Copy and redistribute the material
- **Adapt**: Remix, transform, and build upon the material

Under the following terms:

- **Attribution**: You must give appropriate credit
- **ShareAlike**: If you remix, transform, or build upon the material, you must distribute your contributions under the same license

See the [LICENSE](LICENSE) file for full details.

---

## 👨‍💻 Author

**anderssxn**

Website: [dev.mitl.it](https://dev.mitl.it)

---

## ⭐ Support

If you find InviteList useful, please consider:

- ⭐ Starring the repository
- 🐛 Reporting bugs and issues
- 💡 Suggesting new features
- 🤝 Contributing code improvements
- 📖 Improving documentation

---

## 📝 Changelog

### Version 1.0.0

- Initial release
- SQLite and MySQL database support
- High-performance in-memory caching
- Player invite system
- Admin management commands
- Console-only administrative commands
- Database migration tool
- Customizable messages with MiniMessage support
- Multi-server support

---

## 🔮 Roadmap

Future features under consideration:

- [ ] Invite expiration system
- [ ] Invite limits per player
- [ ] Invite cooldowns
- [ ] Web interface for whitelist management
- [ ] Discord bot integration
- [ ] Invite codes system
- [ ] Temporary invites
- [ ] Whitelist application system

Have a feature request? [Open an issue](../../issues)!
