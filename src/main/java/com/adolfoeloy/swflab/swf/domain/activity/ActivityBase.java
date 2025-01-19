package com.adolfoeloy.swflab.swf.domain.activity;

import com.adolfoeloy.swflab.swf.domain.Task;

/**
 * Starting with the design inspired by the development guide from AWS SWF
 * <a href="https://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-sns-tutorial-implementing-activities.html">Implementing Activities</a>
 * The design is horrible imo given the introduced mutability in this class.
 * TODO: Make this all immutable pls!
 */
public class ActivityBase {
    private String results;

    public boolean doActivity(Task task) {
        results = task.input();
        return true;
    }
}
