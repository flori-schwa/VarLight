package me.shawlaf.varlight.spigot.command.old.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.util.NamespacedID;
import org.bukkit.Material;

import java.util.concurrent.CompletableFuture;

public class ItemTypeArgumentType extends AbstractTypeArgumentType {

    protected ItemTypeArgumentType(VarLightPlugin plugin) {
        super(plugin);
    }

    public static ItemTypeArgumentType item(VarLightPlugin plugin) {
        return new ItemTypeArgumentType(plugin);
    }

    @Override
    public Material parse(StringReader stringReader) throws CommandSyntaxException {
        return plugin.getNmsAdapter().getItemFromKey(new NamespacedID(readKey(stringReader)));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        plugin.getNmsAdapter().getAllMinecraftItemKeys().forEach((id) -> builder.suggest(id.toString()));

        return builder.buildFuture();
    }
}
