package org.sterl.pmw.quartz.job;

import java.time.Duration;

import org.sterl.pmw.exception.WorkflowException;

@FunctionalInterface
public interface RetryDelayStrategy {
    RetryDelayStrategy NO_DELAY = (c, e) -> Duration.ZERO;
    RetryDelayStrategy LINEAR_DELAY = (c, e) -> Duration.ofMinutes(c + 1);
    RetryDelayStrategy EXPONENTIAL_DELAY = (c, e) -> Duration.ofMinutes((long)Math.pow(c + 1, 2));

    /**
     * By default a linear retry strategy, adding one minute for each failed try.
     */
    Duration retryAt(int stepRetryCount, WorkflowException.WorkflowFailedDoRetryException error);
}
