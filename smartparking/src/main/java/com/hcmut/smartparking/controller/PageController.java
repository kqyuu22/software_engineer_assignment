package com.hcmut.smartparking.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

@RestController
public class PageController {

    @GetMapping("/member")
    public void member(HttpServletResponse response) throws IOException {
        response.sendRedirect("/member.html");
    }

    @GetMapping("/operator")
    public void operator(HttpServletResponse response) throws IOException {
        response.sendRedirect("/operator.html");
    }

    @GetMapping("/admin")
    public void admin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/admin.html");
    }
}