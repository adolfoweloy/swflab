package com.adolfoeloy.swflab.swf.domain.activity;

import com.adolfoeloy.swflab.swf.config.ObjectMapperConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscribeTopicActivityTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SnsClient snsClientMock;

    private final ObjectMapperConfiguration objectMapperConfiguration = new ObjectMapperConfiguration();
    private final ObjectMapper objectMapper = objectMapperConfiguration.objectMapper();

    private SubscribeTopicActivity subject;

    @BeforeEach
    void setUp() {
        subject = new SubscribeTopicActivity(snsClientMock, objectMapper, new ActivityMessageBuilder(objectMapper));
    }

    @Test
    void createActivityData_should_create_subscription_data() throws JsonProcessingException {
        // Act
        var result = subject.createActivityData(
                "topic-arn",
                Map.of("email", "juca@test.com", "sms", "1232131")
        );

        // Assert
        assertThat(result.isPresent()).isTrue();
        var r = result.get();
        assertThat(r.topicArn()).isEqualTo("topic-arn");
        assertThat(r.endpointConfig().get("email").get("endpoint")).isEqualTo("juca@test.com");
        assertThat(r.endpointConfig().get("sms").get("endpoint")).isEqualTo("1232131");
    }

    @Test
    void createActivityData_should_create_mutable_subscription_data() throws JsonProcessingException {
        // Act
        var result = subject.createActivityData(
                "topic-arn",
                Map.of("email", "juca@test.com", "sms", "1232131")
        );

        // Assert
        assertThat(result.isPresent()).isTrue();
        var r = result.get();

        r.endpointConfig().get("email").put("subscription_arn", "abc");
        assertThat(r.endpointConfig().get("email").get("subscription_arn")).isEqualTo("abc");
    }

    @Test
    void subscribeIfPossible_should_subscribe() throws JsonProcessingException {
        // Arrange
        var activityData = subject.createActivityData(
                "topic-arn",
                Map.of("email", "juca@test.com", "sms", "1232131")
        ).get();

        var subscribeResponseMock = mock(SubscribeResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(subscribeResponseMock.sdkHttpResponse().isSuccessful()).thenReturn(true);
        when(subscribeResponseMock.subscriptionArn()).thenReturn("topic-arn-sub-1");
        when(snsClientMock.subscribe(Mockito.any(SubscribeRequest.class))).thenReturn(subscribeResponseMock);

        // Act
        Optional<String> subscription = subject.subscribeIfPossible("email", activityData, "topic-arn");

        // Assert
        assertThat(subscription).isPresent();
        var topicArn = subscription.get();
        assertThat(topicArn).isEqualTo("topic-arn-sub-1");

        assertThat(activityData.endpointConfig().get("email")).isEqualTo(Map.of(
                "endpoint", "juca@test.com",
                "subscription_arn", "topic-arn-sub-1"
        ));
    }
}