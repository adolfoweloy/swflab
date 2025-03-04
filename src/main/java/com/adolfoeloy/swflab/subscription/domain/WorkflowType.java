package com.adolfoeloy.swflab.subscription.domain;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.Decision;
import software.amazon.awssdk.services.swf.model.DecisionType;
import software.amazon.awssdk.services.swf.model.RespondActivityTaskFailedRequest;
import software.amazon.awssdk.services.swf.model.RespondDecisionTaskCompletedRequest;
import software.amazon.awssdk.services.swf.model.StartWorkflowExecutionRequest;
import software.amazon.awssdk.services.swf.model.TaskList;
import software.amazon.awssdk.services.swf.model.TerminateWorkflowExecutionRequest;

/**
 * WorkflowTypes are made available to the application as a managed bean
 *
 * @param domain
 * @param name
 * @param version
 */
public record WorkflowType(Domain domain, String name, UUID version, String decisionTaskList) {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowType.class);

    public boolean isSame(String otherName, UUID otherVersion) {
        return name.equals(otherName) && version.equals(otherVersion);
    }

    public String getDecisionTaskListFor(String workflowId) {
        return workflowId + "_" + decisionTaskList();
    }

    public WorkflowExecution startExecution(SwfClient client, UUID workflowId) {
        var decisionTaskList = getDecisionTaskListFor(workflowId.toString());
        var request = StartWorkflowExecutionRequest.builder()
                .taskList(TaskList.builder().name(decisionTaskList).build())
                .workflowType(software.amazon.awssdk.services.swf.model.WorkflowType.builder()
                        .name(name())
                        .version(version().toString())
                        .build())
                .domain(domain().name())
                .workflowId(workflowId.toString())
                .executionStartToCloseTimeout("3600")
                .build();

        var response = client.startWorkflowExecution(request);

        return new WorkflowExecution(workflowId, response.runId(), decisionTaskList, domain);
    }

    public void signalCompleted(SwfClient client, String taskToken, DecisionType decisionType) {
        var decisions = List.of(Decision.builder().decisionType(decisionType).build());

        var request = RespondDecisionTaskCompletedRequest.builder()
                .taskToken(taskToken)
                .taskList(TaskList.builder().name(decisionTaskList()).build())
                .decisions(decisions)
                .build();

        client.respondDecisionTaskCompleted(request);
        logger.info("Responded with {} workflow execution to SWF", decisionType.name());
    }

    public void signalTerminate(SwfClient client, WorkflowExecution workflowExecution, String reason) {
        var request = TerminateWorkflowExecutionRequest.builder()
                .domain(domain().name())
                .workflowId(workflowExecution.workflowId().toString())
                .runId(workflowExecution.runId())
                .reason(reason)
                .build();

        client.terminateWorkflowExecution(request);
        logger.info("Responded with task terminated signal to SWF");
    }

    public void signalFail(SwfClient client, String taskToken, String reason) {
        var request = RespondActivityTaskFailedRequest.builder()
                .taskToken(taskToken)
                .reason(reason)
                .build();

        client.respondActivityTaskFailed(request);
        logger.info("Responded with task failed signal to SWF with reason {}", reason);
    }
}
