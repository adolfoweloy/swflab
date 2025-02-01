package com.adolfoeloy.swflab.swf.domain.activity;

import com.adolfoeloy.swflab.swf.config.ObjectMapperConfiguration;
import com.adolfoeloy.swflab.swf.domain.Task;
import com.adolfoeloy.swflab.swf.domain.workflow.SwfWorkflowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.swf.SwfClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GetContactActivityTest {

    @Mock
    private SwfWorkflowRepository swfWorkflowRepositoryMock;

    @Mock
    private SwfClient swfClientMock;

    @Test
    void waitForContactInformation_should_get_result_in_the_expected_json_format() {
        var config = new ObjectMapperConfiguration();
        var subject = new GetContactActivityForTest(config.objectMapper(), swfClientMock, swfWorkflowRepositoryMock);
        var task = new Task("1", "", "token");

        var result = subject.doActivity(task);

        assertThat(result).isTrue();
        assertThat(subject.getResults()).isEqualTo("{\"email\":\"jujuba@test.com\",\"sms\":\"+55 11 21767000\"}");
    }

    static class GetContactActivityForTest extends GetContactActivity {

        protected GetContactActivityForTest(ObjectMapper objectMapper, SwfClient swfClient, SwfWorkflowRepository swfWorkflowRepository) {
            super(objectMapper, swfClient, swfWorkflowRepository);
        }

        @Override
        CompletableFuture<Map<String, String>> waitForContactInformation(String s) {
            var future = new CompletableFuture<Map<String, String>>();
            future.complete(
                Map.of(
                        "email", "jujuba@test.com",
                        "sms", "+55 11 21767000"
                ));
            return future;
        }

    }
}