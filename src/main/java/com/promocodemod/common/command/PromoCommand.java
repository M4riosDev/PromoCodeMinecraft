package com.promocodemod.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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
            .executes(ctx -> openGui(ctx.getSource()))
            .then(Commands.literal("create")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("code", StringArgumentType.word())
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests(ITEM_SUGGESTIONS)
                        .executes(ctx -> createFromArgs(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "code"),
                            StringArgumentType.getString(ctx, "args")
                        )))))
            .then(Commands.literal("createraw")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("definition", StringArgumentType.greedyString())
                    .executes(ctx -> createCode(ctx.getSource(), StringArgumentType.getString(ctx, "definition")))))
            .then(Commands.literal("delete")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("code", StringArgumentType.word())
                    .executes(ctx -> deleteCode(ctx.getSource(), StringArgumentType.getString(ctx, "code")))))
            .then(Commands.literal("deleteall")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> deleteAllCodes(ctx.getSource())))
            .then(Commands.literal("list")
                .executes(ctx -> listCodes(ctx.getSource())))
            .then(Commands.literal("help")
                .executes(ctx -> showHelp(ctx.getSource())))
        );

        dispatcher.register(Commands.literal("promocode")
            .executes(ctx -> openGui(ctx.getSource()))
            .then(Commands.literal("create")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("code", StringArgumentType.word())
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests(ITEM_SUGGESTIONS)
                        .executes(ctx -> createFromArgs(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "code"),
                            StringArgumentType.getString(ctx, "args")
                        )))))
            .then(Commands.literal("createraw")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("definition", StringArgumentType.greedyString())
                    .executes(ctx -> createCode(ctx.getSource(), StringArgumentType.getString(ctx, "definition")))))
            .then(Commands.literal("delete")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("code", StringArgumentType.word())
                    .executes(ctx -> deleteCode(ctx.getSource(), StringArgumentType.getString(ctx, "code")))))
            .then(Commands.literal("deleteall")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> deleteAllCodes(ctx.getSource())))
            .then(Commands.literal("list")
                .executes(ctx -> listCodes(ctx.getSource())))
        );
    }


    private static int createFromArgs(CommandSource source, String code, String args) {
        String[] tokens = args.trim().split("\\s+");

        java.util.List<String> items  = new java.util.ArrayList<>();
        java.util.List<Integer> counts = new java.util.ArrayList<>();
        int maxUses     = 0;
        long expiryEpoch = 0L;

        int i = 0;
        while (i < tokens.length && items.size() < 3) {
            if (tokens[i].matches("\\d+")) break;
            if (i + 1 >= tokens.length) {
                source.sendFailure(new StringTextComponent(
                    "Missing count for item: " + tokens[i] + ". Use /promo help"));
                return 0;
            }
            int count;
            try {
                count = Integer.parseInt(tokens[i + 1]);
            } catch (NumberFormatException e) {
                source.sendFailure(new StringTextComponent(
                    "Expected a count after " + tokens[i] + " but got: " + tokens[i + 1]));
                return 0;
            }
            if (count < 1 || count > 64) {
                source.sendFailure(new StringTextComponent(
                    "Count must be 1-64, got: " + count));
                return 0;
            }
            items.add(tokens[i].toLowerCase(java.util.Locale.ROOT));
            counts.add(count);
            i += 2;
        }

        if (items.isEmpty()) {
            source.sendFailure(new StringTextComponent("No items provided. Use /promo help"));
            return 0;
        }

        if (i < tokens.length) {
            try { maxUses = Integer.parseInt(tokens[i++]); }
            catch (NumberFormatException e) {
                source.sendFailure(new StringTextComponent("Expected maxUses integer, got: " + tokens[i - 1]));
                return 0;
            }
        }


        if (i < tokens.length) {
            try { expiryEpoch = Long.parseLong(tokens[i++]); }
            catch (NumberFormatException e) {
                source.sendFailure(new StringTextComponent("Expected expiryEpoch integer, got: " + tokens[i - 1]));
                return 0;
            }
        }

 
        if (i < tokens.length) {
            source.sendFailure(new StringTextComponent("Too many arguments. Use /promo help"));
            return 0;
        }

        return createMultiItemCode(
            source, code,
            items.toArray(new String[0]),
            counts.toArray(new Integer[0]),
            maxUses, expiryEpoch
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
                source.sendSuccess(new StringTextComponent("Promo code created successfully."), true);
                break;
            case DUPLICATE_CODE:
                source.sendFailure(new StringTextComponent("A promo code with this name already exists."));
                break;
            default:
                source.sendFailure(new StringTextComponent("Invalid format. Use /promocode help for examples."));
                break;
        }
        return result == PromoCodeManager.AddCodeResult.SUCCESS ? 1 : 0;
    }

    private static int createMultiItemCode(CommandSource source, String code, String[] items, Integer[] counts, int maxUses, long expiryEpoch) {
        StringBuilder definition = new StringBuilder(code).append(":");
        for (int i = 0; i < items.length; i++) {
            if (i > 0) definition.append(";");
            definition.append(items[i].toLowerCase(java.util.Locale.ROOT)).append(",").append(counts[i]);
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

    private static int deleteAllCodes(CommandSource source) {
        int deleted = PromoCodeManager.get().deleteAllCodes();
        source.sendSuccess(new StringTextComponent("Deleted " + deleted + " promo code(s)."), true);
        return deleted > 0 ? 1 : 0;
    }

    private static int listCodes(CommandSource source) {
        source.sendSuccess(new StringTextComponent("=== ACTIVE PROMO CODES ==="), false);
        java.util.List<String> codes = PromoCodeManager.get().getAllCodesInfo();
        if (codes.isEmpty()) {
            source.sendSuccess(new StringTextComponent("No promo codes available."), false);
            return 0;
        }
        for (String codeInfo : codes) {
            source.sendSuccess(new StringTextComponent(codeInfo), false);
        }
        source.sendSuccess(new StringTextComponent("Total: " + codes.size() + " code(s)"), false);
        return codes.size();
    }

    private static int showHelp(CommandSource source) {
        source.sendSuccess(new StringTextComponent("=== PROMO CODE HELP ==="), false);
        source.sendSuccess(new StringTextComponent("Usage: /promocode create CODE <items> [maxUses] [expiryEpoch]"), false);
        source.sendSuccess(new StringTextComponent("  1 item:  /promocode create SUMMER minecraft:diamond 5"), false);
        source.sendSuccess(new StringTextComponent("  2 items: /promocode create SUMMER minecraft:diamond 5 minecraft:apple 10"), false);
        source.sendSuccess(new StringTextComponent("  3 items: /promocode create SUMMER minecraft:diamond 5 minecraft:apple 10 minecraft:stick 3"), false);
        source.sendSuccess(new StringTextComponent("  w/ max uses: /promocode create SUMMER minecraft:diamond 5 50"), false);
        source.sendSuccess(new StringTextComponent("  w/ expiry:   /promocode create SUMMER minecraft:diamond 5 50 1735689600"), false);
        source.sendSuccess(new StringTextComponent("Advanced: /promocode createraw CODE:item,count;item2,count2|maxUses|expiryEpoch"), false);
        source.sendSuccess(new StringTextComponent("Delete: /promocode delete CODE"), false);
        source.sendSuccess(new StringTextComponent("List:   /promocode list"), false);
        source.sendSuccess(new StringTextComponent("Notes: maxUses=0 unlimited, expiryEpoch=0 never expires"), false);
        return 1;
    }
}
