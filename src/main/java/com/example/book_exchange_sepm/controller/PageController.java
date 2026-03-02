package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.service.CarouselSlideService;
import com.example.book_exchange_sepm.service.PageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final PageService pageService;
    private final CarouselSlideService carouselSlideService;

    public PageController(PageService pageService, CarouselSlideService carouselSlideService) {
        this.pageService = pageService;
        this.carouselSlideService = carouselSlideService;
    }

    @GetMapping({"/", "/landingpage"})
    public String landingPage(Model model) {
        populateModel(model, "landingpage");
        model.addAttribute("carouselSlides", carouselSlideService.getActiveSlides());
        return "landingpage";
    }

    @GetMapping("/browse")
    public String browsePage(Model model) {
        populateModel(model, "browse");
        return "browse";
    }

    @GetMapping("/book")
    public String bookPage(Model model) {
        populateModel(model, "book");
        return "book";
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        populateModel(model, "profile");
        return "profile";
    }

    private void populateModel(Model model, String pageKey) {
        model.addAttribute("navItems", pageService.getNavigation());
        model.addAttribute("page", pageService.getPageContent(pageKey));
    }
}
