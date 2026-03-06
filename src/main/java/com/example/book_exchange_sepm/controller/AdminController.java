package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.controller.form.AdminCarouselForm;
import com.example.book_exchange_sepm.model.NavItem;
import com.example.book_exchange_sepm.controller.form.AdminFeedCardForm;
import com.example.book_exchange_sepm.model.CarouselSlide;
import com.example.book_exchange_sepm.model.FeedCard;
import com.example.book_exchange_sepm.model.FeedCardType;
import com.example.book_exchange_sepm.service.CarouselSlideService;
import com.example.book_exchange_sepm.service.FeedCardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AdminController {

    private static final List<NavItem> ADMIN_NAV_ITEMS = List.of(
            new NavItem("Dashboard", "/admin"),
            new NavItem("UI", "/admin/ui"),
            new NavItem("Users", "/admin/users"),
            new NavItem("Books", "/admin/books"),
            new NavItem("Exchanges", "/admin/exchanges"),
            new NavItem("Requests", "/admin/requests"),
            new NavItem("Reports", "/admin/reports"),
            new NavItem("Settings", "/admin/settings")
    );

    private final CarouselSlideService carouselSlideService;
    private final FeedCardService feedCardService;

    public AdminController(CarouselSlideService carouselSlideService,
                           FeedCardService feedCardService) {
        this.carouselSlideService = carouselSlideService;
        this.feedCardService = feedCardService;
    }

    @GetMapping("/admin")
    public String adminPage(Model model) {
        populateAdminModel(
                model,
                "Book Exchange | Admin Dashboard",
                "Admin Dashboard",
                "Manage all admin modules for the book exchange platform.",
                "/admin"
        );
        return "admin";
    }

    @GetMapping("/admin/users")
    public String adminUsersPage(Model model) {
        return populateSectionPage(
                model,
                "Book Exchange | Admin Users",
                "User Management",
                "Manage user accounts, reader profiles, and moderation actions.",
                "/admin/users"
        );
    }

    @GetMapping("/admin/books")
    public String adminBooksPage(Model model) {
        return populateSectionPage(
                model,
                "Book Exchange | Admin Books",
                "Book Catalog Management",
                "Review shared books, metadata quality, and listing status.",
                "/admin/books"
        );
    }

    @GetMapping("/admin/exchanges")
    public String adminExchangesPage(Model model) {
        return populateSectionPage(
                model,
                "Book Exchange | Admin Exchanges",
                "Exchange Operations",
                "Track active exchanges and resolve handoff issues.",
                "/admin/exchanges"
        );
    }

    @GetMapping("/admin/requests")
    public String adminRequestsPage(Model model) {
        return populateSectionPage(
                model,
                "Book Exchange | Admin Requests",
                "Request Queue",
                "Process pending borrow and exchange requests.",
                "/admin/requests"
        );
    }

    @GetMapping("/admin/reports")
    public String adminReportsPage(Model model) {
        return populateSectionPage(
                model,
                "Book Exchange | Admin Reports",
                "Reports & Insights",
                "Review activity, engagement, and platform health reports.",
                "/admin/reports"
        );
    }

    @GetMapping("/admin/settings")
    public String adminSettingsPage(Model model) {
        return populateSectionPage(
                model,
                "Book Exchange | Admin Settings",
                "Admin Settings",
                "Configure platform-level behavior and content rules.",
                "/admin/settings"
        );
    }

    @GetMapping("/admin/ui")
    public String adminUiPage(@RequestParam(value = "status", required = false) String status, Model model) {
        populateAdminModel(
                model,
                "Book Exchange | Admin UI",
                "UI Content Manager",
                "Add carousel slides and feed cards for news, books, and authors.",
                "/admin/ui"
        );
        model.addAttribute("carouselForm", new AdminCarouselForm());
        model.addAttribute("feedCardForm", new AdminFeedCardForm());
        model.addAttribute("feedTypes", FeedCardType.values());
        model.addAttribute("carouselSlides", carouselSlideService.getActiveSlides());
        model.addAttribute("newsCards", feedCardService.getActiveCardsByType(FeedCardType.NEWS));
        model.addAttribute("bookCards", feedCardService.getActiveCardsByType(FeedCardType.BOOK));
        model.addAttribute("authorCards", feedCardService.getActiveCardsByType(FeedCardType.AUTHOR));
        model.addAttribute("status", status);
        return "admin-ui";
    }

    @PostMapping("/admin/ui/carousel")
    public String addCarouselSlide(@ModelAttribute("carouselForm") AdminCarouselForm form) {
        CarouselSlide slide = new CarouselSlide(
                form.getTitle(),
                form.getSubtitle(),
                form.getImageUrl(),
                form.getDisplayOrder(),
                form.getActive()
        );
        carouselSlideService.createSlide(slide);
        return "redirect:/admin/ui?status=carousel-added";
    }

    @PostMapping("/admin/ui/feed")
    public String addFeedCard(@ModelAttribute("feedCardForm") AdminFeedCardForm form) {
        FeedCard card = new FeedCard(
                form.getType(),
                form.getHeadline(),
                form.getShortText(),
                form.getImageUrl(),
                form.getReadingTime(),
                form.getDisplayOrder(),
                form.getActive()
        );
        feedCardService.createCard(card);
        return "redirect:/admin/ui?status=feed-added";
    }

    private String populateSectionPage(Model model,
                                       String pageTitle,
                                       String pageHeading,
                                       String pageDescription,
                                       String activePath) {
        populateAdminModel(model, pageTitle, pageHeading, pageDescription, activePath);
        return "admin-section";
    }

    private void populateAdminModel(Model model,
                                    String pageTitle,
                                    String pageHeading,
                                    String pageDescription,
                                    String activePath) {
        model.addAttribute("adminNavItems", ADMIN_NAV_ITEMS);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageHeading", pageHeading);
        model.addAttribute("pageDescription", pageDescription);
        model.addAttribute("activePath", activePath);
    }
}
