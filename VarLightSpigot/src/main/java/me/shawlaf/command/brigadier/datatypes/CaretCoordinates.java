package me.shawlaf.command.brigadier.datatypes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.command.brigadier.BrigadierCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CaretCoordinates implements ICoordinates {

    public static final float DEG_TO_RAD = (float) (Math.PI / 180d);

    private static Method METHOD_SIN, METHOD_COS;

    static {
        try {

            final String packageName = Bukkit.getServer().getClass().getPackage().getName();
            final String version = packageName.substring(packageName.lastIndexOf('.') + 1);

            final Class<?> mathHelperClass = Class.forName("net.minecraft.server." + version + ".MathHelper");

            METHOD_SIN = mathHelperClass.getDeclaredMethod("sin", float.class);
            METHOD_COS = mathHelperClass.getDeclaredMethod("cos", float.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private final double left, up, forwards;

    public CaretCoordinates(StringReader stringReader) throws CommandSyntaxException {
        this.left = parseCaret(stringReader);
        stringReader.skipWhitespace();
        this.up = parseCaret(stringReader);
        stringReader.skipWhitespace();
        this.forwards = parseCaret(stringReader);
    }

    private static float sin(float x) {
        try {
            return (float) METHOD_SIN.invoke(null, x);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static float cos(float x) {
        try {
            return (float) METHOD_COS.invoke(null, x);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private double parseCaret(StringReader reader) throws CommandSyntaxException {
        if (!(reader.peek() == '^')) { // Check for caret notation
            throw BrigadierCommand.literal("Expected Caret notation!");
        }

        reader.skip(); // Caret has been verified

        return StringReader.isAllowedNumber(reader.peek()) ? reader.readDouble() : 0d;
    }

    private double parseDouble(String input) {
        if (input.length() == 0) {
            return 0d;
        }

        return Double.parseDouble(input);
    }


    @Override
    public Location toLocation(CommandSender source) throws CommandSyntaxException {
        if (!(source instanceof LivingEntity)) {
            throw BrigadierCommand.literal("You may not use caret (^) notation!");
        }

        final LivingEntity entity = (LivingEntity) source;
        final Location entityLocation = entity.getLocation();

        final float yaw = entityLocation.getYaw(), pitch = entityLocation.getPitch();
        final double x = entityLocation.getX(), y = entityLocation.getY(), z = entityLocation.getZ();

        // Vec2F#i -> pitch
        // Vec2F#j -> yaw

        // I have no idea how this math works, just ripped from ArgumentVectorPosition#a(CommandListenerWrapper)

        final float var3 = cos((yaw + 90f) * DEG_TO_RAD);
        final float var4 = sin((yaw + 90f) * DEG_TO_RAD);

        final float var5 = cos(-pitch * DEG_TO_RAD);
        final float var6 = sin(-pitch * DEG_TO_RAD);

        final float var7 = cos((-pitch + 90f) * DEG_TO_RAD);
        final float var8 = sin((-pitch + 90f) * DEG_TO_RAD);

        final Vector er = new Vector(var3 * var5, var6, var4 * var5);
        final Vector minusEt = new Vector(var3 * var7, var8, var4 * var7);

        final Vector var11 = er.clone().crossProduct(minusEt).multiply(-1);

        final double deltaX = er.getX() * forwards + minusEt.getX() * up + var11.getX() * left;
        final double deltaY = er.getY() * forwards + minusEt.getY() * up + var11.getY() * left;
        final double deltaZ = er.getZ() * forwards + minusEt.getZ() * up + var11.getZ() * left;

        return new Location(null, x + deltaX, y + deltaY, z + deltaZ);
    }

    @Override
    public String toString() {
        return String.format("CaretCoordinates[^%f, ^%f, ^%f]", left, up, forwards);
    }
}
