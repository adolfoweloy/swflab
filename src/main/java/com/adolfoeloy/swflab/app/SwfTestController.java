package com.adolfoeloy.swflab.app;

import com.adolfoeloy.swflab.swf.domain.Workflow;
import com.adolfoeloy.swflab.swf.domain.WorkflowExecution;
import com.adolfoeloy.swflab.swf.service.WorkflowStarter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("swf")
class SwfTestController {

    private final Workflow workflow;
    private final WorkflowStarter workflowStarter;

    SwfTestController(Workflow workflow, WorkflowStarter workflowStarter) {
        this.workflow = workflow;
        this.workflowStarter = workflowStarter;
    }

    @GetMapping("/domain")
    String getRegisteredDomain() {
        return workflow.domain().name();
    }

    @GetMapping("/workflow")
    Workflow getWorkflow() {
        return workflow;
    }

    @PostMapping("/start")
    WorkflowExecution startWorkflow() {
        return workflowStarter.start();
    }
}
