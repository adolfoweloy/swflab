package com.adolfoeloy.swflab.swf.domain.activity;

import com.adolfoeloy.swflab.swf.config.ObjectMapperConfiguration;
import com.adolfoeloy.swflab.swf.domain.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


class GetContactActivityTest {

    @Test
    void waitForContactInformation_should_bla() {
        var config = new ObjectMapperConfiguration();
        var subject = new GetContactActivityForTest(config.objectMapper());
        var task = new Task("1", "");

        var result = subject.doActivity(task);

        assertThat(result).isTrue();
        assertThat(subject.getResults()).isEqualTo("{\"email\":\"jujuba@test.com\",\"sms\":\"+55 11 21767000\"}");
    }

    static class GetContactActivityForTest extends GetContactActivity {

        protected GetContactActivityForTest(ObjectMapper objectMapper) {
            super(objectMapper);
        }

        @Override
        Map<String, String> waitForContactInformation() {
            return Map.of(
                    "email", "jujuba@test.com",
                    "sms", "+55 11 21767000"
            );
        }

    }
}