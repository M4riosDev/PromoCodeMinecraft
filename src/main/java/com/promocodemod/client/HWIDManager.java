package com.promocodemod.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class HWIDManager {
    
    private static String HWID = null;
    public static String getHWID() {
        if (HWID == null) {
            HWID = generateHWID();
        }
        return HWID;
    }
    
    private static String generateHWID() {
        try {
            StringBuilder hwidSource = new StringBuilder();
            
            // System properties that are hardware-related
            hwidSource.append(System.getProperty("os.name", ""));
            hwidSource.append(System.getProperty("os.arch", ""));
            hwidSource.append(System.getProperty("os.version", ""));
            hwidSource.append(Runtime.getRuntime().availableProcessors());
            hwidSource.append(System.getProperty("java.version", ""));
            hwidSource.append(System.getProperty("user.home", ""));
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(hwidSource.toString().getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString().substring(0, 32);
            
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
