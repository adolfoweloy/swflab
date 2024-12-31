package com.adolfoeloy.swflab.swf.domain;

import com.adolfoeloy.swflab.swf.domain.Decider.ActivityTaskOptions.ActivityTaskOptionsWithInput;
import com.adolfoeloy.swflab.swf.domain.Decider.ActivityTaskOptions.ActivityTaskOptionsWithoutInput;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.ActivityType;
import software.amazon.awssdk.services.swf.model.Decision;
import software.amazon.awssdk.services.swf.model.DecisionType;
import software.amazon.awssdk.services.swf.model.EventType;
import software.amazon.awssdk.services.swf.model.HistoryEvent;
import software.amazon.awssdk.services.swf.model.PollForDecisionTaskRequest;
import software.amazon.awssdk.services.swf.model.PollForDecisionTaskResponse;
import software.amazon.awssdk.services.swf.model.RespondDecisionTaskCompletedRequest;
import software.amazon.awssdk.services.swf.model.ScheduleActivityTaskDecisionAttributes;
import software.amazon.awssdk.services.swf.model.TaskList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;

/**
 * According to AWS docs, the decider directs the workflow by receiving decision tasks from Amazon SWF and responding
 * back to Amazon SWF with decisions. This can be simply translated to: the decider is a program that developers write
 * which polls decision tasks from SWF. This class name is consistent at the extent that this is the entrypoint of deciders
 * which I'm actually implementing as {@code Runnable} submitted to an {@code ExecutorService}.
 * The Java example using SDK 1.0 calls it {@code WorkflowWorker}
 */
public class Decider implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Decider.class);

    private final SwfClient client;
    private final Workflow workflow;

    public Decider(SwfClient swfClient, Workflow workflow) {
        this.client = swfClient;
        this.workflow = workflow;
    }

    @Override
    public void run() {

        while (true) {
            var activityList = new Stack<Activity>();

            // The decider uses the workflow execution's task list name to receive decision tasks to respond to
            var workflowId = workflow.decisionTaskList();
            var requestBuilder = getDecisionTaskRequestBuilder(workflowId);
            var task = client.pollForDecisionTask(requestBuilder.build());

            mapDecisionTasks(task, requestBuilder).ifPresent(decisionTask -> {

                final List<HistoryEvent> newEvents;
                if (task.previousStartedEventId() == null || task.previousStartedEventId() == 0) {
                    workflow.activities().reversed().forEach(activityList::push);
                    newEvents = task.events();
                } else {
                    newEvents = decisionTask.newEvents(task.previousStartedEventId());
                }

                newEvents.forEach(event -> {
                    // respond to decision tasks by either
                    // - scheduling new activities
                    // - cancelling and restarting activities
                    // - setting state of the workflow (e.g. complete, cancelled or failed).

                    var activityId = workflowId + "-activities";

                    switch (event.eventType()) {

                        case EventType.WORKFLOW_EXECUTION_STARTED -> {
                            var options = new ActivityTaskOptionsWithoutInput(activityId);
                            scheduleActivityTask(activityList.peek(), options);
                        }

                        case EventType.ACTIVITY_TASK_COMPLETED -> {
                            var lastActivity = activityList.pop();

                            if (activityList.isEmpty()) {
                                completeWorkflowExecution();
                            } else {
                                logger.info("Scheduling activity task {}", activityList.peek());

                                var eventAttributesResult = event.activityTaskCompletedEventAttributes().result();
                                var options = (eventAttributesResult != null)
                                        ? new ActivityTaskOptionsWithInput(activityId, eventAttributesResult)
                                        : new ActivityTaskOptionsWithoutInput(activityId);
                                scheduleActivityTask(activityList.peek(), options);
                            }
                        }

                        case EventType.ACTIVITY_TASK_TIMED_OUT,
                             EventType.ACTIVITY_TASK_FAILED -> failWorkflowExecution();

                        case EventType.WORKFLOW_EXECUTION_COMPLETED -> terminate();

                        default -> otherwise(event);
                    }
                });

            });

        }

    }

    sealed interface ActivityTaskOptions {
        record ActivityTaskOptionsWithInput(String activityId, String input) implements ActivityTaskOptions {}
        record ActivityTaskOptionsWithoutInput(String activityId) implements ActivityTaskOptions {}
    }

    private void terminate() {
        signalResponse(
                DecisionType.COMPLETE_WORKFLOW_EXECUTION,
                client::respondDecisionTaskCompleted);
    }

    private void failWorkflowExecution() {
        signalResponse(
                DecisionType.FAIL_WORKFLOW_EXECUTION,
                client::respondDecisionTaskCompleted);
    }

    private void completeWorkflowExecution() {
        signalResponse(
                DecisionType.COMPLETE_WORKFLOW_EXECUTION,
                client::respondDecisionTaskCompleted);
    }

    private void signalResponse(DecisionType decisionType, Consumer<RespondDecisionTaskCompletedRequest> swfCall) {
        var decisions = List.of(
                Decision.builder()
                        .decisionType(decisionType)
                        .build()
        );

        var request = RespondDecisionTaskCompletedRequest.builder()
                .taskList(TaskList.builder().name(workflow.decisionTaskList()).build())
                .decisions(decisions)
                .build();

        swfCall.accept(request);
        logger.info("Responded with {} workflow execution to SWF", decisionType.name());
    }

    private void scheduleActivityTask(Activity activity, ActivityTaskOptions options) {
        var attrsBuilder = ScheduleActivityTaskDecisionAttributes.builder()
                .activityType(ActivityType.builder()
                        .name(activity.name())
                        .version(activity.version())
                        .build()
                );

        switch (options) {
            case ActivityTaskOptionsWithInput(String activityId, String input) -> attrsBuilder.input(input).activityId(activityId);
            case ActivityTaskOptionsWithoutInput(String activityId) -> attrsBuilder.activityId(activityId);
        }

        var attrs = attrsBuilder.build();

        var decisions = List.of(
                Decision.builder()
                        .decisionType(DecisionType.SCHEDULE_ACTIVITY_TASK)
                        .scheduleActivityTaskDecisionAttributes(attrs)
                        .build()
        );

        var request = RespondDecisionTaskCompletedRequest.builder()
                .taskList(TaskList.builder().name(workflow.decisionTaskList()).build())
                .decisions(decisions)

                .build();

        client.respondDecisionTaskCompleted(request);
        logger.info("Responded decision task completed to SWF");
    }

    private void otherwise(HistoryEvent event) {
        logger.info("handling an unknown event {}", event.eventType().name());
    }

    private Optional<DecisionTask> mapDecisionTasks(PollForDecisionTaskResponse response, PollForDecisionTaskRequest.Builder requestBuilder) {

        if (StringUtils.isNotBlank(response.taskToken())) {
            logger.info("Decision task received with token {}", response.taskToken());

            // fetch all events
            var nextToken = response.nextPageToken();
            var events = new ArrayList<>(response.events());

            while (StringUtils.isNotBlank(nextToken)) {
                var nextResponse = client.pollForDecisionTask(requestBuilder.nextPageToken(nextToken).build());
                events.addAll(nextResponse.events());
            }

            return Optional.of(new DecisionTask(response.startedEventId(), events));
        }

        return Optional.empty();
    }

    private PollForDecisionTaskRequest.Builder getDecisionTaskRequestBuilder(String decisionTaskList) {
        return PollForDecisionTaskRequest.builder()
                .domain(workflow.domain().name())
                .identity(Thread.currentThread().getName())
                .taskList(TaskList.builder().name(decisionTaskList).build())
                .maximumPageSize(1000)
                .reverseOrder(false);
    }

    private record DecisionTask(Long starterEventId, List<HistoryEvent> events) {
        List<HistoryEvent> newEvents(Long previousDecisionStartedEventId) {
            return events.stream()
                    .filter(event -> event.eventId().equals(previousDecisionStartedEventId))
                    .toList();
        }
    }
}
