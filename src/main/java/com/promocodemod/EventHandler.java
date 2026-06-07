package com.promocodemod;

import com.promocodemod.common.command.PromoCommand;
import com.promocodemod.common.config.PromoCodeManager;
import com.promocodemod.common.network.NetworkHandler;
import com.promocodemod.common.network.SaltSyncPacket;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PromoCodeMod.MOD_ID)
public class EventHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        PromoCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isClientSide()) {
            net.minecraft.world.server.ServerWorld world =
                (net.minecraft.world.server.ServerWorld) event.getWorld();
            java.nio.file.Path saveDir =
                world.getServer().getWorldPath(net.minecraft.world.storage.FolderName.ROOT);
            PromoCodeManager.get().init(saveDir);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof net.minecraft.entity.player.ServerPlayerEntity)) return;
        net.minecraft.entity.player.ServerPlayerEntity player =
            (net.minecraft.entity.player.ServerPlayerEntity) event.getPlayer();

        String uuid = player.getUUID().toString();
        String salt  = PromoCodeManager.get().generateSessionSalt(uuid);

        NetworkHandler.CHANNEL.sendTo(
            new SaltSyncPacket(salt),
            player.connection.connection,
            net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT
        );

        PromoCodeMod.LOGGER.debug("[PromoCode] Sent session salt to {}", player.getName().getString());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String uuid = event.getPlayer().getUUID().toString();
        PromoCodeManager.get().clearSessionSalt(uuid);
    }
}
