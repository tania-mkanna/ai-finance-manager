package com.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@#$%^&+=!_.*?])[A-Za-z\\d@#$%^&+=!_.*?]{8,}$",
                message = "Password must contain letters, numbers, and special characters"
        )
        String password,

        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Default currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String defaultCurrency,

        @NotBlank(message = "Timezone is required")
        String timezone
) {
}
