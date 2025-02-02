package com.adolfoeloy.swflab.swf.service;

import com.adolfoeloy.swflab.swf.domain.Domain;
import com.adolfoeloy.swflab.swf.domain.Workflow;
import java.util.UUID;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.ChildPolicy;
import software.amazon.awssdk.services.swf.model.ListWorkflowTypesRequest;
import software.amazon.awssdk.services.swf.model.RegisterWorkflowTypeRequest;
import software.amazon.awssdk.services.swf.model.RegistrationStatus;
import software.amazon.awssdk.services.swf.model.TaskList;

@Service
public class WorkflowInitializerService {
    private final SwfClient client;
    private final DomainInitializerService domainInitializerService;
    private final WorkflowProperties workflowProperties;

    WorkflowInitializerService(
            SwfClient client,
            DomainInitializerService domainInitializerService,
            WorkflowProperties workflowProperties) {
        this.client = client;
        this.domainInitializerService = domainInitializerService;
        this.workflowProperties = workflowProperties;
    }

    public Workflow initWorkflow() {
        var workflowName = workflowProperties.workflow();
        var version = workflowProperties.getWorkflowVersion();

        var domain = domainInitializerService.initDomain();

        var listRequest = ListWorkflowTypesRequest.builder()
                .domain(domain.name())
                .name(workflowName)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .build();

        return client.listWorkflowTypes(listRequest).typeInfos().stream()
                .map(w -> new Workflow(
                        domain,
                        w.workflowType().name(),
                        UUID.fromString(w.workflowType().version()),
                        workflowProperties.decisionTaskList()))
                .filter(w -> w.isSameWorkflow(workflowName, version))
                .findFirst()
                .orElseGet(() -> registerWorkflow(domain, workflowName, version));
    }

    private Workflow registerWorkflow(Domain domain, String workflowName, UUID version) {
        var defaultTaskList = "default_" + workflowProperties.decisionTaskList();
        var taskList = TaskList.builder().name(defaultTaskList).build();
        var registerRequest = RegisterWorkflowTypeRequest.builder()
                .domain(domain.name())
                .name(workflowName)
                .version(version.toString())

                // default task list for executions of this workflow type being registered
                // a task list can be interpreted as a dynamic queue in the sense that I don't need to register them.
                // this default task list will be used if a task list is not provided when starting a workflow.
                .defaultTaskList(taskList)
                .defaultChildPolicy(ChildPolicy.TERMINATE)
                .defaultTaskStartToCloseTimeout(Integer.valueOf(3600).toString())
                .defaultExecutionStartToCloseTimeout("3600")
                .build();

        client.registerWorkflowType(registerRequest);

        return new Workflow(domain, workflowName, version, taskList.name());
    }
}
