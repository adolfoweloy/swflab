package com.adolfoeloy.swflab.subscription.domain.activity.model;

import java.util.Map;

public record SubscriptionData(
        String topicArn,
        Map<String, Map<String, String>> endpointConfig // email => { :endpoint => nil, :subscription_arn => nil }
        ) {}
