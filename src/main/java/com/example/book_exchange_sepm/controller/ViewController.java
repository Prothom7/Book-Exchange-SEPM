package com.example.book_exchange_sepm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
