package com.example.buildpro.config;

import com.example.buildpro.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Autowired
        private CustomUserDetailsService userDetailsService;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/", "/login", "/index", "/homepage", "/register",
                                                                "/registration-verification",
                                                                "/categories/view", "/categories", "/cart/add/**",
                                                                "/otp-verification", "/forgot-password",
                                                                "/reset-password", "/products", "/products/add/**",
                                                                "/products/view/**", "/cart/view", "/cart",
                                                                "/checkout/**",
                                                                "/payment/**", "/products/search-results",
                                                                "/products/search",
                                                                "/products/category/**", "/products/brand/**",
                                                                "/api/chat/**", "/api/location/**",
                                                                "/profile", "/profile/**",
                                                                "/css/**", "/js/**", "/images/**",
                                                                "/cart/checkout", "/error",
                                                                "/favicon.ico",
                                                                "/buynow/**", "/payment/buynow/**")
                                                .permitAll()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/user/**").hasRole("USER")
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                // Redirect unauthenticated users to /login
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        String requestedUrl = request.getRequestURI();
                                                        response.sendRedirect("/login?redirect=" + requestedUrl);
                                                })
                                                // Redirect authenticated users who lack the role to /login
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        response.sendRedirect("/login?error=access_denied");
                                                }))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(java.util.List.of("*"));
                configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
