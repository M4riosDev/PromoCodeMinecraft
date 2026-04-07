package com.promocodemod.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.promocodemod.common.config.PromoCodeManager;
import com.promocodemod.common.network.NetworkHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkDirection;

public class PromoCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("promo")
            .executes(ctx -> {
                return openGui(ctx.getSource());
            })
            .then(Commands.literal("create")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("definition", StringArgumentType.greedyString())
                    .executes(ctx -> createCode(ctx.getSource(), StringArgumentType.getString(ctx, "definition")))))
        );

        dispatcher.register(Commands.literal("promocode")
            .executes(ctx -> openGui(ctx.getSource()))
            .then(Commands.literal("create")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("definition", StringArgumentType.greedyString())
                    .executes(ctx -> createCode(ctx.getSource(), StringArgumentType.getString(ctx, "definition")))))
        );
    }

    private static int openGui(CommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
            PromoCodeManager.Stats stats = PromoCodeManager.get().getStats();

            NetworkHandler.CHANNEL.sendTo(
                new NetworkHandler.OpenGuiPacket(),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
            );
            NetworkHandler.CHANNEL.sendTo(
                new NetworkHandler.PromoStatsPacket(stats.redeemedTotal, stats.maxCapacity, stats.redeemedPlayers),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
            );
        }
        return 1;
    }

    private static int createCode(CommandSource source, String definition) {
        PromoCodeManager.AddCodeResult result = PromoCodeManager.get().addCode(definition);

        switch (result) {
            case SUCCESS:
                source.sendSuccess(new StringTextComponent("§aPromo code created successfully."), true);
                break;
            case DUPLICATE_CODE:
                source.sendFailure(new StringTextComponent("§cA promo code with this name already exists."));
                break;
            default:
                source.sendFailure(new StringTextComponent(
                    "§cInvalid format. Use: CODE:item1,count1;item2,count2|maxUses|expiryEpoch"
                ));
                break;
        }

        return result == PromoCodeManager.AddCodeResult.SUCCESS ? 1 : 0;
    }
}
