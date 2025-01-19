package com.adolfoeloy.swflab.swf.domain.decider;

import com.adolfoeloy.swflab.swf.domain.activity.ActivityTaskOptions;
import com.adolfoeloy.swflab.swf.domain.activity.ActivityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.Decision;
import software.amazon.awssdk.services.swf.model.DecisionType;
import software.amazon.awssdk.services.swf.model.HistoryEvent;
import software.amazon.awssdk.services.swf.model.RespondDecisionTaskCompletedRequest;
import software.amazon.awssdk.services.swf.model.ScheduleActivityTaskDecisionAttributes;
import software.amazon.awssdk.services.swf.model.TaskList;

import java.util.List;
import java.util.UUID;

/**
 * Abstraction of a task returned after polling for decision task from SWF.
 */
record DecisionTask(
        String taskToken,
        Long starterEventId,
        Long previousStartedEventId,
        List<HistoryEvent> events
) {

    private static final Logger logger = LoggerFactory.getLogger(DecisionTask.class);

    public void scheduleActivityTask(SwfClient client, ActivityType activityType, ActivityTaskOptions options) {
        var activityId = UUID.randomUUID() + "_activity";
        var attrsBuilder = ScheduleActivityTaskDecisionAttributes.builder()
                .activityType(software.amazon.awssdk.services.swf.model.ActivityType.builder()
                        .name(activityType.name())
                        .version(activityType.version())
                        .build()
                )
                .activityId(activityId);

        switch (options) {
            case ActivityTaskOptions.ActivityTaskOptionsWithInput(String taskList, String input) ->
                    attrsBuilder.input(input).taskList(TaskList.builder().name(taskList).build());

            case ActivityTaskOptions.ActivityTaskOptionsWithoutInput(String taskList) ->
                    attrsBuilder.taskList(TaskList.builder().name(taskList).build());
        }

        var attrs = attrsBuilder.build();

        var decisions = List.of(
                Decision.builder()
                        .decisionType(DecisionType.SCHEDULE_ACTIVITY_TASK)
                        .scheduleActivityTaskDecisionAttributes(attrs)
                        .build()
        );

        var request = RespondDecisionTaskCompletedRequest.builder()
                .decisions(decisions)
                .taskToken(taskToken)
                .build();

        client.respondDecisionTaskCompleted(request);
        logger.info("Responded decision task completed to SWF");
    }

    /**
     * The intention with this method is to fetch only the new events.
     */
    public List<HistoryEvent> getNewEvents() {
        final List<HistoryEvent> newEvents;
        if (previousStartedEventId == null || previousStartedEventId == 0) {
            newEvents = events();
        } else {
            newEvents = newEvents(previousStartedEventId());
        }
        return newEvents;
    }

    private List<HistoryEvent> newEvents(Long previousDecisionStartedEventId) {
        return events.stream()
                .filter(event -> event.eventId().equals(previousDecisionStartedEventId))
                .toList();
    }
}
