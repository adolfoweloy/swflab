package com.adolfoeloy.swflab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.swf.SwfClient;

@Configuration
public class SwfClientConfig {
    private static final String SWF_DOMAIN = "test.adolfoeloy.com";
    private static final String SWF_WORKFLOW_NAME = "swf-sns-workflow";

    private static final Logger logger = LoggerFactory.getLogger(SwfClientConfig.class);

    @Bean
    public SwfService swfService(Environment environment) {
        return new SwfService.Builder(createSwfClient(environment))
                .initDomain(SWF_DOMAIN)
                .buildWithWorkflow(SWF_WORKFLOW_NAME);
    }

    private SwfClient createSwfClient(Environment environment) {
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
