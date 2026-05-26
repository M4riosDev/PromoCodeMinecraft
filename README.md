# PromoCode Mod

Forge 1.16.5 mod that adds a promo code system to your server. Admins create codes, players redeem them in-game and get items.

---

## Installation

Drop the `.jar` into your server's `mods/` folder and start the server. That's it — players don't need to install anything client-side.

Requires Forge 36.x (1.16.5).

---

## How it works

Players type `/promo` to open a GUI, enter a code, and get their reward. Each player can only redeem a code once (tracked by HWID), so no abuse.

Codes are saved in `<world>/promocodes_data.json` automatically — no database, no config needed.

---

## Commands

Players just need `/promo`. Everything below requires permission level 2.

```
/promo create <CODE> <item> <count>
/promo create <CODE> <item> <count> <maxUses> <expiry>
/promo delete <CODE>
/promo deleteall
/promo list
/promo help
```

`/promocode` also works as an alias for all of the above.

For expiry, pass a Unix timestamp or `0` for no expiry.

**Example:**
```
/promo create LAUNCH2025 minecraft:diamond 5 100 0
```
Gives 5 diamonds, max 100 total uses, never expires.

---

## Building

```bash
./gradlew build
```

Output goes to `build/libs/`. Needs JDK 8.
