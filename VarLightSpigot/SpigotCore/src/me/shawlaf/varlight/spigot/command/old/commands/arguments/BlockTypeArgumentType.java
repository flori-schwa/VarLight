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

public class BlockTypeArgumentType extends AbstractTypeArgumentType {


    protected BlockTypeArgumentType(VarLightPlugin plugin) {
        super(plugin);
    }

    public static BlockTypeArgumentType block(VarLightPlugin plugin) {
        return new BlockTypeArgumentType(plugin);
    }

    @Override
    public Material parse(StringReader stringReader) throws CommandSyntaxException {
        return plugin.getNmsAdapter().getBlockFromKey(new NamespacedID(readKey(stringReader)));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        plugin.getNmsAdapter().getAllMinecraftBlockKeys().forEach((id) -> builder.suggest(id.toString()));

        return builder.buildFuture();
    }
}
