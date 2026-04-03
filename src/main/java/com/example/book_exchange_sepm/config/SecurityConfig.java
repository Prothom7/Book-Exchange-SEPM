package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.security.JwtAuthenticationFilter;
import com.example.book_exchange_sepm.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.http.HttpStatus;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private RoleBasedAuthenticationSuccessHandler roleBasedAuthenticationSuccessHandler;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        RequestMatcher apiMatcher = request -> request.getRequestURI() != null
            && request.getRequestURI().startsWith("/api/");

        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/login", "/register", "/verify-email", "/resend-verification", "/access-denied", "/error", "/landingpage").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/ws-chat/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // Public authentication APIs
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/verify-email").permitAll()

                // Role-specific APIs
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/delivery/**").hasRole("DELIVERY_MAN")
                .requestMatchers("/api/moderator/**").hasRole("MODERATOR")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/delivery/**").hasRole("DELIVERY_MAN")
                .requestMatchers("/api/user/**", "/api/books/**", "/api/exchange-requests/**", "/api/exchange/**", "/api/wishlist/**", "/api/notifications/**")
                .hasAnyRole("USER", "MODERATOR", "ADMIN")

                // REST API endpoints
                .requestMatchers("/api/books-rest", "/api/books-rest/**").authenticated()
                .requestMatchers("/api/exchange-rest", "/api/exchange-rest/**").authenticated()
                .requestMatchers("/api/wishlist-rest", "/api/wishlist-rest/**").authenticated()

                // All other app pages and requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(roleBasedAuthenticationSuccessHandler)
                .failureForwardUrl("/login-failure")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    apiMatcher
                )
                .defaultAccessDeniedHandlerFor(
                    (request, response, accessDeniedException) -> response.sendError(HttpStatus.FORBIDDEN.value(), accessDeniedException.getMessage()),
                    apiMatcher
                )
                .accessDeniedPage("/access-denied")
            )
            .httpBasic(withDefaults())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
