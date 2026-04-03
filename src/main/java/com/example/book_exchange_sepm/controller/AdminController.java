package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.controller.form.AdminCarouselForm;
import com.example.book_exchange_sepm.controller.form.AdminFeedCardForm;
import com.example.book_exchange_sepm.dto.UserResponse;
import com.example.book_exchange_sepm.model.CarouselSlide;
import com.example.book_exchange_sepm.model.FeedCard;
import com.example.book_exchange_sepm.model.FeedCardType;
import com.example.book_exchange_sepm.model.NavItem;
import com.example.book_exchange_sepm.service.AdminDashboardService;
import com.example.book_exchange_sepm.service.CarouselSlideService;
import com.example.book_exchange_sepm.service.FeedCardService;
import com.example.book_exchange_sepm.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final List<NavItem> ADMIN_NAV_ITEMS = List.of(
        new NavItem("Dashboard", "/admin/dashboard"),
        new NavItem("Users", "/admin/users"),
        new NavItem("Books", "/admin/books"),
        new NavItem("Exchanges", "/admin/exchanges"),
        new NavItem("Wishlists", "/admin/wishlists"),
        new NavItem("UI", "/admin/ui")
    );

    private final CarouselSlideService carouselSlideService;
    private final FeedCardService feedCardService;
    private final AdminDashboardService adminDashboardService;
    private final UserService userService;

    public AdminController(CarouselSlideService carouselSlideService,
                           FeedCardService feedCardService,
                           AdminDashboardService adminDashboardService,
                           UserService userService) {
        this.carouselSlideService = carouselSlideService;
        this.feedCardService = feedCardService;
        this.adminDashboardService = adminDashboardService;
        this.userService = userService;
    }

    @GetMapping({"", "/"})
    public String adminPage(Model model) {
        return adminDashboardPage(model);
    }

    @GetMapping("/dashboard")
    public String adminDashboardPage(Model model) {
        model.addAttribute("stats", adminDashboardService.getDashboardStats());
        model.addAttribute("activity", adminDashboardService.getActivitySummary());
        model.addAttribute("pendingRequests", adminDashboardService.getPendingExchangeRequests());
        model.addAttribute("pageTitle", "Admin Dashboard");
        model.addAttribute("adminNavItems", ADMIN_NAV_ITEMS);
        model.addAttribute("activePath", "/admin/dashboard");
        return "admin-dashboard";
    }

    @GetMapping("/users")
    public String adminUsersPage(Model model) {
        List<Map<String, Object>> users = adminDashboardService.getAllUsersWithStats();
        model.addAttribute("users", users);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("verifiedUsers", users.stream().filter(u -> Boolean.TRUE.equals(u.get("isEmailVerified"))).count());
        model.addAttribute("moderatorCount", users.stream().filter(u -> "MODERATOR".equals(String.valueOf(u.get("role")))).count());
        model.addAttribute("pendingDeliveryRequests", users.stream().filter(u -> "PENDING".equals(String.valueOf(u.get("deliveryRequestStatus")))).count());
        model.addAttribute("pageTitle", "User Management");
        model.addAttribute("adminNavItems", ADMIN_NAV_ITEMS);
        model.addAttribute("activePath", "/admin/users");
        return "admin-users";
    }

    @GetMapping("/books")
    public String adminBooksPage(Model model) {
        List<Map<String, Object>> books = adminDashboardService.getAllBooksWithDetails();
        model.addAttribute("books", books);
        model.addAttribute("totalBooks", books.size());
        model.addAttribute("availableBooks", books.stream().filter(b -> "AVAILABLE".equals(String.valueOf(b.get("status")))).count());
        model.addAttribute("allocatedBooks", books.stream().filter(b -> "ALLOCATED".equals(String.valueOf(b.get("status")))).count());
        model.addAttribute("pageTitle", "Book Management");
        model.addAttribute("adminNavItems", ADMIN_NAV_ITEMS);
        model.addAttribute("activePath", "/admin/books");
        return "admin-books";
    }

    @GetMapping("/exchanges")
    public String adminExchangesPage(Model model) {
        List<Map<String, Object>> exchanges = adminDashboardService.getAllExchangeRequests();
        model.addAttribute("exchanges", exchanges);
        model.addAttribute("totalExchanges", exchanges.size());
        model.addAttribute("pendingCount", exchanges.stream().filter(e -> "PENDING".equals(String.valueOf(e.get("status")))).count());
        model.addAttribute("approvedCount", exchanges.stream().filter(e -> "APPROVED".equals(String.valueOf(e.get("status")))).count());
        model.addAttribute("rejectedCount", exchanges.stream().filter(e -> "REJECTED".equals(String.valueOf(e.get("status")))).count());
        model.addAttribute("cancelledCount", exchanges.stream().filter(e -> "CANCELLED".equals(String.valueOf(e.get("status")))).count());
        model.addAttribute("pageTitle", "Exchange Management");
        model.addAttribute("adminNavItems", ADMIN_NAV_ITEMS);
        model.addAttribute("activePath", "/admin/exchanges");
        return "admin-exchanges";
    }

    @GetMapping("/pending")
    public String adminPendingPage(Model model) {
        List<Map<String, Object>> pendingRequests = adminDashboardService.getPendingExchangeRequests();
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("count", pendingRequests.size());
        model.addAttribute("pageTitle", "Pending Approvals");
        model.addAttribute("adminNavItems", ADMIN_NAV_ITEMS);
        model.addAttribute("activePath", "/admin/pending");
        return "admin-pending";
    }

    @GetMapping("/wishlists")
    public String adminWishlistsPage(Model model) {
        List<Map<String, Object>> wishlists = adminDashboardService.getAllWishlistSubscriptions();
        model.addAttribute("wishlists", wishlists);
        model.addAttribute("totalWishlists", wishlists.size());
        model.addAttribute("activeWishlists", wishlists.stream().filter(w -> Boolean.TRUE.equals(w.get("active"))).count());
        model.addAttribute("inactiveWishlists", wishlists.stream().filter(w -> !Boolean.TRUE.equals(w.get("active"))).count());
        model.addAttribute("pageTitle", "Wishlist Management");
        model.addAttribute("adminNavItems", ADMIN_NAV_ITEMS);
        model.addAttribute("activePath", "/admin/wishlists");
        return "admin-wishlists";
    }

    @GetMapping("/ui")
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

    @PostMapping("/ui/carousel")
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

    @PostMapping("/ui/feed")
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

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(adminDashboardService.getDashboardStats());
    }

    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getUsersData() {
        return ResponseEntity.ok(adminDashboardService.getAllUsersWithStats());
    }

    @GetMapping("/api/books")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getBooksData() {
        return ResponseEntity.ok(adminDashboardService.getAllBooksWithDetails());
    }

    @GetMapping("/api/exchanges")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getExchangesData() {
        return ResponseEntity.ok(adminDashboardService.getAllExchangeRequests());
    }

    @PostMapping("/api/users/{id}/promote-to-moderator")
    @ResponseBody
    public ResponseEntity<UserResponse> promoteUserToModerator(
            @PathVariable Long id,
            @RequestParam(value = "bookQuota", required = false) Integer bookQuota) {
        UserResponse promoted = userService.promoteToModerator(id, bookQuota != null ? bookQuota : 5);
        return new ResponseEntity<>(promoted, HttpStatus.OK);
    }

    @PostMapping("/api/users/{id}/approve-delivery")
    @ResponseBody
    public ResponseEntity<UserResponse> approveUserAsDeliveryMan(@PathVariable Long id) {
        UserResponse approved = userService.approveDeliveryManRole(id);
        return new ResponseEntity<>(approved, HttpStatus.OK);
    }

    private void populateAdminModel(Model model,
                                    String title,
                                    String heading,
                                    String description,
                                    String activePath) {
        model.addAttribute("adminNavItems", ADMIN_NAV_ITEMS);
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageHeading", heading);
        model.addAttribute("pageDescription", description);
        model.addAttribute("activePath", activePath);
    }
}
