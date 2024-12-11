package com.adolfoeloy.swflab;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("swf")
public class SwfTestController {

    private final SwfService swfService;

    public SwfTestController(SwfService swfService) {
        this.swfService = swfService;
    }

    @GetMapping("/domains")
    public List<String> getRegisteredDomains() {
        return swfService.getRegisteredDomains();
    }
}
