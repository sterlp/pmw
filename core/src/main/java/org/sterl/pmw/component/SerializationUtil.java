package org.sterl.pmw.component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

import org.sterl.pmw.boundary.WorkflowUmlService;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;

public class SerializationUtil {
    
    public static void writeAsPlantUmlSvg(String filename, Workflow<? extends WorkflowState> workflow) throws IOException {
        final File d = new File(filename);
        if (d.exists()) d.delete();
        
        try (FileOutputStream out = new FileOutputStream(d)) {
            new WorkflowUmlService(null).printWorkflowAsPlantUmlSvg(workflow, out);
        }
    }

    public static byte[] serialize(WorkflowState state) throws IOException {
        Objects.requireNonNull(state, "WorkflowState cannot be null");
        byte[] result;
        try (var baos = new ByteArrayOutputStream(512)) {
            try (var oos = new ObjectOutputStream(baos)) {
                oos.writeObject(state);
                result = baos.toByteArray();
            }
        }
        return result;
    }

    public static WorkflowState deserializeWorkflowState(final byte[] userState) throws IOException, ClassNotFoundException {
        try(var bais = new ObjectInputStream(new ByteArrayInputStream(userState))) {
            return (WorkflowState)bais.readObject();
        }
    }
    
    /**
     * Checks that the given state is assignable to the given workflow
     */
    public static void verifyStateType(Workflow<?> w, WorkflowState state) {
        if (state != null) {
            final Class<?> newContextClass = w.newEmtyContext().getClass();
            if (!newContextClass.isAssignableFrom(state.getClass())) {
                throw new IllegalArgumentException("Context of type "
                        + state.getClass().getName()
                        + " is not compatible to " + newContextClass.getName());
            }
        }
    }
}
