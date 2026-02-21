package com.example.backend.config;

import com.example.backend.repository.member.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private CustomOAuth2UserService customOAuth2UserService;

        @Autowired
        private ClientRegistrationRepository clientRegistrationRepository;

        @Bean
        public BCryptPasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                /**
                                 * - 綠界回呼 /api/payment/** 多為外部 POST，若不排除常見 403
                                 * - /login/member：註解說是解 403 的關鍵，保留
                                 * - /api/auth/**：前端跨域呼叫常見，保留
                                 */
                                .csrf(csrf -> csrf.ignoringRequestMatchers(
                                                /*
                                                 * (翌帆)這邊我要說明一下 原本寫法是這樣
                                                 * "/api/payment/**", // 綠界回呼的 POST 請求
                                                 * "/login/member", // 放行前端登入請求
                                                 * "/api/auth/**" // 放行所有認證相關請求
                                                 * 但是會發生其他功能模組的 POST 請求也被擋下來的問題，還有後台管理員登入無法進入的問題。
                                                 * 所以我先改成放行所有後端 API 的請求，跟管理員登入請求，未來再視情況調整。
                                                 */
                                                "/api/**", // 放行所有後端 API (工單、金流、座位、分析...)
                                                "/rec-rent/**", // 解決前端歸還座位時，PUT請求被CSRF阻擋的問題
                                                "/login/**", // 放行所有登入請求
                                                "/oauth2/**", // 放行 OAuth2 相關 (通常不需要，但加著保險)
                                                "/spot/**",
                                                "/seats/**",
                                                "/admins/**"))

                                // 2. 載入自定義的 CORS 設定
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // 3. 處理 iframe 同源問題
                                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                                // 4. 請求權限控管
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/favicon.ico", "/error", "/css/**", "/js/**",
                                                                "/images/**")
                                                .permitAll()
                                                .requestMatchers(
                                                                "/api/**", // 其實下方API前綴都已經被包含了，只是寫了也不影響就不特別刪除。
                                                                "/login/**",
                                                                "/oauth2/**",
                                                                "/api/auth/**",
                                                                "/api/analyze/**",
                                                                "/api/forgot-password/**",
                                                                "/api/admin/forgot-password/**",
                                                                "/api/payment/**")
                                                .permitAll()
                                                .anyRequest().permitAll() // 開發期間放行所有請求
                                )

                                // 5. OAuth2 登入配置
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(authorization -> authorization
                                                                .authorizationRequestResolver(
                                                                                authorizationRequestResolver(
                                                                                                clientRegistrationRepository)))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .defaultSuccessUrl("http://localhost:5173/", true))

                                // 6. 登出配置
                                .logout(logout -> logout
                                                .logoutUrl("/api/auth/logout")
                                                .logoutSuccessHandler((request, response, authentication) -> {
                                                        response.setStatus(200);
                                                })
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID"));

                return http.build();
        }

        /**
         * 強制 Google 顯示帳號選擇視窗
         */
        private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
                        ClientRegistrationRepository clientRegistrationRepository) {
                DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                                clientRegistrationRepository, "/oauth2/authorization");
                resolver.setAuthorizationRequestCustomizer(customizer -> customizer
                                .additionalParameters(params -> params.put("prompt", "select_account")));
                return resolver;
        }

        /**
         * 整合後的 CORS 設定
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                // A. 前端 Vue 使用 (需 credentials, 用 Pattern)
                CorsConfiguration frontendConfig = new CorsConfiguration();
                frontendConfig.setAllowedOriginPatterns(Arrays.asList(
                                "http://localhost:5173",
                                "https://*.ngrok-free.dev",
                                "https://*.trycloudflare.com",
                                "https://*.loca.lt"));
                // 加入 PATCH 方法，支援 /toggle 狀態切換 API(翌帆工單系統有用到)
                frontendConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                frontendConfig.setAllowedHeaders(Arrays.asList("*"));
                frontendConfig.setAllowCredentials(true);

                // B. 綠界回傳使用 (不允許 credentials, 用 *)
                CorsConfiguration ecpayConfig = new CorsConfiguration();
                ecpayConfig.setAllowedOrigins(Arrays.asList("*"));
                ecpayConfig.setAllowedMethods(Arrays.asList("POST", "GET"));
                ecpayConfig.setAllowedHeaders(Arrays.asList("*"));
                ecpayConfig.setAllowCredentials(false);

                // 註冊路徑：具體路徑優先權高
                source.registerCorsConfiguration("/api/payment/payment-success", ecpayConfig);
                source.registerCorsConfiguration("/**", frontendConfig);

                return source;
        }
}
