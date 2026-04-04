# Book Exchange SEPM File Explanations

This guide explains the repository file-by-file:

- what each file does
- how it works in the project
- the key functions, methods, or responsibilities inside it

Notes:

- For DTOs, entities, forms, and simple models, many methods are normal constructors/getters/setters. This guide focuses on the important methods and the file's real responsibility.
- For HTML/CSS files, there are no Java methods; their "functions" are page structure and styling responsibilities.

## 1. Root And Infrastructure Files

- `.dockerignore`: Excludes unnecessary files from Docker builds so images stay smaller and faster to build.
- `.env`: Local runtime variables for app/database startup.
- `.env.example`: Safe template showing the variables expected by the app.
- `.gitattributes`: Git normalization settings such as line-ending handling.
- `.gitignore`: Prevents generated files, secrets, and local IDE files from being committed.
- `DESIGN_PATTERNS.md`: Explains the Strategy, Factory, and Singleton pattern usage in this project.
- `docker-compose.yml`: Defines local multi-container startup, usually app plus PostgreSQL.
- `Dockerfile`: Builds the Spring Boot application image.
- `EMAIL_SETUP.md`: Setup guide for the email verification/password email flow.
- `EXCHANGE_SYSTEM_REBUILD.md`: Deep explanation of the rebuilt exchange workflow and related design choices.
- `full-test-suite.ps1`: PowerShell helper to run tests more easily on Windows.
- `IMPLEMENTATION_COMPLETE.md`: Milestone-style summary of completed features.
- `mvnw` / `mvnw.cmd`: Maven Wrapper scripts for Unix and Windows.
- `pom.xml`: Main Maven build file. It defines Spring Boot, JPA, Security, Thymeleaf, WebSocket, Mail, JWT, Passay, PostgreSQL, H2, and test plugins. Key build logic: Surefire runs unit tests; Failsafe runs integration tests.
- `PROJECT_INDEX.md`: Existing repository index and implementation map.
- `promote-users.ps1`: Helper script for role/user promotion workflows during development or demos.
- `QUICK_START.md`: Short getting-started guide for new developers.
- `README.md`: Main project documentation covering purpose, stack, endpoints, architecture, and run steps.
- `render.yaml`: Render deployment config.
- `RENDER_DEPLOY.md`: Notes for deploying to Render.
- `SESSION_COMPLETION_REPORT.md`: Session summary/history document.
- `test-api.ps1`: Script for manual API testing.
- `TESTING_GUIDE.md`: Detailed testing instructions.
- `.github/workflows/ci.yml`: GitHub Actions CI workflow.
- `.idea/workspace.xml`: Local IntelliJ metadata; not part of runtime behavior.
- `.mvn/wrapper/maven-wrapper.properties`: Maven Wrapper version/config file.
- `docs/architecture.png`: Architecture diagram used by the docs.
- `docs/er_diagram.png`: ER diagram used by the docs.

## 2. Application Entry Point

- `src/main/java/com/example/book_exchange_sepm/BookExchangeSepmApplication.java`: Spring Boot bootstrap class. Main method: `main`.

## 3. Configuration Package

- `config/BookSeeder.java`: Seeds initial book/user data. Helper method: `ensureUser`.
- `config/CarouselSlideSeeder.java`: Seeds landing-page carousel slides at startup.
- `config/ChatSchemaConstraintInitializer.java`: Repairs/adds chat-related DB constraints. Main method: `run`.
- `config/ComprehensiveDataSeeder.java`: Large demo-data seeder for users, books, exchanges, and wishlists. Main methods: `createUsers`, `createUser`, `createBooks`, `createExchangeRequests`, `createExchangeRequest`, `createWishlistSubscriptions`.
- `config/DeliveryAssignmentCompatibilityInitializer.java`: Backfills delivery assignment compatibility. Main methods: `run`, `needsApprovedDriver`, `getActiveDeliveryCount`.
- `config/ExchangeDataIntegrityInitializer.java`: Validates and repairs broken exchange records. Main methods: `validateAndRepairExchangeData`, `isExchangeValid`, `repairExchange`, `getRepairReason`, `backfillOwnerIfMissing`.
- `config/ExchangeSchemaConstraintInitializer.java`: Applies exchange-related DB constraints. Main method: `run`.
- `config/FeedCardSeeder.java`: Seeds landing-page feed-card content.
- `config/LocalUserBookTopUpSeeder.java`: Adds extra books for local development users. Main methods: `isLocalDevelopmentDatabase`, `isNormalUser`, `createTopUpBook`, `buildUniqueIsbn`, `insertBookCompatibly`, `hasColumn`.
- `config/ProductionDataSeeder.java`: Safer production/demo data seeder. Main methods: `ensureUsers`, `createUserIfMissing`, `createBooks`, `createExchangeRequests`, `createWishlistSubscriptions`.
- `config/RoleBasedAuthenticationSuccessHandler.java`: Redirects users after login based on role. Main method: `onAuthenticationSuccess`.
- `config/RoleInitializer.java`: Ensures the required roles exist. Main methods: `run`, `createRoleIfNotFound`.
- `config/SecurityConfig.java`: Central Spring Security config. It defines password encoding, authentication manager, URL access rules, form login, JWT filter insertion, API entry points, and session behavior. Main methods: `passwordEncoder`, `authenticationManager`, `filterChain`.
- `config/UserSchemaCompatibilityInitializer.java`: Repairs/backfills user-schema compatibility. Main method: `run`.
- `config/WebSocketConfig.java`: STOMP/WebSocket config. It enables `/topic` and `/queue`, uses `/app` for application destinations, registers `/ws` and `/ws-chat`, and attaches the chat interceptor. Main methods: `configureMessageBroker`, `registerStompEndpoints`, `configureClientInboundChannel`.

## 4. Controllers

- `controller/AdminController.java`: Serves admin pages and admin data APIs. Main methods: `adminDashboardPage`, `adminUsersPage`, `adminBooksPage`, `adminExchangesPage`, `adminWishlistsPage`, `adminUiPage`, `addCarouselSlide`, `addFeedCard`, `promoteUserToModerator`, `approveUserAsDeliveryMan`.
- `controller/AuthenticationController.java`: REST auth endpoints under `/api/auth`. Main methods: `register`, `login`, `verifyEmail`.
- `controller/AuthPageController.java`: MVC page/controller flow for login, register, verify-email, and resend-verification pages/actions. Main methods: `root`, `login`, `loginFailure`, `registerPage`, `register`, `verifyEmail`, `resendVerification`.
- `controller/BookController.java`: Main REST API for books under `/api/books`. Main methods: `createBook`, `getAllBooks`, `getBookById`, `getAvailableBooks`, `getMyBooks`, `updateBook`, `deleteBook`, `markAvailability`.
- `controller/BooksRestController.java`: Alternate/legacy book REST surface under `/api/books-rest`. Main methods: `addBook`, `browseAvailableBooks`, `searchBooks`, `getBookDetails`.
- `controller/ChatController.java`: REST endpoints for exchange chat, message history, and chat list retrieval. Main methods: `getExchangeMessages`, `sendMessageToExchange`, `getActiveChatRooms`, `getExchangeChatRoom`.
- `controller/ChatWebSocketController.java`: STOMP controller for live exchange chat. Main methods: `sendMessageToExchange`, `sendMessageToChatAlias`, `sendMessage`.
- `controller/DeliveryController.java`: Delivery-man API. Main methods: `getMyAssignments`, `updateStatus`.
- `controller/ExchangeRequestController.java`: Main exchange-request REST API. Main methods: `createExchangeRequest`, `getMyBookExchangeRequests`, `getMyExchangeRequests`, `getPendingForModeration`, `getExchangeRequestById`, `approveExchangeRequest`, `rejectExchangeRequest`, `acceptExchangeRequest`, `cancelExchangeRequest`, `completeExchangeRequest`.
- `controller/ExchangeRestController.java`: Alternate/legacy exchange REST API. Main methods: `requestExchange`, `getMyRequests`, `updateStatus`.
- `controller/ModeratorController.java`: Moderator-only endpoints/dashboard helpers. Main methods: `deleteInappropriateBook`, `moderatorDashboard`.
- `controller/NotificationController.java`: Notification feed API. Main methods: `getMyNotifications`, `markRead`.
- `controller/PageController.java`: Main Thymeleaf page router for landing, browse, book, profile, exchange, delivery, wishlist, and exchange-chat views. Main methods: `landingPage`, `browsePage`, `bookPage`, `profilePage`, `exchangePage`, `deliveryPage`, `wishlistPage`, `exchangeChatPage`, `populateModel`.
- `controller/UserController.java`: Current-user profile API. Main methods: `getCurrentUserProfile`, `updateProfileImage`, `requestDeliveryRole`, `userDashboard`.
- `controller/ViewController.java`: Simple view controller such as access-denied page routing. Main method: `accessDenied`.
- `controller/WishlistController.java`: Main wishlist REST API under `/api/wishlist`. Main methods: `subscribe`, `getMySubscriptions`, `deactivate`.
- `controller/WishlistRestController.java`: Alternate/legacy wishlist API under `/api/wishlist-rest`. Main methods: `addWishlist`, `getWishlist`, `removeWishlist`.

## 5. Form Objects

- `controller/form/AdminCarouselForm.java`: Form bean for admin carousel creation. It mainly uses getters/setters for title, subtitle, image URL, order, and active state.
- `controller/form/AdminFeedCardForm.java`: Form bean for admin feed-card creation. It mainly uses getters/setters for type, headline, text, image, reading time, order, and active state.
- `controller/form/BookSearchForm.java`: MVC search filter object for browse page. Key fields cover keyword/title/author/genre/language/isbn/condition/year range plus availability flags.

## 6. DTO Classes

- `dto/AuthResponse.java`: Response model for register/login/verify flows.
- `dto/BookRequest.java`: Input model for creating/updating books.
- `dto/BookResponse.java`: Output model for book data.
- `dto/ChatConversationResponse.java`: Chat conversation summary. Functions are mainly getters/setters.
- `dto/ChatMessageRequest.java`: Input model for sending a chat message.
- `dto/ChatMessageResponse.java`: Output model for chat messages.
- `dto/ChatParticipantResponse.java`: Lightweight participant DTO. Functions: `getId`, `setId`, `getUsername`, `setUsername`.
- `dto/ChatStartConversationRequest.java`: Request DTO for username-based conversation start. Functions: `getUsername`, `setUsername`.
- `dto/DeliveryResponse.java`: Output model for delivery assignment data.
- `dto/DeliveryStatusUpdateRequest.java`: Input model for delivery status changes.
- `dto/ErrorResponse.java`: Standard API error body. Main getters: `getStatus`, `getMessage`, `getError`, `getPath`, `getTimestamp`.
- `dto/ExchangeChatRoomResponse.java`: Chat-room summary for exchange chat lists. Functions are getters/setters for room, other user, book, last message, and timestamps.
- `dto/ExchangeRequestRequest.java`: Input model for creating an exchange request.
- `dto/ExchangeRequestResponse.java`: Detailed exchange-request response model.
- `dto/ExchangeStatusUpdateRequest.java`: Moderator status update request, mainly approve/reject.
- `dto/LoginRequest.java`: Input model for login.
- `dto/ProfileImageUpdateRequest.java`: Input model carrying profile image data URL.
- `dto/RegisterRequest.java`: Input model for registration.
- `dto/UserNotificationResponse.java`: Output model for notification feed items.
- `dto/UserResponse.java`: Output model for user profile and role data.
- `dto/WishlistSubscriptionRequest.java`: Input model for wishlist subscription creation.
- `dto/WishlistSubscriptionResponse.java`: Output model for wishlist subscription data.

## 7. Entities

- `entity/Book.java`: JPA entity for a listed book. It stores metadata, owner, availability, image URL, and timestamps. Main lifecycle methods: `onCreate`, `onUpdate`.
- `entity/ChatMessage.java`: JPA entity for one chat message. Main lifecycle method: `onCreate`.
- `entity/ChatRoom.java`: JPA entity for an exchange-linked chat room. Main lifecycle method: `onCreate`.
- `entity/Delivery.java`: JPA entity for delivery assignment and progress tracking. Main lifecycle methods: `onCreate`, `onUpdate`.
- `entity/ExchangeRequest.java`: JPA entity for the exchange workflow. It stores requester, owner, requested/offered books, status, moderator review, participant acceptance, and completion timestamps. Main lifecycle methods: `onCreate`, `onUpdate`.
- `entity/Role.java`: JPA entity for security roles.
- `entity/User.java`: JPA entity for users, credentials, roles, verification, profile image, and delivery-role request state. Main lifecycle methods: `onCreate`, `onUpdate`.
- `entity/UserNotification.java`: JPA entity for wishlist-triggered notifications. Main lifecycle method: `onCreate`.
- `entity/WishlistSubscription.java`: JPA entity for wishlist entries. Main lifecycle methods: `onCreate`, `onUpdate`.

## 8. Events, Exceptions, And View Models

- `event/BookAvailableEvent.java`: Event published when a book becomes available. Main getters: `getBookId`, `getTitle`, `getAuthor`, `getGenre`.
- `exception/DuplicateUserException.java`: Custom exception for duplicate username/email cases.
- `exception/GlobalExceptionHandler.java`: Centralized REST exception mapper. Main methods: `handleResourceNotFound`, `handleUnauthorizedAction`, `handleDuplicateUser`, `handleAuthenticationException`, `handleAccessDenied`, `handleValidationError`, `handleIllegalArgument`, `handleGenericException`.
- `exception/ResourceNotFoundException.java`: Custom exception for missing resources.
- `exception/UnauthorizedActionException.java`: Custom exception for forbidden business actions.
- `model/CarouselSlide.java`: Landing-page carousel content model.
- `model/FeedCard.java`: Landing-page feed-card content model.
- `model/FeedCardType.java`: Enum for feed-card types like news/book/author.
- `model/NavItem.java`: Navigation item model. Main constructor: `NavItem`.
- `model/PageContent.java`: Page content model. Main constructor: `PageContent`.

## 9. Design Patterns

- `pattern/factory/ExchangeRequestFactory.java`: Factory helper for creating exchange requests. Main methods: `createPending`, `createReviewed`, `create`.
- `pattern/factory/UserFactory.java`: Factory helper for creating users by type. Main method: `create`.
- `pattern/singleton/BookAvailabilitySubscriber.java`: Subscriber contract for book-availability notifications.
- `pattern/singleton/BookEventManager.java`: Singleton event bus for book-availability events. Main methods: `getInstance`, `subscribe`, `unsubscribe`, `publish`.
- `pattern/strategy/AuthorSearchStrategy.java`: Search strategy for author matching. Main methods: `matches`, `containsIgnoreCase`.
- `pattern/strategy/BookSearchStrategy.java`: Strategy interface for search implementations.
- `pattern/strategy/BookSearchStrategyResolver.java`: Maps `SearchMode` to a concrete strategy. Main method: `resolve`.
- `pattern/strategy/GenreSearchStrategy.java`: Search strategy for genre matching. Main methods: `matches`, `equalsIgnoreCase`.
- `pattern/strategy/KeywordSearchStrategy.java`: Search strategy for multi-field keyword matching. Main methods: `matches`, `containsIgnoreCase`.
- `pattern/strategy/SearchMode.java`: Enum for selecting search strategy.
- `pattern/strategy/TitleSearchStrategy.java`: Search strategy for title matching. Main methods: `matches`, `containsIgnoreCase`.

## 10. Repositories

- `repository/BookRepository.java`: Book data access. Key queries: `findByOwner`, `findByAvailableTrue`, `findAllByOrderByTitleAsc`, `findDistinctGenres`, `findDistinctLanguages`.
- `repository/CarouselSlideRepository.java`: Carousel slide data access. Key query: `findByActiveTrueOrderByDisplayOrderAsc`.
- `repository/ChatMessageRepository.java`: Chat message data access. Key queries: `findByChatRoom_IdOrderByCreatedAtAsc`, `findByChatRoom_IdOrderByCreatedAtDesc`, `findByChatRoomAndSender`.
- `repository/ChatRoomRepository.java`: Chat room data access. Key queries: `findByExchangeRequest`, `findByExchangeRequest_Id`, `existsByExchangeRequest_Id`.
- `repository/DeliveryRepository.java`: Delivery data access. Key queries: `findByExchangeRequest_Id`, `existsByExchangeRequest_Id`, `findByDeliveryMan_IdOrderByUpdatedAtDesc`, `countByDeliveryMan_IdAndStatusIn`.
- `repository/ExchangeRequestRepository.java`: Exchange workflow data access. Key queries include `findByOwner_IdOrderByCreatedAtDesc`, `findByRequester_IdOrderByCreatedAtDesc`, `findByStatusAndRequesterAcceptedAtNotNullAndOwnerAcceptedAtNotNullOrderByCreatedAtDesc`, `existsByRequester_IdAndBook_IdAndStatus`, `existsByRequester_IdAndBook_IdAndOfferedBook_IdAndStatus`, `findVisibleByIdForUser`, `findConflictingPendingRequests`.
- `repository/FeedCardRepository.java`: Feed-card data access. Key query: `findByTypeAndActiveTrueOrderByDisplayOrderAsc`.
- `repository/PageRepository.java`: Abstraction for navigation/page content lookup. Main methods: `findNavigation`, `findPageContent`.
- `repository/RoleRepository.java`: Role data access. Key query: `findByName`.
- `repository/UserNotificationRepository.java`: Notification data access. Key queries: `findByUserIdOrderByCreatedAtDesc`, `findByIdAndUserId`.
- `repository/UserRepository.java`: User data access. Key queries: `findByUsername`, `findByEmail`, `findByUsernameOrEmail`, `findByVerificationToken`, `findByRoles_Name`, `findTop10ByUsernameContainingIgnoreCaseAndIdNot`, `existsByUsername`, `existsByEmail`.
- `repository/WishlistSubscriptionRepository.java`: Wishlist data access. Key queries: `findByUserIdOrderByCreatedAtDesc`, `findByUserIdAndActiveTrue`, `findByIdAndUserId`, `findByActiveTrue`.
- `repository/impl/InMemoryPageRepository.java`: In-memory implementation of `PageRepository`. Main methods: `findNavigation`, `findPageContent`.

## 11. Security Package

- `security/ChatChannelInterceptor.java`: Guards inbound WebSocket/STOMP traffic and checks exchange-chat access. Main methods: `preSend`, `extractExchangeRequestId`, `parseLongOrNull`.
- `security/JwtAuthenticationFilter.java`: Reads JWT from requests, validates it, and populates Spring Security context. Main method: `doFilterInternal`.
- `security/JwtUtil.java`: JWT helper for signing, parsing, generating, and validating tokens. Main methods: `getSigningKey`, `extractUsername`, `extractExpiration`, `extractAllClaims`, `isTokenExpired`, `generateToken`, `createToken`, `validateToken`.

## 12. Services

- `service/AdminDashboardService.java`: Aggregates admin dashboard counts, summaries, and management data for admin pages.
- `service/AuthenticationService.java`: Handles registration, login, email verification, and verification resend. Main methods: `register`, `login`, `verifyEmail`, `resendVerificationEmail`.
- `service/BookService.java`: Core book business logic. It supports search, CRUD, availability toggling, ownership checks, image resolution, and availability event publishing. Main methods: `searchBooks`, `getGenres`, `getLanguages`, `searchAvailableBooks`, `createBook`, `getAllBooks`, `getBookById`, `getAvailableBooks`, `getUserBooks`, `getMyBooks`, `updateBook`, `deleteBook`, `deleteBookAsModerator`, `markBookAvailability`, `findBookById`, `publishBookAvailableEvent`.
- `service/CarouselSlideService.java`: Service interface for landing-page carousel content.
- `service/ChatRoomService.java`: Real chat business service tied to exchange requests. It creates chat rooms, validates participants, stores messages, builds active-chat lists, and supports REST/WebSocket access checks. Main methods: `createChatRoomForExchange`, `getChatRoomForExchange`, `getChatRoomMessages`, `sendMessageToRoom`, `sendMessageToRoomAsUser`, `getActiveChatsForCurrentUser`, `canUserAccessExchangeChat`.
- `service/ChatService.java`: Deprecated placeholder kept for backward compatibility; replaced by `ChatRoomService`.
- `service/CustomUserDetailsService.java`: Loads users into Spring Security `UserDetails`. Main method: `loadUserByUsername`.
- `service/DeliveryService.java`: Handles delivery creation, automatic delivery-man assignment, delivery status updates, and final ownership transfer at delivery completion. Main methods: `createPendingDelivery`, `autoAssignDeliveryMan`, `getAssignedDeliveriesForCurrentDeliveryMan`, `updateDeliveryStatus`, `getDeliveryByExchangeRequestId`, `finalizeExchangeCompletion`.
- `service/EmailService.java`: Sends verification and password emails and builds email bodies/base URLs. Main methods: `resolveBaseUrl`, `resolveLanBaseUrl`, `sendVerificationEmail`, `buildVerificationEmailBody`, `sendPasswordResetEmail`, `buildPasswordResetEmailBody`.
- `service/ExchangeRequestService.java`: Core exchange workflow logic. It validates request rules, records participant acceptance, enforces moderator approval, creates delivery assignments, cancels conflicting requests, and returns rich response DTOs. Main methods: `createExchangeRequest`, `getMyBookExchangeRequests`, `getMyExchangeRequests`, `getPendingRequestsForModeration`, `acceptExchangeRequest`, `approveExchangeRequest`, `rejectExchangeRequest`, `cancelExchangeRequest`, `updateRequestStatus`, `completeExchangeRequest`, `getExchangeRequestById`.
- `service/FeedCardService.java`: Service interface for landing-page feed cards.
- `service/NotificationService.java`: Wishlist-notification service and event subscriber. It listens for `BookAvailableEvent`, finds matching subscriptions, and saves notification records. Main methods: `registerSubscriber`, `unregisterSubscriber`, `getMyNotifications`, `markAsRead`, `onBookAvailable`.
- `service/PageService.java`: Service interface for page/navigation content.
- `service/UserService.java`: Current-user, role, ownership, profile-image, delivery-role request, and moderator-promotion helper service. Main methods: `getUserById`, `getUserByUsername`, `getCurrentUser`, `getCurrentUserId`, `getCurrentUserEntity`, `findByUsername`, `findById`, `isOwnerOrAdmin`, `validateOwnershipOrAdmin`, `isAdmin`, `isModerator`, `isDeliveryMan`, `hasRole`, `getDeliveryMen`, `getApprovedDeliveryMenForAssignment`, `requestDeliveryManRole`, `approveDeliveryManRole`, `updateCurrentUserProfileImage`, `promoteToModerator`.
- `service/WishlistService.java`: Wishlist creation/listing/deactivation/deletion logic. Main methods: `subscribe`, `getMySubscriptions`, `deactivateSubscription`, `removeSubscription`.
- `service/impl/CarouselSlideServiceImpl.java`: Concrete carousel service. Main methods: `getActiveSlides`, `createSlide`.
- `service/impl/FeedCardServiceImpl.java`: Concrete feed-card service. Main methods: `getActiveCardsByType`, `createCard`.
- `service/impl/PageServiceImpl.java`: Concrete page service. Main methods: `getNavigation`, `getPageContent`.

## 13. Validation And Config Files

- `validation/PasswordStrengthValidator.java`: Wraps Passay rules to reject weak passwords. Main methods: `validate`; nested result helpers: `valid`, `invalid`, `isValid`, `getMessage`.
- `src/main/resources/application.yaml`: Default runtime config. It reads PostgreSQL datasource values from environment variables and sets the server port.
- `src/main/resources/application-postgres.yaml`: PostgreSQL-oriented alternate config/profile file.

## 14. Static Frontend Files

- `src/main/resources/static/index.html`: Standalone API tester page rather than the main Thymeleaf UI.

### CSS

- `static/css/admin.css`: Styling for admin screens.
- `static/css/auth.css`: Styling for login/register/verification pages.
- `static/css/book.css`: Styling for the book page.
- `static/css/browse.css`: Styling for browse/search pages.
- `static/css/chat.css`: Styling for the older chat UI.
- `static/css/common.css`: Shared/common styles.
- `static/css/delivery.css`: Styling for delivery screens.
- `static/css/exchange-chat.css`: Styling for exchange-chat UI.
- `static/css/exchange.css`: Styling for exchange-request screens.
- `static/css/footer.css`: Footer styling.
- `static/css/header.css`: Header/navigation styling.
- `static/css/landingpage.css`: Landing-page styling.
- `static/css/profile.css`: Profile-page styling.
- `static/css/wishlist.css`: Wishlist/notification styling.

### JavaScript

- `static/js/book.js`: Book-page logic. Main functions: `withAuthHeaders`, `setStatus`, `loadMyBooks`, `getCover`, `createBook`.
- `static/js/browse.js`: Browse-page logic. Main functions: `browseHeaders`, `getMyBooksForOffer`, `setCardStatus`, `setExchangeModalStatus`, `closeExchangeModal`, `openExchangeModal`, `initBrowsePagination`, `renderPage`, `start`.
- `static/js/chat.js`: Older/general chat logic. Main functions: `getStoredToken`, `authHeaders`, `setStatus`, `setConnectionState`, `escapeHtml`, `formatTime`, `messageBelongsToActiveConversation`, `appendMessageBubble`, `renderConversationList`, `loadCurrentUser`, `loadConversations`, `loadMessages`, `connectWebSocket`, `sendViaHttpFallback`, `initChatPage`.
- `static/js/delivery.js`: Delivery dashboard logic. Main functions: `deliveryHeaders`, `parseDeliveryError`, `deliveryCardTemplate`, `deliveredAction`, `fetchAssignments`, `loadAssignments`, `updateDeliveryStatus`.
- `static/js/exchange-chat.js`: Exchange-chat logic for list loading, WebSocket subscription, and message sending. Main functions: `init`, `setupEventListeners`, `loadExchanges`, `renderExchangeList`, `selectExchange`, `loadMessages`, `renderMessages`, `handleMessageSubmit`, `sendMessage`, `sendViaHttp`, `connectWebSocket`, `subscribeToExchangeMessages`, `withAuthHeaders`, `loadCurrentUser`, `getStoredToken`, `escapeHtml`, `formatTime`.
- `static/js/exchange.js`: Exchange-request page logic. Main functions: `exchangeHeaders`, `statusChip`, `requestCardTemplate`, `parseErrorMessage`, `fetchJson`, `loadMyRequests`, `loadOwnerRequests`, `loadModerationQueue`, `actOnRequest`.
- `static/js/landingpage-carousel.js`: Carousel helper. Main functions: `syncHeaderHeight`, `renderSlide`.
- `static/js/profile.js`: Profile-page logic. Main functions: `getStoredToken`, `clearStoredTokens`, `withAuthHeaders`, `setStatus`, `fallbackAvatar`, `safeName`, `renderRoles`, `updateDeliveryRequestButton`, `updateDeliverySection`, `formatDate`, `loadProfile`, `readImageAsDataUrl`.
- `static/js/wishlist.js`: Wishlist/notification logic. Main functions: `wishlistHeaders`, `setWishlistStatus`, `getJson`, `loadWishlist`, `loadNotifications`.

## 15. Thymeleaf Templates

- `templates/access-denied.html`: Forbidden-access page.
- `templates/admin-books.html`: Admin books page.
- `templates/admin-dashboard.html`: Admin dashboard page.
- `templates/admin-exchanges.html`: Admin exchanges page.
- `templates/admin-pending.html`: Pending-approvals page.
- `templates/admin-section.html`: Shared admin section/wrapper template.
- `templates/admin-ui.html`: Admin UI management page for carousel/feed content.
- `templates/admin-users.html`: Admin user-management page.
- `templates/admin-wishlists.html`: Admin wishlist-management page.
- `templates/admin.html`: Generic admin landing/wrapper template.
- `templates/book.html`: Book page.
- `templates/browse.html`: Browse/search page.
- `templates/chat.html`: Older chat page.
- `templates/delivery.html`: Delivery dashboard page.
- `templates/error.html`: Generic error page.
- `templates/exchange-chat.html`: Exchange-chat page.
- `templates/exchange.html`: Exchange-request page.
- `templates/landingpage.html`: Main landing page.
- `templates/login.html`: Login page.
- `templates/profile.html`: Profile page.
- `templates/register.html`: Registration page.
- `templates/user-home.html`: User home/dashboard page.
- `templates/verify-email.html`: Email verification page.
- `templates/wishlist.html`: Wishlist and notifications page.
- `templates/fragments/admin-header.html`: Reusable admin header/nav fragment.
- `templates/fragments/footer.html`: Reusable footer fragment.
- `templates/fragments/header.html`: Reusable main header/nav fragment.

## 16. Tests

- `src/test/java/com/example/book_exchange_sepm/BookExchangeSepmApplicationTests.java`: Basic application-context smoke test.
- `src/test/java/com/example/book_exchange_sepm/ServiceLayerSmokeIntegrationTest.java`: Integration smoke tests for context and servlet setup.
- `src/test/java/com/example/book_exchange_sepm/controller/SecurityAuthorizationIntegrationTest.java`: Integration tests for authorization behavior.
- `src/test/java/com/example/book_exchange_sepm/service/AuthenticationLifecycleIntegrationTest.java`: Integration tests for registration, verification, and resend-verification lifecycle.
- `src/test/java/com/example/book_exchange_sepm/service/AuthenticationServiceIntegrationTest.java`: Integration tests for auth-service behavior.
- `src/test/java/com/example/book_exchange_sepm/service/AuthenticationServiceTest.java`: Unit tests for auth-service logic.
- `src/test/java/com/example/book_exchange_sepm/service/BookServiceIntegrationTest.java`: Integration tests for book-service flows. Helper methods include `ensureRole`, `saveUser`, `saveBook`, `authenticateAs`.
- `src/test/java/com/example/book_exchange_sepm/service/BookServiceTest.java`: Unit tests for book-service logic.
- `src/test/java/com/example/book_exchange_sepm/service/ExchangeRequestServiceIntegrationTest.java`: Integration tests for exchange workflow. Helper methods include `ensureRole`, `saveUser`, `saveBook`, `authenticateAs`.
- `src/test/java/com/example/book_exchange_sepm/service/ExchangeRequestServiceTest.java`: Unit tests for exchange-request logic.
- `src/test/java/com/example/book_exchange_sepm/service/UserServiceTest.java`: Unit tests for profile-image validation and moderator promotion. Helper method: `setCurrentAuth`.
- `src/test/resources/application-test.yaml`: Test config using H2 in PostgreSQL mode, a test JWT secret, and non-failing mail config.

## 17. Best Files To Read First

If you want to understand the project quickly, read these first:

1. `BookExchangeSepmApplication.java`
2. `config/SecurityConfig.java`
3. `config/WebSocketConfig.java`
4. `service/AuthenticationService.java`
5. `service/BookService.java`
6. `service/ExchangeRequestService.java`
7. `service/DeliveryService.java`
8. `service/ChatRoomService.java`
9. `service/NotificationService.java`
10. `controller/PageController.java`
