package me.shawlaf.varlight.util;

import lombok.experimental.UtilityClass;

import java.util.stream.Stream;

@UtilityClass
public class StreamUtil {

    @SuppressWarnings("unchecked")
    public <F, T> Stream<T> ofType(Stream<F> self, Class<T> resultType) {
        return self.map(f -> (T) f);
    }

}
