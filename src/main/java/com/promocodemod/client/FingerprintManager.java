package com.promocodemod.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;


@OnlyIn(Dist.CLIENT)
public class FingerprintManager {

    private static volatile String cachedHmac   = null;
    private static volatile String sessionSalt  = null;

    public static void receiveSalt(String salt) {
        sessionSalt = salt;
        cachedHmac  = null; // invalidate ????? cache
    }

    public static String getSignedFingerprint() {
        if (sessionSalt == null) return null;
        if (cachedHmac  != null) return cachedHmac;
        cachedHmac = computeHmac(collectRaw(), sessionSalt);
        return cachedHmac;
    }

    private static String collectRaw() {
        StringBuilder sb = new StringBuilder();

        sb.append(Runtime.getRuntime().availableProcessors()).append('|');

        sb.append(getFirstMac()).append('|');

        sb.append(System.getProperty("os.name",    "")).append('|');
        sb.append(System.getProperty("os.arch",    "")).append('|');
        sb.append(System.getProperty("os.version", "")).append('|');

        sb.append(System.getProperty("user.name", "")).append('|');

        sb.append(System.getProperty("java.vendor", ""));

        return sb.toString();
    }

    private static String getFirstMac() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces == null) return "no-net";
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback() || iface.isVirtual()) continue;
                byte[] mac = iface.getHardwareAddress();
                if (mac == null || mac.length == 0) continue;
                StringBuilder sb = new StringBuilder();
                for (byte b : mac) sb.append(String.format("%02x", b));
                return sb.toString();
            }
        } catch (SocketException ignored) {}
        return "no-mac";
    }

    private static String computeHmac(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return sha256Hex(data + key);
        }
    }

    private static String sha256Hex(String input) {
        try {
            byte[] raw = MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "fallback-" + Math.abs(input.hashCode());
        }
    }
}
