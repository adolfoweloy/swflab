package com.adolfoeloy.swflab;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.*;

import java.util.UUID;

public class SwfService {

    private static final Logger log = LoggerFactory.getLogger(SwfService.class);
    private final SwfClient client;
    private final String domainName;
    private final Workflow workflow;

    private SwfService(SwfClient client, String domainName, Workflow workflow) {
        this.client = client;
        this.domainName = domainName;
        this.workflow = workflow;
    }

    public static class Builder {
        private final SwfClient client;

        public Builder(SwfClient client) {
            this.client = client;
        }

        public DomainInitializedBuilder initDomain(String domainName) {
            // initialises the domain
            var domains = client.listDomains(
                    ListDomainsRequest.builder()
                            .registrationStatus(RegistrationStatus.REGISTERED)
                            .build());
            var domainNames = domains.domainInfos().stream().map(DomainInfo::name).toList();

            var registeredDomainName = domainNames.stream().filter(d -> d.equals(domainName))
                    .findFirst()
                    .orElseGet(() -> registerDomain(client, domainName));

            return new DomainInitializedBuilder(client, registeredDomainName);
        }

        private String registerDomain(SwfClient swfClient, String domainName) {
            RegisterDomainRequest registerDomainRequest = RegisterDomainRequest.builder()
                    .name(domainName)
                    .workflowExecutionRetentionPeriodInDays("1")
                    .build();

            RegisterDomainResponse registerDomainResponse = client.registerDomain(registerDomainRequest);
            var httpResponse = registerDomainResponse.sdkHttpResponse();
            if (httpResponse.isSuccessful()) {
                return domainName;
            } else {
                throw new SwfServiceException("Could not create domain " + domainName);
            }
        }
    }

    public record Workflow(String name, String version) {
    }

    public static class DomainInitializedBuilder {
        private final SwfClient client;
        private final String domainName;

        DomainInitializedBuilder(SwfClient client, String domainName) {
            this.client = client;
            this.domainName = domainName;
        }

        public SwfService buildWithWorkflow(String workflowName) {
            var listRequest = ListWorkflowTypesRequest.builder()
                    .domain(domainName)
                    .name(workflowName)
                    .registrationStatus(RegistrationStatus.REGISTERED)
                    .build();

            var version = UUID.randomUUID().toString();
            var workflow = client.listWorkflowTypes(listRequest).typeInfos().stream()
                    .map(w -> new Workflow(w.workflowType().name(), w.workflowType().version()))
                    .filter(w -> w.name().equals(workflowName) && w.version().equals(version))
                    .findFirst()
                    .orElseGet(() -> {
                        var taskList = TaskList.builder().name("initialTaskList").build();
                        var registerRequest = RegisterWorkflowTypeRequest.builder()
                            .domain(domainName)
                            .name(workflowName)
                            .version(version)
                            .defaultTaskList(taskList)
                            .build();
                        client.registerWorkflowType(registerRequest);
                        log.info("Workflow created {} version {}", workflowName, version);
                        return new Workflow(workflowName, version);
                    });

            return new SwfService(client, domainName, workflow);
        }
    }

    public String getDomainName() {
        return domainName;
    }

    public Workflow getWorkflow() {
        return workflow;
    }
}
