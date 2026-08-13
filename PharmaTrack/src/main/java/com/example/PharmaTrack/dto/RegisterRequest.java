package com.example.PharmaTrack.dto;

import jakarta.validation.constraints.*;

import java.util.List;

/*
 * ARCHITECTURE: DTO for the public registration endpoint. It carries exactly what the
 * registration requirement specifies: a username, a password, and one or more authority
 * names. Validation is declared with Bean Validation annotations and enforced by the
 * controller's @Valid.
 */
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    // One or more authority names (e.g. ["ADMIN"], ["PHARMACIST", "AUDITOR"])
    @NotEmpty(message = "At least one authority is required")
    private List<String> authorityNames;

    public RegisterRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public List<String> getAuthorityNames() { return authorityNames; }
    public void setAuthorityNames(List<String> authorityNames) { this.authorityNames = authorityNames; }
}
