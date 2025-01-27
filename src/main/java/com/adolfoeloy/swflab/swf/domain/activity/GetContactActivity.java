package com.adolfoeloy.swflab.swf.domain.activity;

import com.adolfoeloy.swflab.swf.domain.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetContactActivity extends ActivityBase {

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected GetContactActivity() {
        super("get_contact_activity");
    }

    @Override
    public boolean doActivity(Task task) {

        var contactInfo = waitForContactInformation();

        try {
            String result = objectMapper.writeValueAsString(contactInfo);
            setResults(result);
            return true;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private Contact waitForContactInformation() {
        // TODO: wait for contact info to be available (the ruby example reads from stdio, but here I have a web app)
        throw new UnsupportedOperationException();
    }

    public record Contact(String email, String phoneNumber) {}
}
