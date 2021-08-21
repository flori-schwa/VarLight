package me.shawlaf.varlight.spigot.command.old.util;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.command.brigadier.datatypes.ICoordinates;
import me.shawlaf.varlight.spigot.command.old.commands.WorldEditUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface IPlayerSelection {

    RequiredArgumentBuilder<CommandSender, ICoordinates> getPositionArgumentA();

    RequiredArgumentBuilder<CommandSender, ICoordinates> getPositionArgumentB();

    WorldEditUtil getWorldEditUtil();

    default Location[] getSelection(CommandContext<CommandSender> context, boolean worldEdit) throws CommandSyntaxException {
        Player player = ((Player) context.getSource());

        if (worldEdit) {
            return getWorldEditUtil().getSelection(player, player.getWorld());
        }

        Location a, b;

        a = context.getArgument(getPositionArgumentA().getName(), ICoordinates.class).toLocation(context.getSource());
        b = context.getArgument(getPositionArgumentB().getName(), ICoordinates.class).toLocation(context.getSource());

        return new Location[] {a, b};
    }

}
