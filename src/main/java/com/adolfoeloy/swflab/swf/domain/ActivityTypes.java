package com.adolfoeloy.swflab.swf.domain;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Represents a stack of registered activity types
 */
public class ActivityTypes {
    private final List<ActivityType> activityTypes;

    public ActivityTypes(List<ActivityType> listOfActivityTypes) {
        this.activityTypes = Collections.unmodifiableList(listOfActivityTypes);
    }

    public Stack<ActivityType> stackOfActivityTypes() {
        Stack<ActivityType> activityTypesStack = new Stack<>();
        activityTypes.reversed().forEach(activityTypesStack::push);
        return activityTypesStack;
    }

}
