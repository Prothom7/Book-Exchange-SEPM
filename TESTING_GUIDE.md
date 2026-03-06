# Book Exchange SEPM - Complete Testing Guide

## 1. BUILD & RUN THE PROJECT

### Step 1: Compile the Project
```bash
.\mvnw.cmd clean compile
```

### Step 2: Run the Application
```bash
.\mvnw.cmd spring-boot:run
```

Or build and run with jar:
```bash
.\mvnw.cmd clean package
java -jar target/Book_Exchange_SEPM-0.0.1-SNAPSHOT.jar
```

**Expected Output:**
```
Started BookExchangeSepmApplication in X seconds
```

### Step 3: Verify Application is Running
- Application runs on: `http://localhost:8080`
- Database: PostgreSQL at `localhost:5432`

---

## 2. TEST ENDPOINTS (Using PowerShell/cURL)

### Pre-requisites
- Have PostgreSQL running and accessible
- Application running at `http://localhost:8080`
- Use PowerShell or install cURL

### Base URL
```
http://localhost:8080/api
```

---

## 3. AUTHENTICATION TESTS

### Test 1: Register a New User
```powershell
$body = @{
    username = "john_doe"
    email = "john@example.com"
    password = "password123"
} | ConvertTo-Json

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/auth/register" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "roles": ["ROLE_USER"],
  "message": "User registered successfully"
}
```

---

### Test 2: Login
```powershell
$body = @{
    username = "john_doe"
    password = "password123"
} | ConvertTo-Json

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/auth/login" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body `
  -Authentication Basic
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "roles": ["ROLE_USER"],
  "message": "Login successful"
}
```

---

### Test 3: Register with Duplicate Username (Should Fail - 409)
```powershell
$body = @{
    username = "john_doe"
    email = "newemail@example.com"
    password = "password456"
} | ConvertTo-Json

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/auth/register" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body
```

**Expected Response (409 Conflict):**
```json
{
  "statusCode": 409,
  "message": "Username already exists: john_doe",
  "timestamp": "..."
}
```

---

### Test 4: Validation Error - Weak Password (400)
```powershell
$body = @{
    username = "jane_doe"
    email = "jane@example.com"
    password = "pwd"
} | ConvertTo-Json

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/auth/register" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body
```

**Expected Response (400 Bad Request)** - Password too short

---

## 4. AUTHORIZATION TESTS

### Create Base64 Credentials for Testing
```powershell
# For user john_doe with password 'password123'
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("john_doe:password123"))
Write-Host "Basic $credentials"
```

### Test 5: Access User Endpoint (Should Succeed - 200)
```powershell
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("john_doe:password123"))

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/user/profile" `
  -Method GET `
  -Headers @{
    "Authorization" = "Basic $credentials"
    "Content-Type" = "application/json"
  }
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "roles": ["ROLE_USER"],
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

### Test 6: Access Admin Endpoint Without Admin Role (Should Fail - 403)
```powershell
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("john_doe:password123"))

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/admin/dashboard" `
  -Method GET `
  -Headers @{
    "Authorization" = "Basic $credentials"
  }
```

**Expected Response (403 Forbidden):**
```
Access Denied
```

---

### Test 7: Access Without Authentication (Should Fail - 401)
```powershell
Invoke-WebRequest `
  -Uri "http://localhost:8080/api/user/profile" `
  -Method GET
```

**Expected Response (401 Unauthorized)**

---

## 5. BOOK MANAGEMENT TESTS

### Test 8: Create a Book (USER+)
```powershell
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("john_doe:password123"))

$body = @{
    title = "The Great Gatsby"
    author = "F. Scott Fitzgerald"
    description = "Classic American novel"
    isbn = "978-0743273565"
    condition = "GOOD"
} | ConvertTo-Json

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/books" `
  -Method POST `
  -Headers @{
    "Authorization" = "Basic $credentials"
    "Content-Type" = "application/json"
  } `
  -Body $body
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "title": "The Great Gatsby",
  "author": "F. Scott Fitzgerald",
  "ownerUsername": "john_doe",
  "available": true,
  ...
}
```

---

### Test 9: Get All Books
```powershell
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("john_doe:password123"))

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/books" `
  -Method GET `
  -Headers @{
    "Authorization" = "Basic $credentials"
  }
```

**Expected Response (200 OK):** List of all books

---

### Test 10: Update Own Book (Should Succeed)
```powershell
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("john_doe:password123"))

$body = @{
    title = "The Great Gatsby (Revised)"
    author = "F. Scott Fitzgerald"
    isbn = "978-0743273565"
    condition = "EXCELLENT"
} | ConvertTo-Json

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/books/1" `
  -Method PUT `
  -Headers @{
    "Authorization" = "Basic $credentials"
    "Content-Type" = "application/json"
  } `
  -Body $body
```

**Expected Response (200 OK):** Updated book

---

### Test 11: Update Another User's Book (Should Fail - 403)
1. Create second user "jane_doe"
2. Jane tries to update john's book

```powershell
# Register Jane first
$janeRegister = @{
    username = "jane_doe"
    email = "jane@example.com"
    password = "password456"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/api/auth/register" -Method POST ...

# Now Jane tries to update John's book (ID=1)
$janeCredentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("jane_doe:password456"))

$body = @{
    title = "Hacked Title"
    author = "Hacker"
    isbn = "978-0743273565"
    condition = "POOR"
} | ConvertTo-Json

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/books/1" `
  -Method PUT `
  -Headers @{
    "Authorization" = "Basic $janeCredentials"
    "Content-Type" = "application/json"
  } `
  -Body $body
```

**Expected Response (403 Forbidden):**
```json
{
  "statusCode": 403,
  "message": "You do not have permission to modify this resource",
  "timestamp": "..."
}
```

✅ **OWNERSHIP VALIDATION WORKS!**

---

## 6. EXCHANGE REQUEST TESTS

### Test 12: Create Exchange Request
```powershell
$janeCredentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("jane_doe:password456"))

$body = @{
    bookId = 1
    message = "I'm interested in this book"
} | ConvertTo-Json

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/exchange-requests" `
  -Method POST `
  -Headers @{
    "Authorization" = "Basic $janeCredentials"
    "Content-Type" = "application/json"
  } `
  -Body $body
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "requesterId": 2,
  "requesterUsername": "jane_doe",
  "bookId": 1,
  "bookTitle": "The Great Gatsby",
  "bookOwnerId": 1,
  "bookOwnerUsername": "john_doe",
  "status": "PENDING",
  "message": "I'm interested in this book",
  ...
}
```

---

### Test 13: Approve Exchange Request (Book Owner Only)
```powershell
$johnCredentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("john_doe:password123"))

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/exchange-requests/1/approve" `
  -Method PATCH `
  -Headers @{
    "Authorization" = "Basic $johnCredentials"
  }
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "status": "APPROVED",
  ...
}
```

---

### Test 14: Jane (Non-Owner) Cannot Approve (403)
```powershell
$janeCredentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("jane_doe:password456"))

# Try to approve request for a book she doesn't own
Invoke-WebRequest `
  -Uri "http://localhost:8080/api/exchange-requests/1/approve" `
  -Method PATCH `
  -Headers @{
    "Authorization" = "Basic $janeCredentials"
  }
```

**Expected Response (403 Forbidden)**

✅ **OWNERSHIP VALIDATION WORKS!**

---

## 7. ROLE-BASED ACCESS TESTS

### Create Admin User (Update Database Directly)
```sql
-- Connect to your PostgreSQL database
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');
INSERT INTO users (username, email, password) VALUES ('admin_user', 'admin@example.com', '$2a$10...');
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id 
FROM users u, roles r WHERE u.username='admin_user' AND r.name='ROLE_ADMIN';
```

### Test 15: Admin Can Access Admin Endpoints
```powershell
$adminCredentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin_user:password123"))

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/admin/dashboard" `
  -Method GET `
  -Headers @{
    "Authorization" = "Basic $adminCredentials"
  }
```

**Expected Response (200 OK):**
```
Welcome to Admin Dashboard
```

---

### Test 16: Admin Can Delete Any Book (Override Ownership)
```powershell
$adminCredentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin_user:password123"))

Invoke-WebRequest `
  -Uri "http://localhost:8080/api/books/1" `
  -Method DELETE `
  -Headers @{
    "Authorization" = "Basic $adminCredentials"
  }
```

**Expected Response (204 No Content)** - Book deleted even though Admin didn't own it

✅ **ADMIN CAN OVERRIDE OWNERSHIP!**

---

## 8. QUICK TEST CHECKLIST

Use this checklist to verify everything works:

- [ ] **Registration**
  - [ ] Successfully register new user
  - [ ] Get 409 on duplicate username
  - [ ] Get 400 on validation errors

- [ ] **Login**
  - [ ] Successfully login with correct credentials
  - [ ] Get 404 on invalid credentials

- [ ] **Authorization**
  - [ ] Get 401 without authentication
  - [ ] Get 403 without proper role
  - [ ] Can access endpoints with proper role

- [ ] **Ownership**
  - [ ] User can edit/delete own books
  - [ ] User CANNOT edit/delete others' books
  - [ ] Admin can edit/delete any book
  - [ ] Only book owner can approve/reject exchange requests

- [ ] **Books**
  - [ ] Create book successfully
  - [ ] List all books
  - [ ] Get specific book
  - [ ] Update own book
  - [ ] Delete own book
  - [ ] Cannot update others' books

- [ ] **Exchange Requests**
  - [ ] Create exchange request
  - [ ] List requests
  - [ ] Approve own request (owner only)
  - [ ] Reject own request (owner only)
  - [ ] Cancel own request (requester only)
  - [ ] Cannot approve/reject others' requests

---

## 9. DATABASE SCHEMA VERIFICATION

Connect to PostgreSQL and verify tables:
```sql
\dt
```

Should show:
- users
- roles
- user_roles
- books
- exchange_requests

---

## 10. LOGS & DEBUGGING

Check application logs for:
```
- Role creation: "Creating ROLE_ADMIN", "Creating ROLE_MODERATOR", "Creating ROLE_USER"
- User authentication: username/password attempts
- Authorization failures: "Access denied" messages
```

---

## SUMMARY

The authentication and authorization system is working properly if:

1. ✅ Users can register with secure password encryption (BCrypt)
2. ✅ Users can login and get role information
3. ✅ Role-based access control (ROLE_USER < ROLE_MODERATOR < ROLE_ADMIN)
4. ✅ Ownership-based security (users can only edit their own resources)
5. ✅ Admin can override ownership restrictions
6. ✅ Proper HTTP status codes returned (201, 200, 400, 401, 403, 404, 409, etc.)
7. ✅ Validation errors handled properly

All tests should pass and demonstrate the system is working correctly!

---

## AUTOMATED TESTING (Updated)

### ? Quick Start - Run Full Test Suite
```powershell
.\full-test-suite.ps1
```

This comprehensive script tests:
- ? Frontend UI (Login/Register pages, CSS, Access Denied)
- ? API Authentication (Registration, Login)
- ? Authorization (401 Unauthorized, 403 Forbidden)
- ? Form-Based Login (Role-based redirects)
- ? Book Management (Create, List, My Books)

Expected: **All 14 tests pass**

### ?? Manual Browser Testing

1. **Open**: http://localhost:8080/login
2. **Register** a new account at /register
3. **Login** ? auto-redirects to /user/home
4. **Verify pastel design**:
   - Floating book icon animation
   - Glass-morphism card effects
   - Soft color palette (#ECE7E1, #DBD4CE, #BBAEA5)
5. **Try accessing** http://localhost:8080/admin/home
   - Should see **Access Denied** page
6. **Logout** redirects to /login

### ?? Testing Different Roles

Run the role promotion script:
```powershell
.\promote-users.ps1
```

Options:
- 1 = Add MODERATOR role
- 2 = Add ADMIN role  
- 3 = Add both MODERATOR + ADMIN

**After promoting:**
1. Logout from web UI
2. Login again
3. Try accessing:
   - http://localhost:8080/moderator/home (MODERATOR+)
   - http://localhost:8080/admin/home (ADMIN only)

### ?? Role Access Matrix

| Endpoint | USER | MODERATOR | ADMIN |
|----------|------|-----------|-------|
| /user/home | ? | ? | ? |
| /moderator/home | ? 403 | ? | ? |
| /admin/home | ? 403 | ? 403 | ? |
| /api/books/* | ? | ? | ? |
| /api/moderator/* | ? 403 | ? | ? |
| /api/admin/* | ? 403 | ? 403 | ? |

### ?? Design Checklist

Your Thymeleaf pages should have:
- ? Pastel color scheme (cream #ECE7E1, taupe #DBD4CE)
- ? Playfair Display font for headings
- ? Animated SVG book icon
- ? Smooth fade-in transitions
- ? Glass-morphism card effects
- ? Responsive mobile layout

### ?? Troubleshooting

**Port 8080 in use:**
```powershell
Get-NetTCPConnection -LocalPort 8080 | Select-Object -ExpandProperty OwningProcess | ForEach-Object { Stop-Process -Id $_ -Force }
.\mvnw.cmd spring-boot:run
```

**Database not running:**
```powershell
docker-compose up -d postgres
```

**Reset everything:**
```powershell
docker-compose down -v
docker-compose up -d postgres
.\mvnw.cmd spring-boot:run
```

---

## TEST OUTPUT EXAMPLE

```
========================================
   BOOK EXCHANGE - FULL TEST SUITE
========================================

[SECTION 1] Frontend UI Tests
-------------------------------------
[TEST 1.1] Login page accessibility...
SUCCESS: Login page loads correctly

[TEST 1.2] Register page accessibility...
SUCCESS: Register page loads correctly

[TEST 1.3] CSS styling loaded...
SUCCESS: Pastel styling CSS loaded

[TEST 1.4] Access Denied page...
SUCCESS: Access Denied page renders

[SECTION 2] API Authentication Tests
-------------------------------------
[TEST 2.1] API User Registration...
SUCCESS: User registered via API (ID: 5, Roles: ROLE_USER)

[TEST 2.2] API User Login...
SUCCESS: User logged in via API (testuser_9312)

[SECTION 3] Authorization Tests
-------------------------------------
[TEST 3.1] Unauthenticated access to protected endpoint...
SUCCESS: Correctly blocked with 401 Unauthorized

[TEST 3.2] Authenticated access to user endpoint...
SUCCESS: User profile retrieved (testuser_9312)

[TEST 3.3] USER role attempting ADMIN endpoint...
SUCCESS: Correctly blocked with 403 Forbidden

[SECTION 4] Form-Based Login Tests
-------------------------------------
[TEST 4.1] Web form login with valid credentials...
SUCCESS: Form login redirects to /user/home (role-based redirect works)

[TEST 4.2] Invalid credentials...
SUCCESS: Invalid login redirects to error page

[SECTION 5] Book Management Tests
-------------------------------------
[TEST 5.1] Create a book...
SUCCESS: Book created (ID: 2, Title: Test Book 9312)

[TEST 5.2] Get all books...
SUCCESS: Retrieved 2 books

[TEST 5.3] Get user's books...
SUCCESS: Retrieved 1 user books

========================================
   TEST SUITE COMPLETED
========================================
```

---

**? ALL SYSTEMS OPERATIONAL**  
**Start at:** http://localhost:8080/login
