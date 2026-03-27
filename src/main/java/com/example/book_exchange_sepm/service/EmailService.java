package com.example.book_exchange_sepm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@bookexchange.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private String resolveBaseUrl() {
        if (baseUrl == null) {
            return "http://localhost:8080";
        }

        String trimmed = baseUrl.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return "http://localhost:8080";
        }

        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String resolveLanBaseUrl(String effectiveBaseUrl) {
        if (!effectiveBaseUrl.contains("localhost") && !effectiveBaseUrl.contains("127.0.0.1")) {
            return null;
        }

        try {
            String localIp = InetAddress.getLocalHost().getHostAddress();
            if (localIp == null || localIp.isBlank() || localIp.startsWith("127.")) {
                return null;
            }

            return effectiveBaseUrl
                .replace("localhost", localIp)
                .replace("127.0.0.1", localIp);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void sendVerificationEmail(String toEmail, String username, String token) {
        String effectiveBaseUrl = resolveBaseUrl();
        if (mailSender == null) {
            // Mail sender not configured, log instead
            System.out.println("=== EMAIL VERIFICATION ===");
            System.out.println("To: " + toEmail);
            System.out.println("Verification Link: " + effectiveBaseUrl + "/verify-email?token=" + token);
            System.out.println("==========================");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Verify Your Email - Book Exchange");
            message.setText(buildVerificationEmailBody(username, token, effectiveBaseUrl));
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    private String buildVerificationEmailBody(String username, String token, String effectiveBaseUrl) {
        String verificationLink = effectiveBaseUrl + "/verify-email?token=" + token;
        String lanBaseUrl = resolveLanBaseUrl(effectiveBaseUrl);

        String alternateLinkText = "";
        if (lanBaseUrl != null) {
            alternateLinkText = "\nIf you open this email on your phone, use this link instead:\n"
                + lanBaseUrl + "/verify-email?token=" + token + "\n";
        }

        return String.format("""
            Hello %s,
            
            Thank you for registering with Book Exchange!
            
            Please verify your email address by clicking the link below:
            %s
            %s
            
            This link will expire in 24 hours.
            
            If you did not create an account, please ignore this email.
            
            Best regards,
            Book Exchange Team
            """, username, verificationLink, alternateLinkText);
    }

    public void sendPasswordResetEmail(String toEmail, String username, String token) {
        String effectiveBaseUrl = resolveBaseUrl();
        if (mailSender == null) {
            System.out.println("=== PASSWORD RESET ===");
            System.out.println("To: " + toEmail);
            System.out.println("Reset Link: " + effectiveBaseUrl + "/reset-password?token=" + token);
            System.out.println("======================");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Request - Book Exchange");
            message.setText(buildPasswordResetEmailBody(username, token, effectiveBaseUrl));
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
        }
    }

    private String buildPasswordResetEmailBody(String username, String token, String effectiveBaseUrl) {
        return String.format("""
            Hello %s,
            
            We received a request to reset your password.
            
            Click the link below to reset your password:
            %s/reset-password?token=%s
            
            This link will expire in 1 hour.
            
            If you did not request a password reset, please ignore this email.
            
            Best regards,
            Book Exchange Team
            """, username, effectiveBaseUrl, token);
    }
}
