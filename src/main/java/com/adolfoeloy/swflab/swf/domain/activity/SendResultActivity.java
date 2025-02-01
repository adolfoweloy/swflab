package com.adolfoeloy.swflab.swf.domain.activity;

import com.adolfoeloy.swflab.swf.domain.Task;
import com.adolfoeloy.swflab.swf.domain.activity.model.SubscriptionData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

public class SendResultActivity extends ActivityBase{
    private final ActivityMessageBuilder activityMessageBuilder;
    private final ObjectMapper objectMapper;
    private final SnsClient snsClient;

    protected SendResultActivity(ActivityMessageBuilder activityMessageBuilder, ObjectMapper objectMapper, SnsClient snsClient) {
        super("send_result_activity");
        this.activityMessageBuilder = activityMessageBuilder;
        this.objectMapper = objectMapper;
        this.snsClient = snsClient;
    }

    @Override
    public boolean doActivity(Task task) {
        if (task.input() == null) {
            setResults(activityMessageBuilder.errorMessage("Didn't receive any input!"));
            return false;
        }

        try {
            var subscriptionData = objectMapper.readValue(task.input(), SubscriptionData.class);

            var message = "Thanks, you've successfully confirmed registration, and your workflow is complete";
            var publishRequest = PublishRequest.builder()
                .topicArn(subscriptionData.topicArn())
                .message(message)
                .build();

            snsClient.publish(publishRequest);

            return true;
        } catch (JsonProcessingException e) {
            setResults(activityMessageBuilder.errorMessage("Could not load subscription data!"));
            return false;
        }
    }
}
