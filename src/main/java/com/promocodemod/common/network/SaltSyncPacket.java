package com.promocodemod.common.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;


public class SaltSyncPacket {

    public final String sessionSalt;

    public SaltSyncPacket(String sessionSalt) {
        this.sessionSalt = sessionSalt;
    }

    public static void encode(SaltSyncPacket p, PacketBuffer b) {
        b.writeUtf(p.sessionSalt, 64);
    }

    public static SaltSyncPacket decode(PacketBuffer b) {
        return new SaltSyncPacket(b.readUtf(64));
    }

    public static void handle(SaltSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                () -> com.promocodemod.client.FingerprintManager.receiveSalt(pkt.sessionSalt)
            )
        );
        ctx.get().setPacketHandled(true);
    }
}
