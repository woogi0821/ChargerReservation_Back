package com.simplecoding.chargerreservation.config;

import com.simplecoding.chargerreservation.common.jwt.JwtAuthenticationFilter;
import com.simplecoding.chargerreservation.common.jwt.JwtTokenProvider;
import com.simplecoding.chargerreservation.common.jwt.OAuth2SuccessHandler;
import com.simplecoding.chargerreservation.member.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(hp -> hp.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/download/**", "/images/**", "/css/**", "/js/**", "/favicon.ico").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/notices/**").permitAll()
                        .requestMatchers("/api/member/join", "/api/member/check-id", "/api/email/**").permitAll()
                        .requestMatchers("/api/member/login", "/api/member/refresh").permitAll()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/member/find-id", "/api/member/find-pw").permitAll()
                        .requestMatchers("/api/stations/**").permitAll()
                        .requestMatchers("/api/admin/**", "/admin/**").hasAuthority("Y")
                        .requestMatchers("/ws-charger/**").permitAll()
                        .requestMatchers("/kiosk/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            response.sendRedirect("http://localhost:5173/");
                        })
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=utf-8");
                            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"인증에 실패했습니다. 유효한 토큰을 제공하세요.\"}");
                        })
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ 수정 — 라이브 서버 포트 추가
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",      // React 프론트
                "http://localhost:5500",      // 라이브 서버
                "http://127.0.0.1:5500",      // 라이브 서버
                "http://localhost:5501",      // 라이브 서버 (포트 다를 때)
                "http://127.0.0.1:5501"       // 라이브 서버 (포트 다를 때)
        ));

        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}