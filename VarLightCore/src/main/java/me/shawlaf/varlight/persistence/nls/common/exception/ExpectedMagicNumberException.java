package me.shawlaf.varlight.persistence.nls.common.exception;

import lombok.Getter;

public class ExpectedMagicNumberException extends RuntimeException {

    @Getter
    private int expected;
    @Getter
    private int got;

    public ExpectedMagicNumberException(int expected, int got) {
        super(String.format("Expected Magic number %08x, got %08x", expected, got));

        this.expected = expected;
        this.got = got;
    }
}
