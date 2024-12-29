package com.adolfoeloy.swflab.swf.service;

import com.adolfoeloy.swflab.swf.model.Activity;
import com.adolfoeloy.swflab.swf.model.Domain;
import com.adolfoeloy.swflab.swf.model.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SwfService {
    private final static Logger logger = LoggerFactory.getLogger(SwfService.class);
    private final SwfClient client;

    public SwfService(SwfClient swfClient) {
        this.client = swfClient;
    }

    public Optional<Domain> findRegisteredDomain(String domainName) {
        var listDomainsRequest = ListDomainsRequest.builder().registrationStatus(RegistrationStatus.REGISTERED).build();

        return client.listDomains(listDomainsRequest).domainInfos().stream()
                .map(DomainInfo::name)
                .filter(d -> d.equals(domainName))
                .findFirst()
                .map(Domain::new);
    }

    public Domain registerDomain(String domainName) {
        RegisterDomainRequest registerDomainRequest = RegisterDomainRequest.builder()
                .name(domainName)
                .workflowExecutionRetentionPeriodInDays("1")
                .build();

        RegisterDomainResponse registerDomainResponse = client.registerDomain(registerDomainRequest);
        var httpResponse = registerDomainResponse.sdkHttpResponse();
        if (httpResponse.isSuccessful()) {
            return new Domain(domainName);
        } else {
            throw new SwfServiceException("Could not create domain " + domainName);
        }
    }

    public Optional<Workflow> findWorkflowType(
            String workflowName,
            UUID version,
            Domain domain,
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
            Domain domain,
            List<Activity> activities
    ) {
        var taskList = TaskList.builder().name("initialTaskList").build();
        var registerRequest = RegisterWorkflowTypeRequest.builder()
                .domain(domain.name())
                .name(workflowName)
                .version(version.toString())
                .defaultTaskList(taskList)
                .defaultChildPolicy(ChildPolicy.TERMINATE)
                .defaultTaskStartToCloseTimeout(Integer.valueOf(24 * 3600).toString())
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
