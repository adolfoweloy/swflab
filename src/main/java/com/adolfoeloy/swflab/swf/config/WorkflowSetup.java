package com.adolfoeloy.swflab.swf.config;


import com.adolfoeloy.swflab.swf.model.Activity;
import com.adolfoeloy.swflab.swf.model.Domain;
import com.adolfoeloy.swflab.swf.model.Workflow;
import com.adolfoeloy.swflab.swf.service.SwfService;

import java.util.List;
import java.util.UUID;

/**
 * Building a workflow with a domain requires a setup. That means finding or registering the domain and the workspace.
 * Since this process is slightly complex, it was defined as a workflow setup which is actually a builder.
 */
class WorkflowSetup {
    private final static UUID STATIC_WORKFLOW_VERSION = UUID.randomUUID();

    private final SwfService swfService;

    WorkflowSetup(SwfService swfService) {
        this.swfService = swfService;
    }

    SetupWithInitializedDomain initDomain(String domainName) {
        var registeredDomainName = swfService
                .findRegisteredDomain(domainName)
                .orElseGet(() -> swfService.registerDomain(domainName));

        return new SetupWithInitializedDomain(swfService, registeredDomainName);
    }

    static class SetupWithInitializedDomain {
        private final SwfService swfService;
        private final Domain domain;

        SetupWithInitializedDomain(SwfService swfService, Domain domain) {
            this.swfService = swfService;
            this.domain = domain;
        }

        Workflow setup(String workflowName, List<Activity> activityList) {
            return swfService
                    .findWorkflowType(workflowName, STATIC_WORKFLOW_VERSION, domain, activityList)
                    .orElseGet(() -> swfService.registerWorkflow(
                            workflowName,
                            STATIC_WORKFLOW_VERSION,
                            domain,
                            activityList
                    ));
        }
    }

}
