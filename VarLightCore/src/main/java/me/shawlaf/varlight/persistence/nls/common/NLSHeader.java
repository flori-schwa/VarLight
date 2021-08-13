package me.shawlaf.varlight.persistence.nls.common;

import lombok.Getter;
import me.shawlaf.varlight.persistence.nls.common.exception.ExpectedMagicNumberException;
import me.shawlaf.varlight.persistence.nls.common.io.NLSCommonInputStream;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class NLSHeader {

    @Getter
    private final ExpectedMagicNumberException exception;

    @Getter
    private final int version;

    public static NLSHeader readFromStream(InputStream iStream) throws IOException {
        NLSCommonInputStream in = new NLSCommonInputStream(iStream);

        try {
            in.verifyMagic();

            return new NLSHeader(in.readInt(), null);
        } catch (ExpectedMagicNumberException e) {
            return new NLSHeader(-1, e);
        }
    }

    private NLSHeader(int version, @Nullable ExpectedMagicNumberException exception) {
        this.version = version;
        this.exception = exception;
    }

    public void validRequired() {
        if (!isValid()) {
            throw Objects.requireNonNull(exception);
        }
    }

    public boolean isValid() {
        return version != -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NLSHeader)) return false;

        NLSHeader nlsHeader = (NLSHeader) o;

        return version == nlsHeader.version;
    }

    @Override
    public int hashCode() {
        return version;
    }
}
