package com.adolfoeloy.swflab.swf.service;

import com.adolfoeloy.swflab.swf.domain.Activity;
import com.adolfoeloy.swflab.swf.domain.Domain;
import com.adolfoeloy.swflab.swf.domain.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class WorkflowService {
    private final static Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    private final SwfClient client;
    private final DomainService domainService;
    private final WorkflowProperties workflowProperties;

    WorkflowService(SwfClient client, DomainService domainService, WorkflowProperties workflowProperties) {
        this.client = client;
        this.domainService = domainService;
        this.workflowProperties = workflowProperties;
    }

    public Workflow initWorkflow() {
        var workflowName = workflowProperties.workflow();
        var version = workflowProperties.getWorkflowVersion();
        var activities = workflowProperties.activities();

        var listRequest = ListWorkflowTypesRequest.builder()
                .domain(workflowProperties.domain())
                .name(workflowName)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .build();

        var domain = domainService.initDomain();

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
