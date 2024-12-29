package com.adolfoeloy.swflab;

import com.adolfoeloy.swflab.swf.model.Workflow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("swf")
class SwfTestController {

    private final Workflow workflow;

    SwfTestController(Workflow workflow) {
        this.workflow = workflow;
    }

    @GetMapping("/domain")
    String getRegisteredDomain() {
        return workflow.domain().name();
    }

    @GetMapping("/workflow")
    Workflow getWorkflow() {
        return workflow;
    }
}
