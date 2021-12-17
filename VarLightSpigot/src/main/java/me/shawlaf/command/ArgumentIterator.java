package me.shawlaf.command;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A fancy {@link String} {@link Iterator}
 */
public class ArgumentIterator implements Iterator<String> {
    public final int length;
    private final String[] arguments;
    private int position = 0;

    public ArgumentIterator(String[] args) {
        this.length = args.length;
        this.arguments = args;
    }

    @Override
    public boolean hasNext() {
        return position < length;
    }

    /**
     * Peeks the next Argument without consuming it.
     *
     * @return The next argument
     */
    public String peek() {
        return arguments[position];
    }

    /**
     * @param index The index of the argument
     * @return The argument at the specified position
     */
    public String get(int index) {
        return arguments[index];
    }

    @Override
    public String next() {
        return arguments[position++];
    }

    /**
     * @param required The amount of arguments required
     * @return True if there are atleast {@code required} arguments left. False otherwise
     */
    public boolean hasParameters(int required) {
        return (length - position) >= required;
    }

    /**
     * @return The previous argument
     */
    public String previous() {
        return arguments[position - 1];
    }

    /**
     * Gets the next argument and applies it to the given {@link Function}
     *
     * @param function The {@link Function} the argument should be applied on
     * @param <P>      The resulting Type of the {@link Function}
     * @return The result of the {@link Function} applied to {@link ArgumentIterator#next()}
     */
    public <P> P parseNext(Function<String, P> function) {
        return function.apply(next());
    }

    /**
     * @return A {@link String} containing all remaining Arguments separated by a whitespace (' ').
     */
    public String join() {
        return join(" ");
    }

    /**
     * @return A {@link Stream} containing all remaining Arguments
     */
    public Stream<String> streamRemaining() {
        Stream.Builder<String> streamBuilder = Stream.builder();
        forEachRemaining(streamBuilder);
        return streamBuilder.build();
    }

    /**
     * @param delimiter The delimiter to use
     * @return A {@link String} containing all remaining Arguments separated by the specified {@code delimiter}.
     */
    public String join(String delimiter) {
        return streamRemaining().collect(Collectors.joining(delimiter));
    }

    /**
     * @return The cursors current position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Set the cursors position
     *
     * @param position The new position of the cursor
     * @return The argument at the new position
     */
    public String jumpTo(int position) {
        return arguments[this.position = position];
    }

    public String[] getArguments() {
        return Arrays.copyOf(arguments, arguments.length);
    }

    @Override
    public String toString() {
        return "ArgumentIterator{" +
                "length=" + length +
                ", arguments=" + Arrays.toString(arguments) +
                ", position=" + position +
                '}';
    }
}