# Probable Project Questions And Answers

This file collects likely viva, interview, presentation, and reviewer questions about the Book Exchange SEPM project, with short answers based on this repository.

## 1. Core Understanding

1. **What problem does this project solve?**  
   It provides a controlled platform where users can list books, request exchanges, chat with exchange partners, receive wishlist notifications, and complete exchanges through moderator review and delivery tracking.

2. **What is the tech stack?**  
   Java 17, Spring Boot 4, Spring MVC, Spring Security, Spring Data JPA, PostgreSQL, Thymeleaf, WebSocket/STOMP, Maven, Docker, and GitHub Actions.

3. **Is this project only backend?**  
   No. It is a full-stack monolith with a Spring Boot backend and a Thymeleaf/CSS/JavaScript frontend.

4. **What are the main roles?**  
   `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`, and `ROLE_DELIVERY_MAN`.

5. **What are the major modules?**  
   Authentication, books, exchange requests, delivery, chat, wishlist, notifications, admin dashboard, and server-rendered pages.

6. **What is the high-level workflow?**  
   A user lists a book, another user requests an exchange by offering their own book, both participants accept, a moderator approves, delivery is assigned, and final ownership transfer happens on delivery completion.

## 2. Architecture

7. **What architecture does the project follow?**  
   A layered architecture: controller -> service -> repository -> database, with DTOs separating API responses from JPA entities.

8. **Why use DTOs here?**  
   DTOs keep the API clean, avoid exposing entity internals, and allow custom response shapes such as exchange details plus delivery info.

9. **Where is most business logic located?**  
   In the service layer, especially `AuthenticationService`, `BookService`, `ExchangeRequestService`, `DeliveryService`, `ChatRoomService`, `UserService`, `WishlistService`, and `NotificationService`.

10. **Why keep controllers thin?**  
    Thin controllers are easier to maintain and test. They focus on request mapping and response creation while services handle the rules.

## 3. Authentication And Security

11. **How is authentication implemented?**  
    The app supports form login, HTTP Basic, and JWT-based API authentication.

12. **Where is security configured?**  
    In `src/main/java/com/example/book_exchange_sepm/config/SecurityConfig.java`.

13. **How are roles enforced?**  
    With URL-level rules inside `SecurityConfig` and method-level checks using `@PreAuthorize`.

14. **How are passwords protected?**  
    Passwords are hashed with BCrypt, and strength is validated using Passay in `PasswordStrengthValidator`.

15. **Do users need email verification?**  
    Yes. `AuthenticationService.login` blocks unverified users.

16. **How are JWT tokens handled?**  
    `JwtUtil` signs, parses, and validates them, while `JwtAuthenticationFilter` extracts them from requests and populates the Spring Security context.

17. **How are errors standardized for APIs?**  
    `GlobalExceptionHandler` converts common exceptions into `ErrorResponse`.

## 4. Book Module

18. **What does the book module do?**  
    It supports creating, browsing, updating, deleting, searching, and changing availability of books.

19. **How is ownership enforced on books?**  
    `UserService.validateOwnershipOrAdmin` allows only the owner or admin to modify a book, with a separate moderator delete path where intended.

20. **How does search work?**  
    The browse page uses `BookSearchForm` filtering, while the API-style search uses the Strategy pattern through `BookSearchStrategyResolver`.

21. **What happens when a book becomes available again?**  
    `BookService` publishes a `BookAvailableEvent`, and `NotificationService` checks wishlist matches and creates notifications.

22. **How are book images resolved?**  
    The service first uses an explicit image URL, then tries Open Library by ISBN, and finally falls back to a placeholder.

## 5. Exchange Workflow

23. **Where is the main exchange logic?**  
    In `src/main/java/com/example/book_exchange_sepm/service/ExchangeRequestService.java`.

24. **What validations happen before creating an exchange request?**  
    The requester cannot request their own book, both books must be available, the offered book must belong to the requester, and duplicate pending requests are blocked.

25. **Why do both participants accept before moderation?**  
    It ensures mutual consent and records timestamps for both sides before moderator action.

26. **Who can approve or reject exchanges?**  
    Moderators perform the final approve/reject operations in the current design.

27. **Why is manual completion disabled?**  
    Because ownership transfer is tied to delivery completion for better consistency.

28. **What happens on approval?**  
    The books become unavailable, the exchange status becomes `APPROVED`, conflicting pending requests are cancelled, a chat room is ensured, and a delivery assignment is created.

29. **What happens when delivery is completed?**  
    `DeliveryService.finalizeExchangeCompletion` swaps owners, makes both books available again, and marks the exchange `COMPLETED`.

## 6. Delivery, Chat, And Notifications

30. **How is a delivery man selected?**  
    `DeliveryService.autoAssignDeliveryMan` chooses the approved delivery man with the fewest active deliveries.

31. **Can any user become a delivery man?**  
    No. A user requests the role first, then an admin flow approves it.

32. **Why use both REST and WebSocket for chat?**  
    WebSocket gives real-time updates, and REST provides history retrieval plus an HTTP fallback path.

33. **Where is WebSocket configured?**  
    In `src/main/java/com/example/book_exchange_sepm/config/WebSocketConfig.java`.

34. **How is chat access controlled?**  
    `ChatRoomService` checks exchange participation, and `ChatChannelInterceptor` guards inbound STOMP traffic.

35. **Why is each chat room tied to an exchange?**  
    So the conversation stays linked to a specific transaction, books, and participants.

36. **How are wishlist notifications generated?**  
    `NotificationService` listens for `BookAvailableEvent`, matches active wishlist subscriptions, and saves notification records.

## 7. Design Patterns

37. **Which design patterns are explicitly implemented?**  
    Strategy, Factory, and Singleton.

38. **Where is the Strategy pattern used?**  
    In book search, where different search modes such as keyword, title, author, and genre are selected through `BookSearchStrategyResolver`.

39. **Why is the Strategy pattern useful here?**  
    It keeps search logic modular and makes adding new search types easier without rewriting one giant method.

40. **Where is the Factory pattern used?**  
    In `UserFactory` and `ExchangeRequestFactory`.

41. **Why is the Factory pattern useful?**  
    It centralizes object-creation rules and avoids duplicated setup logic.

42. **Where is the Singleton pattern used?**  
    In `BookEventManager`, which acts as the event hub for book-availability notifications.

43. **Why use a Singleton there?**  
    Because the app wants one central publisher/subscriber registry for that event flow.

## 8. Database And Persistence

44. **Which database is used in real runs?**  
    PostgreSQL.

45. **Which database is used in tests?**  
    H2 in PostgreSQL compatibility mode, configured in `src/test/resources/application-test.yaml`.

46. **How is data access implemented?**  
    Through Spring Data JPA repositories backed by JPA entities.

47. **What are the most important entities?**  
    `User`, `Role`, `Book`, `ExchangeRequest`, `Delivery`, `ChatRoom`, `ChatMessage`, `WishlistSubscription`, and `UserNotification`.

48. **Why do many entities have `onCreate` and `onUpdate` methods?**  
    Those lifecycle callbacks automatically manage timestamps.

49. **Why are there schema initializer classes?**  
    They help maintain compatibility and repair data or constraints as the schema evolves.

## 9. Frontend And MVC

50. **Does the project expose only REST endpoints?**  
    No. It also renders Thymeleaf pages such as landing page, browse, book, profile, exchange, delivery, wishlist, and admin pages.

51. **What is the role of `PageController`?**  
    It routes browser requests to the correct templates and injects page/navigation data into the model.

52. **Why are there separate CSS and JS files per page?**  
    It keeps frontend concerns isolated and easier to maintain.

53. **What is `static/index.html` for?**  
    It is a standalone API tester page, not the main application UI.

## 10. Admin, Testing, And Deployment

54. **What can an admin do in this project?**  
    View dashboard metrics, manage users/books/exchanges/wishlists, manage landing-page UI content, promote moderators, and approve delivery-role requests.

55. **What types of tests exist?**  
    Unit tests, integration tests, security authorization tests, and smoke tests.

56. **How are unit and integration tests separated?**  
    Surefire skips integration tests, while Failsafe runs `*IntegrationTest.java` and application tests.

57. **Why use Docker and Render config files?**  
    They make deployment and environment setup more repeatable.

58. **What is GitHub Actions used for here?**  
    CI automation for build and test verification.

## 11. Improvement-Oriented Questions

59. **What is one security improvement you could mention?**  
    Tighter CSRF strategy, stricter WebSocket origin rules, and stronger secret management.

60. **What is one scalability improvement you could mention?**  
    Replacing the simple broker/singleton event flow with external messaging if the app needs horizontal scaling.

61. **What is one maintainability improvement you could mention?**  
    Consolidating duplicate/legacy controller surfaces like `/api/books` and `/api/books-rest`.

62. **What is one data-consistency improvement you could mention?**  
    Stronger locking or additional database constraints around exchange approval and ownership transfer.

## 12. Short Presentation Answers

63. **Why is this more than a CRUD project?**  
    Because it includes multi-step workflow control, moderation, delivery assignment, real-time chat, notifications, and role-based operations.

64. **What is the most interesting feature to highlight?**  
    The full exchange lifecycle: request -> participant acceptance -> moderator approval -> delivery -> ownership transfer, with chat and notifications around it.

65. **What is the best short project summary?**  
    "Book Exchange SEPM is a full-stack Spring Boot application for controlled peer-to-peer book exchange. It supports secure registration and email verification, role-based access, book listing and search, exchange request workflow with participant acceptance and moderator approval, delivery assignment with final ownership transfer, real-time exchange chat using WebSocket, wishlist-based notifications, and an admin dashboard."
