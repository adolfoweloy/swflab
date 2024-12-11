package com.adolfoeloy.swflab;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.swf.SwfClient;

@Configuration
public class SwfClientConfig {

    @Bean
    public SwfService swfService(Environment environment) {
        var accessKey = environment.getProperty("AWS_ACCESS_KEY_ID");
        var secretAccessKey = environment.getProperty("AWS_SECRET_ACCESS_KEY");

        var swfClient = SwfClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                accessKey,
                                secretAccessKey
                        ))
                ).build();

        return new SwfService(swfClient);
    }
}
