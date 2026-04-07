package com.promocodemod.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PromoCodeManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Set<String>> redeemedCodes = new HashMap<>();
    private final Map<String, Integer> redemptionCounts = new HashMap<>();
    private final List<String> customCodes = new ArrayList<>();

    private Path dataFile;

    private static PromoCodeManager INSTANCE;

    public static PromoCodeManager get() {
        if (INSTANCE == null) INSTANCE = new PromoCodeManager();
        return INSTANCE;
    }

    public void init(Path worldDir) {
        this.dataFile = worldDir.resolve("promocodes_data.json");
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(dataFile)) return;
        try (Reader r = new FileReader(dataFile.toFile())) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> root = GSON.fromJson(r, type);
            if (root == null) return;

            Map<String, List<String>> redeemed = (Map<String, List<String>>) root.getOrDefault("redeemed", new HashMap<>());
            redeemed.forEach((uuid, codes) -> redeemedCodes.put(uuid, new HashSet<>(codes)));

            Map<String, Double> counts = (Map<String, Double>) root.getOrDefault("counts", new HashMap<>());
            counts.forEach((code, count) -> redemptionCounts.put(code, count.intValue()));

            List<String> loadedCodes = (List<String>) root.getOrDefault("customCodes", new ArrayList<>());
            for (String entry : loadedCodes) {
                if (entry != null && !entry.trim().isEmpty()) {
                    customCodes.add(entry.trim());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load promo code data", e);
        }
    }

    private void save() {
        try (Writer w = new FileWriter(dataFile.toFile())) {
            Map<String, Object> root = new HashMap<>();
            Map<String, List<String>> redeemed = new HashMap<>();
            redeemedCodes.forEach((uuid, codes) -> redeemed.put(uuid, new ArrayList<>(codes)));
            root.put("redeemed", redeemed);
            root.put("counts", redemptionCounts);
            root.put("customCodes", customCodes);
            GSON.toJson(root, w);
        } catch (Exception e) {
            LOGGER.error("Failed to save promo code data", e);
        }
    }

    public enum RedeemResult {
        SUCCESS, INVALID_CODE, ALREADY_USED, MAX_USES_REACHED, EXPIRED
    }

    public static class RedeemOutcome {
        public final RedeemResult result;
        public final List<ItemStack> rewards;

        public RedeemOutcome(RedeemResult result, List<ItemStack> rewards) {
            this.result = result;
            this.rewards = rewards;
        }
    }

    public enum AddCodeResult {
        SUCCESS, INVALID_FORMAT, DUPLICATE_CODE
    }

    public static class Stats {
        public final int redeemedTotal;
        public final int maxCapacity;
        public final int redeemedPlayers;

        public Stats(int redeemedTotal, int maxCapacity, int redeemedPlayers) {
            this.redeemedTotal = redeemedTotal;
            this.maxCapacity = maxCapacity;
            this.redeemedPlayers = redeemedPlayers;
        }
    }

    private static class PromoDefinition {
        final String raw;
        final String code;
        final List<ItemStack> rewards;
        final int maxUses;
        final long expiryEpoch;

        PromoDefinition(String raw, String code, List<ItemStack> rewards, int maxUses, long expiryEpoch) {
            this.raw = raw;
            this.code = code;
            this.rewards = rewards;
            this.maxUses = maxUses;
            this.expiryEpoch = expiryEpoch;
        }
    }

    private PromoDefinition parseEntry(String entry) {
        if (entry == null) return null;
        String trimmed = entry.trim();
        if (trimmed.isEmpty()) return null;

        String[] parts = trimmed.split("\\|");
        if (parts.length < 3) return null;

        String[] codePart = parts[0].split(":", 2);
        if (codePart.length < 2) return null;

        String code = codePart[0].trim().toUpperCase();
        if (code.isEmpty()) return null;

        int maxUses;
        long expiryEpoch;
        try {
            maxUses = Integer.parseInt(parts[1].trim());
            expiryEpoch = Long.parseLong(parts[2].trim());
        } catch (NumberFormatException ex) {
            return null;
        }

        if (maxUses < 0 || expiryEpoch < 0) return null;

        List<ItemStack> rewards = new ArrayList<>();
        String[] itemEntries = codePart[1].split(";");
        for (String itemEntry : itemEntries) {
            String[] itemParts = itemEntry.trim().split(",");
            if (itemParts.length < 2) continue;

            ResourceLocation rl;
            try {
                rl = new ResourceLocation(itemParts[0].trim());
            } catch (Exception ex) {
                continue;
            }

            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null) continue;

            int count;
            try {
                count = Integer.parseInt(itemParts[1].trim());
            } catch (NumberFormatException ex) {
                continue;
            }
            if (count <= 0) continue;
            rewards.add(new ItemStack(item, count));
        }

        if (rewards.isEmpty()) return null;
        return new PromoDefinition(trimmed, code, rewards, maxUses, expiryEpoch);
    }

    private List<String> getAllCodeEntries() {
        List<String> entries = new ArrayList<>();
        entries.addAll(PromoCodeConfig.SERVER.promoCodes.get());
        entries.addAll(customCodes);
        return entries;
    }

    public synchronized AddCodeResult addCode(String rawDefinition) {
        PromoDefinition def = parseEntry(rawDefinition);
        if (def == null) return AddCodeResult.INVALID_FORMAT;

        for (String existing : getAllCodeEntries()) {
            PromoDefinition existingDef = parseEntry(existing);
            if (existingDef != null && existingDef.code.equals(def.code)) {
                return AddCodeResult.DUPLICATE_CODE;
            }
        }

        customCodes.add(def.raw);
        save();
        return AddCodeResult.SUCCESS;
    }

    public synchronized AddCodeResult addSimpleCode(String code, String itemId, int count, int maxUses, long expiryEpoch) {
        if (code == null || itemId == null) return AddCodeResult.INVALID_FORMAT;
        if (count <= 0 || maxUses < 0 || expiryEpoch < 0) return AddCodeResult.INVALID_FORMAT;

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        if (!normalizedCode.matches("[A-Z0-9_-]+")) return AddCodeResult.INVALID_FORMAT;

        ResourceLocation rl;
        try {
            rl = new ResourceLocation(itemId.trim());
        } catch (Exception ex) {
            return AddCodeResult.INVALID_FORMAT;
        }
        if (ForgeRegistries.ITEMS.getValue(rl) == null) return AddCodeResult.INVALID_FORMAT;

        String rawDefinition = normalizedCode + ":" + rl.toString() + "," + count + "|" + maxUses + "|" + expiryEpoch;
        return addCode(rawDefinition);
    }

    public synchronized Stats getStats() {
        int redeemedTotal = 0;
        for (Integer value : redemptionCounts.values()) {
            if (value != null && value > 0) redeemedTotal += value;
        }

        int maxCapacity = 0;
        for (String entry : getAllCodeEntries()) {
            PromoDefinition def = parseEntry(entry);
            if (def != null && def.maxUses > 0) {
                maxCapacity += def.maxUses;
            }
        }

        int redeemedPlayers = 0;
        for (Set<String> used : redeemedCodes.values()) {
            if (used != null && !used.isEmpty()) redeemedPlayers++;
        }

        return new Stats(redeemedTotal, maxCapacity, redeemedPlayers);
    }

    public synchronized RedeemOutcome redeem(UUID playerUUID, String code) {
        String upperCode = code.trim().toUpperCase();
        for (String entry : getAllCodeEntries()) {
            PromoDefinition def = parseEntry(entry);
            if (def == null || !def.code.equals(upperCode)) continue;

            if (def.expiryEpoch > 0 && System.currentTimeMillis() / 1000 > def.expiryEpoch) {
                return new RedeemOutcome(RedeemResult.EXPIRED, Collections.emptyList());
            }

            int usedCount = redemptionCounts.getOrDefault(upperCode, 0);
            if (def.maxUses > 0 && usedCount >= def.maxUses) {
                return new RedeemOutcome(RedeemResult.MAX_USES_REACHED, Collections.emptyList());
            }

            String uuid = playerUUID.toString();
            Set<String> playerCodes = redeemedCodes.computeIfAbsent(uuid, k -> new HashSet<>());
            if (playerCodes.contains(upperCode)) {
                return new RedeemOutcome(RedeemResult.ALREADY_USED, Collections.emptyList());
            }

            playerCodes.add(upperCode);
            redemptionCounts.put(upperCode, usedCount + 1);
            save();

            return new RedeemOutcome(RedeemResult.SUCCESS, def.rewards);
        }

        return new RedeemOutcome(RedeemResult.INVALID_CODE, Collections.emptyList());
    }
}
