package com.adolfoeloy.swflab.swf.service;

import com.adolfoeloy.swflab.swf.SwfConfigData;
import com.adolfoeloy.swflab.swf.model.Domain;
import com.adolfoeloy.swflab.swf.model.Workflow;
import com.adolfoeloy.swflab.swf.model.WorkflowExecution;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.StartWorkflowExecutionRequest;
import software.amazon.awssdk.services.swf.model.TaskList;
import software.amazon.awssdk.services.swf.model.WorkflowType;

import java.util.UUID;

@Component
public class WorkflowStarter {

    private final SwfClient client;
    private final Domain domain;

    public WorkflowStarter(SwfClient client, Domain domain) {
        this.client = client;
        this.domain = domain;
    }

    public WorkflowExecution start() {
        var workflowId = UUID.randomUUID();
        var request = StartWorkflowExecutionRequest.builder()
                .taskList(TaskList.builder().name(Workflow.INITIAL_DECISION_TASK_LIST).build())
                .workflowType(WorkflowType.builder()
                        .name(SwfConfigData.SWF_WORKFLOW_NAME)
                        .version(SwfConfigData.STATIC_WORKFLOW_VERSION.toString())
                        .build())

                .domain(domain.name())
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
