package org.sterl.pmw;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;

import org.junit.jupiter.api.Test;

class FooTest {
    @Test
    void test() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        System.err.println(now.get(ChronoField.YEAR));
    }

}
