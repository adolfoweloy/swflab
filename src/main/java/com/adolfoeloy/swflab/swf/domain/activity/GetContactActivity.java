package com.adolfoeloy.swflab.swf.domain.activity;

import com.adolfoeloy.swflab.swf.domain.Task;
import com.adolfoeloy.swflab.swf.domain.workflow.SwfWorkflowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.RespondActivityTaskFailedRequest;

public class GetContactActivity extends ActivityBase {

    private final ObjectMapper objectMapper;
    private final SwfClient swfClient;
    private final SwfWorkflowRepository swfWorkflowRepository;

    protected GetContactActivity(
            ObjectMapper objectMapper, SwfClient swfClient, SwfWorkflowRepository swfWorkflowRepository) {
        super("get_contact_activity");
        this.objectMapper = objectMapper;
        this.swfClient = swfClient;
        this.swfWorkflowRepository = swfWorkflowRepository;
    }

    @Override
    public boolean doActivity(Task task) {

        try {
            var contactInfoMap = waitForContactInformation(task.workflowId()).get(5, TimeUnit.MINUTES);

            String result = objectMapper.writeValueAsString(contactInfoMap);
            setResults(result);
            return true;

        } catch (Exception e) {

            // signal SWF that the activity failed
            var taskFailedRequest = RespondActivityTaskFailedRequest.builder()
                    .taskToken(task.taskToken())
                    .reason("Could not retrieve contact information")
                    .build();

            swfClient.respondActivityTaskFailed(taskFailedRequest);
            return false;
        }
    }

    @VisibleForTesting
    CompletableFuture<Map<String, String>> waitForContactInformation(String workflowId) {
        // TODO: wait for contact info to be available (the ruby example reads from stdio, but here I have a web app)
        // results = { :email => email, :sms => phone }

        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        new Thread(() -> {
                    while (true) {
                        var maybeWorkflow = swfWorkflowRepository.findByWorkflowId(workflowId);

                        if (maybeWorkflow.isPresent()) {
                            var workflow = maybeWorkflow.get();
                            var result = Map.of(
                                    "email", workflow.getEmail(),
                                    "sms", workflow.getPhone());
                            future.complete(result);
                            break; // no need to wait
                        }

                        try {
                            Thread.sleep(Duration.ofSeconds(10));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .start();

        return future;
    }
}
