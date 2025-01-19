package com.adolfoeloy.swflab.swf.domain;

import com.adolfoeloy.swflab.swf.domain.ActivityTaskOptions.ActivityTaskOptionsWithInput;
import com.adolfoeloy.swflab.swf.domain.ActivityTaskOptions.ActivityTaskOptionsWithoutInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.DecisionType;
import software.amazon.awssdk.services.swf.model.EventType;
import software.amazon.awssdk.services.swf.model.HistoryEvent;
import software.amazon.awssdk.services.swf.model.PollForDecisionTaskRequest;
import software.amazon.awssdk.services.swf.model.TaskList;

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
    private final ActivityTypes activityTypes;
    private final WorkflowExecution workflowExecution;
    private final DecisionTaskHistoryEventsHandler decisionTaskHistoryEventsHandler;

    public Decider(
            SwfClient swfClient,
            Workflow workflow,
            ActivityTypes activityTypes,
            WorkflowExecution workflowExecution,
            DecisionTaskHistoryEventsHandler decisionTaskHistoryEventsHandler
    ) {
        this.client = swfClient;
        this.workflow = workflow;
        this.activityTypes = activityTypes;
        this.workflowExecution = workflowExecution;
        this.decisionTaskHistoryEventsHandler = decisionTaskHistoryEventsHandler;
    }

    @Override
    public void run() {
        // The workflowId here is simply the task list (hopefully created dynamically)
        // A workflow was started with this ID
        var workflowId = workflowExecution.workflowId().toString();
        var activityTypesStack = activityTypes.stackOfActivityTypes();
        var pollingRequestBuilder = pollingRequestBuilder(workflowId);

        var pollForDecisionTaskResponse = client.pollForDecisionTask(pollingRequestBuilder.build());
        var decisionTask = decisionTaskHistoryEventsHandler.createDecisionTaskWithEvents(
                pollForDecisionTaskResponse,
                pollingRequestBuilder
        );
        var newEvents = decisionTask.getNewEvents();

        for (HistoryEvent event : newEvents) {
            var taskList = workflowId + "_activities";

            switch (event.eventType()) {

                case EventType.WORKFLOW_EXECUTION_STARTED -> {
                    var options = new ActivityTaskOptionsWithoutInput(taskList);
                    decisionTask.scheduleActivityTask(client, activityTypesStack.peek(), options);
                }

                case EventType.ACTIVITY_TASK_COMPLETED -> {
                    var lastActivity = activityTypesStack.pop();

                    if (activityTypesStack.isEmpty()) {
                        workflow.signalCompleted(client, DecisionType.COMPLETE_WORKFLOW_EXECUTION);
                        return; // finishes processing the workflow

                    } else {
                        var eventAttributesResult = event.activityTaskCompletedEventAttributes().result();
                        var options = (eventAttributesResult != null)
                                ? new ActivityTaskOptionsWithInput(taskList, eventAttributesResult)
                                : new ActivityTaskOptionsWithoutInput(taskList);

                        logger.info("Scheduling activity task {}", activityTypesStack.peek());
                        decisionTask.scheduleActivityTask(client, activityTypesStack.peek(), options);
                    }
                }

                case EventType.ACTIVITY_TASK_TIMED_OUT -> workflow.signalFail(client, pollForDecisionTaskResponse.taskToken(), "Task timed out");

                case EventType.ACTIVITY_TASK_FAILED -> workflow.signalFail(client, pollForDecisionTaskResponse.taskToken(), "Activity task failed");

                case EventType.WORKFLOW_EXECUTION_COMPLETED -> workflow.signalTerminate(client, workflowExecution, "Workflow execution is complete!");

                default -> handleSignal(event.eventType());
            }
        }
    }

    public void handleSignal(EventType eventType) {
        logger.info("Received signal from SWF: {}", eventType.name());
    }

    private PollForDecisionTaskRequest.Builder pollingRequestBuilder(String workflowId) {
        logger.info("Polling decision tasks from decision task list {}", workflow.getDecisionTaskListFor(workflowId));
        return PollForDecisionTaskRequest.builder()
                .domain(workflow.domain().name())
                .identity(Thread.currentThread().getName())
                .taskList(TaskList.builder().name(workflow.getDecisionTaskListFor(workflowId)).build())
                .maximumPageSize(1000)
                .reverseOrder(false);
    }

}
