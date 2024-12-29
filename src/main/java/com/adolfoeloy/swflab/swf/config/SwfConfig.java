package com.adolfoeloy.swflab.swf.config;

import com.adolfoeloy.swflab.swf.model.Activity;
import com.adolfoeloy.swflab.swf.model.Workflow;
import com.adolfoeloy.swflab.swf.service.SwfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.swf.SwfClient;

import java.util.List;

/**
 * Configures the static workflow types and SWF client.
 */
@Configuration
class SwfConfig {
    private static final String SWF_DOMAIN = "test.adolfoeloy.com";
    private static final String SWF_WORKFLOW_NAME = "swf-sns-workflow";
    private static final List<Activity> ACTIVITIES = List.of(
            new Activity("get_contact_activity", "v1"),
            new Activity("subscribe_topic_activity", "v1"),
            new Activity("wait_for_confirmation_activity", "v1"),
            new Activity("send_result_activity", "v1")
    );

    private static final Logger logger = LoggerFactory.getLogger(SwfConfig.class);

    @Bean
    Workflow mainWorkflowType(SwfService SwfService) {
        return new WorkflowSetup(SwfService)
                .initDomain(SWF_DOMAIN)
                .setup(
                    SWF_WORKFLOW_NAME,
                    ACTIVITIES
                );
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
