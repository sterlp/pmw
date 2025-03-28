package org.sterl.pmw.component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

import org.sterl.pmw.WorkflowUmlService;
import org.sterl.pmw.model.Workflow;

public class SerializationUtil {

    public static void writeAsPlantUmlSvg(String filename, Workflow<? extends Serializable> workflow) throws IOException {
        final File d = new File(filename);
        if (d.exists()) d.delete();

        try (FileOutputStream out = new FileOutputStream(d)) {
            new WorkflowUmlService(null).printWorkflowAsPlantUmlSvg(workflow, out);
        }
    }
    public static void writeAsPlantUmlSvg(String filename, String name, WorkflowUmlService service) throws IOException {
        final File d = new File(filename);
        if (d.exists()) d.delete();

        try (FileOutputStream out = new FileOutputStream(d)) {
            service.printWorkflowAsPlantUmlSvg(name, out);
        }
    }

    public static byte[] serialize(Serializable state) throws IOException {
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

    public static Serializable deserializeWorkflowState(final byte[] userState) throws IOException, ClassNotFoundException {
        try(var bais = new ObjectInputStream(new ByteArrayInputStream(userState))) {
            return (Serializable)bais.readObject();
        }
    }
}
