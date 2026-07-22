package com.factshare.service;
import com.factshare.dto.*;
import com.factshare.model.User;
import com.factshare.repository.UserRepository;
import com.factshare.security.JwtUtil;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service

public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            return AuthResponse.builder().status("error").message("Email already exists").build();
        if (userRepository.existsByUsername(req.getUsername()))
            return AuthResponse.builder().status("error").message("Username already exists").build();
        User user = new User();
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhoneNumber(req.getPhoneNumber());
        user.setGender(req.getGender());
        user.setTermsAccepted(req.isTermsAccepted());
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        return AuthResponse.builder().status("success").message("User registered successfully").build();
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        if (user == null)
            return AuthResponse.builder().status("error").message("User not found").build();
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            return AuthResponse.builder().status("error").message("Invalid credentials").build();
        String token = jwtUtil.generateToken(user.getId());
        return AuthResponse.builder().status("success").message("Login successful")
            .token(token).userId(user.getId()).username(user.getUsername()).build();
    }
}
