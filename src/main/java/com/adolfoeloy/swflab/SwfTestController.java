package com.adolfoeloy.swflab;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("swf")
public class SwfTestController {

    private final SwfService swfService;

    public SwfTestController(SwfService swfService) {
        this.swfService = swfService;
    }

    @GetMapping("/domain")
    public String getRegisteredDomain() {
        return swfService.getDomainName();
    }

    @GetMapping("/workflow")
    public SwfService.Workflow getWorkflow() {
        return swfService.getWorkflow();
    }
}
