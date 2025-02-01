package com.adolfoeloy.swflab.swf.domain.activity;

import com.adolfoeloy.swflab.swf.domain.Task;
import com.adolfoeloy.swflab.swf.domain.activity.model.SubscriptionData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import io.micrometer.common.util.StringUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class SubscribeTopicActivity extends ActivityBase {
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final ActivityMessageBuilder activityMessageBuilder;

    public SubscribeTopicActivity(SnsClient snsClient, ObjectMapper objectMapper, ActivityMessageBuilder activityMessageBuilder) {
        super("subscribe_topic_activity");
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.activityMessageBuilder = activityMessageBuilder;
    }

    @Override
    public boolean doActivity(Task task) {
        if (task.input() != null) {

            try {
                var maybeTopicArn = createTopic();
                if (maybeTopicArn.isEmpty()) {
                    setResults(activityMessageBuilder.errorMessage("Couldn't create the SNS topic"));
                    return false;
                }

                var topicArn = maybeTopicArn.get();

                // create activity data
                var typeReference = new TypeReference<HashMap<String, String>>() {};
                var input = objectMapper.readValue(task.input(), typeReference);
                var activityDataOptional = createActivityData(topicArn, input);
                if (activityDataOptional.isEmpty()) {
                    setResults(activityMessageBuilder.errorMessage("Could not create activity data"));
                    return false;
                }
                var activityData = activityDataOptional.get();

                // subscribe logic
                // for each protocol entry


                var subscriptions = Stream
                        .of("email", "sms")
                        .map(protocol -> subscribeIfPossible(protocol, activityData, topicArn))
                        .filter(Optional::isPresent)
                        .toList();

                if (subscriptions.isEmpty()) {
                    setResults(activityMessageBuilder.errorMessage("Could not subscribe to an SNS topic"));
                    return false;
                } else {
                    setResults(objectMapper.writeValueAsString(activityData));
                    return true;
                }

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            setResults(activityMessageBuilder.errorMessage("Didn't receive any input"));
            return false;
        }
    }

    @VisibleForTesting
    Optional<String> subscribeIfPossible(String protocol, SubscriptionData subscriptionData, String topicArn) {
        // get the endpoint for that protocol
        // if the endpoint is not empty
        // then subscribe to the given topic arn using the protocol and the endpoint found
        // once subscribed, set the new topic_arn to activityData[protocol][subscription_arn] = topic_arn

        var endpoint = subscriptionData.endpointConfig().get(protocol).getOrDefault("endpoint", "");
        if (StringUtils.isNotBlank(endpoint)) {
            var subscribeRequest = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol(protocol)
                .endpoint(endpoint)
                .build();
            SubscribeResponse response = snsClient.subscribe(subscribeRequest);
            if (response.sdkHttpResponse().isSuccessful()) {
                // set the subscription ARN for the given protocol/endpoint
                subscriptionData.endpointConfig().get(protocol).put("subscription_arn", response.subscriptionArn());
                return Optional.of(response.subscriptionArn());
            }
        }

        return Optional.empty();
    }

    @VisibleForTesting
    Optional<SubscriptionData> createActivityData(String topicArn, Map<String, String> input) throws JsonProcessingException {
        if (input != null) {
            var email = input.get("email");
            var sms = input.get("sms");

            var activityData = new SubscriptionData(
                    topicArn,
                    new HashMap<>(Map.of(
                        "email", new HashMap<>(Map.of("endpoint", email)),
                        "sms", new HashMap<>(Map.of("endpoint", sms))
                    ))
            );

            return Optional.of(activityData);
        }

        return Optional.empty();
    }

    private Optional<String> createTopic() {
        var request = CreateTopicRequest.builder()
                .name("SWF_Sample_Topic")
                .build();
        var topic = snsClient.createTopic(request);

        if (topic.topicArn() != null) {
            var topicAttributesRequest = SetTopicAttributesRequest.builder()
                    .topicArn(topic.topicArn())
                    .attributeName("DisplayName")
                    .attributeValue("SWFSample")
                    .build();

            snsClient.setTopicAttributes(topicAttributesRequest);

            return Optional.of(topic.topicArn());
        } else {
            return Optional.empty();
        }
    }

}
