Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

function New-Canvas {
    param(
        [int]$Width,
        [int]$Height
    )

    $bitmap = New-Object System.Drawing.Bitmap($Width, $Height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $graphics.Clear([System.Drawing.Color]::FromArgb(250, 251, 253))

    return @{
        Bitmap = $bitmap
        Graphics = $graphics
    }
}

function New-RoundedRectPath {
    param(
        [float]$X,
        [float]$Y,
        [float]$Width,
        [float]$Height,
        [float]$Radius
    )

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $diameter = $Radius * 2
    $path.AddArc($X, $Y, $diameter, $diameter, 180, 90)
    $path.AddArc($X + $Width - $diameter, $Y, $diameter, $diameter, 270, 90)
    $path.AddArc($X + $Width - $diameter, $Y + $Height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($X, $Y + $Height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function Draw-RoundedBox {
    param(
        [System.Drawing.Graphics]$Graphics,
        [float]$X,
        [float]$Y,
        [float]$Width,
        [float]$Height,
        [string]$Title,
        [string]$Body,
        [System.Drawing.Color]$FillColor,
        [System.Drawing.Color]$BorderColor
    )

    $path = New-RoundedRectPath -X $X -Y $Y -Width $Width -Height $Height -Radius 22
    $fillBrush = New-Object System.Drawing.SolidBrush($FillColor)
    $borderPen = New-Object System.Drawing.Pen($BorderColor, 2.4)
    $titleBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(34, 40, 49))
    $bodyBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(74, 85, 104))
    $titleFont = New-Object System.Drawing.Font("Segoe UI", 18, [System.Drawing.FontStyle]::Bold)
    $bodyFont = New-Object System.Drawing.Font("Segoe UI", 11)

    try {
        $Graphics.FillPath($fillBrush, $path)
        $Graphics.DrawPath($borderPen, $path)

        $titleRect = [System.Drawing.RectangleF]::new([float]($X + 18), [float]($Y + 14), [float]($Width - 36), [float]32)
        $bodyRect = [System.Drawing.RectangleF]::new([float]($X + 18), [float]($Y + 52), [float]($Width - 36), [float]($Height - 66))

        $Graphics.DrawString($Title, $titleFont, $titleBrush, $titleRect)
        $Graphics.DrawString($Body, $bodyFont, $bodyBrush, $bodyRect)
    }
    finally {
        $path.Dispose()
        $fillBrush.Dispose()
        $borderPen.Dispose()
        $titleBrush.Dispose()
        $bodyBrush.Dispose()
        $titleFont.Dispose()
        $bodyFont.Dispose()
    }
}

function Draw-Arrow {
    param(
        [System.Drawing.Graphics]$Graphics,
        [float]$X1,
        [float]$Y1,
        [float]$X2,
        [float]$Y2,
        [string]$Label = ""
    )

    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(90, 100, 120), 3)
    $pen.CustomEndCap = New-Object System.Drawing.Drawing2D.AdjustableArrowCap(5, 6)
    $font = New-Object System.Drawing.Font("Segoe UI", 10, [System.Drawing.FontStyle]::Bold)
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(90, 100, 120))

    try {
        $Graphics.DrawLine($pen, $X1, $Y1, $X2, $Y2)
        if ($Label) {
            $midX = ($X1 + $X2) / 2
            $midY = ($Y1 + $Y2) / 2
            $Graphics.DrawString($Label, $font, $brush, $midX - 26, $midY - 18)
        }
    }
    finally {
        $pen.Dispose()
        $font.Dispose()
        $brush.Dispose()
    }
}

function Draw-Title {
    param(
        [System.Drawing.Graphics]$Graphics,
        [string]$Text,
        [int]$Width
    )

    $font = New-Object System.Drawing.Font("Segoe UI", 24, [System.Drawing.FontStyle]::Bold)
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(28, 37, 65))
    $rect = [System.Drawing.RectangleF]::new([float]0, [float]18, [float]$Width, [float]40)
    $format = New-Object System.Drawing.StringFormat
    $format.Alignment = [System.Drawing.StringAlignment]::Center

    try {
        $Graphics.DrawString($Text, $font, $brush, $rect, $format)
    }
    finally {
        $font.Dispose()
        $brush.Dispose()
        $format.Dispose()
    }
}

function Save-Canvas {
    param(
        [hashtable]$Canvas,
        [string]$Path
    )

    try {
        $Canvas.Bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $Canvas.Graphics.Dispose()
        $Canvas.Bitmap.Dispose()
    }
}

$docsDir = Join-Path (Get-Location) "docs"
if (-not (Test-Path -LiteralPath $docsDir)) {
    New-Item -ItemType Directory -Path $docsDir | Out-Null
}

# Diagram 1: Project engineering focus
$canvas1 = New-Canvas -Width 1600 -Height 900
$g1 = $canvas1.Graphics
Draw-Title -Graphics $g1 -Text "Book Exchange SEPM: Engineering Focus" -Width 1600

Draw-RoundedBox -Graphics $g1 -X 620 -Y 110 -Width 360 -Height 110 `
    -Title "Project Core Goal" `
    -Body "Build a full-stack project with industry-style structure, security, testing, teamwork, and deployment readiness." `
    -FillColor ([System.Drawing.Color]::FromArgb(229, 239, 255)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(73, 119, 201))

Draw-RoundedBox -Graphics $g1 -X 120 -Y 300 -Width 290 -Height 160 `
    -Title "Architecture" `
    -Body "Layered Spring Boot design`nController -> Service -> Repository`nDTOs, entities, config, security modules" `
    -FillColor ([System.Drawing.Color]::FromArgb(235, 247, 238)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(76, 145, 95))

Draw-RoundedBox -Graphics $g1 -X 470 -Y 300 -Width 290 -Height 160 `
    -Title "Security" `
    -Body "Spring Security, JWT, BCrypt, email verification, role-based authorization, ownership checks" `
    -FillColor ([System.Drawing.Color]::FromArgb(255, 242, 230)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(210, 122, 52))

Draw-RoundedBox -Graphics $g1 -X 820 -Y 300 -Width 290 -Height 160 `
    -Title "Quality" `
    -Body "Unit tests, integration tests, security tests, smoke validation, exception handling" `
    -FillColor ([System.Drawing.Color]::FromArgb(246, 235, 255)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(147, 90, 196))

Draw-RoundedBox -Graphics $g1 -X 1170 -Y 300 -Width 290 -Height 160 `
    -Title "Operations" `
    -Body "GitHub Actions CI, Docker, Compose, Render deployment, environment config, docs" `
    -FillColor ([System.Drawing.Color]::FromArgb(234, 246, 248)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(60, 144, 164))

Draw-RoundedBox -Graphics $g1 -X 350 -Y 575 -Width 360 -Height 165 `
    -Title "Full-Stack Delivery" `
    -Body "Thymeleaf pages, page-specific CSS and JS, REST APIs, WebSocket chat, PostgreSQL persistence" `
    -FillColor ([System.Drawing.Color]::FromArgb(255, 249, 220)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(189, 160, 46))

Draw-RoundedBox -Graphics $g1 -X 890 -Y 575 -Width 360 -Height 165 `
    -Title "Team Workflow" `
    -Body "Two-person collaboration, branch-based development, pull requests, CI checks, maintainable repository structure" `
    -FillColor ([System.Drawing.Color]::FromArgb(240, 240, 240)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(120, 120, 120))

Draw-Arrow -Graphics $g1 -X1 800 -Y1 220 -X2 265 -Y2 300 -Label ""
Draw-Arrow -Graphics $g1 -X1 800 -Y1 220 -X2 615 -Y2 300 -Label ""
Draw-Arrow -Graphics $g1 -X1 800 -Y1 220 -X2 965 -Y2 300 -Label ""
Draw-Arrow -Graphics $g1 -X1 800 -Y1 220 -X2 1315 -Y2 300 -Label ""
Draw-Arrow -Graphics $g1 -X1 620 -Y1 460 -X2 530 -Y2 575 -Label ""
Draw-Arrow -Graphics $g1 -X1 980 -Y1 460 -X2 1070 -Y2 575 -Label ""

Save-Canvas -Canvas $canvas1 -Path (Join-Path $docsDir "report_engineering_focus.png")

# Diagram 2: Exchange and workflow lifecycle
$canvas2 = New-Canvas -Width 1700 -Height 900
$g2 = $canvas2.Graphics
Draw-Title -Graphics $g2 -Text "Book Exchange SEPM: Project Workflow" -Width 1700

Draw-RoundedBox -Graphics $g2 -X 80 -Y 320 -Width 220 -Height 120 `
    -Title "1. Register" `
    -Body "User creates account and verifies email." `
    -FillColor ([System.Drawing.Color]::FromArgb(229, 239, 255)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(73, 119, 201))

Draw-RoundedBox -Graphics $g2 -X 390 -Y 320 -Width 220 -Height 120 `
    -Title "2. List Book" `
    -Body "Owner adds a book with metadata and availability." `
    -FillColor ([System.Drawing.Color]::FromArgb(235, 247, 238)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(76, 145, 95))

Draw-RoundedBox -Graphics $g2 -X 700 -Y 320 -Width 220 -Height 120 `
    -Title "3. Request Exchange" `
    -Body "Another user offers one of their own books." `
    -FillColor ([System.Drawing.Color]::FromArgb(255, 242, 230)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(210, 122, 52))

Draw-RoundedBox -Graphics $g2 -X 1010 -Y 320 -Width 220 -Height 120 `
    -Title "4. Review & Accept" `
    -Body "Participants accept and moderator approves or rejects." `
    -FillColor ([System.Drawing.Color]::FromArgb(246, 235, 255)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(147, 90, 196))

Draw-RoundedBox -Graphics $g2 -X 1320 -Y 320 -Width 220 -Height 120 `
    -Title "5. Deliver & Complete" `
    -Body "Delivery is tracked and ownership is transferred." `
    -FillColor ([System.Drawing.Color]::FromArgb(234, 246, 248)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(60, 144, 164))

Draw-RoundedBox -Graphics $g2 -X 545 -Y 570 -Width 280 -Height 150 `
    -Title "Supporting Services" `
    -Body "Real-time chat`nWishlist notifications`nRole-based access control`nDatabase integrity" `
    -FillColor ([System.Drawing.Color]::FromArgb(255, 249, 220)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(189, 160, 46))

Draw-RoundedBox -Graphics $g2 -X 940 -Y 570 -Width 360 -Height 150 `
    -Title "Engineering Lifecycle Around Workflow" `
    -Body "Tests, CI, Docker setup, deployment docs, repository structure, collaboration process" `
    -FillColor ([System.Drawing.Color]::FromArgb(240, 240, 240)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(120, 120, 120))

Draw-Arrow -Graphics $g2 -X1 300 -Y1 380 -X2 390 -Y2 380
Draw-Arrow -Graphics $g2 -X1 610 -Y1 380 -X2 700 -Y2 380
Draw-Arrow -Graphics $g2 -X1 920 -Y1 380 -X2 1010 -Y2 380
Draw-Arrow -Graphics $g2 -X1 1230 -Y1 380 -X2 1320 -Y2 380
Draw-Arrow -Graphics $g2 -X1 810 -Y1 440 -X2 685 -Y2 570
Draw-Arrow -Graphics $g2 -X1 1110 -Y1 440 -X2 1120 -Y2 570

Save-Canvas -Canvas $canvas2 -Path (Join-Path $docsDir "report_project_workflow.png")

# Diagram 3: Project structure and architecture
$canvas3 = New-Canvas -Width 1700 -Height 980
$g3 = $canvas3.Graphics
Draw-Title -Graphics $g3 -Text "Book Exchange SEPM: Project Structure and Architecture" -Width 1700

Draw-RoundedBox -Graphics $g3 -X 650 -Y 95 -Width 400 -Height 100 `
    -Title "Presentation Layer" `
    -Body "Thymeleaf pages, static CSS and JS, page controllers, REST controllers, WebSocket controllers" `
    -FillColor ([System.Drawing.Color]::FromArgb(229, 239, 255)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(73, 119, 201))

Draw-RoundedBox -Graphics $g3 -X 650 -Y 260 -Width 400 -Height 120 `
    -Title "Service Layer" `
    -Body "AuthenticationService, BookService, ExchangeRequestService, DeliveryService, ChatRoomService, NotificationService" `
    -FillColor ([System.Drawing.Color]::FromArgb(235, 247, 238)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(76, 145, 95))

Draw-RoundedBox -Graphics $g3 -X 650 -Y 450 -Width 400 -Height 100 `
    -Title "Persistence Layer" `
    -Body "Spring Data JPA repositories, DTO mapping, entity relationships, query methods" `
    -FillColor ([System.Drawing.Color]::FromArgb(255, 242, 230)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(210, 122, 52))

Draw-RoundedBox -Graphics $g3 -X 650 -Y 620 -Width 400 -Height 95 `
    -Title "Database" `
    -Body "PostgreSQL for runtime, H2 compatibility mode for tests" `
    -FillColor ([System.Drawing.Color]::FromArgb(246, 235, 255)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(147, 90, 196))

Draw-RoundedBox -Graphics $g3 -X 120 -Y 250 -Width 340 -Height 180 `
    -Title "Cross-Cutting Security" `
    -Body "Spring Security`nJWT filter and utility`nBCrypt + Passay`nRole-based access control`nWebSocket interceptor" `
    -FillColor ([System.Drawing.Color]::FromArgb(255, 249, 220)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(189, 160, 46))

Draw-RoundedBox -Graphics $g3 -X 1180 -Y 250 -Width 360 -Height 180 `
    -Title "Project Organization" `
    -Body "controller`nservice`nrepository`nentity`ndto`nconfig`nsecurity`ntemplates and static assets" `
    -FillColor ([System.Drawing.Color]::FromArgb(234, 246, 248)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(60, 144, 164))

Draw-RoundedBox -Graphics $g3 -X 260 -Y 760 -Width 420 -Height 150 `
    -Title "Quality and Collaboration" `
    -Body "Unit tests, integration tests, CI workflow, pull requests, maintainable documentation" `
    -FillColor ([System.Drawing.Color]::FromArgb(240, 240, 240)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(120, 120, 120))

Draw-RoundedBox -Graphics $g3 -X 980 -Y 760 -Width 420 -Height 150 `
    -Title "Operations and Deployment" `
    -Body "Dockerfile, docker-compose.yml, .env.example, render.yaml, deployment notes" `
    -FillColor ([System.Drawing.Color]::FromArgb(240, 240, 240)) `
    -BorderColor ([System.Drawing.Color]::FromArgb(120, 120, 120))

Draw-Arrow -Graphics $g3 -X1 850 -Y1 195 -X2 850 -Y2 260
Draw-Arrow -Graphics $g3 -X1 850 -Y1 380 -X2 850 -Y2 450
Draw-Arrow -Graphics $g3 -X1 850 -Y1 550 -X2 850 -Y2 620
Draw-Arrow -Graphics $g3 -X1 460 -Y1 340 -X2 650 -Y2 320
Draw-Arrow -Graphics $g3 -X1 1180 -Y1 340 -X2 1050 -Y2 320
Draw-Arrow -Graphics $g3 -X1 680 -Y1 715 -X2 560 -Y2 760
Draw-Arrow -Graphics $g3 -X1 1020 -Y1 715 -X2 1140 -Y2 760

Save-Canvas -Canvas $canvas3 -Path (Join-Path $docsDir "report_project_structure.png")

Write-Output "Created report diagrams in $docsDir"
