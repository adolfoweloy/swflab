package com.adolfoeloy.swflab;


import com.adolfoeloy.swflab.swf.service.WorkflowProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties(WorkflowProperties.class)
public class SwflabApplication {

	public static void main(String[] args) {
		SpringApplication.run(SwflabApplication.class, args);
	}

}
