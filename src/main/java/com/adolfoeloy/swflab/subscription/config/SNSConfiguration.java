package com.adolfoeloy.swflab.subscription.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class SNSConfiguration {

    @Bean
    public SnsClient snsClient() {
        var region = Region.US_EAST_1;
        return SnsClient.builder().region(region).build();
    }
}
