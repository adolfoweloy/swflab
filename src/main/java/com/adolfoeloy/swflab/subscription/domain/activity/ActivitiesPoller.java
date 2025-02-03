package com.adolfoeloy.swflab.subscription.domain.activity;

import com.adolfoeloy.swflab.subscription.domain.Task;
import com.adolfoeloy.swflab.subscription.domain.WorkflowExecution;
import com.adolfoeloy.swflab.subscription.domain.workflow.SwfWorkflowRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.PollForActivityTaskRequest;
import software.amazon.awssdk.services.swf.model.RespondActivityTaskCompletedRequest;
import software.amazon.awssdk.services.swf.model.RespondActivityTaskFailedRequest;
import software.amazon.awssdk.services.swf.model.TaskList;

@Component
public class ActivitiesPoller {
    private static final Logger logger = LoggerFactory.getLogger(ActivitiesPoller.class);

    private final SwfClient swfClient;
    private final ObjectMapper objectMapper;
    private final Map<String, ActivityBase> activityRegistry = new HashMap<>();

    private final List<ActivityBase> activities;

    public ActivitiesPoller(
            SnsClient snsClient,
            SwfClient swfClient,
            ActivityMessageBuilder activityMessageBuilder,
            ObjectMapper objectMapper,
            SwfWorkflowRepository swfWorkflowRepository) {
        this.swfClient = swfClient;
        this.objectMapper = objectMapper;

        activities = List.of(
                new GetContactActivity(objectMapper, swfClient, swfWorkflowRepository),
                new SubscribeTopicActivity(snsClient, objectMapper, activityMessageBuilder),
                new WaitForConfirmationActivity(objectMapper, snsClient, swfClient, activityMessageBuilder),
                new SendResultActivity(activityMessageBuilder, objectMapper, snsClient));
    }

    @PostConstruct
    void initialize() {
        for (var activity : activities) {
            activityRegistry.put(activity.getName(), activity);
        }
    }

    /**
     * Trigger activity poller for a given workflowID
     */
    public void triggerPollingFor(WorkflowExecution workflowExecution) {

        new Thread(() -> {
                    while (true) {
                        var pollForActivityTaskRequest = PollForActivityTaskRequest.builder()
                                .domain(workflowExecution.domain().name())
                                .taskList(TaskList.builder()
                                        .name(workflowExecution.workflowId().toString() + "_activities")
                                        .build())
                                .build();

                        var result = swfClient.pollForActivityTask(pollForActivityTaskRequest);

                        var activityType = result.activityType();
                        var activity = activityRegistry.get(activityType.name());

                        if (activity != null) {
                            logger.info("** Starting activity task: {}", activity.getName());

                            if (activity.doActivity(new Task(
                                    result.workflowExecution().workflowId(), result.input(), result.taskToken()))) {
                                logger.info("++ Activity task completed: {}", activity.getName());

                                // must signal that the activity completed
                                var completedRequest = RespondActivityTaskCompletedRequest.builder()
                                        .taskToken(result.taskToken())
                                        .result(activity.getResults()) // this is how the output of an activity becomes
                                        // the input for another activity
                                        .build();
                                swfClient.respondActivityTaskCompleted(completedRequest);

                                if (activity.getName().equals("send_result_activity")) {
                                    break; // stop polling
                                }
                            } else {
                                logger.info("-- Activity task failed: {}", activity.getName());
                                try {
                                    var errorResult = objectMapper.readValue(
                                            activity.getResults(), ActivityMessageBuilder.Error.class);
                                    var failedRequest = RespondActivityTaskFailedRequest.builder()
                                            .taskToken(result.taskToken())
                                            .reason(errorResult.reason())
                                            .details(errorResult.detail())
                                            .build();

                                    // must signal that the activity failed
                                    swfClient.respondActivityTaskFailed(failedRequest);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        } else {
                            logger.error("couldn't find key in activitiesRegistry: {}", activityType.name());
                        }
                    }
                })
                .start();
    }
}
