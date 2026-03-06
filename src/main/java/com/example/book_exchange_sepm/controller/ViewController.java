package com.example.book_exchange_sepm.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        return "home";
    }

    @GetMapping("/user/home")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public String userHome(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        return "user-home";
    }

    @GetMapping("/moderator/home")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String moderatorHome(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        return "moderator-home";
    }

    @GetMapping("/admin/home")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminHome(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        return "admin-home";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
