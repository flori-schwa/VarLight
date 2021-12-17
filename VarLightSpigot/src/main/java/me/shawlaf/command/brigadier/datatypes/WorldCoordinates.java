package me.shawlaf.command.brigadier.datatypes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.command.brigadier.BrigadierCommand;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public class WorldCoordinates implements ICoordinates {

    private double x, y, z;
    private boolean xRelative, yRelative, zRelative;

    public WorldCoordinates(StringReader reader) throws CommandSyntaxException {
        readCoordinate(reader, x -> this.x = x, f -> this.xRelative = f);
        reader.skipWhitespace();
        readCoordinate(reader, y -> this.y = y, f -> this.yRelative = f);
        reader.skipWhitespace();
        readCoordinate(reader, z -> this.z = z, f -> this.zRelative = f);
    }

    private void readCoordinate(StringReader reader, DoubleConsumer coordinateSetter, Consumer<Boolean> flagSetter) throws CommandSyntaxException {
        if (reader.peek() == '~') {
            reader.skip();

            flagSetter.accept(true);

            if (StringReader.isAllowedNumber(reader.peek())) {
                coordinateSetter.accept(reader.readDouble());
            } else {
                coordinateSetter.accept(0d);
            }
        } else {
            coordinateSetter.accept(reader.readDouble());
        }
    }

    @Override
    public Location toLocation(CommandSender source) throws CommandSyntaxException {
        if (xRelative | yRelative | zRelative) {
            if (!(source instanceof LivingEntity)) {
                throw BrigadierCommand.literal("You may not use relative coordinates!");
            }

            Location entityLocation = ((LivingEntity) source).getLocation();
            Location location = new Location(null, Double.NaN, Double.NaN, Double.NaN);

            location.setX(x + (xRelative ? entityLocation.getX() : 0));
            location.setY(y + (yRelative ? entityLocation.getY() : 0));
            location.setZ(z + (zRelative ? entityLocation.getZ() : 0));

            return location;
        }

        return new Location(null, x, y, z);
    }

    @Override
    public String toString() {
        return "WorldCoordinates[" + (xRelative ? "~" : "") + x + ", " + (yRelative ? "~" : "") + y + ", " + (zRelative ? "~" : "") + z + "]";
    }
}
