package com.adolfoeloy.swflab.swf.domain.decider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.PollForDecisionTaskRequest;
import software.amazon.awssdk.services.swf.model.PollForDecisionTaskResponse;

import java.util.ArrayList;

@Component
public class DecisionTaskHistoryEventsHandler {
    private static final Logger logger = LoggerFactory.getLogger(DecisionTaskHistoryEventsHandler.class);

    private final SwfClient client;

    public DecisionTaskHistoryEventsHandler(SwfClient client) {
        this.client = client;
    }

    DecisionTask createDecisionTaskWithEvents(
            PollForDecisionTaskResponse response,
            PollForDecisionTaskRequest.Builder requestBuilder
    ) {
        logger.info("Decision task received with token {}", response.taskToken());

        var nextPageToken = response.nextPageToken();
        var events = new ArrayList<>(response.events());

        while (StringUtils.isNotBlank(nextPageToken)) {
            var nextResponse = client.pollForDecisionTask(requestBuilder.nextPageToken(nextPageToken).build());
            events.addAll(nextResponse.events());
            nextPageToken = nextResponse.nextPageToken();
        }

        return new DecisionTask(
                response.taskToken(), // taskToken should not be confused with nextPageToken
                response.startedEventId(),
                response.previousStartedEventId(),
                events
        );
    }

}
