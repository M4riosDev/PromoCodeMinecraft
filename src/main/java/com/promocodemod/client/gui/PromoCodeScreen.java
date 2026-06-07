package com.promocodemod.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.promocodemod.client.FingerprintManager;
import com.promocodemod.common.network.NetworkHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

public class PromoCodeScreen extends Screen {

    private static final int BOX_W = 220;
    private static final int BOX_H = 160;
    private static final String PLACEHOLDER = "Enter promo code...";

    private TextFieldWidget codeField;
    private Button confirmButton;

    private String feedbackMessage = "";
    private boolean feedbackSuccess = false;
    private int feedbackTimer = 0;

    private static String  pendingMessage       = null;
    private static boolean pendingSuccess        = false;
    private static int     pendingRedeemedTotal  = 0;
    private static int     pendingMaxCapacity    = 0;
    private static int     pendingRedeemedPlayers = 0;

    private int redeemedTotal   = 0;
    private int maxCapacity     = 0;
    private int redeemedPlayers = 0;

    public PromoCodeScreen() {
        super(new StringTextComponent("Promo Codes"));
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - BOX_W) / 2;
        int y = (height - BOX_H) / 2;

        codeField = new TextFieldWidget(font,
            x + 10, y + 60, BOX_W - 20, 20,
            new StringTextComponent(""));
        codeField.setMaxLength(64);
        codeField.setBordered(true);
        codeField.setFocus(true);
        addWidget(codeField);

        confirmButton = new Button(
            x + 10, y + 90, BOX_W - 20, 20,
            new StringTextComponent("CONFIRM"),
            btn -> redeemCode()
        );
        addButton(confirmButton);
    }

    private void redeemCode() {
        String code = codeField.getValue().trim();
        if (code.isEmpty()) {
            feedbackMessage = "?ePlease enter a code!";
            feedbackSuccess = false;
            feedbackTimer   = 80;
            return;
        }

        String hwHmac = FingerprintManager.getSignedFingerprint();
        if (hwHmac == null) {
            feedbackMessage = "?eConnecting... please try again.";
            feedbackSuccess = false;
            feedbackTimer   = 100;
            return;
        }

        feedbackMessage = "?7Checking...";
        feedbackTimer   = 200;
        NetworkHandler.CHANNEL.sendToServer(new NetworkHandler.RedeemCodePacket(code, hwHmac));
    }

    public static void handleResult(boolean success, String message) {
        pendingSuccess = success;
        pendingMessage = message;
    }

    public static void handleStats(int redeemedTotal, int maxCapacity, int redeemedPlayers) {
        pendingRedeemedTotal   = redeemedTotal;
        pendingMaxCapacity     = maxCapacity;
        pendingRedeemedPlayers = redeemedPlayers;
    }

    @Override
    public void tick() {
        super.tick();
        codeField.tick();

        if (pendingMessage != null) {
            feedbackSuccess = pendingSuccess;
            feedbackMessage = pendingMessage;
            feedbackTimer   = 100;
            pendingMessage  = null;
            if (feedbackSuccess) codeField.setValue("");
        }

        redeemedTotal   = pendingRedeemedTotal;
        maxCapacity     = pendingMaxCapacity;
        redeemedPlayers = pendingRedeemedPlayers;

        if (feedbackTimer > 0) feedbackTimer--;
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        renderBackground(ms);

        int x = (width - BOX_W) / 2;
        int y = (height - BOX_H) / 2;

        fill(ms, x, y, x + BOX_W, y + BOX_H, 0xCC1E1E1E);
        fill(ms, x, y, x + BOX_W, y + 3, 0xFF3CB043);
        fill(ms, x, y + BOX_H - 3, x + BOX_W, y + BOX_H, 0xFF3CB043);

        drawCenteredString(ms, font, "PROMO CODES", width / 2, y + 10, 0x3CB043);
        drawCenteredString(ms, font,
            "Enter your code and press Confirm",
            width / 2, y + 25, 0xAAAAAA);

        String slots = "Redeemed: " + redeemedTotal + "/" + maxCapacity;
        String users = "Players: " + redeemedPlayers;
        drawString(ms, font, slots, x + 8, y + BOX_H - 16, 0x3CB043);
        drawString(ms, font, users, x + BOX_W - font.width(users) - 8, y + BOX_H - 16, 0x5DADE2);

        if (feedbackTimer > 0 && !feedbackMessage.isEmpty()) {
            drawCenteredString(ms, font, feedbackMessage, width / 2, y + 118, 0xFFFFFF);
        }

        codeField.render(ms, mx, my, pt);
        if (codeField.getValue().isEmpty() && !codeField.isFocused()) {
            drawString(ms, font, PLACEHOLDER, x + 14, y + 66, 0x888888);
        }
        super.render(ms, mx, my, pt);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 257 || key == 335) { redeemCode(); return true; }
        if (codeField.keyPressed(key, scan, mod)) return true;
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public boolean charTyped(char c, int mod) {
        if (codeField.charTyped(c, mod)) return true;
        return super.charTyped(c, mod);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
