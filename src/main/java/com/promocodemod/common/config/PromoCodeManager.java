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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;

public class PromoCodeManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson   GSON   = new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RNG = new SecureRandom();

    private final Map<String, String> sessionSalts = new HashMap<>();

    private final Map<String, Set<String>> redeemedByFingerprint = new HashMap<>();
    private final Map<String, Set<String>> redeemedByUUID        = new HashMap<>();

    private final Map<String, Integer> redemptionCounts = new HashMap<>();
    private final List<String>         customCodes      = new ArrayList<>();
    private final Set<String>          deletedCodes     = new HashSet<>();

    private static final Path NO_PATH = Paths.get("");
    private Path dataFile = NO_PATH;

    private static volatile PromoCodeManager INSTANCE;
    public static synchronized PromoCodeManager get() {
        if (INSTANCE == null) INSTANCE = new PromoCodeManager();
        return INSTANCE;
    }
    public synchronized String generateSessionSalt(String uuid) {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        String salt = sb.toString();
        sessionSalts.put(uuid, salt);
        return salt;
    }

    public synchronized String getSessionSalt(String uuid) {
        return sessionSalts.get(uuid);
    }

    public synchronized void clearSessionSalt(String uuid) {
        sessionSalts.remove(uuid);
    }


    public void init(Path worldDir) {
        boolean alreadyInited = dataFile != NO_PATH;
        this.dataFile = worldDir.resolve("promocodes_data.json");
        if (!alreadyInited) load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (dataFile == NO_PATH) return;

        customCodes.clear();
        redeemedByFingerprint.clear();
        redeemedByUUID.clear();
        redemptionCounts.clear();
        deletedCodes.clear();

        if (!Files.exists(dataFile)) return;
        try (Reader r = new FileReader(dataFile.toFile())) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> root = GSON.fromJson(r, type);
            if (root == null) return;

            Map<String, List<String>> byFp = (Map<String, List<String>>)
                root.getOrDefault("redeemedByFingerprint", new HashMap<>());
            byFp.forEach((fp, codes) -> redeemedByFingerprint.put(fp, new HashSet<>(codes)));

            Map<String, List<String>> byUUID = (Map<String, List<String>>)
                root.getOrDefault("redeemedByUUID", new HashMap<>());
            byUUID.forEach((uuid, codes) -> redeemedByUUID.put(uuid, new HashSet<>(codes)));

            Map<String, Double> counts = (Map<String, Double>)
                root.getOrDefault("counts", new HashMap<>());
            counts.forEach((code, cnt) -> redemptionCounts.put(code, cnt.intValue()));

            List<String> loaded = (List<String>) root.getOrDefault("customCodes", new ArrayList<>());
            for (String e : loaded)
                if (e != null && !e.trim().isEmpty()) customCodes.add(e.trim());

            List<String> del = (List<String>) root.getOrDefault("deletedCodes", new ArrayList<>());
            deletedCodes.addAll(del);

        } catch (Exception e) {
            LOGGER.error("Failed to load promo code data", e);
        }
    }

    private void save() {
        if (dataFile == NO_PATH) return; // FIX #5
        try (Writer w = new FileWriter(dataFile.toFile())) {
            Map<String, Object> root = new LinkedHashMap<>();

            Map<String, List<String>> byFp = new HashMap<>();
            redeemedByFingerprint.forEach((fp, codes) -> byFp.put(fp, new ArrayList<>(codes)));
            root.put("redeemedByFingerprint", byFp);

            Map<String, List<String>> byUUID = new HashMap<>();
            redeemedByUUID.forEach((uuid, codes) -> byUUID.put(uuid, new ArrayList<>(codes)));
            root.put("redeemedByUUID", byUUID);

            root.put("counts",      redemptionCounts);
            root.put("customCodes", customCodes);
            root.put("deletedCodes", new ArrayList<>(deletedCodes));
            GSON.toJson(root, w);
        } catch (Exception e) {
            LOGGER.error("Failed to save promo code data", e);
        }
    }

    public enum RedeemResult {
        SUCCESS, INVALID_CODE, ALREADY_USED, MAX_USES_REACHED, EXPIRED
    }

    public static class RedeemOutcome {
        public final RedeemResult  result;
        public final List<ItemStack> rewards;
        public RedeemOutcome(RedeemResult r, List<ItemStack> rewards) {
            this.result = r; this.rewards = rewards;
        }
    }

    public synchronized RedeemOutcome redeem(String uuid, String fingerprint, String code) {
        String upper = code.trim().toUpperCase(Locale.ROOT);

        for (String entry : getAllCodeEntries()) {
            PromoDefinition def = parseEntry(entry);
            if (def == null || !def.code.equals(upper)) continue;

            if (def.expiryEpoch > 0 && System.currentTimeMillis() / 1000 > def.expiryEpoch)
                return new RedeemOutcome(RedeemResult.EXPIRED, Collections.emptyList());

            int used = redemptionCounts.getOrDefault(upper, 0);
            if (def.maxUses > 0 && used >= def.maxUses)
                return new RedeemOutcome(RedeemResult.MAX_USES_REACHED, Collections.emptyList());

            Set<String> uuidCodes = redeemedByUUID.computeIfAbsent(uuid, k -> new HashSet<>());
            if (uuidCodes.contains(upper))
                return new RedeemOutcome(RedeemResult.ALREADY_USED, Collections.emptyList());

            Set<String> fpCodes = redeemedByFingerprint.computeIfAbsent(fingerprint, k -> new HashSet<>());
            if (fpCodes.contains(upper))
                return new RedeemOutcome(RedeemResult.ALREADY_USED, Collections.emptyList());

            uuidCodes.add(upper);
            fpCodes.add(upper);
            redemptionCounts.put(upper, used + 1);
            save();
            return new RedeemOutcome(RedeemResult.SUCCESS, def.rewards);
        }

        return new RedeemOutcome(RedeemResult.INVALID_CODE, Collections.emptyList());
    }


    public enum AddCodeResult    { SUCCESS, INVALID_FORMAT, DUPLICATE_CODE }
    public enum DeleteCodeResult { SUCCESS, CODE_NOT_FOUND }

    public static class Stats {
        public final int redeemedTotal, maxCapacity, redeemedPlayers;
        public Stats(int rt, int mc, int rp) {
            this.redeemedTotal = rt; this.maxCapacity = mc; this.redeemedPlayers = rp;
        }
    }

    public synchronized Stats getStats() {
        int total   = redemptionCounts.values().stream().mapToInt(i -> i).sum();
        int cap     = 0;
        for (String e : getAllCodeEntries()) {
            PromoDefinition d = parseEntry(e);
            if (d != null && d.maxUses > 0) cap += d.maxUses;
        }
        int players = (int) redeemedByUUID.values().stream().filter(s -> !s.isEmpty()).count();
        return new Stats(total, cap, players);
    }

    public synchronized AddCodeResult addCode(String raw) {
        PromoDefinition def = parseEntry(raw);
        if (def == null) return AddCodeResult.INVALID_FORMAT;
        for (String e : getAllCodeEntries()) {
            PromoDefinition ex = parseEntry(e);
            if (ex != null && ex.code.equals(def.code)) return AddCodeResult.DUPLICATE_CODE;
        }
        customCodes.add(def.raw);
        save();
        return AddCodeResult.SUCCESS;
    }

    public synchronized DeleteCodeResult deleteCode(String codeName) {
        String upper = codeName.trim().toUpperCase(Locale.ROOT);
        for (int i = 0; i < customCodes.size(); i++) {
            PromoDefinition d = parseEntry(customCodes.get(i));
            if (d != null && d.code.equals(upper)) {
                customCodes.remove(i);
                redemptionCounts.remove(upper);
                save();
                return DeleteCodeResult.SUCCESS;
            }
        }
        for (String e : PromoCodeConfig.SERVER.promoCodes.get()) {
            PromoDefinition d = parseEntry(e);
            if (d != null && d.code.equals(upper)) {
                deletedCodes.add(upper);
                save();
                return DeleteCodeResult.SUCCESS;
            }
        }
        return DeleteCodeResult.CODE_NOT_FOUND;
    }

    public synchronized int deleteAllCodes() {
        int count = customCodes.size();
        for (String e : PromoCodeConfig.SERVER.promoCodes.get()) {
            PromoDefinition d = parseEntry(e);
            if (d != null) { deletedCodes.add(d.code); count++; }
        }
        customCodes.clear();
        redemptionCounts.clear();
        redeemedByFingerprint.clear();
        redeemedByUUID.clear();
        save();
        return count;
    }

    public synchronized List<String> getAllCodesInfo() {
        List<String> info = new ArrayList<>();
        for (String e : getAllCodeEntries()) {
            PromoDefinition d = parseEntry(e);
            if (d != null) {
                int used = redemptionCounts.getOrDefault(d.code, 0);
                String expiry = d.expiryEpoch > 0
                    ? " | Expires: " + new Date(d.expiryEpoch * 1000) : "";
                info.add("?f" + d.code + "?7: ?b" + used + "/" + d.maxUses + expiry);
            }
        }
        return info;
    }

    private List<String> getAllCodeEntries() {
        List<String> out = new ArrayList<>();
        for (String e : PromoCodeConfig.SERVER.promoCodes.get()) {
            PromoDefinition d = parseEntry(e);
            if (d != null && !deletedCodes.contains(d.code)) out.add(e);
        }
        out.addAll(customCodes);
        return out;
    }

    private static class PromoDefinition {
        final String raw, code;
        final List<ItemStack> rewards;
        final int maxUses;
        final long expiryEpoch;

        PromoDefinition(String raw, String code, List<ItemStack> rewards,
                        int maxUses, long expiryEpoch) {
            this.raw = raw; this.code = code; this.rewards = rewards;
            this.maxUses = maxUses; this.expiryEpoch = expiryEpoch;
        }
    }

    private PromoDefinition parseEntry(String entry) {
        if (entry == null) return null;
        String t = entry.trim();
        if (t.isEmpty()) return null;
        String[] parts = t.split("\\|");
        if (parts.length < 3) return null;
        String[] codePart = parts[0].split(":", 2);
        if (codePart.length < 2) return null;
        String code = codePart[0].trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) return null;
        int maxUses; long expiryEpoch;
        try {
            maxUses     = Integer.parseInt(parts[1].trim());
            expiryEpoch = Long.parseLong(parts[2].trim());
        } catch (NumberFormatException ex) { return null; }
        if (maxUses < 0 || expiryEpoch < 0) return null;
        List<ItemStack> rewards = new ArrayList<>();
        for (String itemEntry : codePart[1].split(";")) {
            String[] ip = itemEntry.trim().split(",");
            if (ip.length < 2) continue;
            ResourceLocation rl;
            try { rl = new ResourceLocation(ip[0].trim().toLowerCase(Locale.ROOT)); }
            catch (Exception ex) { continue; }
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null) continue;
            int cnt;
            try { cnt = Integer.parseInt(ip[1].trim()); } catch (NumberFormatException ex) { continue; }
            if (cnt <= 0) continue;
            rewards.add(new ItemStack(item, cnt));
        }
        if (rewards.isEmpty()) return null;
        return new PromoDefinition(t, code, rewards, maxUses, expiryEpoch);
    }
}
