package com.adolfoeloy.swflab.rest;

import com.adolfoeloy.swflab.swf.domain.Workflow;
import com.adolfoeloy.swflab.swf.domain.WorkflowExecution;
import com.adolfoeloy.swflab.swf.domain.workflow.SwfWorkflow;
import com.adolfoeloy.swflab.swf.domain.workflow.SwfWorkflowRepository;
import com.adolfoeloy.swflab.swf.service.WorkflowStarter;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("swf")
class SwfTestController {

    private final Workflow workflow;
    private final WorkflowStarter workflowStarter;
    private final SwfWorkflowRepository swfWorkflowRepository;

    SwfTestController(Workflow workflow, WorkflowStarter workflowStarter, SwfWorkflowRepository swfWorkflowRepository) {
        this.workflow = workflow;
        this.workflowStarter = workflowStarter;
        this.swfWorkflowRepository = swfWorkflowRepository;
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
        var workflowId = UUID.randomUUID();
        return workflowStarter.start(workflowId);
    }

    @PostMapping("/{workflowId}/contact")
    ResponseEntity<Void> sendContactInformation(
            @PathVariable("workflowId") String workflowId, @RequestBody ContactRequest contactRequest) {
        persistContactInformation(workflowId, contactRequest);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Transactional
    private void persistContactInformation(String workflowId, ContactRequest contactRequest) {
        swfWorkflowRepository.save(new SwfWorkflow(workflowId, contactRequest.email(), contactRequest.phone()));
    }

    public record ContactRequest(String email, String phone) {}
}
