package com.adolfoeloy.swflab.swf.service;

import com.adolfoeloy.swflab.swf.domain.Activity;
import com.adolfoeloy.swflab.swf.domain.Domain;
import com.adolfoeloy.swflab.swf.domain.Workflow;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.ChildPolicy;
import software.amazon.awssdk.services.swf.model.ListWorkflowTypesRequest;
import software.amazon.awssdk.services.swf.model.RegisterWorkflowTypeRequest;
import software.amazon.awssdk.services.swf.model.RegistrationStatus;
import software.amazon.awssdk.services.swf.model.TaskList;

import java.util.List;
import java.util.UUID;

@Service
public class WorkflowInitializerService {
    private final SwfClient client;
    private final DomainInitializerService domainInitializerService;
    private final WorkflowProperties workflowProperties;

    WorkflowInitializerService(SwfClient client, DomainInitializerService domainInitializerService, WorkflowProperties workflowProperties) {
        this.client = client;
        this.domainInitializerService = domainInitializerService;
        this.workflowProperties = workflowProperties;
    }

    public Workflow initWorkflow() {
        var workflowName = workflowProperties.workflow();
        var version = workflowProperties.getWorkflowVersion();
        var activities = workflowProperties.activities();

        var domain = domainInitializerService.initDomain();

        var listRequest = ListWorkflowTypesRequest.builder()
                .domain(domain.name())
                .name(workflowName)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .build();

        return client
                .listWorkflowTypes(listRequest).typeInfos().stream()
                .map(w -> new Workflow(
                        domain,
                        w.workflowType().name(),
                        UUID.fromString(w.workflowType().version()),
                        workflowProperties.decisionTaskList(),
                        activities)
                )
                .filter(w -> w.isSameWorkflow(workflowName, version))
                .findFirst()
                .orElseGet(() ->registerWorkflow(domain, workflowName, version, activities));
    }

    private Workflow registerWorkflow(Domain domain, String workflowName, UUID version, List<Activity> activities) {
        var taskList = TaskList.builder().name(workflowProperties.decisionTaskList()).build();
        var registerRequest = RegisterWorkflowTypeRequest.builder()
                .domain(domain.name())
                .name(workflowName)
                .version(version.toString())
                .defaultTaskList(taskList)
                .defaultChildPolicy(ChildPolicy.TERMINATE)
                .defaultTaskStartToCloseTimeout(Integer.valueOf(3600).toString())
                .defaultExecutionStartToCloseTimeout("3600")
                .build();

        client.registerWorkflowType(registerRequest);

        return new Workflow(
                domain,
                workflowName,
                version,
                taskList.name(),
                activities
        );
    }

}
