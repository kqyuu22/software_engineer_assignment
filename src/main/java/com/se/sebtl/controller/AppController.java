package com.se.sebtl.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppController {

    // Handles the absolute root: localhost:8080
    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    // Handles the URL: localhost:8080/login
    @GetMapping("/login")
    public String login() {
        // Points to src/main/resources/static/auth.html
        return "forward:/auth.html"; 
    }

    // Handles the URL: localhost:8080/operator
    @GetMapping("/operator")
    public String operator() {
        // Points to src/main/resources/static/operator.html
        return "forward:/operator.html"; 
    }

    // Handles the URL: localhost:8080/admin
    @GetMapping("/admin")
    public String admin() {
        // Points to src/main/resources/static/admin.html
        return "forward:/admin.html"; 
    }

    @GetMapping("/member")
    public String member(){

        return "forward:/member.html";
    }
}
