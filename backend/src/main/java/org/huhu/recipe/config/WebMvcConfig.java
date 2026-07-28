package org.huhu.recipe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.local-dir:uploads}")
    private String localDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地上传目录映射为静态资源，MinIO 未启用时通过 /uploads/xxx 访问
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + localDir + "/");
    }
}
