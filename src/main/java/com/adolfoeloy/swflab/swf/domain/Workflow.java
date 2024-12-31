package com.adolfoeloy.swflab.swf.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.ActivityType;
import software.amazon.awssdk.services.swf.model.Decision;
import software.amazon.awssdk.services.swf.model.DecisionType;
import software.amazon.awssdk.services.swf.model.RespondActivityTaskFailedRequest;
import software.amazon.awssdk.services.swf.model.RespondDecisionTaskCompletedRequest;
import software.amazon.awssdk.services.swf.model.ScheduleActivityTaskDecisionAttributes;
import software.amazon.awssdk.services.swf.model.StartWorkflowExecutionRequest;
import software.amazon.awssdk.services.swf.model.TaskList;
import software.amazon.awssdk.services.swf.model.TerminateWorkflowExecutionRequest;
import software.amazon.awssdk.services.swf.model.WorkflowType;

import java.util.List;
import java.util.Stack;
import java.util.UUID;

/**
 * WorkflowTypes are made available to the application as a managed bean
 *
 * @param domain
 * @param name
 * @param version
 * @param activities
 */
public record Workflow(
        Domain domain,
        String name,
        UUID version,
        String decisionTaskList,
        List<Activity> activities
) {
    private static final Logger logger = LoggerFactory.getLogger(Workflow.class);

    public boolean isSameWorkflow(String otherName, UUID otherVersion) {
        return name.equals(otherName) && version.equals(otherVersion);
    }

    public WorkflowExecution startExecution(SwfClient client, UUID workflowId) {
        var request = StartWorkflowExecutionRequest.builder()
                .taskList(TaskList.builder().name(
//                        decisionTaskList()
                        workflowId.toString()
                ).build())
                .workflowType(WorkflowType.builder()
                        .name(name())
                        .version(version().toString())
                        .build())

                .domain(domain().name())
                .workflowId(workflowId.toString())
                .executionStartToCloseTimeout("3600")
                .build();

        var response = client.startWorkflowExecution(request);

        var stack = new Stack<Activity>();
        activities.reversed().forEach(stack::push);

        return new WorkflowExecution(
                workflowId,
                response.runId(),
                stack
        );
    }

    void scheduleActivityTask(SwfClient client, String taskToken, Activity activity, ActivityTaskOptions options) {
        var attrsBuilder = ScheduleActivityTaskDecisionAttributes.builder()
                .activityType(ActivityType.builder()
                        .name(activity.name())
                        .version(activity.version())
                        .build()
                );

        switch (options) {
            case ActivityTaskOptions.ActivityTaskOptionsWithInput(String activityId, String input) -> attrsBuilder.input(input).activityId(activityId);
            case ActivityTaskOptions.ActivityTaskOptionsWithoutInput(String activityId) -> attrsBuilder.activityId(activityId);
        }

        var attrs = attrsBuilder.build();

        var decisions = List.of(
                Decision.builder()
                        .decisionType(DecisionType.SCHEDULE_ACTIVITY_TASK)
                        .scheduleActivityTaskDecisionAttributes(attrs)
                        .build()
        );

        var request = RespondDecisionTaskCompletedRequest.builder()
                .taskList(TaskList.builder().name(decisionTaskList()).build())
                .decisions(decisions)
                .taskToken(taskToken)
                .build();

        client.respondDecisionTaskCompleted(request);
        logger.info("Responded decision task completed to SWF");
    }

    public void signalCompleted(SwfClient client, DecisionType decisionType) {
        var decisions = List.of(
                Decision.builder()
                        .decisionType(decisionType)
                        .build()
        );

        var request = RespondDecisionTaskCompletedRequest.builder()
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
