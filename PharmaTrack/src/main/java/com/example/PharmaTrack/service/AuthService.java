package com.example.PharmaTrack.service;

import com.example.PharmaTrack.dto.RegisterRequest;
import com.example.PharmaTrack.entity.User;

public interface AuthService {
    // Registers a new user: encodes the password and saves the user with the given authorities
    User register(RegisterRequest request);
    // Returns the user (with authorities) matching the logged-in username
    User getCurrentUser(String username);
}
