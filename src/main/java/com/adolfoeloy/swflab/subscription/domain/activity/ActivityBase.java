package com.adolfoeloy.swflab.subscription.domain.activity;

import com.adolfoeloy.swflab.subscription.domain.Task;

/**
 * Starting with the design inspired by the development guide from AWS SWF
 * <a href="https://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-sns-tutorial-implementing-activities.html">Implementing Activities</a>
 * The design is horrible imo given the introduced mutability in this class.
 * TODO: Make this all immutable pls!
 */
public abstract class ActivityBase {
    private final String name;
    private String results;

    protected ActivityBase(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract boolean doActivity(Task task);

    protected void setResults(String results) {
        this.results = results;
    }

    public String getResults() {
        return results;
    }
}
