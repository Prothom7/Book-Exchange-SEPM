# Email Configuration Guide

This guide explains how to enable actual email sending for the Book Exchange application.

## Quick Setup

The application requires SMTP credentials to send emails. You can configure this using environment variables.

### Gmail Configuration (Recommended for Testing)

1. **Enable 2-Step Verification** on your Gmail account:
   - Go to: https://myaccount.google.com/security
   - Enable "2-Step Verification"

2. **Generate App Password**:
   - Go to: https://myaccount.google.com/apppasswords
   - Select "Mail" and your device
   - Copy the 16-character password

3. **Set Environment Variables**:

   **Windows PowerShell:**
   ```powershell
   $env:SPRING_MAIL_USERNAME="your-email@gmail.com"
   $env:SPRING_MAIL_PASSWORD="your-16-char-app-password"
   .\mvnw.cmd spring-boot:run
   ```

   **Windows Command Prompt:**
   ```cmd
   set SPRING_MAIL_USERNAME=your-email@gmail.com
   set SPRING_MAIL_PASSWORD=your-16-char-app-password
   .\mvnw.cmd spring-boot:run
   ```

   **Linux/Mac:**
   ```bash
   export SPRING_MAIL_USERNAME="your-email@gmail.com"
   export SPRING_MAIL_PASSWORD="your-16-char-app-password"
   ./mvnw spring-boot:run
   ```

## Alternative SMTP Providers

### Outlook/Hotmail

```powershell
$env:SPRING_MAIL_HOST="smtp-mail.outlook.com"
$env:SPRING_MAIL_PORT="587"
$env:SPRING_MAIL_USERNAME="your-email@outlook.com"
$env:SPRING_MAIL_PASSWORD="your-password"
```

### SendGrid

```powershell
$env:SPRING_MAIL_HOST="smtp.sendgrid.net"
$env:SPRING_MAIL_PORT="587"
$env:SPRING_MAIL_USERNAME="apikey"
$env:SPRING_MAIL_PASSWORD="your-sendgrid-api-key"
```

### Mailgun

```powershell
$env:SPRING_MAIL_HOST="smtp.mailgun.org"
$env:SPRING_MAIL_PORT="587"
$env:SPRING_MAIL_USERNAME="your-mailgun-username"
$env:SPRING_MAIL_PASSWORD="your-mailgun-password"
```

### Custom SMTP Server

```powershell
$env:SPRING_MAIL_HOST="smtp.your-domain.com"
$env:SPRING_MAIL_PORT="587"
$env:SPRING_MAIL_USERNAME="your-email@your-domain.com"
$env:SPRING_MAIL_PASSWORD="your-password"
```

## Production Deployment

### Docker Compose

Update your `docker-compose.yml` or create a `.env` file:

```env
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
APP_BASE_URL=https://your-domain.com
```

### Application Properties File

Alternatively, create `application-prod.yaml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password

app:
  base-url: https://your-domain.com
```

Run with: `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod`

## Verify Email Configuration

1. **Start the application** with email credentials configured
2. **Register a new user**:
   ```powershell
   Invoke-WebRequest -Uri "http://localhost:8080/api/auth/register" `
     -Method POST `
     -Headers @{"Content-Type"="application/json"} `
     -Body '{"username":"testuser","email":"your-test-email@gmail.com","password":"TestPass@123"}'
   ```
3. **Check your email inbox** for the verification email
4. The console should show "Email sent successfully" instead of logging the verification link

## Troubleshooting

### Authentication Failed
- Verify Gmail App Password is correct (16 characters, no spaces)
- Ensure 2-Step Verification is enabled
- Check if "Less secure app access" is disabled (use App Passwords instead)

### Connection Timeout
- Check firewall settings (port 587 or 465 must be open)
- Try alternative port 465 with SSL: `SPRING_MAIL_PORT=465`

### Email Not Received
- Check spam/junk folder
- Verify recipient email address is correct
- Check application logs for error messages

### Console Still Shows Verification Links
- Ensure environment variables are set before starting the application
- Verify JavaMailSender bean is configured (check startup logs)
- Restart the application after setting environment variables

## Testing Without Real Email

For development, the application automatically falls back to console logging when email credentials are not configured. Verification links will be printed to the console so you can test the flow without setting up SMTP.

## Security Notes

- **Never commit email credentials** to version control
- Use environment variables or secure configuration management
- For production, use dedicated email service accounts
- Consider using email services with higher sending limits (SendGrid, Mailgun, etc.)
- Rotate passwords regularly
- Use App Passwords instead of account passwords when possible
