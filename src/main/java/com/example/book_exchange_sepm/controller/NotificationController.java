package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.dto.UserNotificationResponse;
import com.example.book_exchange_sepm.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<UserNotificationResponse>> getMyNotifications() {
        return new ResponseEntity<>(notificationService.getMyNotifications(), HttpStatus.OK);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<UserNotificationResponse> markRead(@PathVariable Long id) {
        return new ResponseEntity<>(notificationService.markAsRead(id), HttpStatus.OK);
    }
}
