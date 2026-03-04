# ========================================
# BOOK EXCHANGE - COMPLETE TEST SUITE
# ========================================

$baseUrl = "http://localhost:8080"
$apiUrl = "$baseUrl/api"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "   BOOK EXCHANGE - FULL TEST SUITE" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# ========================================
# 1. FRONTEND UI TESTS
# ========================================
Write-Host "[SECTION 1] Frontend UI Tests" -ForegroundColor Yellow
Write-Host "-------------------------------------`n" -ForegroundColor Yellow

Write-Host "[TEST 1.1] Login page accessibility..." -ForegroundColor White
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/login" -UseBasicParsing
    if ($response.StatusCode -eq 200 -and $response.Content -match "Book Exchange") {
        Write-Host "SUCCESS: Login page loads correctly" -ForegroundColor Green
    } else {
        Write-Host "FAILED: Login page content issue" -ForegroundColor Red
    }
} catch {
    Write-Host "FAILED: Cannot reach login page - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n[TEST 1.2] Register page accessibility..." -ForegroundColor White
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/register" -UseBasicParsing
    if ($response.StatusCode -eq 200 -and $response.Content -match "Create Account") {
        Write-Host "SUCCESS: Register page loads correctly" -ForegroundColor Green
    } else {
        Write-Host "FAILED: Register page content issue" -ForegroundColor Red
    }
} catch {
    Write-Host "FAILED: Cannot reach register page - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n[TEST 1.3] CSS styling loaded..." -ForegroundColor White
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/css/auth.css" -UseBasicParsing
    if ($response.StatusCode -eq 200 -and $response.Content -match "Playfair Display") {
        Write-Host "SUCCESS: Pastel styling CSS loaded" -ForegroundColor Green
    } else {
        Write-Host "FAILED: CSS not found or incomplete" -ForegroundColor Red
    }
} catch {
    Write-Host "FAILED: Cannot load CSS - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n[TEST 1.4] Access Denied page..." -ForegroundColor White
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/access-denied" -UseBasicParsing
    if ($response.StatusCode -eq 200 -and $response.Content -match "Access Denied") {
        Write-Host "SUCCESS: Access Denied page renders" -ForegroundColor Green
    } else {
        Write-Host "FAILED: Access Denied page issue" -ForegroundColor Red
    }
} catch {
    Write-Host "FAILED: Cannot reach access denied page - $($_.Exception.Message)" -ForegroundColor Red
}

# ========================================
# 2. API AUTHENTICATION TESTS
# ========================================
Write-Host "`n`n[SECTION 2] API Authentication Tests" -ForegroundColor Yellow
Write-Host "-------------------------------------`n" -ForegroundColor Yellow

$randomId = Get-Random -Max 9999
$testUser = @{
    username = "testuser_$randomId"
    email = "test${randomId}@example.com"
    password = "Str0ngP@ssw0rd_$randomId"
}

Write-Host "[TEST 2.1] API User Registration..." -ForegroundColor White
try {
    $regBody = $testUser | ConvertTo-Json
    $response = Invoke-WebRequest `
        -Uri "$apiUrl/auth/register" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $regBody `
        -UseBasicParsing
    
    if ($response.StatusCode -eq 201) {
        $userData = $response.Content | ConvertFrom-Json
        Write-Host "SUCCESS: User registered via API (ID: $($userData.id), Roles: $($userData.roles -join ', '))" -ForegroundColor Green
    }
} catch {
    Write-Host "FAILED: API registration failed - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n[TEST 2.2] API User Login..." -ForegroundColor White
try {
    $loginBody = @{
        username = $testUser.username
        password = $testUser.password
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest `
        -Uri "$apiUrl/auth/login" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $loginBody `
        -UseBasicParsing
    
    if ($response.StatusCode -eq 200) {
        $loginData = $response.Content | ConvertFrom-Json
        Write-Host "SUCCESS: User logged in via API ($($loginData.username))" -ForegroundColor Green
    }
} catch {
    Write-Host "FAILED: API login failed - $($_.Exception.Message)" -ForegroundColor Red
}

# ========================================
# 3. AUTHORIZATION TESTS
# ========================================
Write-Host "`n`n[SECTION 3] Authorization Tests" -ForegroundColor Yellow
Write-Host "-------------------------------------`n" -ForegroundColor Yellow

Write-Host "[TEST 3.1] Unauthenticated access to protected endpoint..." -ForegroundColor White
try {
    Invoke-WebRequest -Uri "$apiUrl/user/profile" -UseBasicParsing -ErrorAction Stop | Out-Null
    Write-Host "FAILED: Should have been blocked (401)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "SUCCESS: Correctly blocked with 401 Unauthorized" -ForegroundColor Green
    } else {
        Write-Host "UNEXPECTED: Status $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
    }
}

Write-Host "`n[TEST 3.2] Authenticated access to user endpoint..." -ForegroundColor White
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$($testUser.username):$($testUser.password)"))
try {
    $response = Invoke-WebRequest `
        -Uri "$apiUrl/user/profile" `
        -Headers @{"Authorization"="Basic $credentials"} `
        -UseBasicParsing
    
    if ($response.StatusCode -eq 200) {
        $profile = $response.Content | ConvertFrom-Json
        Write-Host "SUCCESS: User profile retrieved ($($profile.username))" -ForegroundColor Green
    }
} catch {
    Write-Host "FAILED: Cannot access user profile - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n[TEST 3.3] USER role attempting ADMIN endpoint..." -ForegroundColor White
try {
    Invoke-WebRequest `
        -Uri "$apiUrl/admin/dashboard" `
        -Headers @{"Authorization"="Basic $credentials"} `
        -UseBasicParsing -ErrorAction Stop | Out-Null
    Write-Host "FAILED: Should have been blocked (403)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 403) {
        Write-Host "SUCCESS: Correctly blocked with 403 Forbidden" -ForegroundColor Green
    } else {
        Write-Host "UNEXPECTED: Status $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
    }
}

# ========================================
# 4. FORM-BASED LOGIN TESTS
# ========================================
Write-Host "`n`n[SECTION 4] Form-Based Login Tests" -ForegroundColor Yellow
Write-Host "-------------------------------------`n" -ForegroundColor Yellow

Write-Host "[TEST 4.1] Web form login with valid credentials..." -ForegroundColor White
try {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $response = Invoke-WebRequest `
        -Uri "$baseUrl/login" `
        -Method POST `
        -WebSession $session `
        -UseBasicParsing `
        -Body @{username=$testUser.username; password=$testUser.password} `
        -MaximumRedirection 0 `
        -ErrorAction SilentlyContinue
    
    if ($response.StatusCode -eq 302) {
        $location = $response.Headers.Location
        if ($location -match "/user/home") {
            Write-Host "SUCCESS: Form login redirects to /user/home (role-based redirect works)" -ForegroundColor Green
        } else {
            Write-Host "WARNING: Redirected to $location instead of /user/home" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "FAILED: Form login failed - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n[TEST 4.2] Invalid credentials..." -ForegroundColor White
try {
    $response = Invoke-WebRequest `
        -Uri "$baseUrl/login" `
        -Method POST `
        -UseBasicParsing `
        -Body @{username="invalid"; password="wrong"} `
        -MaximumRedirection 0 `
        -ErrorAction SilentlyContinue
    
    if ($response.StatusCode -eq 302 -and $response.Headers.Location -match "error") {
        Write-Host "SUCCESS: Invalid login redirects to error page" -ForegroundColor Green
    }
} catch {
    Write-Host "Expected behavior: redirect occurred" -ForegroundColor Gray
}

# ========================================
# 5. BOOK MANAGEMENT TESTS
# ========================================
Write-Host "`n`n[SECTION 5] Book Management Tests" -ForegroundColor Yellow
Write-Host "-------------------------------------`n" -ForegroundColor Yellow

Write-Host "[TEST 5.1] Create a book..." -ForegroundColor White
try {
    $bookBody = @{
        title = "Test Book $randomId"
        author = "Test Author"
        isbn = "ISBN-$randomId"
        genre = "Fiction"
        condition = "GOOD"
        available = $true
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest `
        -Uri "$apiUrl/books" `
        -Method POST `
        -Headers @{
            "Authorization"="Basic $credentials"
            "Content-Type"="application/json"
        } `
        -Body $bookBody `
        -UseBasicParsing
    
    if ($response.StatusCode -eq 201) {
        $book = $response.Content | ConvertFrom-Json
        Write-Host "SUCCESS: Book created (ID: $($book.id), Title: $($book.title))" -ForegroundColor Green
        $global:testBookId = $book.id
    }
} catch {
    Write-Host "FAILED: Cannot create book - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n[TEST 5.2] Get all books..." -ForegroundColor White
try {
    $response = Invoke-WebRequest `
        -Uri "$apiUrl/books" `
        -Headers @{"Authorization"="Basic $credentials"} `
        -UseBasicParsing
    
    if ($response.StatusCode -eq 200) {
        $books = $response.Content | ConvertFrom-Json
        Write-Host "SUCCESS: Retrieved $($books.Count) books" -ForegroundColor Green
    }
} catch {
    Write-Host "FAILED: Cannot retrieve books - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n[TEST 5.3] Get user's books..." -ForegroundColor White
try {
    $response = Invoke-WebRequest `
        -Uri "$apiUrl/books/my-books" `
        -Headers @{"Authorization"="Basic $credentials"} `
        -UseBasicParsing
    
    if ($response.StatusCode -eq 200) {
        $myBooks = $response.Content | ConvertFrom-Json
        Write-Host "SUCCESS: Retrieved $($myBooks.Count) user books" -ForegroundColor Green
    }
} catch {
    Write-Host "FAILED: Cannot retrieve user books - $($_.Exception.Message)" -ForegroundColor Red
}

# ========================================
# SUMMARY
# ========================================
Write-Host "`n`n========================================" -ForegroundColor Cyan
Write-Host "   TEST SUITE COMPLETED" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Manual Testing Checklist:" -ForegroundColor White
Write-Host "  1. Visit http://localhost:8080/login in browser" -ForegroundColor Gray
Write-Host "  2. Register a new account at /register" -ForegroundColor Gray
Write-Host "  3. Login and verify redirect to /user/home" -ForegroundColor Gray
Write-Host "  4. Check pastel design and animations" -ForegroundColor Gray
Write-Host "  5. Try accessing /admin/home (should see Access Denied)" -ForegroundColor Gray
Write-Host "  6. Logout and verify redirect to /login" -ForegroundColor Gray
Write-Host "`nAll automated tests completed!`n" -ForegroundColor Green
