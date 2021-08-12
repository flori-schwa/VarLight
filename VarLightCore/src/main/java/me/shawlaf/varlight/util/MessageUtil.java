package me.shawlaf.varlight.util;

import lombok.experimental.UtilityClass;

import java.util.*;

@UtilityClass
public class MessageUtil {

    public String[] splitLongMessage(String message, int maxLineLength) {
        if (maxLineLength < 1) {
            throw new IllegalArgumentException(String.format("Invalid Max Line Length: %d, must be >= 1", maxLineLength));
        }

        List<String> lines = new ArrayList<>();
        Queue<String> words = new LinkedList<>(Arrays.asList(message.split(" ")));

        StringBuilder lineBuilder = new StringBuilder();
        String word;

        while ((word = words.poll()) != null) {
            lineBuilder.append(word).append(' ');

            String next = words.peek();

            if (next == null || lineBuilder.length() + next.length() > maxLineLength) {
                final String nextLine = lineBuilder.toString().trim();
                lineBuilder = new StringBuilder();

                if (!lines.isEmpty()) {
                    lines.add(" " + nextLine);
                } else {
                    lines.add(nextLine);
                }

            }
        }

        return lines.toArray(new String[0]);
    }

}
