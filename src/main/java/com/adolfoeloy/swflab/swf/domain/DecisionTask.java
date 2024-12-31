package com.adolfoeloy.swflab.swf.domain;

import software.amazon.awssdk.services.swf.model.HistoryEvent;
import software.amazon.awssdk.services.swf.model.PollForDecisionTaskResponse;

import java.util.List;
import java.util.Stack;

/**
 * Abstraction of a task returned after polling for decision task from SWF.
 */
record DecisionTask(String taskToken, Long starterEventId, Long previousStartedEventId, List<HistoryEvent> events) {

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
