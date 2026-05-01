package com.godswill.matrimony.config;

import com.godswill.matrimony.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    // ── Static resource handler (unchanged) ───────────────────────────────────
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = new File(uploadDir).getAbsolutePath();
        if (!uploadPath.endsWith(File.separator)) {
            uploadPath = uploadPath + File.separator;
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath)
                .setCachePeriod(3600);
    }

    // ── View controllers (unchanged) ──────────────────────────────────────────
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/contact").setViewName("contact");
    }

    // ── Admin interceptor — blocks /admin/** and /api/admin/** for non-admins ─
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {

                    @Override
                    public boolean preHandle(HttpServletRequest request,
                                             HttpServletResponse response,
                                             Object handler) throws Exception {

                        HttpSession session = request.getSession(false);
                        User user = (session != null) ? (User) session.getAttribute("user") : null;

                        boolean isAdmin = user != null
                                && user.getRole() != null
                                && "ADMIN".equalsIgnoreCase(user.getRole());

                        if (isAdmin) return true; // ✅ admin — allow through

                        // Check if it's a REST/API call or a page request
                        String uri    = request.getRequestURI();
                        String accept = request.getHeader("Accept");
                        boolean isApi = uri.startsWith("/api/")
                                || (accept != null && accept.contains("application/json"));

                        if (isApi) {
                            // Return 403 JSON for REST calls
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Access denied. Admins only.\"}");
                        } else {
                            // Redirect page requests
                            if (user == null) {
                                // Not logged in → send to login
                                response.sendRedirect("/login");
                            } else {
                                // Logged in but not admin → send to home
                                response.sendRedirect("/?error=Access+denied");
                            }
                        }

                        return false; // block the request
                    }
                })
                // ── Protect all admin page routes ──
                .addPathPatterns(
                        "/admin",
                        "/admin/**"
                )
                // ── Protect all admin REST routes ──
                .addPathPatterns(
                        "/api/admin/**",
                        "/api/orders/admin/**"
                );
    }
}