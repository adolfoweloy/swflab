package com.adolfoeloy.swflab.swf.domain;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.PollForDecisionTaskRequest;
import software.amazon.awssdk.services.swf.model.PollForDecisionTaskResponse;

import java.util.ArrayList;

@Component
public class DecisionTaskResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(DecisionTaskResponseHandler.class);

    private final SwfClient client;

    public DecisionTaskResponseHandler(SwfClient client) {
        this.client = client;
    }

    DecisionTask handle(
            PollForDecisionTaskResponse response,
            PollForDecisionTaskRequest.Builder requestBuilder
    ) {
        logger.info("Decision task received with token {}", response.taskToken());

        var nextToken = response.nextPageToken();
        var events = new ArrayList<>(response.events());

        while (StringUtils.isNotBlank(nextToken)) {
            var nextResponse = client.pollForDecisionTask(requestBuilder.nextPageToken(nextToken).build());
            events.addAll(nextResponse.events());
        }

        return new DecisionTask(response.startedEventId(), response.previousStartedEventId(), events);
    }

}
