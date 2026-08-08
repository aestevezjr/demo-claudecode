package com.facturas.api.service.impl;

import com.facturas.api.dto.AuthRequestDTO;
import com.facturas.api.entity.User;
import com.facturas.api.repository.UserRepository;
import com.facturas.api.service.JwtService;
import lombok.RequiredArgsConstructor;
import com.facturas.api.exception.UsernameAlreadyExistsException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registers a new user. Does not auto-login — the client must call /auth/login separately.
     */
    @Transactional
    public void register(AuthRequestDTO request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }
        User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }

    /**
     * Validates credentials and returns a signed JWT string.
     * The controller is responsible for placing this token in an HttpOnly cookie.
     */
    @Transactional(readOnly = true)
    public String login(AuthRequestDTO request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return jwtService.generateToken(user.getUsername(), user.getTokenVersion(), user.getRole());
    }

    /**
     * Bumps the user's tokenVersion so every JWT issued before this call is
     * rejected by JwtAuthFilter, regardless of its expiration. Called on logout
     * so a copied/stolen cookie can't outlive the session that logged out.
     */
    @Transactional
    public void invalidateTokens(String username) {
        userRepository.findByUsername(username)
                .ifPresent(user -> user.setTokenVersion(user.getTokenVersion() + 1));
    }
}
