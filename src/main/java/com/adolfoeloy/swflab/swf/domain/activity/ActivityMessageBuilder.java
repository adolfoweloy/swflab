package com.adolfoeloy.swflab.swf.domain.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * I know this class is horrible. It doesn't declare its purpose,
 * the class can just grow with lots of unrelated methods that would contribute to poor legibility.
 * TODO: https://github.com/adolfoweloy/swflab/issues/1 (this refactoring will remove the need for this ugly class).
 */
@Component
public class ActivityMessageBuilder {
    private final ObjectMapper objectMapper;

    public ActivityMessageBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String errorMessage(String message) {
        var error = new Error(message, "");
        try {
            return objectMapper.writeValueAsString(error);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public record Error(String reason, String detail) {
    }
}
