package com.example.book_exchange_sepm.validation;

import org.passay.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class PasswordStrengthValidator {

    private final PasswordValidator validator;

    public PasswordStrengthValidator() {
        this.validator = new PasswordValidator(Arrays.asList(
            // Length rule: 8-128 characters
            new LengthRule(8, 128),
            
            // At least one upper case letter
            new CharacterRule(EnglishCharacterData.UpperCase, 1),
            
            // At least one lower case letter
            new CharacterRule(EnglishCharacterData.LowerCase, 1),
            
            // At least one digit
            new CharacterRule(EnglishCharacterData.Digit, 1),
            
            // At least one special character
            new CharacterRule(EnglishCharacterData.Special, 1),
            
            // No whitespace allowed
            new WhitespaceRule()
        ));
    }

    public ValidationResult validate(String password) {
        RuleResult result = validator.validate(new PasswordData(password));
        
        if (result.isValid()) {
            return ValidationResult.valid();
        }
        
        List<String> messages = validator.getMessages(result);
        return ValidationResult.invalid(String.join(", ", messages));
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, "Password is strong");
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
