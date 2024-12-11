package com.adolfoeloy.swflab;


import software.amazon.awssdk.services.swf.SwfClient;
import software.amazon.awssdk.services.swf.model.DomainInfo;
import software.amazon.awssdk.services.swf.model.ListDomainsRequest;

import java.util.List;

public class SwfService {

    private final SwfClient client;

    public SwfService(SwfClient client) {
        this.client = client;
    }

    public List<String> getRegisteredDomains() {
        var domains = client.listDomains(
                ListDomainsRequest.builder()
                        .registrationStatus("REGISTERED")
                        .build());
        return domains.domainInfos().stream().map(DomainInfo::name).toList();
    }
}
