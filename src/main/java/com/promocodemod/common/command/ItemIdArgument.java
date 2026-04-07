package com.promocodemod.common.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.ISuggestionProvider;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.concurrent.CompletableFuture;

/**
 * Custom argument type for Minecraft item IDs (e.g., minecraft:diamond).
 * Allows names with namespace:id format without requiring quotes.
 */
public class ItemIdArgument implements ArgumentType<String> {

    public static ItemIdArgument itemId() {
        return new ItemIdArgument();
    }

    public static String getItemId(CommandContext<?> context, String nodeName) {
        return context.getArgument(nodeName, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        
        // Read characters until we hit whitespace or end of string
        while (reader.canRead()) {
            char c = reader.peek();
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                break;
            }
            reader.skip();
        }
        
        String itemId = reader.getString().substring(start, reader.getCursor());
        
        // Basic validation: should be namespace:id format
        if (!itemId.contains(":")) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                .dispatcherUnknownCommand()
                .createWithContext(reader);
        }
        
        return itemId;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return ISuggestionProvider.suggestResource(ForgeRegistries.ITEMS.getKeys(), builder);
    }
}
