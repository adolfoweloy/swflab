package com.adolfoeloy.swflab.swf.service;

import com.adolfoeloy.swflab.swf.model.Activity;
import com.adolfoeloy.swflab.swf.model.Domain;
import com.adolfoeloy.swflab.swf.model.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkflowService {
    private final static Logger logger = LoggerFactory.getLogger(WorkflowService.class);
    private final SwfClient client;
    private final Domain domain;

    public WorkflowService(Domain domain, SwfClient client) {
        this.client = client;
        this.domain = domain;
    }

    public Optional<Workflow> findWorkflowType(
            String workflowName,
            UUID version,
            List<Activity> activities
    ) {
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
                        activities)
                )
                .filter(w -> w.isSameWorkflow(workflowName, version))
                .findFirst();
    }

    public Workflow registerWorkflow(
            String workflowName,
            UUID version,
            List<Activity> activities
    ) {
        var taskList = TaskList.builder().name(Workflow.INITIAL_DECISION_TASK_LIST).build();
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

        logger.info("Workflow created {} version {}", workflowName, version);

        return new Workflow(
                domain,
                workflowName,
                version,
                activities
        );
    }
}
