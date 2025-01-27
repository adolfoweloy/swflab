package com.adolfoeloy.swflab.swf.domain.activity.model;

public record SubscriptionData(String topicArn, SnsEndpoint email,
                               SnsEndpoint sns) {
}
