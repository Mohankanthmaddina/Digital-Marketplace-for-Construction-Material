package com.example.buildpro.controller;

import com.example.buildpro.model.OTP;
import com.example.buildpro.model.User;
import com.example.buildpro.service.AuthService;
import com.example.buildpro.dto.LoginRequest;
import com.example.buildpro.dto.RegisterRequest;
import com.example.buildpro.dto.OTPRequest;
import com.example.buildpro.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.example.buildpro.util.JwtUtil;
import com.example.buildpro.dto.AuthResponse;

import java.util.HashMap;
import java.util.Map;
import com.example.buildpro.dto.UserSummaryDTO;
import java.util.Optional;
import com.example.buildpro.model.RefreshToken;
import com.example.buildpro.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpStatus;

@Controller
@CrossOrigin(origins = "*")
public class AuthController {

    /**
     * Basic email format validation (fallback)
     */
    private boolean isValidEmailFormat(String email) {
        return email != null && email.contains("@") && email.contains(".") && email.length() > 5;
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home() {
        return "homepage";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletResponse response) {

        // Step 1: Authenticate credentials
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid email or password",
                    "message", "Please check your credentials and try again",
                    "type", "invalid_credentials"));
        }

        // Step 2: Load user safely
        Optional<User> userOpt = userService.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "User not found",
                    "message", "No account found with this email address",
                    "type", "invalid_credentials"));
        }
        User user = userOpt.get();

        // Step 3: Check verification status
        if (!user.getIsVerified()) {
            if (user.getRole() == User.Role.ADMIN) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Admin account pending approval",
                        "message",
                        "Your admin account is awaiting verification by an existing administrator. Please contact support.",
                        "type", "admin_pending"));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Account not verified",
                        "message", "Please verify your email address to activate your account",
                        "type", "user_pending"));
            }
        }

        // Step 4: Check role matches login type selection
        String requestedRole = request.getRole();
        if (requestedRole == null || !requestedRole.equalsIgnoreCase(user.getRole().name())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Role mismatch",
                    "message", "Please select the correct login type (User or Admin)",
                    "type", "role_mismatch"));
        }

        // Step 5: Generate tokens — wrap in try-catch to surface any DB/runtime errors
        try {
            final String jwt = jwtUtil.generateToken(user.getId(), user.getRole().name());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            String redirectUrl = (user.getRole() == User.Role.ADMIN) ? "/admin/dashboard" : "/homepage";

            Cookie refreshCookie = new Cookie("refreshToken", refreshToken.getToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60);
            response.addCookie(refreshCookie);

            Cookie jwtCookie = new Cookie("authToken", jwt);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(15 * 60);
            response.addCookie(jwtCookie);

            return ResponseEntity.ok(new AuthResponse(jwt, "Login successful",
                    new UserSummaryDTO(user.getId(), user.getName(), user.getRole().name()), redirectUrl));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Login failed due to server error",
                    "message", e.getMessage() != null ? e.getMessage() : "Internal server error",
                    "type", "server_error"));
        }
    }

    @PostMapping("/refresh-token")
    @ResponseBody
    public ResponseEntity<?> refreshToken(jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Refresh Token is missing!"));
        }

        final String token = refreshToken;

        return refreshTokenService.findByToken(token)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newJwt = jwtUtil.generateToken(user.getId(), user.getRole().name());

                    // Update authToken cookie
                    Cookie jwtCookie = new Cookie("authToken", newJwt);
                    jwtCookie.setHttpOnly(true);
                    jwtCookie.setPath("/");
                    jwtCookie.setMaxAge(15 * 60); // 15 minutes
                    response.addCookie(jwtCookie);

                    return ResponseEntity.ok(Map.of(
                            "accessToken", newJwt,
                            "refreshToken", token));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    @ResponseBody
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        boolean sent = authService.initiatePasswordReset(email);
        if (sent) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("email", email);
            response.put("type", "reset");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Email not found or not verified"));
        }
    }

    @PostMapping("/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otpCode = request.get("otpCode");
        String newPassword = request.get("newPassword");

        boolean success = authService.resetPassword(email, otpCode, newPassword);
        if (success) {
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Password reset successful"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("status", "FAIL", "message", "Invalid OTP or email"));
        }
    }

    @PostMapping("/verify-otp")
    @ResponseBody
    public ResponseEntity<?> verifyOTP(@RequestBody OTPRequest request) {
        boolean isValid = authService.verifyOTP(request.getEmail(), request.getOtpCode(), request.getPurpose());
        if (isValid) {
            return ResponseEntity.ok("OTP verified successfully");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired OTP");
        }
    }

    @GetMapping("/otp-verification")
    public String otpVerificationPage(@RequestParam String type, @RequestParam String email,
            Map<String, Object> model) {
        model.put("type", type);
        model.put("email", email);
        return "otp-verification";
    }

    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // Check if email already exists in verified users table
        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Email already exists",
                    "message",
                    "This email address is already registered. Please use a different email or try logging in.",
                    "type", "email_exists"));
        }

        // Basic email format validation
        if (!isValidEmailFormat(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid email format",
                    "message", "Please enter a valid email address format (e.g., user@example.com).",
                    "type", "email_invalid"));
        }

        // Create user setters
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // Password will be encoded in AuthService
        user.setName(request.getName());

        // set role based on request
        try {
            user.setRole(User.Role.valueOf(request.getRole().toUpperCase())); // USER or ADMIN
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid role provided",
                    "message", "Please select a valid role (USER or ADMIN)",
                    "type", "invalid_role"));
        }

        try {
            // Store registration data in database (pending state) and send OTP
            // User will be created ONLY after OTP verification
            com.example.buildpro.model.PendingRegistration pendingData = authService.registerUser(user);

            return ResponseEntity.ok(Map.of(
                    "message", "OTP sent to email. Please verify to complete registration.",
                    "email", pendingData.getEmail(),
                    "role", pendingData.getRole().name(),
                    "success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Registration failed",
                    "message", e.getMessage(),
                    "type", "registration_error"));
        } catch (Exception e) {
            System.out.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Registration failed",
                    "message", "An error occurred during registration. Please try again.",
                    "type", "registration_error"));
        }
    }

    @GetMapping("/registration-verification")
    public String registrationVerificationPage(@RequestParam String email, Map<String, Object> model) {
        model.put("type", "registration");
        model.put("email", email);
        return "otp-verification";
    }

    @PostMapping("/verify-otp-submit") // Changed URL to avoid conflict if any
    public String verifyOTP(@RequestParam String email, @RequestParam String otpCode,
            org.springframework.ui.Model model) {
        if (authService.verifyOTP(email, otpCode, OTP.Purpose.REGISTRATION)) {
            return "redirect:/login?verified=true";
        } else {
            model.addAttribute("error", "Invalid or expired OTP");
            model.addAttribute("email", email);

            // Re-fetch pending data to keep user context
            java.util.Optional<com.example.buildpro.model.PendingRegistration> pendingOpt = authService
                    .getPendingRegistration(email);
            if (pendingOpt.isPresent()) {
                model.addAttribute("role", pendingOpt.get().getRole());
            }

            return "otp-verification"; // Use existing view name
        }
    }

    @PostMapping("/resend-otp-submit") // Changed URL to avoid conflict
    public String resendOTP(@RequestParam String email, org.springframework.ui.Model model) {
        if (authService.hasPendingRegistration(email)) {
            java.util.Optional<com.example.buildpro.model.PendingRegistration> pendingOpt = authService
                    .getPendingRegistration(email);
            if (pendingOpt.isPresent()) {
                String newOtp = authService.generateOTP();
                authService.updatePendingRegistrationOTP(email, newOtp);
                authService.sendOTPEmail(email, newOtp, OTP.Purpose.REGISTRATION);
                model.addAttribute("message", "OTP sent successfully");
                model.addAttribute("email", email);
                model.addAttribute("role", pendingOpt.get().getRole());
                return "otp-verification"; // Use existing view name
            }
        }
        model.addAttribute("error", "No pending registration found");
        return "register";
    }

    @PostMapping("/registration-verification")
    @ResponseBody
    public ResponseEntity<?> registrationVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otpCode = request.get("otp");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Email is required",
                    "message", "Please provide a valid email address",
                    "type", "email_required"));
        }

        if (otpCode == null || otpCode.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "OTP is required",
                    "message", "Please enter the verification code",
                    "type", "otp_required"));
        }

        try {
            boolean verified = authService.verifyOTP(email, otpCode, OTP.Purpose.REGISTRATION);

            if (verified) {
                return ResponseEntity.ok(Map.of(
                        "message", "Account verified successfully",
                        "success", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid or expired OTP",
                        "message", "The verification code you entered is invalid or has expired. Please try again.",
                        "type", "invalid_otp"));
            }
        } catch (Exception e) {
            System.out.println("OTP verification error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Verification failed",
                    "message", "An error occurred during verification. Please try again.",
                    "type", "verification_error"));
        }
    }

    @GetMapping("/homepage")
    public String redirectToHomepage() {
        return "homepage";
    }

}
