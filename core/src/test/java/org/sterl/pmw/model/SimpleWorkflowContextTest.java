package org.sterl.pmw.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleWorkflowContextTest {

    @BeforeEach
    void setUp() throws Exception {
    }

    @Test
    void test() {
        SimpleWorkflowContext ws = new SimpleWorkflowContext();
        
        assertThat(ws.getInternalWorkflowContext()).isNotNull();
    }

}
