package com.adolfoeloy.swflab.swf.domain;

import java.util.Stack;
import java.util.UUID;

public record WorkflowExecution(UUID workflowId, String runId, Stack<Activity> activityList) {
}
