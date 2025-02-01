package com.adolfoeloy.swflab.swf.domain;

import java.util.UUID;

public record WorkflowExecution(
        UUID workflowId,
        String runId,
        String decisionTaskList,
        Domain domain
) {
}
