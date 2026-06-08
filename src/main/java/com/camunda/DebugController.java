package com.camunda;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    @GetMapping("/camunda/debug")
    public String debug(HttpServletRequest request) {
        return "scheme=" + request.getScheme()
                + ", port=" + request.getServerPort()
                + ", forwarded=" + request.getHeader("X-Forwarded-Proto");
    }
}