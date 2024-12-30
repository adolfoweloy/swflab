package com.adolfoeloy.swflab.swf.service;

import com.adolfoeloy.swflab.swf.domain.Activity;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.UUID;

@ConfigurationProperties(prefix = "swf")
record WorkflowProperties(
    String domain,
    String workflow,
    String workflowVersion,
    String decisionTaskList,
    List<Activity> activities
) {

    UUID getWorkflowVersion() {
        return UUID.fromString(workflowVersion);
    }
}
