package com.adolfoeloy.swflab.swf.config;

import com.adolfoeloy.swflab.swf.domain.Workflow;
import com.adolfoeloy.swflab.swf.service.WorkflowInitializerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.swf.SwfClient;

/**
 * Configures the domain classes for SWF and {@code SwfClient}.
 */
@Configuration
class SwfDomainConfiguration {

    @Bean
    Workflow workflow(WorkflowInitializerService workflowInitializerService) {
        return workflowInitializerService.initWorkflow();
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
