package com.parking.auth;

import com.parking.common.service.EmailService;
import com.parking.config.AppUserDetailsService;
import com.parking.config.JwtService;
import com.parking.entity.Role;
import com.parking.entity.User;
import com.parking.repository.PasswordResetTokenRepository;
import com.parking.repository.RoleRepository;
import com.parking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private AppUserDetailsService userDetailsService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        Role driverRole = new Role();
        driverRole.setRoleName("Driver");
        lenient().when(roleRepository.findByRoleName("Driver")).thenReturn(Optional.of(driverRole));
    }

    @Test
    void testStartRegistrationReturnsMaskedEmail() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setFullName("Test User");
        request.setEmail("testuser@example.com");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        doNothing().when(emailService).sendRegistrationOtp(anyString(), anyString(), anyInt());

        // When
        String maskedEmail = authService.startRegistration(request);

        // Then
        assertNotNull(maskedEmail);
        assertTrue(maskedEmail.contains("***"), "Email returned should be masked");
        verify(emailService).sendRegistrationOtp(eq("testuser@example.com"), anyString(), anyInt());
    }

    @Test
    void testStartRegistrationRejectsExistingUsername() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("password");
        request.setFullName("Test User");
        request.setEmail("new@example.com");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When / Then
        assertThrows(Exception.class, () -> authService.startRegistration(request));
        verify(emailService, never()).sendRegistrationOtp(anyString(), anyString(), anyInt());
    }
}
