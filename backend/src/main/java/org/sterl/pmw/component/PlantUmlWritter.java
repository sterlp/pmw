package org.sterl.pmw.component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

import org.sterl.pmw.WorkflowUmlService;
import org.sterl.pmw.model.Workflow;

public class PlantUmlWritter {

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
}
