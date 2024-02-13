package org.sterl.pmw.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class WorkflowIdTest {

    @Test
    void test() {
        WorkflowId id = WorkflowId.newWorkflowId(OffsetDateTime.now());
        System.err.println(id);
    }
    
    @Test
    void testPadZero() {
        assertThat(WorkflowId.padZero(0)).isEqualTo("00");
        assertThat(WorkflowId.padZero(8)).isEqualTo("08");
        assertThat(WorkflowId.padZero(11)).isEqualTo("11");
        assertThat(WorkflowId.padZero(101)).isEqualTo("101");
    }

}
