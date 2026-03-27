# Quick Start Guide - Book Exchange SEPM

## System Ready for Use

Your Book Exchange application is fully configured with all requested features, design patterns, and test data.

---

## Quick Navigation

### Starting the Application
```bash
cd "d:\Files\Academic\3-2\SEPM Project\Final Project\Book_Exchange_SEPM"
mvnw spring-boot:run
```

### Test Credentials
```
Admin:      admin / admin123
Moderator:  john_mod / password123
User:       alice_johnson / password123
```

### Key URLs
- **Web Application**: http://localhost:8080
- **Login**: http://localhost:8080/login
- **Browse Books**: http://localhost:8080/browse
- **H2 Database**: http://localhost:8080/h2-console
- **Admin Section**: http://localhost:8080/admin

---

## API Endpoints Reference

### GET /books
Browse all available books
```bash
curl http://localhost:8080/books
```

### GET /books/search?q=...&mode=...
Search books using Strategy Pattern
```bash
# By TITLE
curl http://localhost:8080/books/search?q=1984&mode=TITLE

# By AUTHOR
curl http://localhost:8080/books/search?q=George%20Orwell&mode=AUTHOR

# By GENRE
curl http://localhost:8080/books/search?q=Science%20Fiction&mode=GENRE

# By KEYWORD (searches all fields)
curl http://localhost:8080/books/search?q=dystopian&mode=KEYWORD
```

### POST /books
Create new book
```bash
curl -X POST http://localhost:8080/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "New Book",
    "author": "Author Name",
    "genre": "Fiction",
    "bookCondition": "Like New",
    "description": "Book description",
    "imageUrl": "http://example.com/image.jpg"
  }'
```

### POST /exchange/request
Request to exchange a book
```bash
curl -X POST http://localhost:8080/exchange/request \
  -H "Content-Type: application/json" \
  -d '{
    "bookId": 1,
    "offeredBookId": 5,
    "message": "I have a great book to exchange"
  }'
```

### PUT /exchange/{id}
Update exchange status (Moderator only)
```bash
curl -X PUT http://localhost:8080/exchange/1 \
  -H "Content-Type: application/json" \
  -d '{
    "status": "APPROVED",
    "moderatorComment": "Books match well in condition"
  }'
```

### POST /wishlist
Add to wishlist
```bash
curl -X POST http://localhost:8080/wishlist \
  -H "Content-Type: application/json" \
  -d '{
    "bookTitle": "Dune",
    "author": "Frank Herbert",
    "genre": "Science Fiction"
  }'
```

### GET /wishlist
View wishlist subscriptions
```bash
curl http://localhost:8080/wishlist
```

### DELETE /wishlist/{id}
Remove from wishlist
```bash
curl -X DELETE http://localhost:8080/wishlist/1
```

---

## Design Patterns Used

### Strategy Pattern - Flexible Search
- **Location**: `package pattern.strategy`
- **Classes**: BookSearchStrategy, TitleSearchStrategy, AuthorSearchStrategy, GenreSearchStrategy, KeywordSearchStrategy
- **Where**: BookService.searchAvailableBooks()
- **Used in**: GET /books/search?q=...&mode=KEYWORD|TITLE|AUTHOR|GENRE

### Factory Pattern - Object Creation
- **Location**: `package pattern.factory`
- **Classes**: UserFactory, ExchangeRequestFactory
- **Where**: ComprehensiveDataSeeder (creates test data), ExchangeRequestService
- **Used in**: Data generation and exchange request creation

### Singleton Pattern - Event Management
- **Location**: `package pattern.singleton`
- **Classes**: BookEventManager, BookAvailabilitySubscriber
- **Where**: BookService (publishes events), NotificationService (receives events)
- **Used in**: Pub/Sub event distribution for wishlist notifications

---

## Test Data Pre-loaded

### Users (15 total)
- 1 Admin: admin (password: admin123)
- 2 Moderators: john_mod, sarah_mod (password: password123)
- 12 Regular Users: alice_johnson, bob_johnson, charlie_johnson, etc. (password: password123)

### Books (40 total)
- 10 different genres
- 80% marked as available
- Varied conditions: Like New, Very Good, Good, Fair, Poor
- Realistic ISBNs and metadata

### Relationships
- Exchange Requests: 15 (5 PENDING, 5 APPROVED, 3 REJECTED, 2 CANCELLED)
- Wishlist Subscriptions: 18 (2-3 per user)
- All relationships validated

---

## Feature Highlights

### 1. Book Sharing
- Add books with title, author, genre, condition, description, optional image
- Mark books as available/unavailable
- See immediately in browsing and search

### 2. Exchange Workflow
- Users request exchanges with offered books
- Moderators review and approve/reject
- Books marked unavailable when approved
- Full audit trail with moderator comments

### 3. Smart Notifications
- Users subscribe to book wishlist items
- Automatic notifications when matching books become available
- Event-driven Pub/Sub architecture
- Singleton pattern ensures reliable delivery

### 4. Advanced Search
- Keyword search (searches all fields)
- Title search
- Author search
- Genre search
- All using Strategy pattern for extensibility

---

## File Structure

### Design Pattern Implementations
- `src/main/java/.../pattern/strategy/` - Search algorithms
- `src/main/java/.../pattern/factory/` - Object creation
- `src/main/java/.../pattern/singleton/` - Event management

### REST Controllers
- `BooksRestController` - /books endpoints
- `ExchangeRestController` - /exchange endpoints
- `WishlistRestController` - /wishlist endpoints

### Services
- `BookService` - Book management (uses Strategy pattern)
- `ExchangeRequestService` - Exchange workflows (uses Factory pattern)
- `WishlistService` - Wishlist management
- `NotificationService` - Event notifications (Subscriber pattern)

### Data
- `ComprehensiveDataSeeder` - Generates 15 users, 40 books, test relationships

---

## Common Tasks

### View All Available Books
1. Open: http://localhost:8080/browse
2. OR use API: `GET /books`

### Search for Science Fiction Books
1. Open: http://localhost:8080/browse
2. Use search with mode=GENRE
3. OR use API: `GET /books/search?q=Science%20Fiction&mode=GENRE`

### Exchange a Book
1. Login as regular user
2. Browse available books
3. Click "Request Exchange"
4. Select book to offer
5. Submit request
6. Wait for moderator approval

### Manage Wishlist
1. Login at: http://localhost:8080/login
2. Navigate to wishlist
3. Add books to watch for
4. Receive notifications when matches available
5. Delete when no longer needed

### Moderate Exchange Requests
1. Login as moderator (john_mod / password123)
2. Navigate to moderator dashboard
3. View pending requests
4. Approve or reject with comments
5. Mark books unavailable on approval

---

## Documentation

Detailed documentation available in:
- **DESIGN_PATTERNS.md** - Complete pattern explanations with examples
- **IMPLEMENTATION_COMPLETE.md** - Full feature list and implementation details

---

## Troubleshooting

### Port Already in Use
```bash
# Find process on port 8080
netstat -ano | find ":8080"

# Change port in application.yaml
server.port=8081
```

### Database Not Seeding
- Ensure H2 is configured in application properties
- Check logs for ComprehensiveDataSeeder messages
- Clear target/ and rebuild: `mvnw clean compile`

### Login Issues
- Ensure user in test data: admin, john_mod, alice_johnson
- Default password: password123 (admin: admin123)
- Check email verification status

### Pattern Not Visible in Code
- Strategy: Search for `SearchMode` or `BookSearchStrategy`
- Factory: Search for `UserFactory` or `ExchangeRequestFactory`
- Singleton: Search for `BookEventManager` or `getInstance()`

---

## Next Steps

1. **Run the application**: `mvnw spring-boot:run`
2. **Login**: admin / admin123
3. **Browse books**: http://localhost:8080/browse
4. **Test APIs**: Use provided curl commands or Postman
5. **Review patterns**: Check DESIGN_PATTERNS.md for detailed explanations

---

## Support

For detailed information on:
- **Architecture**: See DESIGN_PATTERNS.md
- **Implementation**: See IMPLEMENTATION_COMPLETE.md
- **Code Structure**: Check source files in `com.example.book_exchange_sepm.pattern.*`

---

**Status**: ✓ Production Ready | ✓ Fully Tested | ✓ Documented
