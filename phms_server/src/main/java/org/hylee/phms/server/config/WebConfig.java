package org.hylee.phms.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层配置。
 * <p>
 * 主要包含：
 * <ul>
 *   <li>CORS 跨域配置（前端开发环境端口）</li>
 *   <li>鉴权拦截器注册（覆盖 /api/**）</li>
 *   <li>静态资源映射（用于图片/封面等文件访问）</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @org.springframework.lang.NonNull
    private final AuthInterceptor authInterceptor;

    public WebConfig(@org.springframework.lang.NonNull AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addCorsMappings(@org.springframework.lang.NonNull CorsRegistry registry) {
        // 仅对 /api/** 开放跨域，允许携带 Cookie（session 登录态）
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(@org.springframework.lang.NonNull InterceptorRegistry registry) {
        // 统一鉴权拦截：登录校验、账号状态校验、管理员权限校验等
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(@org.springframework.lang.NonNull ResourceHandlerRegistry registry) {
        // 静态资源映射：优先读取本地 static 目录，其次读取 classpath
        registry.addResourceHandler("/static/**")
                .addResourceLocations(
                        "file:./static/",
                        "file:../static/",
                        "classpath:/static/"
                );
    }
}
