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


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService; // 소셜 정보 처리 서비스
    private final OAuth2SuccessHandler oAuth2SuccessHandler;       // 성공 시 JWT 발급 핸들러

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // REST API이므로 기본 설정 해제(비활성화)
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .httpBasic(hp -> hp.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL별 권한 설정(통행증 검사 규칙)
            // 재발급 경로 허용: "/member/refresh"를 추가해줘야 리액트가 AT 만료 시 RT를 들고 조용히 접근할 수 있음 -> 추가 예정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/email/**").permitAll()
                .requestMatchers("/member/login", "/member/join", "/member/refresh").permitAll()
                .requestMatchers("/api/stations/**").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("Y")
                .requestMatchers("/admin/**").hasAuthority("Y")
                .requestMatchers("/download/**", "/images/**", "/css/**","/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**","/v3/api-docs.yaml").permitAll()
                .requestMatchers("/").permitAll()
                .anyRequest().authenticated() // 그 외 모든 요청은 토큰이 있어야 함
            )
            // 소셜 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)) // 소셜 서비스 연결
                .successHandler(oAuth2SuccessHandler) // 인증 성공 시 실행될 로직
            )
            // 인증/인가 예외 처리 (가짜 토큰, 토큰 없음 등)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"인증에 실패했습니다. 유효한 토큰을 제공하세요.\"}");
                })
            )

            // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 끼워 넣기
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 세부 설정 Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.addAllowedOrigin("http://localhost:5173");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


}