# Book Exchange SEPM - Design Patterns & Architecture Documentation

## Project Overview

This document explains the software design patterns implemented in the Book Exchange SEPM application and their architectural significance.

---

## Design Patterns Implemented

### 1. **Strategy Pattern**
**Purpose**: Enable flexible, interchangeable search/filter algorithms without modifying client code.

#### Implementation Location
- **Package**: `com.example.book_exchange_sepm.pattern.strategy`
- **Key Classes**:
  - `BookSearchStrategy.java` (Interface)
  - `TitleSearchStrategy.java` (Concrete implementation)
  - `AuthorSearchStrategy.java` (Concrete implementation)
  - `GenreSearchStrategy.java` (Concrete implementation)
  - `KeywordSearchStrategy.java` (Concrete implementation)
  - `BookSearchStrategyResolver.java` (Strategy selector)
  - `SearchMode.java` (Enum for strategy selection)

#### Why Used
- **Problem**: Originally, book search was monolithic and didn't support flexible filtering by different criteria
- **Solution**: Abstracted search algorithms into separate strategy classes that implement the `BookSearchStrategy` interface
- **Benefit**: 
  - New search strategies can be added without modifying BookService
  - Each strategy is independently testable
  - Client code remains unchanged when new strategies are added

#### Code Example
```java
// In BookService.searchAvailableBooks()
public List<BookResponse> searchAvailableBooks(String query, SearchMode mode) {
    BookSearchStrategy strategy = bookSearchStrategyResolver.resolve(mode);
    return availableBooks.stream()
        .filter(book -> strategy.matches(book, query))
        .map(this::convertToResponse)
        .collect(Collectors.toList());
}

// Usage - clients can switch strategies without changing their code
// GET /books/search?q=Dune&mode=TITLE
// GET /books/search?q=Frank&mode=AUTHOR
// GET /books/search?q=Science%20Fiction&mode=GENRE
// GET /books/search?q=dystopian&mode=KEYWORD
```

#### Search Modes
- **KEYWORD**: Searches in title, author, genre, description, and ISBN
- **TITLE**: Searches only in book title
- **AUTHOR**: Searches only in author name
- **GENRE**: Searches only in genre field

---

### 2. **Factory Pattern**
**Purpose**: Encapsulate object creation logic to promote consistency and reduce complexity.

#### Implementation Location
- **Package**: `com.example.book_exchange_sepm.pattern.factory`
- **Key Classes**:
  - `UserFactory.java` - Creates User objects with different roles
  - `ExchangeRequestFactory.java` - Creates ExchangeRequest objects with predefined states
  - `UserType.java` (Enum: USER, MODERATOR)

#### Why Used
- **Problem**: User creation and exchange request initialization involved repetitive validation and setup
- **Solution**: Centralized object creation logic in factory classes with type-safe methods
- **Benefit**:
  - Ensures consistent object initialization across the application
  - Simplifies client code by hiding complexity
  - Makes it easier to change object creation logic in one place
  - Testable factories for unit tests

#### Code Example
```java
// UserFactory - Creating users with specific roles
User regularUser = UserFactory.create(
    UserType.USER, 
    "alice_johnson", 
    "alice@example.com", 
    "password123", 
    passwordEncoder, 
    Collections.singleton(userRole)
);

User moderator = UserFactory.create(
    UserType.MODERATOR,
    "john_mod",
    "john.mod@example.com",
    "password123",
    passwordEncoder,
    Collections.singleton(moderatorRole)
);

// ExchangeRequestFactory - Creating exchange requests with proper initialization
ExchangeRequest pendingRequest = ExchangeRequestFactory.createPending(
    requester, 
    requestedBook, 
    offeredBook, 
    "I'd like to exchange this book"
);

ExchangeRequest reviewedRequest = ExchangeRequestFactory.createReviewed(
    requester,
    requestedBook, 
    offeredBook,
    ExchangeRequest.Status.APPROVED,
    moderator,
    "Approved - books match in condition"
);
```

#### Usage in ComprehensiveDataSeeder
The data seeder demonstrates factory pattern usage through:
- `createUser()` - Factory method for user creation
- `createExchangeRequest()` - Factory method for exchange request creation

---

### 3. **Singleton Pattern**
**Purpose**: Ensure a class has only one instance and provide global access to it in a thread-safe manner.

#### Implementation Location
- **Package**: `com.example.book_exchange_sepm.pattern.singleton`
- **Key Classes**:
  - `BookEventManager.java` - Singleton for centralized pub/sub event management
  - `BookAvailabilitySubscriber.java` (Interface for subscribers)

#### Why Used
- **Problem**: Multiple event publishers competing or creating duplicate notification delivery
- **Solution**: Centralized event manager enforced as singleton via private constructor and static instance
- **Benefit**:
  - Single point of truth for event management
  - Thread-safe event distribution using synchronized collection
  - Proper lifecycle management (subscribe/unsubscribe)
  - No dependency on Spring's event bus, giving explicit control

#### Code Example
```java
// Singleton pattern - Thread-safe instance
@Getter
private static final BookEventManager instance = new BookEventManager();

private BookEventManager() {
    // Private constructor prevents instantiation
    this.subscribers = Collections.synchronizedList(new ArrayList<>());
}

// Global access point
public static BookEventManager getInstance() {
    return instance;
}

// Event publishing - all subscribers receive notification
public void publish(BookAvailableEvent event) {
    subscribers.forEach(subscriber -> subscriber.onBookAvailable(event));
}

// Subscriber lifecycle management
public void subscribe(BookAvailabilitySubscriber subscriber) {
    if (!subscribers.contains(subscriber)) {
        subscribers.add(subscriber);
    }
}

public void unsubscribe(BookAvailabilitySubscriber subscriber) {
    subscribers.remove(subscriber);
}
```

#### Integration Points
- **BookService**: Publishes `BookAvailableEvent` when books are marked available
- **NotificationService**: Implements `BookAvailabilitySubscriber`, registers via `@PostConstruct`, unregisters via `@PreDestroy`

#### Event Flow
```
Book marked available
    ↓
BookService.markBookAvailability() called
    ↓
BookEventManager.getInstance().publish(BookAvailableEvent)
    ↓
Singleton broadcasts to all registered subscribers
    ↓
NotificationService.onBookAvailable() receives event
    ↓
Check matching WishlistSubscriptions for same book criteria
    ↓
Create UserNotification for each matching subscription
    ↓
User receives notification
```

---

## REST API Endpoints

### Books API (`/books`)
```
POST   /books                    - Create a new book
GET    /books                    - Get all available books
GET    /books/search?q=...&mode= - Search books (supports KEYWORD|TITLE|AUTHOR|GENRE)
GET    /books/{id}               - Get book details
```

#### Search Example
```bash
# Keyword search - searches multiple fields
GET /books/search?q=dystopian&mode=KEYWORD

# Title search - exact title matching
GET /books/search?q=1984&mode=TITLE

# Author search
GET /books/search?q=George%20Orwell&mode=AUTHOR

# Genre search
GET /books/search?q=Science%20Fiction&mode=GENRE
```

### Exchange API (`/exchange`)
```
POST   /exchange/request         - Request to exchange a book
GET    /exchange                 - View user's exchange requests
PUT    /exchange/{id}            - Update request status (moderator only)
```

**Status Transitions** (PUT /exchange/{id}):
- `APPROVED` - Mark as approved (moderator only), marks both books unavailable
- `REJECTED` - Mark as rejected (moderator only)

### Wishlist API (`/wishlist`)
```
POST   /wishlist                 - Add book to wishlist (subscribe)
GET    /wishlist                 - View wishlist subscriptions
DELETE /wishlist/{id}            - Remove from wishlist
```

---

## Database Schema

### Key Entities

#### User
- `id` (PK)
- `username` (UNIQUE, NOT NULL)
- `email` (UNIQUE, NOT NULL)
- `password` (NOT NULL)
- `emailVerified` (BOOLEAN)
- `roles` (M:M relationship with Role)

#### Book
- `id` (PK)
- `title` (NOT NULL)
- `author` (NOT NULL)
- `genre` (Strategy pattern application)
- `language` (DEFAULT: 'English')
- `publication_year`
- `isbn` (UNIQUE)
- `book_condition` (Enum: Like New, Very Good, Good, Fair, Poor)
- `description`
- `imageUrl` (Optional - for display)
- `owner_id` (FK to User)
- `available` (BOOLEAN - Strategy pattern filter)
- `createdAt` (TIMESTAMP)
- `updatedAt` (TIMESTAMP)

#### ExchangeRequest
- `id` (PK)
- `requester_id` (FK to User)
- `book_id` (FK to Book - requested)
- `offered_book_id` (FK to Book - what user offers)
- `message` (TEXT)
- `status` (Enum: PENDING, APPROVED, REJECTED, CANCELLED)
- `moderator_comment` (TEXT)
- `reviewed_by_id` (FK to User - moderator who reviewed)
- `reviewed_at` (TIMESTAMP)
- `createdAt` (TIMESTAMP)
- `updatedAt` (TIMESTAMP)

#### WishlistSubscription
- `id` (PK)
- `user_id` (FK to User)
- `book_title` (NOT NULL)
- `author` (OPTIONAL)
- `genre` (OPTIONAL)
- `active` (BOOLEAN)
- `createdAt` (TIMESTAMP)
- `updatedAt` (TIMESTAMP)

#### UserNotification
- `id` (PK)
- `user_id` (FK to User)
- `event_type` (String - e.g., "BOOK_AVAILABLE")
- `message` (TEXT)
- `is_read` (BOOLEAN)
- `createdAt` (TIMESTAMP)
- `updatedAt` (TIMESTAMP)

---

## Pub/Sub Architecture

### Event-Driven Flow

1. **User subscribes to book** via `POST /wishlist`
   - Creates `WishlistSubscription` with book title, author, genre

2. **User shares a book** via `POST /books`
   - Creates `Book` entity
   - Calls `bookService.markBookAvailability()`

3. **Event Publishing** (Strategy Pattern for search)
   - `BookEventManager.getInstance().publish(BookAvailableEvent)`
   - Event contains book details

4. **Event Notification** (Singleton + Subscriber Pattern)
   - `NotificationService` (subscriber) receives event via `onBookAvailable()`
   - Queries all active `WishlistSubscription` using Strategy pattern search
   - Creates `UserNotification` for matching subscriptions

5. **User receives notification**
   - Via `GET /api/notifications` endpoint
   - Via UI notification panel

### Why Singleton for Event Manager?
- **Centralized State**: Single point of truth for all event subscribers
- **Thread Safety**: Synchronized access to subscriber list
- **Explicit Control**: Not dependent on Spring's event bus
- **Easy Testing**: Can mock the singleton for unit tests
- **Explicit Lifecycle**: Clear subscribe/unsubscribe semantics

---

## Test Data Seeding

### ComprehensiveDataSeeder.java
Demonstrates all three design patterns in action:

#### Factory Pattern Usage (User Creation)
```java
User admin = createUser("admin", "admin@example.com", "admin123", 
    true, Collections.singleton(roles.get("ROLE_ADMIN")), userRepository);

User moderator = createUser("john_mod", "john.mod@example.com", "password123",
    true, Collections.singleton(roles.get("ROLE_MODERATOR")), userRepository);
```

#### Strategy Pattern Usage (Genre-based Variety)
```java
// Books created with multiple genres (10 different genre strategies)
String[] genres = {"Fiction", "Science Fiction", "Mystery", "Romance", 
                   "History", "Biography", "Science", "Technology", "Art", "Philosophy"};

// Each genre uses a different search strategy
book.setGenre(genres[i % genres.length]);  // Distributes across genres
```

#### Singleton Pattern Usage (Event Manager)
```java
// When books are created as available, they would publish events
BookEventManager.getInstance().publish(new BookAvailableEvent(book));
```

### Generated Test Data
- **Users**: 15 total
  - 1 Admin (admin / admin123)
  - 2 Moderators (john_mod / password123, sarah_mod / password123)
  - 12 Regular users (alice_johnson, bob_johnson, etc. / password123)

- **Books**: 40 total
  - 10 Different genres
  - Varied conditions (Like New, Very Good, Good, Fair, Poor)
  - 80% marked as available
  - Different owners (distributed among users)
  - Realistic ISBNs and metadata

- **Exchange Requests**: 15 total
  - 5 PENDING requests
  - 5 APPROVED requests
  - 3 REJECTED requests
  - 2 CANCELLED requests

- **Wishlist Subscriptions**: 18 total
  - 2-3 subscriptions per regular user
  - Varied book titles and genres

---

## Security Configuration

### Role-Based Access Control
```
/books              → Authenticated users (USER, MODERATOR, ADMIN)
/exchange           → Authenticated users (USER, MODERATOR, ADMIN)
/exchange/{id}      → Moderators only (MODERATOR, ADMIN) for PUT
/wishlist           → Authenticated users (USER, MODERATOR, ADMIN)
```

### Authentication Methods
- Form-based login (`/login`)
- JWT token-based authentication
- Session-based with CSRF protection disabled for API endpoints

---

## How Design Patterns Work Together

### Request to Exchange Books (Example Flow)

```
User searches for "Dune" (STRATEGY PATTERN)
    ↓
BooksRestController.searchBooks("Dune", "TITLE")
    ↓
BookSearchStrategyResolver resolves TITLE strategy
    ↓
TitleSearchStrategy.matches(book, "Dune") checks each book
    ↓
Returns list of matching books
    ↓
User requests to exchange with book A, offering book B
    ↓
POST /exchange/request
    ↓
ExchangeRequestService.createExchangeRequest() (FACTORY PATTERN)
    ↓
ExchangeRequestFactory creates exchange request with proper state
    ↓
Request status set to PENDING
    ↓
Moderator reviews and approves
    ↓
PUT /exchange/{id} with status=APPROVED
    ↓
ExchangeRequestService.updateRequestStatus() (FACTORY PATTERN)
    ↓
Books marked unavailable
    ↓
BookEventManager.getInstance().publish(BookUnavailableEvent) (SINGLETON PATTERN)
    ↓
NotificationService receives event (SUBSCRIBER)
    ↓
Wallbox: System notifies other users with wishlist subscriptions
```

---

## Development Notes

### Adding New Search Strategy
```java
// 1. Create new strategy class
@Component
public class YearPublishedStrategy implements BookSearchStrategy {
    @Override
    public boolean matches(Book book, String query) {
        return book.getPublicationYear() == Integer.parseInt(query);
    }
}

// 2. Add to SearchMode enum
public enum SearchMode {
    KEYWORD, TITLE, AUTHOR, GENRE, YEAR_PUBLISHED
}

// 3. Register in BookSearchStrategyResolver
private Map<SearchMode, BookSearchStrategy> initializeStrategies() {
    strategies.put(SearchMode.YEAR_PUBLISHED, yearPublishedStrategy);
}

// 4. Use new strategy
GET /books/search?q=2024&mode=YEAR_PUBLISHED
```

### Adding New Subscriber
```java
// 1. Implement BookAvailabilitySubscriber
@Component
public class EmailNotificationService implements BookAvailabilitySubscriber {
    @Override
    public void onBookAvailable(BookAvailableEvent event) {
        // Send email notification
    }
    
    @PostConstruct
    public void subscribe() {
        BookEventManager.getInstance().subscribe(this);
    }
}

// 2. Auto-registers with singleton on application startup
```

---

## Compilation & Testing

### Build the Project
```bash
./mvnw clean compile
./mvnw clean package
```

### Run the Application
```bash
./mvnw spring-boot:run
```

### Access Points
- **Web UI**: http://localhost:8080
- **Login Page**: http://localhost:8080/login
- **API Endpoints**: http://localhost:8080/books, /exchange, /wishlist
- **H2 Console**: http://localhost:8080/h2-console (admin credentials: admin@example.com / admin123)

---

## Conclusion

This application demonstrates how design patterns can be effectively used to create:
- **Flexible Systems**: Strategy pattern allows multiple implementations without code changes
- **Maintainable Code**: Factory pattern centralizes creation logic
- **Reliable Event Distribution**: Singleton pattern ensures consistent, thread-safe event management

The patterns work together to create a robust, extensible architecture that follows SOLID principles and best practices in software design.
