package com.adolfoeloy.swflab.swf.service;

import com.adolfoeloy.swflab.swf.domain.activity.ActivitiesPoller;
import com.adolfoeloy.swflab.swf.domain.activity.ActivityTypes;
import com.adolfoeloy.swflab.swf.domain.decider.Decider;
import com.adolfoeloy.swflab.swf.domain.decider.DecisionTaskHistoryEventsHandler;
import com.adolfoeloy.swflab.swf.domain.Workflow;
import com.adolfoeloy.swflab.swf.domain.WorkflowExecution;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.swf.SwfClient;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class WorkflowStarter {
    private final Workflow workflow;
    private final ActivityTypes activityTypes;
    private final SwfClient swfClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DecisionTaskHistoryEventsHandler decisionTaskHistoryEventsHandler;
    private final ActivitiesPoller activitiesPoller;
    public WorkflowStarter(SwfClient swfClient, Workflow workflow, ActivityTypes activityTypes, DecisionTaskHistoryEventsHandler decisionTaskHistoryEventsHandler, ActivitiesPoller activitiesPoller) {
        this.swfClient = swfClient;
        this.workflow = workflow;
        this.activityTypes = activityTypes;
        this.decisionTaskHistoryEventsHandler = decisionTaskHistoryEventsHandler;
        this.activitiesPoller = activitiesPoller;
    }

    public WorkflowExecution start(UUID workflowId) {
        var workflowExecution = workflow.startExecution(swfClient, workflowId);
        pollForDecisions(workflowExecution);
        activitiesPoller.triggerPollingFor(workflowExecution);
        return workflowExecution;
    }

    private void pollForDecisions(WorkflowExecution workflowExecution) {
        try {
            executor.submit(new Decider(
                    swfClient,
                    workflow,
                    activityTypes,
                    workflowExecution,
                    decisionTaskHistoryEventsHandler
            )).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
