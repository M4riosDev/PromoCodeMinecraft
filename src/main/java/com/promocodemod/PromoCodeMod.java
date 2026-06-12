package com.promocodemod;

import com.promocodemod.common.config.PromoCodeConfig;
import com.promocodemod.common.network.NetworkHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PromoCodeMod.MOD_ID)
public class PromoCodeMod {

    public static final String MOD_ID = "promocodemod";
    public static final Logger LOGGER = LogManager.getLogger();

    public PromoCodeMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::clientSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PromoCodeConfig.SERVER_SPEC);
    }

    private void setup(final FMLCommonSetupEvent event) {
        NetworkHandler.register();
        LOGGER.info("PromoCode Mod initialized!");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("PromoCode Mod client setup done!");
    }
}