package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.ExchangeRequestRequest;
import com.example.book_exchange_sepm.dto.ExchangeRequestResponse;
import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.ChatRoom;
import com.example.book_exchange_sepm.entity.Delivery;
import com.example.book_exchange_sepm.entity.ExchangeRequest;
import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.repository.BookRepository;
import com.example.book_exchange_sepm.repository.ChatMessageRepository;
import com.example.book_exchange_sepm.repository.ChatRoomRepository;
import com.example.book_exchange_sepm.repository.DeliveryRepository;
import com.example.book_exchange_sepm.repository.ExchangeRequestRepository;
import com.example.book_exchange_sepm.repository.RoleRepository;
import com.example.book_exchange_sepm.repository.UserNotificationRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import com.example.book_exchange_sepm.repository.WishlistSubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ExchangeRequestServiceIntegrationTest {

    @Autowired
    private ExchangeRequestService exchangeRequestService;

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private WishlistSubscriptionRepository wishlistSubscriptionRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        deliveryRepository.deleteAll();
        exchangeRequestRepository.deleteAll();
        userNotificationRepository.deleteAll();
        wishlistSubscriptionRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createExchangeRequest_ShouldPersistPendingRequest_AndCreateChatRoom() {
        Role userRole = ensureRole("ROLE_USER");
        User owner = saveUser("owner_user", "owner@example.com", userRole);
        User requester = saveUser("requester_user", "requester@example.com", userRole);

        Book requestedBook = saveBook("Requested Book", "REQ-ISBN-001", owner, true);
        Book offeredBook = saveBook("Offered Book", "OFF-ISBN-001", requester, true);

        authenticateAs(requester);

        ExchangeRequestRequest request = new ExchangeRequestRequest(
            requestedBook.getId(),
            offeredBook.getId(),
            "Can we exchange these books?"
        );

        ExchangeRequestResponse response = exchangeRequestService.createExchangeRequest(request);

        assertNotNull(response.getId());
        assertEquals("PENDING", response.getStatus());
        assertEquals(requester.getId(), response.getRequesterId());
        assertEquals(owner.getId(), response.getBookOwnerId());
        assertNotNull(response.getRequesterAcceptedAt());

        ExchangeRequest savedExchange = exchangeRequestRepository.findById(response.getId()).orElseThrow();
        assertEquals(ExchangeRequest.Status.PENDING, savedExchange.getStatus());
        assertEquals(owner.getId(), savedExchange.getOwner().getId());

        ChatRoom chatRoom = chatRoomRepository.findByExchangeRequest_Id(savedExchange.getId()).orElseThrow();
        assertEquals(savedExchange.getId(), chatRoom.getExchangeRequest().getId());
    }

    @Test
    void approveExchangeRequest_ShouldAssignDelivery_AndDeliveredStatusShouldCompleteExchange() {
        Role userRole = ensureRole("ROLE_USER");
        Role moderatorRole = ensureRole("ROLE_MODERATOR");
        Role deliveryRole = ensureRole("ROLE_DELIVERY_MAN");

        User owner = saveUser("owner_flow", "owner.flow@example.com", userRole);
        User requester = saveUser("requester_flow", "requester.flow@example.com", userRole);
        User moderator = saveUser("moderator_flow", "moderator.flow@example.com", moderatorRole);
        User deliveryMan = saveUser("delivery_flow", "delivery.flow@example.com", deliveryRole);
        deliveryMan.setDeliveryRequestStatus(User.DeliveryRequestStatus.APPROVED);
        userRepository.save(deliveryMan);

        Book requestedBook = saveBook("Moderator Requested Book", "REQ-ISBN-002", owner, true);
        Book offeredBook = saveBook("Moderator Offered Book", "OFF-ISBN-002", requester, true);

        authenticateAs(requester);
        ExchangeRequestResponse created = exchangeRequestService.createExchangeRequest(
            new ExchangeRequestRequest(requestedBook.getId(), offeredBook.getId(), "Ready for approval")
        );

        authenticateAs(owner);
        ExchangeRequestResponse accepted = exchangeRequestService.acceptExchangeRequest(created.getId());
        assertNotNull(accepted.getOwnerAcceptedAt());

        authenticateAs(moderator);
        ExchangeRequestResponse approved = exchangeRequestService.approveExchangeRequest(created.getId());

        assertEquals("APPROVED", approved.getStatus());
        assertEquals("ASSIGNED", approved.getDeliveryStatus());
        assertEquals(deliveryMan.getUsername(), approved.getDeliveryManUsername());

        Book afterApprovalRequested = bookRepository.findById(requestedBook.getId()).orElseThrow();
        Book afterApprovalOffered = bookRepository.findById(offeredBook.getId()).orElseThrow();
        assertTrue(Boolean.FALSE.equals(afterApprovalRequested.getAvailable()));
        assertTrue(Boolean.FALSE.equals(afterApprovalOffered.getAvailable()));

        Delivery delivery = deliveryRepository.findByExchangeRequest_Id(created.getId()).orElseThrow();

        authenticateAs(deliveryMan);
        deliveryService.updateDeliveryStatus(delivery.getId(), Delivery.Status.DELIVERED);

        Book reloadedRequested = bookRepository.findById(requestedBook.getId()).orElseThrow();
        Book reloadedOffered = bookRepository.findById(offeredBook.getId()).orElseThrow();

        assertEquals(requester.getId(), reloadedRequested.getOwner().getId());
        assertEquals(owner.getId(), reloadedOffered.getOwner().getId());
        assertTrue(Boolean.TRUE.equals(reloadedRequested.getAvailable()));
        assertTrue(Boolean.TRUE.equals(reloadedOffered.getAvailable()));

        ExchangeRequest persisted = exchangeRequestRepository.findById(created.getId()).orElseThrow();
        assertEquals(ExchangeRequest.Status.COMPLETED, persisted.getStatus());
        assertEquals(moderator.getId(), persisted.getReviewedBy().getId());
        assertNotNull(persisted.getCompletedAt());
    }

    private Role ensureRole(String roleName) {
        return roleRepository.findByName(roleName)
            .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
    }

    private User saveUser(String username, String email, Role... roles) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setEmailVerified(true);
        user.setRoles(Set.of(roles));
        return userRepository.save(user);
    }

    private Book saveBook(String title, String isbn, User owner, boolean available) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor("Test Author");
        book.setGenre("Test Genre");
        book.setLanguage("English");
        book.setIsbn(isbn);
        book.setPublicationYear(2024);
        book.setBookCondition("Good");
        book.setDescription("Integration test book");
        book.setOwner(owner);
        book.setAvailable(available);
        return bookRepository.save(book);
    }

    private void authenticateAs(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .map(Role::getName)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword(), authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
