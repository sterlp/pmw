package org.sterl.pmw.component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

import org.sterl.pmw.model.WorkflowState;

public class SerializationUtil {

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
}
