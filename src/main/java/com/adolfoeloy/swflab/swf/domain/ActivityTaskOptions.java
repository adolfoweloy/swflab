package com.adolfoeloy.swflab.swf.domain;

/**
 * Used to carry activity ID and input when appropriate.
 */
public sealed interface ActivityTaskOptions {
    record ActivityTaskOptionsWithInput(String activityId, String input) implements ActivityTaskOptions {}
    record ActivityTaskOptionsWithoutInput(String activityId) implements ActivityTaskOptions {}
}