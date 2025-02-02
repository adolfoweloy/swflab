package com.adolfoeloy.swflab.swf.domain.activity;

/**
 * Used to carry activity taskList and input when appropriate.
 */
public sealed interface ActivityTaskOptions {
    record ActivityTaskOptionsWithInput(String taskList, String input) implements ActivityTaskOptions {}

    record ActivityTaskOptionsWithoutInput(String taskList) implements ActivityTaskOptions {}
}
