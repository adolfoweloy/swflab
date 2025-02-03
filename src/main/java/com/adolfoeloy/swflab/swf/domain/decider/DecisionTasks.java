package com.adolfoeloy.swflab.swf.domain.decider;

import com.adolfoeloy.swflab.swf.domain.WorkflowType;
import com.adolfoeloy.swflab.swf.domain.WorkflowExecution;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.PollForDecisionTaskRequest;
import software.amazon.awssdk.services.swf.model.TaskList;

/**
 * This class brings the poll logic that is similar to how the Ruby implementation of SWF client works.
 */
public class DecisionTasks {
    private static final Logger logger = LoggerFactory.getLogger(DecisionTasks.class);

    private final WorkflowType workflowType;
    private final WorkflowExecution workflowExecution;
    private final DecisionTaskHistoryEventsHandler decisionTaskHistoryEventsHandler;
    private final SwfClient swfClient;

    public DecisionTasks(
            WorkflowType workflowType,
            WorkflowExecution workflowExecution,
            DecisionTaskHistoryEventsHandler decisionTaskHistoryEventsHandler,
            SwfClient swfClient) {
        this.workflowType = workflowType;
        this.workflowExecution = workflowExecution;
        this.decisionTaskHistoryEventsHandler = decisionTaskHistoryEventsHandler;
        this.swfClient = swfClient;
    }

    /**
     * This method behaves like the Ruby version of SWF client implementation
     *
     * @param decisionTaskBlock provide the logic to handle the decision task received as a result of polling decision tasks from SWF.
     * @see <a href="https://docs.aws.amazon.com/sdk-for-ruby/v1/api/AWS/SimpleWorkflow/DecisionTaskCollection.html#poll-instance_method">
     *     DecisionTaskCollection#poll-instance_method</a>
     */
    public void poll(Function<DecisionTask, Boolean> decisionTaskBlock) {

        while (true) {
            var workflowId = workflowExecution.workflowId().toString();
            var pollingRequestBuilder = pollingRequestBuilder(workflowId);
            var pollForDecisionTaskResponse = swfClient.pollForDecisionTask(pollingRequestBuilder.build());

            var decisionTask = decisionTaskHistoryEventsHandler.createDecisionTaskWithEvents(
                    pollForDecisionTaskResponse, pollingRequestBuilder);

            try {
                var result = decisionTaskBlock.apply(decisionTask);

                if (!result) {
                    logger.warn("-- Stop polling for decisions");
                    break;
                }

            } catch (Throwable t) {
                logger.error("Error while running logic for a decision task", t);
                break;
            }
        }
    }

    private PollForDecisionTaskRequest.Builder pollingRequestBuilder(String workflowId) {
        logger.info("Polling decision tasks from decision task list {}", workflowType.getDecisionTaskListFor(workflowId));
        return PollForDecisionTaskRequest.builder()
                .domain(workflowType.domain().name())
                .identity(Thread.currentThread().getName())
                .taskList(TaskList.builder()
                        .name(workflowType.getDecisionTaskListFor(workflowId))
                        .build())
                .maximumPageSize(1000)
                .reverseOrder(false);
    }
}
