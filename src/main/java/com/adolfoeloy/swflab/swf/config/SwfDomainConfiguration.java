package com.adolfoeloy.swflab.swf.config;

import com.adolfoeloy.swflab.swf.model.Domain;
import com.adolfoeloy.swflab.swf.model.Workflow;
import com.adolfoeloy.swflab.swf.service.DomainService;
import com.adolfoeloy.swflab.swf.service.WorkflowService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.swf.SwfClient;

import static com.adolfoeloy.swflab.swf.SwfConfigData.*;

/**
 * Configures the static workflow types and SWF client.
 */
@Configuration
class SwfConfiguration {

    @Bean
    Domain domain(DomainService domainService) {
        return domainService.initDomain(SWF_DOMAIN);
    }

    @Bean
    Workflow workflow(WorkflowService workflowService) {
        return workflowService
                .findWorkflowType(SWF_WORKFLOW_NAME, STATIC_WORKFLOW_VERSION, ACTIVITIES)
                .orElseGet(() -> workflowService.registerWorkflow(SWF_WORKFLOW_NAME, STATIC_WORKFLOW_VERSION, ACTIVITIES));
    }

    @Bean
    SwfClient swfClient(Environment environment) {
        var accessKey = environment.getProperty("AWS_ACCESS_KEY_ID");
        var secretAccessKey = environment.getProperty("AWS_SECRET_ACCESS_KEY");

        return SwfClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                accessKey,
                                secretAccessKey
                        ))
                ).build();
    }
}
