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

    @GetMapping("/landingpage")
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
        model.addAttribute("books", bookService.searchBooks(search));
        model.addAttribute("genres", bookService.getGenres());
        model.addAttribute("languages", bookService.getLanguages());
        model.addAttribute("searched", true);
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

    @GetMapping("/exchange")
    public String exchangePage(Model model) {
        populateModel(model, "exchange");
        return "exchange";
    }

    @GetMapping("/wishlist")
    public String wishlistPage(Model model) {
        populateModel(model, "wishlist");
        return "wishlist";
    }

    @GetMapping("/exchange-chat")
    public String exchangeChatPage(Model model) {
        populateModel(model, "exchange-chat");
        return "exchange-chat";
    }

    private void populateModel(Model model, String pageKey) {
        model.addAttribute("navItems", pageService.getNavigation());
        model.addAttribute("page", pageService.getPageContent(pageKey));
    }
}
