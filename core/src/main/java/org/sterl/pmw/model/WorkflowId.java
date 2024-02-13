package org.sterl.pmw.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public record WorkflowId(String value) {
    public static WorkflowId newWorkflowId(Workflow<?> w) {
        return new WorkflowId(w.getName() + ":" + UUID.randomUUID().getMostSignificantBits());
    }
    public WorkflowId {
        if (value == null) throw new NullPointerException("WorkflowId value can't be null");
        else if(value.trim().length() == 0) throw new IllegalArgumentException("WorkflowId can't be an empty string.");
    }
    public static WorkflowId newWorkflowId(OffsetDateTime date) {
        final var result = new StringBuilder();
        result.append(date.getYear())
            .append(padZero(date.getMonthValue()))
            .append(padZero(date.getDayOfMonth()))
            .append('-')
            .append(padZero(date.getHour()))
            .append(':')
            .append(padZero(date.getMinute()))
            .append(':')
            .append(padZero(date.getSecond()))
            .append('-')
            .append(ThreadLocalRandom.current().nextInt(1, 99_999));
        
        return new WorkflowId(result.toString());        
    }
    
    static String padZero(int value) {
        if (value < 10) return "0" + value;
        return Integer.toString(value);
    }
}
