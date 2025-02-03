package com.adolfoeloy.swflab.subscription.service;

import com.adolfoeloy.swflab.subscription.domain.activity.ActivityType;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "swf")
record WorkflowProperties(
        String domain,
        String workflow,
        String workflowVersion,
        String decisionTaskList,
        List<ActivityType> activities) {

    UUID getWorkflowVersion() {
        return UUID.fromString(workflowVersion);
    }
}
