package com.adolfoeloy.swflab.swf.domain.activity;

import com.adolfoeloy.swflab.swf.domain.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import java.util.Map;

public class GetContactActivity extends ActivityBase {

    private final ObjectMapper objectMapper;

    protected GetContactActivity(ObjectMapper objectMapper) {
        super("get_contact_activity");
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean doActivity(Task task) {

        var contactInfoMap = waitForContactInformation();

        try {
            String result = objectMapper.writeValueAsString(contactInfoMap);
            setResults(result);
            return true;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @VisibleForTesting
    Map<String, String> waitForContactInformation() {
        // TODO: wait for contact info to be available (the ruby example reads from stdio, but here I have a web app)
        // results = { :email => email, :sms => phone }
        throw new UnsupportedOperationException();
    }

}
