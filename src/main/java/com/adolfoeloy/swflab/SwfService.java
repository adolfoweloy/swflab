package com.adolfoeloy.swflab;


import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.*;

public class SwfService {

    private final SwfClient client;
    private final String domainName;
    private final String workspaceId;

    private SwfService(SwfClient client, String domainName, String workspaceId) {
        this.client = client;
        this.domainName = domainName;
        this.workspaceId = workspaceId;
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

    public static class DomainInitializedBuilder {
        private final SwfClient client;
        private final String domainName;

        DomainInitializedBuilder(SwfClient client, String domainName) {
            this.client = client;
            this.domainName = domainName;
        }

        public SwfService buildWithWorkspaceId(String workspaceId) {
            return new SwfService(client, domainName, workspaceId);
        }
    }

    public String getDomainName() {
        return domainName;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }
}
