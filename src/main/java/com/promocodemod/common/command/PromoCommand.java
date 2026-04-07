package com.promocodemod.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.promocodemod.common.config.PromoCodeManager;
import com.promocodemod.common.network.NetworkHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.registries.ForgeRegistries;

public class PromoCommand {

    private static final SuggestionProvider<CommandSource> ITEM_SUGGESTIONS =
        (context, builder) -> ISuggestionProvider.suggestResource(ForgeRegistries.ITEMS.getKeys(), builder);

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("promo")
            .executes(ctx -> {
                return openGui(ctx.getSource());
            })
            .then(buildCreateNode())
            .then(Commands.literal("createraw")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("definition", StringArgumentType.greedyString())
                    .executes(ctx -> createCode(ctx.getSource(), StringArgumentType.getString(ctx, "definition")))))
            .then(Commands.literal("delete")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("code", StringArgumentType.word())
                    .executes(ctx -> deleteCode(ctx.getSource(), StringArgumentType.getString(ctx, "code")))))
            .then(Commands.literal("help")
                .executes(ctx -> showHelp(ctx.getSource())))
        );

        dispatcher.register(Commands.literal("promocode")
            .executes(ctx -> openGui(ctx.getSource()))
            .then(buildCreateNode())
            .then(Commands.literal("createraw")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("definition", StringArgumentType.greedyString())
                    .executes(ctx -> createCode(ctx.getSource(), StringArgumentType.getString(ctx, "definition")))))
            .then(Commands.literal("delete")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("code", StringArgumentType.word())
                    .executes(ctx -> deleteCode(ctx.getSource(), StringArgumentType.getString(ctx, "code")))))
            .then(Commands.literal("help")
                .executes(ctx -> showHelp(ctx.getSource())))
        );
    }

    private static ArgumentBuilder<CommandSource, ?> buildCreateNode() {
        return Commands.literal("create")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("code", StringArgumentType.word())
                .then(Commands.argument("item1", ItemIdArgument.itemId())
                    .suggests(ITEM_SUGGESTIONS)
                    .then(Commands.argument("count1", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> createMultiItemCode(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "code"),
                            new String[]{ItemIdArgument.getItemId(ctx, "item1")},
                            new Integer[]{IntegerArgumentType.getInteger(ctx, "count1")},
                            0,
                            0L
                        ))
                        // Optional: item2 + count2
                        .then(Commands.argument("item2", ItemIdArgument.itemId())
                            .suggests(ITEM_SUGGESTIONS)
                            .then(Commands.argument("count2", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> createMultiItemCode(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "code"),
                                    new String[]{ItemIdArgument.getItemId(ctx, "item1"), ItemIdArgument.getItemId(ctx, "item2")},
                                    new Integer[]{IntegerArgumentType.getInteger(ctx, "count1"), IntegerArgumentType.getInteger(ctx, "count2")},
                                    0,
                                    0L
                                ))
                                // Optional: item3 + count3
                                .then(Commands.argument("item3", ItemIdArgument.itemId())
                                    .suggests(ITEM_SUGGESTIONS)
                                    .then(Commands.argument("count3", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> createMultiItemCode(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "code"),
                                            new String[]{ItemIdArgument.getItemId(ctx, "item1"), ItemIdArgument.getItemId(ctx, "item2"), ItemIdArgument.getItemId(ctx, "item3")},
                                            new Integer[]{IntegerArgumentType.getInteger(ctx, "count1"), IntegerArgumentType.getInteger(ctx, "count2"), IntegerArgumentType.getInteger(ctx, "count3")},
                                            0,
                                            0L
                                        ))
                                        // Optional: maxUses
                                        .then(Commands.argument("maxUses", IntegerArgumentType.integer(0))
                                            .executes(ctx -> createMultiItemCode(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "code"),
                                                new String[]{ItemIdArgument.getItemId(ctx, "item1"), ItemIdArgument.getItemId(ctx, "item2"), ItemIdArgument.getItemId(ctx, "item3")},
                                                new Integer[]{IntegerArgumentType.getInteger(ctx, "count1"), IntegerArgumentType.getInteger(ctx, "count2"), IntegerArgumentType.getInteger(ctx, "count3")},
                                                IntegerArgumentType.getInteger(ctx, "maxUses"),
                                                0L
                                            ))
                                            // Optional: expiryEpoch
                                            .then(Commands.argument("expiryEpoch", LongArgumentType.longArg(0L))
                                                .executes(ctx -> createMultiItemCode(
                                                    ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "code"),
                                                    new String[]{ItemIdArgument.getItemId(ctx, "item1"), ItemIdArgument.getItemId(ctx, "item2"), ItemIdArgument.getItemId(ctx, "item3")},
                                                    new Integer[]{IntegerArgumentType.getInteger(ctx, "count1"), IntegerArgumentType.getInteger(ctx, "count2"), IntegerArgumentType.getInteger(ctx, "count3")},
                                                    IntegerArgumentType.getInteger(ctx, "maxUses"),
                                                    LongArgumentType.getLong(ctx, "expiryEpoch")
                                                ))))))
                                // maxUses without item3
                                .then(Commands.argument("maxUses", IntegerArgumentType.integer(0))
                                    .executes(ctx -> createMultiItemCode(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "code"),
                                        new String[]{ItemIdArgument.getItemId(ctx, "item1"), ItemIdArgument.getItemId(ctx, "item2")},
                                        new Integer[]{IntegerArgumentType.getInteger(ctx, "count1"), IntegerArgumentType.getInteger(ctx, "count2")},
                                        IntegerArgumentType.getInteger(ctx, "maxUses"),
                                        0L
                                    ))
                                    .then(Commands.argument("expiryEpoch", LongArgumentType.longArg(0L))
                                        .executes(ctx -> createMultiItemCode(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "code"),
                                            new String[]{ItemIdArgument.getItemId(ctx, "item1"), ItemIdArgument.getItemId(ctx, "item2")},
                                            new Integer[]{IntegerArgumentType.getInteger(ctx, "count1"), IntegerArgumentType.getInteger(ctx, "count2")},
                                            IntegerArgumentType.getInteger(ctx, "maxUses"),
                                            LongArgumentType.getLong(ctx, "expiryEpoch")
                                        )))))
                        )
                        // maxUses without item2
                        .then(Commands.argument("maxUses", IntegerArgumentType.integer(0))
                            .executes(ctx -> createMultiItemCode(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "code"),
                                new String[]{ItemIdArgument.getItemId(ctx, "item1")},
                                new Integer[]{IntegerArgumentType.getInteger(ctx, "count1")},
                                IntegerArgumentType.getInteger(ctx, "maxUses"),
                                0L
                            ))
                            .then(Commands.argument("expiryEpoch", LongArgumentType.longArg(0L))
                                .executes(ctx -> createMultiItemCode(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "code"),
                                    new String[]{ItemIdArgument.getItemId(ctx, "item1")},
                                    new Integer[]{IntegerArgumentType.getInteger(ctx, "count1")},
                                    IntegerArgumentType.getInteger(ctx, "maxUses"),
                                    LongArgumentType.getLong(ctx, "expiryEpoch")
                                )))))));
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
                    "§cInvalid format. Use /promocode help for examples."
                ));
                break;
        }

        return result == PromoCodeManager.AddCodeResult.SUCCESS ? 1 : 0;
    }

    private static int createEasyCode(CommandSource source, String code, String itemId, int count, int maxUses, long expiryEpoch) {
        PromoCodeManager.AddCodeResult result = PromoCodeManager.get().addSimpleCode(code, itemId, count, maxUses, expiryEpoch);

        switch (result) {
            case SUCCESS:
                source.sendSuccess(new StringTextComponent("§aPromo code created. Players can now redeem it."), true);
                break;
            case DUPLICATE_CODE:
                source.sendFailure(new StringTextComponent("§cCode already exists. Choose another name."));
                break;
            default:
                source.sendFailure(new StringTextComponent("§cInvalid values. Check item id/count and try /promocode help."));
                break;
        }

        return result == PromoCodeManager.AddCodeResult.SUCCESS ? 1 : 0;
    }

    private static int createMultiItemCode(CommandSource source, String code, String[] items, Integer[] counts, int maxUses, long expiryEpoch) {
        // Build raw format: CODE:item1,count1;item2,count2|maxUses|expiryEpoch
        StringBuilder definition = new StringBuilder(code).append(":");
        for (int i = 0; i < items.length; i++) {
            if (i > 0) definition.append(";");
            definition.append(items[i]).append(",").append(counts[i]);
        }
        definition.append("|").append(maxUses).append("|").append(expiryEpoch);
        
        return createCode(source, definition.toString());
    }

    private static int deleteCode(CommandSource source, String codeName) {
        PromoCodeManager.DeleteCodeResult result = PromoCodeManager.get().deleteCode(codeName);

        switch (result) {
            case SUCCESS:
                source.sendSuccess(new StringTextComponent("Promo code deleted successfully."), true);
                break;
            case CODE_NOT_FOUND:
                source.sendFailure(new StringTextComponent("Code not found or cannot be deleted."));
                break;
        }

        return result == PromoCodeManager.DeleteCodeResult.SUCCESS ? 1 : 0;
    }

    private static int showHelp(CommandSource source) {
        source.sendSuccess(new StringTextComponent("=== PROMO CODE HELP ==="), false);
        source.sendSuccess(new StringTextComponent("Create (1 item): /promocode create CODE item count [maxUses] [expiryEpoch]"), false);
        source.sendSuccess(new StringTextComponent("Create (multi):  /promocode create CODE item1 count1 item2 count2 [item3 count3] [maxUses] [expiryEpoch]"), false);
        source.sendSuccess(new StringTextComponent("Create (advanced): /promocode createraw CODE:item1,count1;item2,count2|maxUses|expiryEpoch"), false);
        source.sendSuccess(new StringTextComponent("Delete: /promocode delete CODE"), false);
        source.sendSuccess(new StringTextComponent("Help: /promocode help"), false);
        source.sendSuccess(new StringTextComponent(""), false);
        source.sendSuccess(new StringTextComponent("Notes:"), false);
        source.sendSuccess(new StringTextComponent("- Item IDs: namespace:name (e.g., minecraft:diamond)"), false);
        source.sendSuccess(new StringTextComponent("- maxUses=0 for unlimited, expiryEpoch=0 for never expires"), false);
        source.sendSuccess(new StringTextComponent("- Each player can only redeem each code once"), false);
        return 1;
    }
}
