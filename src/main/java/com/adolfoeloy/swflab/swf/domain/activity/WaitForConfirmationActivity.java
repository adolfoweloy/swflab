package com.adolfoeloy.swflab.swf.domain.activity;

import com.adolfoeloy.swflab.swf.domain.Task;
import com.adolfoeloy.swflab.swf.domain.activity.model.SubscriptionData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.RecordActivityTaskHeartbeatRequest;

import java.time.Duration;

public class WaitForConfirmationActivity extends ActivityBase {
    private final Logger logger = LoggerFactory.getLogger(WaitForConfirmationActivity.class);
    private final ObjectMapper objectMapper;
    private final SnsClient snsClient;
    private final SwfClient swfClient;
    private final ActivityMessageBuilder activityMessageBuilder;

    protected WaitForConfirmationActivity(
            ObjectMapper objectMapper,
            SnsClient snsClient,
            SwfClient swfClient,
            ActivityMessageBuilder activityMessageBuilder
    ) {
        super("wait_for_confirmation_activity");
        this.objectMapper = objectMapper;
        this.snsClient = snsClient;
        this.swfClient = swfClient;
        this.activityMessageBuilder = activityMessageBuilder;
    }

    @Override
    public boolean doActivity(Task task) {

        if (task.input() == null) {
            setResults(activityMessageBuilder.errorMessage("Didn't receive any input!"));
            return false;
        }

        try {
            // load subscription data
            var subscriptionData = objectMapper.readValue(task.input(), SubscriptionData.class);

            // read subscriptions from the topic
            var listSubscriptionsRequest = ListSubscriptionsByTopicRequest.builder()
                    .topicArn(subscriptionData.topicArn())
                    .build();
            var listSubscriptionsByTopicResponse = snsClient.listSubscriptionsByTopic(listSubscriptionsRequest);
            if (listSubscriptionsByTopicResponse.hasSubscriptions()) {
                var subscriptions = listSubscriptionsByTopicResponse.subscriptions();

                // loop until subscription is confirmed
                var subscriptionConfirmed = false;
                while (!subscriptionConfirmed) {

                    for (Subscription sub : subscriptions) {
                        var endpointConfig = subscriptionData.endpointConfig().get(sub.protocol());
                        if (endpointConfig != null && endpointConfig.get("endpoint").equals(sub.endpoint())) {
                            if (!sub.subscriptionArn().equals("PendingConfirmation")) {
                                subscriptionData.endpointConfig().get(sub.protocol()).put("subscription_arn", sub.subscriptionArn());
                                logger.info("Topic subscription confirmed for ({}: {})", sub.protocol(), sub.endpoint());
                                try {
                                    setResults(objectMapper.writeValueAsString(subscriptionData));
                                    return true;
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                logger.info("Topic subscription still pending for ({}: {})",
                                        sub.protocol(), sub.endpoint());

                                // signal heartbeat to tell SWF that the activity is still processing
                                var request = RecordActivityTaskHeartbeatRequest.builder()
                                                .taskToken(task.taskToken())
                                                .build();
                                swfClient.recordActivityTaskHeartbeat(request);
                                try {
                                    Thread.sleep(Duration.ofSeconds(4));
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                        }
                    }

                }
            } else {
                setResults(activityMessageBuilder.errorMessage("Couldn't get SWF topic ARN"));
                return false;
            }


        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return false;
    }


}
