package com.promocodemod.client;

import com.promocodemod.client.gui.PromoCodeScreen;
import com.promocodemod.common.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void handleRedeemResult(boolean success, String message) {
        PromoCodeScreen.handleResult(success, message);
    }

    public static void handleOpenGui() {
        Minecraft.getInstance().setScreen(new PromoCodeScreen());
    }

    public static void handleStats(int redeemedTotal, int maxCapacity, int redeemedPlayers) {
        PromoCodeScreen.handleStats(redeemedTotal, maxCapacity, redeemedPlayers);
    }
}