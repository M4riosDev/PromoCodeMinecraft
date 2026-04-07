# PromoCode Mod — Minecraft Forge 1.16.5

Adds a promo code system to Minecraft. Players open a GUI, type a code and get item rewards.

---

## Features
- `/promo` or `/promocode` command opens the GUI
- Codes defined in server config (`promocodemod-server.toml`)
- Each code: custom item rewards, max uses, optional expiry
- Per-player tracking (each player can use a code only once)
- Data saved to world folder (`promocodes_data.json`)

---

## Building

### Requirements
- Java 8 (JDK)
- Git

### Steps
```bash
# 1. Clone / download this folder
cd promocodemod

# 2. Set up Forge MDK (first time only)
./gradlew genEclipseRuns   # or genIntellijRuns

# 3. Build the jar
./gradlew build

# Output: build/libs/promocodemod-1.16.5-1.0.0.jar
```

---

## Installation
1. Copy the `.jar` to your Forge 1.16.5 `mods/` folder (client **and** server).
2. Start the server once — config generates at `config/promocodemod-server.toml`.

---

## Configuring Codes

Edit `config/promocodemod-server.toml`:

```toml
[promocodes]
    # Format: CODE:item,count;item2,count2|maxUses|expiryEpochSeconds
    # expiryEpochSeconds = 0 → never expires
    codes = [
        "EARLY_EASTER:minecraft:diamond,5;minecraft:golden_apple,3|100|0",
        "FREESTART:minecraft:iron_sword,1;minecraft:bread,10|200|0",
        "DIAMONDS4ALL:minecraft:diamond,64|10|0"
    ]
```

### Format breakdown
```
MYCODE:minecraft:diamond,5;minecraft:apple,10|50|0
│      │                  │                │  │
│      └─ item:count pairs separated by ;  │  └─ expiry (0=never)
│                                          └─ max total uses
└─ the code players type
```

You can use any valid item registry name (modded items work too!).

---

## In-Game Usage

Players type `/promo` and a GUI appears. They enter the code and click Confirm (or press Enter).

- ✅ Valid code → items appear in inventory (overflow drops at feet)
- ❌ Invalid / already used / expired → error message shown in GUI

---

## Adding Admin Commands (optional)

You can extend `PromoCommand.java` to add:
- `/promo add <code> ...` — add codes at runtime
- `/promo list` — list all codes
- `/promo reset <player> <code>` — reset a player's redemption

These are left as exercises since the config approach covers most servers.
