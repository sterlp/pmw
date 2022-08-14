package org.sterl.pmw.quartz.component;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.sterl.pmw.model.InternalWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowContext;
import org.sterl.pmw.model.WorkflowState;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkflowStateParserComponent {

    private static final String INTERNAL_WORKFLOW_STATE = "_internalWorkflowState";
    private static final String USER_WORKFLOW_STATE = "_userWorkflowState";
    
    private final ObjectMapper mapper;
    
    public void setState(TriggerBuilder<?> builder, WorkflowState state) throws JsonProcessingException {
        setInternal(builder, state.internalState());
        setUserState(builder, state.userContext());
    }
    public void setInternal(TriggerBuilder<?> builder, InternalWorkflowState internalState) throws JsonProcessingException {
        builder.usingJobData(INTERNAL_WORKFLOW_STATE, mapper.writeValueAsString(internalState));
    }
    public void setUserState(TriggerBuilder<?> builder, WorkflowContext userState) throws JsonProcessingException {
        if (userState != null) {
            builder.usingJobData(USER_WORKFLOW_STATE, mapper.writeValueAsString(userState));
        }
    }
    
    public WorkflowState readWorkflowState(Workflow<?> w, JobExecutionContext context) {
        final JobDataMap jobData = context.getMergedJobDataMap();
        final TriggerKey key = context.getTrigger().getKey();
        WorkflowContext userState = parse(w.newEmtyContext(), jobData, USER_WORKFLOW_STATE, key);
        InternalWorkflowState internalState = parse(new InternalWorkflowState(), jobData, INTERNAL_WORKFLOW_STATE, key);

        return new WorkflowState(w, userState, internalState);
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
