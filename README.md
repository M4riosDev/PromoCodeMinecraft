# PromoCode Mod — Minecraft Forge 1.16.5

A complete promo code system for Minecraft servers. Admins create codes with item rewards, players redeem them for cool loot!

---

## Features
✅ `/promo` GUI for easy code redemption  
✅ Multiple items per code  
✅ Set max uses and expiration dates  
✅ Create/delete codes in-game or via config  
✅ Anti-abuse: each player redeems each code once  
✅ Live statistics (redeemed count, player count)  
✅ Works offline and online  
✅ Data auto-saved to `promocodes_data.json`

---

## Installation

1. Download the mod JAR
2. Place in your Forge 1.16.5 `mods/` folder
3. Start server — config auto-creates at `server_config/promocodemod-server.toml`
4. Configure default codes in the `.toml` file, or use commands to add them

---

## Commands

### Player Commands
```
/promo                    Opens the redeem GUI
/promocode                Same as /promo
/promo help               Show all available commands
```

### Admin Commands (Op level 2+)

**Create Simple Code (1 item):**
```
/promo create CODE item count [maxUses] [expiryEpoch]
```
Examples:
```
/promo create WELCOME minecraft:diamond 5
/promo create SUMMER minecraft:golden_apple 10 100 0
```

**Create Multi-Item Code (2-3 items):**
```
/promo create CODE item1 count1 item2 count2 [item3 count3] [maxUses] [expiryEpoch]
```
Example:
```
/promo create STARTER minecraft:diamond 3 minecraft:iron_sword 1 minecraft:bread 10 150 0
```

**Create Advanced Code (unlimited items):**
```
/promo createraw CODE:item1,count1;item2,count2;item3,count3|maxUses|expiryEpoch
```
Example:
```
/promo createraw MEGA:minecraft:diamond,10;minecraft:emerald,5;minecraft:golden_apple,3|50|0
```

**Delete Code:**
```
/promo delete CODE
```
Example:
```
/promo delete WELCOME
```

---

## Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `CODE` | Code name (letters, numbers, _, -) | `WELCOME`, `SUMMER2024` |
| `item` | Item ID (namespace:name) | `minecraft:diamond` |
| `count` | Item quantity (1-64) | `5` |
| `maxUses` | Total redemptions allowed (0=unlimited) | `100` |
| `expiryEpoch` | Unix timestamp (0=never expires) | `1672531200` |

### Common Item IDs
```
minecraft:diamond
minecraft:emerald
minecraft:golden_apple
minecraft:enchanted_golden_apple
minecraft:netherite_sword
minecraft:diamond_pickaxe
minecraft:diamond_axe
minecraft:respawn_anchor
minecraft:iron_sword
minecraft:bread
```

---

## Configuration

Edit `server_config/promocodemod-server.toml`:

```toml
[[promocodes]]
codes = [
    "EARLY_EASTER:minecraft:diamond,5;minecraft:golden_apple,3|100|0",
    "FREESTART:minecraft:iron_sword,1;minecraft:bread,10|200|0",
    "DIAMONDS4ALL:minecraft:diamond,64|10|0"
]
```

**Format:** `CODE:item1,count1;item2,count2|maxUses|expiryEpoch`

---

## How It Works

1. **Player opens GUI** → `/promo`
2. **Types code** → `WELCOME`
3. **Clicks "CONFIRM"** (or presses Enter)
4. **Items appear** in inventory (overflow drops at feet)

**Statistics shown:**
- Redeemed: `25/100` (25 of 100 items claimed)
- Players: `10` (10 unique players redeemed something)

---

## Anti-Abuse Protection

Each player can redeem each code **exactly once**. This is enforced even if:
- Player changes their name
- Player creates a new account on offline server
- Server restarts
- Player joins on different online account

⚠️ **For offline servers:** Make sure admins moderate carefully, as players could theoretically use mods to bypass the system.

---

## Data Storage

Redemption data saved to:
```
world_folder/promocodes_data.json
```

Contains:
- Which players used which codes
- Redemption count per code
- Custom codes added via commands

This is auto-managed — don't edit manually unless backing up!

---

## Building from Source

### Requirements
- Java 8 JDK
- Git

### Steps
```bash
# Clone the repo
git clone <repo_url>
cd promo

# Generate IDE files (first time)
./gradlew genEclipseRuns

# Build
./gradlew build

# Output: build/libs/promocodemod-1.0.0.jar
```

---

## Examples

### Welcome Bonus
```
/promo create WELCOME minecraft:diamond,5 minecraft:iron_sword,1 minecraft:bread,10 0 0
```
Players get: 5 diamonds, 1 iron sword, 10 bread. Unlimited uses, never expires.

### Limited Event
```
/promo create HALLOWEEN minecraft:pumpkin,20 50 0
```
Players get: 20 pumpkins. Only 50 total redemptions allowed, never expires.

### Timed Event (Unix epoch summer 2024)
```
/promo create SUMMER minecraft:diamond,10 100 1725148800
```
Players get: 10 diamonds. Expires September 1, 2024. (epoch: 1725148800)

### Delete Old Code
```
/promo delete HALLOWEEN
```

---

## Tips for Server Admins

💡 **Starter Codes** — Create beginner codes with tools and food  
💡 **Event Codes** — Rotate seasonal codes with theme-related items  
💡 **Announcements** — Tell players about new codes in chat/Discord  
💡 **Monitor Stats** — Check the GUI to see who's redeeming what  
💡 **Cleanup** — Delete expired codes to keep things organized

---

## Troubleshooting

**"Invalid item" error?**
- Check spelling of item ID (e.g., `minecraft:acacia_log` not `acacia log`)

**"Code already exists"?**
- Delete the old one first: `/promo delete OLDCODE`

**"Can't redeem" but code exists?**
- Maybe you already used it! (each player = 1 redeem per code)
- Check expiry time if set
- Check max uses limit

**Items not appearing?**
- Check inventory space (overflow drops at feet)
- Restart server if just added to config

---

**Happy rewarding! 🎉**

