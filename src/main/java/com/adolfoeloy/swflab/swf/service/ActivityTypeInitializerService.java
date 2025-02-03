package com.adolfoeloy.swflab.swf.service;

import com.adolfoeloy.swflab.swf.domain.activity.ActivityType;
import com.adolfoeloy.swflab.swf.domain.activity.ActivityTypes;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.ListActivityTypesRequest;
import software.amazon.awssdk.services.swf.model.RegisterActivityTypeRequest;
import software.amazon.awssdk.services.swf.model.RegistrationStatus;
import software.amazon.awssdk.services.swf.model.TaskList;

/**
 * Ideally all initializer services created in this project should be implemented in the domain.
 * Activity types and workflow types should be initialized from a Domain.
 * TODO: Refactor this code to a more domain driven style.
 */
@Service
public class ActivityTypeInitializerService {
    private final SwfClient client;
    private final WorkflowProperties workflowProperties;

    ActivityTypeInitializerService(SwfClient swfClient, WorkflowProperties workflowProperties) {
        this.client = swfClient;
        this.workflowProperties = workflowProperties;
    }

    public ActivityTypes initActivityTypes() {
        var listOfActivityTypes = workflowProperties.activities().stream()
                .map(this::findOrCreateActivityType)
                .toList();

        return new ActivityTypes(listOfActivityTypes);
    }

    private ActivityType findOrCreateActivityType(ActivityType activityTypeConfig) {
        var listActivityTypesRequest = ListActivityTypesRequest.builder()
                .domain(workflowProperties.domain())
                .name(activityTypeConfig.name())
                .registrationStatus(RegistrationStatus.REGISTERED)
                .build();

        var listActivityTypesResponse = client.listActivityTypes(listActivityTypesRequest);

        return listActivityTypesResponse.typeInfos().stream()
                .map(activityTypeInfo -> {
                    var name = activityTypeInfo.activityType().name();
                    var version = activityTypeInfo.activityType().version();
                    return new ActivityType(name, version);
                })
                .filter(registeredActivityType -> isSameActivityType(activityTypeConfig, registeredActivityType))
                .findFirst()
                .orElseGet(() -> registerActivityType(
                        workflowProperties.domain(), activityTypeConfig.name(), activityTypeConfig.version()));
    }

    private static boolean isSameActivityType(ActivityType a, ActivityType b) {
        return b.name().equals(a.name()) && b.version().equals(a.version());
    }

    private ActivityType registerActivityType(String domain, String name, String version) {
        var registerActivityTypeRequest = RegisterActivityTypeRequest.builder()
                .domain(domain)
                .name(name)
                .version(version)
                .defaultTaskList(TaskList.builder().name("default_activities").build())
                .defaultTaskHeartbeatTimeout("900")
                .defaultTaskScheduleToStartTimeout("120")
                .defaultTaskScheduleToCloseTimeout("3800")
                .defaultTaskStartToCloseTimeout("3600")
                .build();

        client.registerActivityType(registerActivityTypeRequest);

        return new ActivityType(name, version);
    }
}
