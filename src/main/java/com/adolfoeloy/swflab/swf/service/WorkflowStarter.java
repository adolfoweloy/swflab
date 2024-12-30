package com.adolfoeloy.swflab.swf.service;

import com.adolfoeloy.swflab.swf.domain.Workflow;
import com.adolfoeloy.swflab.swf.domain.WorkflowExecution;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.StartWorkflowExecutionRequest;
import software.amazon.awssdk.services.swf.model.TaskList;
import software.amazon.awssdk.services.swf.model.WorkflowType;

import java.util.UUID;

@Component
public class WorkflowStarter {

    private final SwfClient client;
    private final Workflow workflow;

    public WorkflowStarter(SwfClient client, Workflow workflow) {
        this.client = client;
        this.workflow = workflow;
    }

    public WorkflowExecution start() {
        var workflowId = UUID.randomUUID();
        var request = StartWorkflowExecutionRequest.builder()
                .taskList(TaskList.builder().name(workflow.decisionTaskList()).build())
                .workflowType(WorkflowType.builder()
                        .name(workflow.name())
                        .version(workflow.version().toString())
                        .build())

                .domain(workflow.domain().name())
                .workflowId(workflowId.toString())
                .executionStartToCloseTimeout("3600")
                .build();

        var response = client.startWorkflowExecution(request);

        return new WorkflowExecution(
                workflowId,
                response.runId()
        );
    }
}
