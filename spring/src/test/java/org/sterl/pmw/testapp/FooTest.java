package org.sterl.pmw.testapp;

import java.math.BigInteger;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class FooTest {

    @Test
    void test() {
        UUID uuid = UUID.randomUUID();
        
        BigInteger bigInt = new BigInteger(uuid.toString().replace("-", ""), 16);
        
        System.out.println("UUID:       " + uuid);
        System.out.println("BigInteger: " + bigInt);
        
        long mostSignificant = uuid.getMostSignificantBits();
        long leastSignificant = uuid.getLeastSignificantBits();

        System.out.println("UUID:                    " + uuid);
        System.out.println("Most Significant Bits:   " + mostSignificant);
        System.out.println("Least Significant Bits:  " + leastSignificant);
    }

}
