# Book Exchange API Test Script
# Tests Authentication and Authorization

$baseUrl = "http://localhost:8080/api"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   BOOK EXCHANGE - API TESTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Test 1: Register New User
Write-Host "`n[TEST 1] Registering new user..." -ForegroundColor Yellow
$regBody = @{
    username = "testuser_$(Get-Random -Max 9999)"
    email = "test$(Get-Random -Max 9999)@example.com"
    password = "password123"
} | ConvertTo-Json

try {
    $regResponse = Invoke-WebRequest `
        -Uri "$baseUrl/auth/register" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $regBody `
        -UseBasicParsing
    
    Write-Host "SUCCESS: Registration successful (Status: $($regResponse.StatusCode))" -ForegroundColor Green
    $userData = $regResponse.Content | ConvertFrom-Json
    Write-Host "  User ID: $($userData.id), Username: $($userData.username), Roles: $($userData.roles -join ', ')" -ForegroundColor Gray
    
    $username = $userData.username
} catch {
    Write-Host "FAILED: Registration failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: Login
Write-Host "`n[TEST 2] Logging in..." -ForegroundColor Yellow
$loginBody = @{
    username = $username
    password = "password123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-WebRequest `
        -Uri "$baseUrl/auth/login" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $loginBody `
        -UseBasicParsing
    
    Write-Host "SUCCESS: Login successful (Status: $($loginResponse.StatusCode))" -ForegroundColor Green
    $loginData = $loginResponse.Content | ConvertFrom-Json
    Write-Host "  Logged in as: $($loginData.username)" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: Login failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Access protected endpoint WITHOUT authentication
Write-Host "`n[TEST 3] Accessing protected endpoint WITHOUT authentication..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri "$baseUrl/user/profile" -UseBasicParsing -ErrorAction Stop | Out-Null
    Write-Host "FAILED: Should have been blocked!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "SUCCESS: Correctly blocked (401 Unauthorized)" -ForegroundColor Green
    } else {
        Write-Host "FAILED: Unexpected status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    }
}

# Test 4: Access protected endpoint WITH authentication
Write-Host "`n[TEST 4] Accessing protected endpoint WITH authentication..." -ForegroundColor Yellow
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${username}:password123"))

try {
    $profileResponse = Invoke-WebRequest `
        -Uri "$baseUrl/user/profile" `
        -Headers @{"Authorization"="Basic $credentials"} `
        -UseBasicParsing
    
    Write-Host "SUCCESS: Access granted (Status: $($profileResponse.StatusCode))" -ForegroundColor Green
    $profile = $profileResponse.Content | ConvertFrom-Json
    Write-Host "  Profile: $($profile.username) - $($profile.email)" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: Access denied: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Try to access admin endpoint as regular user
Write-Host "`n[TEST 5] Attempting to access ADMIN endpoint as USER..." -ForegroundColor Yellow
try {
    Invoke-WebRequest `
        -Uri "$baseUrl/admin/dashboard" `
        -Headers @{"Authorization"="Basic $credentials"} `
        -UseBasicParsing -ErrorAction Stop | Out-Null
    Write-Host "FAILED: Should have been blocked!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 403) {
        Write-Host "SUCCESS: Correctly blocked (403 Forbidden)" -ForegroundColor Green
    } else {
        Write-Host "? Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
    }
}

# Test 6: Create a book
Write-Host "`n[TEST 6] Creating a book as authenticated user..." -ForegroundColor Yellow
$bookBody = @{
    title = "Test Book"
    author = "Test Author"
    isbn = "1234567890"
    genre = "Fiction"
    condition = "GOOD"
    available = $true
} | ConvertTo-Json

try {
    $bookResponse = Invoke-WebRequest `
        -Uri "$baseUrl/books" `
        -Method POST `
        -Headers @{
            "Authorization"="Basic $credentials"
            "Content-Type"="application/json"
        } `
        -Body $bookBody `
        -UseBasicParsing
    
    Write-Host "SUCCESS: Book created (Status: $($bookResponse.StatusCode))" -ForegroundColor Green
    $book = $bookResponse.Content | ConvertFrom-Json
    Write-Host "  Book: $($book.title) by $($book.author)" -ForegroundColor Gray
} catch {
    Write-Host "FAILED: Book creation failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "   TESTS COMPLETED" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
