package com.adolfoeloy.swflab.swf;

import com.adolfoeloy.swflab.swf.model.Activity;

import java.util.List;
import java.util.UUID;

public class SwfConfigData {
    public static final String SWF_DOMAIN = "test.adolfoeloy.com";
    public static final String SWF_WORKFLOW_NAME = "swf-sns-workflow";
    public static final UUID STATIC_WORKFLOW_VERSION = UUID.fromString("21e98f23-cd31-4267-b679-efef96d67cc1");
    public static final List<Activity> ACTIVITIES = List.of(
            new Activity("get_contact_activity", "v1"),
            new Activity("subscribe_topic_activity", "v1"),
            new Activity("wait_for_confirmation_activity", "v1"),
            new Activity("send_result_activity", "v1")
    );
}
