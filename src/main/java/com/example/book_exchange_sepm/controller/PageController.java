package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.controller.form.BookSearchForm;
import com.example.book_exchange_sepm.model.FeedCardType;
import com.example.book_exchange_sepm.service.BookService;
import com.example.book_exchange_sepm.service.CarouselSlideService;
import com.example.book_exchange_sepm.service.FeedCardService;
import com.example.book_exchange_sepm.service.PageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;

@Controller
public class PageController {

    private final PageService pageService;
    private final CarouselSlideService carouselSlideService;
    private final FeedCardService feedCardService;
    private final BookService bookService;

    public PageController(PageService pageService,
                          CarouselSlideService carouselSlideService,
                          FeedCardService feedCardService,
                          BookService bookService) {
        this.pageService = pageService;
        this.carouselSlideService = carouselSlideService;
        this.feedCardService = feedCardService;
        this.bookService = bookService;
    }

    @GetMapping({"/", "/landingpage"})
    public String landingPage(Model model) {
        populateModel(model, "landingpage");
        model.addAttribute("carouselSlides", carouselSlideService.getActiveSlides());
        model.addAttribute("newsCards", feedCardService.getActiveCardsByType(FeedCardType.NEWS));
        model.addAttribute("bookCards", feedCardService.getActiveCardsByType(FeedCardType.BOOK));
        model.addAttribute("authorCards", feedCardService.getActiveCardsByType(FeedCardType.AUTHOR));
        return "landingpage";
    }

    @GetMapping("/browse")
    public String browsePage(@ModelAttribute("search") BookSearchForm search, Model model) {
        populateModel(model, "browse");
        boolean searched = search.isSearched();
        model.addAttribute("books", searched ? bookService.searchBooks(search) : Collections.emptyList());
        model.addAttribute("genres", bookService.getGenres());
        model.addAttribute("languages", bookService.getLanguages());
        model.addAttribute("searched", searched);
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
