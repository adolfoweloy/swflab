package com.adolfoeloy.swflab.swf.service;

import com.adolfoeloy.swflab.swf.domain.Decider;
import com.adolfoeloy.swflab.swf.domain.DecisionTaskHistoryEventsHandler;
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
    private final SwfClient swfClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DecisionTaskHistoryEventsHandler decisionTaskHistoryEventsHandler;

    public WorkflowStarter(SwfClient swfClient, Workflow workflow, DecisionTaskHistoryEventsHandler decisionTaskHistoryEventsHandler) {
        this.swfClient = swfClient;
        this.workflow = workflow;
        this.decisionTaskHistoryEventsHandler = decisionTaskHistoryEventsHandler;
    }

    public WorkflowExecution start(UUID workflowId) {
        var workflowExecution = workflow.startExecution(swfClient, workflowId);
        pollForDecisions(workflowExecution);
        return workflowExecution;
    }

    private void pollForDecisions(WorkflowExecution workflowExecution) {
        try {
            executor.submit(new Decider(
                    swfClient,
                    workflow,
                    workflowExecution,
                    decisionTaskHistoryEventsHandler
            )).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
