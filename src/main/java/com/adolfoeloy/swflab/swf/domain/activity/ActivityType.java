package com.adolfoeloy.swflab.swf.domain.activity;

/**
 * Models an activity type registered in AWS SWF.
 * If there's an instance of ActivityType then it must be registered in SWF.
 * @param name
 * @param version
 */
public record ActivityType(String name, String version) {}
