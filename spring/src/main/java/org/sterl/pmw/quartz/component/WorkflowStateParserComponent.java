package org.sterl.pmw.quartz.component;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.sterl.pmw.model.InternalWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;
import org.sterl.pmw.model.RunningWorkflowState;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkflowStateParserComponent {

    private static final String INTERNAL_WORKFLOW_STATE = "_internalWorkflowState";
    private static final String USER_WORKFLOW_STATE = "_userWorkflowState";
    private static final String WORKFLOW_STATUS = "_workflowStatus";
    
    private final ObjectMapper mapper;
    
    public WorkflowStatus getWorkflowStatus(JobDataMap jobData) {
        String status = (String)jobData.getOrDefault(WORKFLOW_STATUS, WorkflowStatus.PENDING.name());
        return WorkflowStatus.valueOf(status);
    }
    public void setWorkflowStatus(TriggerBuilder<?> builder, WorkflowStatus status) {
        builder.usingJobData(WORKFLOW_STATUS, status.name());
    }
    
    public void setState(TriggerBuilder<?> builder, RunningWorkflowState state) throws JsonProcessingException {
        setInternal(builder, state.internalState());
        setUserState(builder, state.userContext());
    }
    public void setInternal(TriggerBuilder<?> builder, InternalWorkflowState internalState) throws JsonProcessingException {
        builder.usingJobData(INTERNAL_WORKFLOW_STATE, mapper.writeValueAsString(internalState));
    }
    public void setUserState(TriggerBuilder<?> builder, WorkflowState userState) throws JsonProcessingException {
        if (userState != null) {
            builder.usingJobData(USER_WORKFLOW_STATE, mapper.writeValueAsString(userState));
        }
    }
    
    public RunningWorkflowState readWorkflowState(Workflow<?> w, JobExecutionContext context) {
        final JobDataMap jobData = context.getMergedJobDataMap();
        final TriggerKey key = context.getTrigger().getKey();
        WorkflowState userState = parse(w.newEmtyContext(), jobData, USER_WORKFLOW_STATE, key);
        InternalWorkflowState internalState = parse(new InternalWorkflowState(), jobData, INTERNAL_WORKFLOW_STATE, key);

        return new RunningWorkflowState(w, userState, internalState);
    }
    
    private <T extends Object> T parse(T initial, JobDataMap data, String name, TriggerKey key) {
        T result = initial;
        final String state = data.getString(name);
        if (state != null && state.length() > 1) {
            try {
                result = (T)mapper.readValue(state, initial.getClass());
            } catch (Exception e) {
                throw new RuntimeException(key + " failed to parse " + initial.getClass() + " of value: " + state, e);
            }
        }
        return result;
    }
}
