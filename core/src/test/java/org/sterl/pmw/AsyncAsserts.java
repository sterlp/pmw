package org.sterl.pmw;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncAsserts {

    private final List<String> values = Collections.synchronizedList(new ArrayList<String>());
    private final Map<String, Integer> counts = new ConcurrentHashMap<>();

    public synchronized void clear() {
        values.clear();
        counts.clear();
    }
    public synchronized int add(String value) {
        values.add(value);
        final int count = getCount(value) + 1;
        counts.put(value, count);
        if (values.size() > 100) {
            throw new IllegalStateException("Workflow has already more than 100 steps, assuming error!");
        }
        return count;
    }
    public int info(String value) {
        int count = this.add(value);
        System.err.println(values.size() + ". " + value + "=" + count);
        return count;
    }
    public int getCount(String value) {
        return counts.getOrDefault(value, 0);
    }
    public void awaitValue(String value) {
        awaitValue(null, value);
    }
    /**
     * Wait for the given value, if not found call the given method
     * @param fn the optional function to call after each wait
     * @param value the value to wait for
     */
    public void awaitValue(Runnable fn, String value) {
        final var now = Instant.now();
        while (!values.contains(value)
                && (System.currentTimeMillis() - now.toEpochMilli() <= 5_000)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                if (Thread.interrupted()) break;
            }
            if (fn != null) fn.run();
        }
        assertThat(new ArrayList<>(values)).contains(value);
    }
    /**
     * Wait for the given value, if not found call the given method
     * @param fn the optional function to call after each wait
     * @param value the value to wait for
     */
    public void awaitValue(Runnable fn, String value, String... values) {
        awaitValue(fn, value);
        if (values != null && values.length > 0) {
            for (String v : values) {
                awaitValue(fn, v);
            }
        }
    }
    public void awaitValue(String value, String... values) {
        awaitValue(null, value, values);
    }
    public void awaitOrdered(String value, String... values) {
        awaitOrdered(null, value, values);
    }
    public void awaitOrdered(Runnable fn, String value, String... values) {
        awaitValue(fn, value, values);

        assertThat(this.values.indexOf(value)).isEqualTo(0);
        if (values != null && values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                assertThat(this.values.indexOf(values[i])).isEqualTo(i + 1);
            }
        }
    }
    public void assertMissing(String value) {
        assertThat(values).doesNotContain(value);
    }
}
