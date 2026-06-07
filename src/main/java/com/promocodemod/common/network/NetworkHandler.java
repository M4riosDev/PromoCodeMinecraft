package com.promocodemod.common.network;

import com.promocodemod.PromoCodeMod;
import com.promocodemod.common.config.PromoCodeManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.Supplier;

public class NetworkHandler {

    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            new net.minecraft.util.ResourceLocation(PromoCodeMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
        );
        CHANNEL.registerMessage(0, RedeemCodePacket.class,  RedeemCodePacket::encode,  RedeemCodePacket::decode,  RedeemCodePacket::handle);
        CHANNEL.registerMessage(1, RedeemResultPacket.class, RedeemResultPacket::encode, RedeemResultPacket::decode, RedeemResultPacket::handle);
        CHANNEL.registerMessage(2, OpenGuiPacket.class,      OpenGuiPacket::encode,      OpenGuiPacket::decode,      OpenGuiPacket::handle);
        CHANNEL.registerMessage(3, PromoStatsPacket.class,   PromoStatsPacket::encode,   PromoStatsPacket::decode,   PromoStatsPacket::handle);
        CHANNEL.registerMessage(4, SaltSyncPacket.class,     SaltSyncPacket::encode,     SaltSyncPacket::decode,     SaltSyncPacket::handle);
    }

    public static class RedeemCodePacket {
        public final String code;
        public final String hwHmac; // HMAC-SHA256(hw_raw, sessionSalt)

        public RedeemCodePacket(String code, String hwHmac) {
            this.code   = code;
            this.hwHmac = hwHmac;
        }

        public static void encode(RedeemCodePacket p, PacketBuffer b) {
            b.writeUtf(p.code,   64);
            b.writeUtf(p.hwHmac, 64);
        }

        public static RedeemCodePacket decode(PacketBuffer b) {
            return new RedeemCodePacket(b.readUtf(64), b.readUtf(64));
        }

        public static void handle(RedeemCodePacket pkt, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayerEntity player = ctx.get().getSender();
                if (player == null) return;

                String uuid = player.getUUID().toString();

                String sessionSalt = PromoCodeManager.get().getSessionSalt(uuid);
                if (sessionSalt == null || !isValidHmacFormat(pkt.hwHmac)) {
                    sendResult(player, false, "?cSession expired. Reopen the GUI.");
                    return;
                }


                String ip          = player.connection.connection
                                         .getRemoteAddress().toString()
                                         .replaceAll("/", "")
                                         .replaceAll(":\\d+$", ""); // strip port
                String fingerprint = sha256Hex(sessionSalt + "|" + pkt.hwHmac + "|" + ip);

                PromoCodeManager.RedeemOutcome outcome =
                    PromoCodeManager.get().redeem(uuid, fingerprint, pkt.code);

                boolean success = false;
                String msg;
                switch (outcome.result) {
                    case SUCCESS:
                        msg = "?aCode redeemed! Enjoy your rewards!";
                        success = true;
                        for (ItemStack stack : outcome.rewards)
                            if (!player.inventory.add(stack)) player.drop(stack, false);
                        break;
                    case ALREADY_USED:
                        msg = "?cYou have already used this code!";
                        break;
                    case MAX_USES_REACHED:
                        msg = "?cThis code has reached its maximum uses!";
                        break;
                    case EXPIRED:
                        msg = "?cThis promo code has expired!";
                        break;
                    default:
                        msg = "?cInvalid promo code. Please try again.";
                        break;
                }

                PromoCodeManager.Stats stats = PromoCodeManager.get().getStats();
                sendResult(player, success, msg);
                CHANNEL.sendTo(
                    new PromoStatsPacket(stats.redeemedTotal, stats.maxCapacity, stats.redeemedPlayers),
                    player.connection.connection,
                    net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT
                );
            });
            ctx.get().setPacketHandled(true);
        }

        private static void sendResult(ServerPlayerEntity player, boolean ok, String msg) {
            CHANNEL.sendTo(
                new RedeemResultPacket(ok, msg),
                player.connection.connection,
                net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT
            );
        }

        private static boolean isValidHmacFormat(String hmac) {
            return hmac != null && hmac.length() == 64 && hmac.matches("[0-9a-f]+");
        }
    }

    static String sha256Hex(String input) {
        try {
            byte[] raw = MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public static class RedeemResultPacket {
        public final boolean success;
        public final String  message;

        public RedeemResultPacket(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static void encode(RedeemResultPacket p, PacketBuffer b) {
            b.writeBoolean(p.success);
            b.writeUtf(p.message, 256);
        }

        public static RedeemResultPacket decode(PacketBuffer b) {
            return new RedeemResultPacket(b.readBoolean(), b.readUtf(256));
        }

        public static void handle(RedeemResultPacket pkt, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                    () -> com.promocodemod.client.ClientPacketHandler.handleRedeemResult(pkt.success, pkt.message)
                )
            );
            ctx.get().setPacketHandled(true);
        }
    }

    public static class OpenGuiPacket {
        public static void encode(OpenGuiPacket p, PacketBuffer b) {}
        public static OpenGuiPacket decode(PacketBuffer b) { return new OpenGuiPacket(); }

        public static void handle(OpenGuiPacket pkt, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                    () -> com.promocodemod.client.ClientPacketHandler.handleOpenGui()
                )
            );
            ctx.get().setPacketHandled(true);
        }
    }

    public static class PromoStatsPacket {
        public final int redeemedTotal, maxCapacity, redeemedPlayers;

        public PromoStatsPacket(int redeemedTotal, int maxCapacity, int redeemedPlayers) {
            this.redeemedTotal   = redeemedTotal;
            this.maxCapacity     = maxCapacity;
            this.redeemedPlayers = redeemedPlayers;
        }

        public static void encode(PromoStatsPacket p, PacketBuffer b) {
            b.writeInt(p.redeemedTotal);
            b.writeInt(p.maxCapacity);
            b.writeInt(p.redeemedPlayers);
        }

        public static PromoStatsPacket decode(PacketBuffer b) {
            return new PromoStatsPacket(b.readInt(), b.readInt(), b.readInt());
        }

        public static void handle(PromoStatsPacket pkt, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                    () -> com.promocodemod.client.ClientPacketHandler.handleStats(
                              pkt.redeemedTotal, pkt.maxCapacity, pkt.redeemedPlayers)
                )
            );
            ctx.get().setPacketHandled(true);
        }
    }
}
