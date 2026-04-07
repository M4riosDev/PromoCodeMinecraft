package com.promocodemod;

import com.promocodemod.common.command.PromoCommand;
import com.promocodemod.common.config.PromoCodeManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

@Mod.EventBusSubscriber(modid = PromoCodeMod.MOD_ID)
public class EventHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        PromoCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isClientSide()) {
            // Get the world save directory and init the manager
            net.minecraft.world.server.ServerWorld world = (net.minecraft.world.server.ServerWorld) event.getWorld();
            java.nio.file.Path saveDir = world.getServer().getWorldPath(net.minecraft.world.storage.FolderName.ROOT);
            PromoCodeManager.get().init(saveDir);
        }
    }
}
