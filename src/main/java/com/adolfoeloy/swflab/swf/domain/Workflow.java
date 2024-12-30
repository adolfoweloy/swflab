package com.adolfoeloy.swflab.swf.domain;

import java.util.List;
import java.util.UUID;

/**
 * WorkflowTypes are made available to the application as a managed bean
 *
 * @param domain
 * @param name
 * @param version
 * @param activities
 */
public record Workflow(
        Domain domain,
        String name,
        UUID version,
        String decisionTaskList,
        List<Activity> activities
) {
    public boolean isSameWorkflow(String otherName, UUID otherVersion) {
        return name.equals(otherName) && version.equals(otherVersion);
    }
}
