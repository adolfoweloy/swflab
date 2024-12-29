package com.adolfoeloy.swflab;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.*;

import java.util.List;
import java.util.UUID;

public class SwfService {
    private static final Logger log = LoggerFactory.getLogger(SwfService.class);
    private final String domainName;
    private final Workflow workflow;

    private SwfService(SwfClient client, String domainName, Workflow workflow) {
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
                    .orElseGet(() -> registerDomain(domainName));

            return new DomainInitializedBuilder(client, registeredDomainName);
        }

        private String registerDomain(String domainName) {
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

    public record Workflow(String name, String version, List<Activity> activityList) {
    }

    public record Activity(String name, String version) {}

    public static class DomainInitializedBuilder {
        private final SwfClient client;
        private final String domainName;

        DomainInitializedBuilder(SwfClient client, String domainName) {
            this.client = client;
            this.domainName = domainName;
        }

        public SwfService buildWithWorkflow(String workflowName, List<Activity> activityList) {
            var listRequest = ListWorkflowTypesRequest.builder()
                    .domain(domainName)
                    .name(workflowName)
                    .registrationStatus(RegistrationStatus.REGISTERED)
                    .build();

            var version = UUID.randomUUID().toString();
            var workflow = client
                    // tries to find existing workflow
                    .listWorkflowTypes(listRequest).typeInfos().stream()
                    .map(w -> new Workflow(w.workflowType().name(), w.workflowType().version(), activityList))
                    .filter(w -> w.name().equals(workflowName) && w.version().equals(version))
                    .findFirst()

                    // otherwise register a new one
                    .orElseGet(() -> {
                        var taskList = TaskList.builder().name("initialTaskList").build();
                        var registerRequest = RegisterWorkflowTypeRequest.builder()
                            .domain(domainName)
                            .name(workflowName)
                            .version(version)
                            .defaultTaskList(taskList)
                            .defaultChildPolicy(ChildPolicy.TERMINATE)
                            .defaultTaskStartToCloseTimeout(Integer.valueOf(24 * 3600).toString())
                            .build();

                        client.registerWorkflowType(registerRequest);

                        log.info("Workflow created {} version {}", workflowName, version);

                        return new Workflow(
                                workflowName,
                                version,
                                activityList
                        );
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
