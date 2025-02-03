package com.adolfoeloy.swflab.subscription.service;

import com.adolfoeloy.swflab.subscription.domain.WorkflowExecution;
import com.adolfoeloy.swflab.subscription.domain.WorkflowType;
import com.adolfoeloy.swflab.subscription.domain.activity.ActivitiesPoller;
import com.adolfoeloy.swflab.subscription.domain.activity.ActivityTypes;
import com.adolfoeloy.swflab.subscription.domain.decider.Decider;
import com.adolfoeloy.swflab.subscription.domain.decider.DecisionTaskHistoryEventsHandler;
import com.adolfoeloy.swflab.subscription.domain.decider.DecisionTasks;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.swf.SwfClient;

@Component
public class WorkflowStarter {
    private final WorkflowType workflowType;
    private final ActivityTypes activityTypes;
    private final SwfClient swfClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DecisionTaskHistoryEventsHandler decisionTaskHistoryEventsHandler;
    private final ActivitiesPoller activitiesPoller;

    public WorkflowStarter(
            SwfClient swfClient,
            WorkflowType workflowType,
            ActivityTypes activityTypes,
            DecisionTaskHistoryEventsHandler decisionTaskHistoryEventsHandler,
            ActivitiesPoller activitiesPoller) {
        this.swfClient = swfClient;
        this.workflowType = workflowType;
        this.activityTypes = activityTypes;
        this.decisionTaskHistoryEventsHandler = decisionTaskHistoryEventsHandler;
        this.activitiesPoller = activitiesPoller;
    }

    public WorkflowExecution start(UUID workflowId) {
        var workflowExecution = workflowType.startExecution(swfClient, workflowId);
        pollForDecisions(workflowExecution);
        activitiesPoller.triggerPollingFor(workflowExecution);
        return workflowExecution;
    }

    private void pollForDecisions(WorkflowExecution workflowExecution) {
        var decisionTasks =
                new DecisionTasks(workflowType, workflowExecution, decisionTaskHistoryEventsHandler, swfClient);

        executor.submit(new Decider(swfClient, workflowType, activityTypes, workflowExecution, decisionTasks));
    }
}
