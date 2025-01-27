package com.adolfoeloy.swflab.swf.domain.activity;

import com.adolfoeloy.swflab.swf.domain.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubscribeTopicActivity(SnsClient snsClient) {
        super("subscribe_topic_activity");
        this.snsClient = snsClient;
    }

    @Override
    public boolean doActivity(Task task) {
        if (task.input() != null) {

            try {
                var topicArn = createTopic();
                if (topicArn == null) {
                    setResults("Could not subscribe to an SNS topic");
                    return false;
                }

                var typeReference = new TypeReference<HashMap<String, String>>() {};
                var input = objectMapper.readValue(task.input(), typeReference);
                var activityDataOptional = createActivityData(topicArn, input);
                if (activityDataOptional.isEmpty()) {
                    setResults("Could not create activity data");
                    return false;
                }

                var activityData = activityDataOptional.get();

                // subscribe logic
                var subscribedToAnEndpoint = Stream
                        .of("email", "sns")
                        .anyMatch(x -> subscribeIfPossible(x, input, topicArn));

                if (subscribedToAnEndpoint) {
                    setResults(objectMapper.writeValueAsString(activityData));
                    return true;
                } else {
                    setResults("Could not subscribe to an SNS topic");
                    return false;
                }

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            setResults("Didn't receive any result");
            return false;
        }
    }

    private boolean subscribeIfPossible(String x, HashMap<String, String> input, String topicArn) {
        var endpoint = input.get(x);
        if (StringUtils.isNotBlank(endpoint)) {
            var subscribeRequest = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol(x)
                .endpoint(endpoint)
                .build();
            SubscribeResponse response = snsClient.subscribe(subscribeRequest);
            return response.sdkHttpResponse().isSuccessful();
        } else {
            return false;
        }
    }

    private Optional<ActivityData> createActivityData(String topicArn, Map<String, String> input) throws JsonProcessingException {
        if (input != null) {
            var email = input.get("email");
            var sms = input.get("sms");

            var activityData = new ActivityData(
                    topicArn,
                    new SnsEndpoint(email, topicArn),
                    new SnsEndpoint(sms, topicArn)
            );

            return Optional.of(activityData);
        }

        return Optional.empty();
    }

    private String createTopic() {
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

            return topic.topicArn();
        } else {
            throw new RuntimeException("Couldn't create the SNS Topic");
        }
    }

    public record ActivityData(String topicArn, SnsEndpoint email, SnsEndpoint sns) {}

    public record SnsEndpoint(String endpoint, String subscriptionArn) {}
}
