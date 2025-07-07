package org.sterl.pmw.spring.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sterl.pmw.WorkflowService;
import org.sterl.pmw.WorkflowUmlService;
import org.sterl.pmw.api.WorkflowDiagram;

import lombok.RequiredArgsConstructor;

@RequestMapping("pmw-api/workflows")
@RestController
@RequiredArgsConstructor
public class WorkflowResource {

    private final WorkflowService<?> workflowService;
    private final WorkflowUmlService umlService;
    
    @GetMapping
    List<String> list() {
        var result = new ArrayList<>(workflowService.listWorkflows());
        Collections.sort(result);
        return result;
    }
    
    @Cacheable
    @GetMapping("{id}")
    WorkflowDiagram getWorkflow(@PathVariable("id") String id) throws IOException {
        var workflowUml = umlService.printWorkflow(id);
        var svg = new ByteArrayOutputStream();
        umlService.convertAsPlantUmlSvg(workflowUml, svg);

        return new WorkflowDiagram(id, workflowUml,
                new String(Base64.getEncoder().encode(svg.toByteArray())));
    }
}
