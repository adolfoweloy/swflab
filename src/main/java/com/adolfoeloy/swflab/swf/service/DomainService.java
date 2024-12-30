package com.adolfoeloy.swflab.swf.service;

import com.adolfoeloy.swflab.swf.model.Domain;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.*;

import java.util.Optional;

@Service
public class DomainService {
    private final SwfClient client;

    public DomainService(SwfClient swfClient) {
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
            throw new DomainServiceException("Could not create domain " + domainName);
        }
    }
}
