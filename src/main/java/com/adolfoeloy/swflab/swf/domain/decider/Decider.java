package com.adolfoeloy.swflab.swf.domain.decider;

import com.adolfoeloy.swflab.swf.domain.Workflow;
import com.adolfoeloy.swflab.swf.domain.WorkflowExecution;
import com.adolfoeloy.swflab.swf.domain.activity.ActivityTaskOptions.ActivityTaskOptionsWithInput;
import com.adolfoeloy.swflab.swf.domain.activity.ActivityTaskOptions.ActivityTaskOptionsWithoutInput;
import com.adolfoeloy.swflab.swf.domain.activity.ActivityTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.DecisionType;
import software.amazon.awssdk.services.swf.model.EventType;
import software.amazon.awssdk.services.swf.model.HistoryEvent;

/**
 * According to AWS docs, the decider directs the workflow by receiving decision tasks from Amazon SWF and responding
 * back to Amazon SWF with decisions. This can be simply translated to: the decider is a program that developers write
 * which polls decision tasks from SWF.
 *
 * Decider responds to decision tasks by either:
 * - scheduling new activities, cancelling and restarting activities
 * - setting the state of the workflow execution as complete, cancelled, or failed
 *
 * PS: This class name is consistent at the extent that this is the entrypoint of deciders
 * which I'm actually implementing as {@code Runnable} submitted to an {@code ExecutorService}.
 * The Java example using SDK 1.0 calls it {@code WorkflowWorker}
 */
public class Decider implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Decider.class);

    private final SwfClient client;
    private final Workflow workflow;
    private final ActivityTypes activityTypes;
    private final WorkflowExecution workflowExecution;
    private final DecisionTasks decisionTasks;

    public Decider(
            SwfClient swfClient,
            Workflow workflow,
            ActivityTypes activityTypes,
            WorkflowExecution workflowExecution,
            DecisionTasks decisionTasks
    ) {
        this.client = swfClient;
        this.workflow = workflow;
        this.activityTypes = activityTypes;
        this.workflowExecution = workflowExecution;
        this.decisionTasks = decisionTasks;
    }

    @Override
    public void run() {
        // The workflowId here is simply the task list (hopefully created dynamically)
        // A workflow was started with this ID
        var workflowId = workflowExecution.workflowId().toString();

        // TODO: maybe this can be created when starting to poll?
        var activityTypesStack = activityTypes.stackOfActivityTypes();

        // this design looks closer to the example in Ruby given by SWF docs from AWS
        // https://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-sns-tutorial-implementing-workflow.html#polling-for-decisions
        decisionTasks.poll(decisionTask -> {

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
                            workflow.signalCompleted(client, decisionTask.taskToken(), DecisionType.COMPLETE_WORKFLOW_EXECUTION);
                            return false;

                        } else {
                            var eventAttributesResult = event.activityTaskCompletedEventAttributes().result();
                            var options = (eventAttributesResult != null)
                                    ? new ActivityTaskOptionsWithInput(taskList, eventAttributesResult)
                                    : new ActivityTaskOptionsWithoutInput(taskList);

                            logger.info("Scheduling activity task {}", activityTypesStack.peek());
                            decisionTask.scheduleActivityTask(client, activityTypesStack.peek(), options);
                        }
                    }

                    case EventType.ACTIVITY_TASK_TIMED_OUT -> {
                        workflow.signalFail(client, decisionTask.taskToken(), "Task timed out");
                        return false;
                    }

                    case EventType.ACTIVITY_TASK_FAILED -> {
                        workflow.signalFail(client, decisionTask.taskToken(), "Activity task failed");
                        return false;
                    }

                    case EventType.WORKFLOW_EXECUTION_COMPLETED -> {
                        logger.info("** End of execution of the workflow {}", workflowId);
                        workflow.signalTerminate(client, workflowExecution, "Workflow execution is complete!");
                        return false;
                    }

                    default -> handleSignal(event.eventType());
                }
            }

            return true;
        });

    }

    public void handleSignal(EventType eventType) {
        logger.info("Received signal from SWF: {}", eventType.name());
    }

}
