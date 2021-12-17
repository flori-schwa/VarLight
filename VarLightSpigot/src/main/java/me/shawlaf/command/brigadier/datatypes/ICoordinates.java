package me.shawlaf.command.brigadier.datatypes;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

public interface ICoordinates {

    Location toLocation(CommandSender source) throws CommandSyntaxException;

}
