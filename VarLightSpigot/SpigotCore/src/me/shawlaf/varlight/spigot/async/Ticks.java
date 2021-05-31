package me.shawlaf.varlight.spigot.async;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Ticks {

    private final static TimeUnitConversion[] UNITS =  {
            new TimeUnitConversion(20 * 60 * 60, "Hour(s)"),
            new TimeUnitConversion(20 * 60, "Minute(s)"),
            new TimeUnitConversion(20, "Second(s)")
    };

    public final long ticks;

    private Ticks(long ticks) {
        this.ticks = ticks;
    }

    public static long calculate(long n, TimeUnit timeUnit) {
        return timeUnit.toSeconds(n) * 20L;
    }

    public static long calculate(Duration duration) {
        return duration.getSeconds() * 20L;
    }

    public static Ticks of(long nTicks) {
        return new Ticks(nTicks);
    }

    public static Ticks of(long n, TimeUnit timeUnit) {
        return new Ticks(calculate(n, timeUnit));
    }

    public static Ticks of(Duration duration) {
        return new Ticks(calculate(duration));
    }

    public String toReadable() {
        StringBuilder builder = new StringBuilder();
        long tmp = this.ticks;

        for (TimeUnitConversion unit : UNITS) {
            long amount = tmp / unit.invfactor;

            if (amount > 0) {
                builder.append(String.format("%d %s ", amount, unit.name));
            }
        }

        if (tmp > 0) {
            builder.append(String.format("%d Milliseconds", (tmp * 50)));
        }

        return builder.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticks ticks1 = (Ticks) o;
        return ticks == ticks1.ticks;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticks);
    }

    private static class TimeUnitConversion {
        public long invfactor;
        public String name;

        public TimeUnitConversion(long factor, String name) {
            this.invfactor = factor;
            this.name = name;
        }
    }
}