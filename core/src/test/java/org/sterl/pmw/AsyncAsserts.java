package org.sterl.pmw;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.awaitility.Awaitility;

public class AsyncAsserts {

    private List<String> values = Collections.synchronizedList(new ArrayList<String>());
    
    public void clear() {
        values.clear();
        System.err.println("-------------");
    }
    public void add(String value) {
        values.add(value);
        if (values.size() > 100) {
            throw new IllegalStateException("Workflow has already more than 100 steps, assuming error!");
        }
    }
    public void info(String value) {
        this.add(value);
        System.err.println(values.size() + ". " + value);
    }
    public void awaitValue(String value) {
        Awaitility.await()
            .until(() -> values.contains(value));
    }
    public void awaitValue(String value, String... values) {
        awaitValue(value);
        if (values != null && values.length > 0) {
            for (String v : values) {
                awaitValue(v);
            }
        }
    }
    public void awaitOrdered(String value, String... values) {
        awaitValue(value, values);
        assertThat(this.values.indexOf(value))
            .isEqualTo(0);
        
        if (values != null && values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                assertThat(this.values.indexOf(values[i]))
                    .isEqualTo(i + 1);
            }
        }
    }
}
