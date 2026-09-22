package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.dto.AuthDtos.AuthResponse;
import com.resumeanalyzer.backend.dto.AuthDtos.LoginRequest;
import com.resumeanalyzer.backend.dto.AuthDtos.RegisterRequest;
import com.resumeanalyzer.backend.model.User;
import com.resumeanalyzer.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        // Only the BCrypt hash is stored, never the real password
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return toResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        // Same message for "no such user" and "wrong password", so nobody can
        // use the login form to find out which emails are registered
        User user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Incorrect email or password"));

        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.createToken(user), user.getId(),
                user.getFullName(), user.getEmail());
    }
}