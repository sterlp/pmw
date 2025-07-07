package org.sterl.spring.pmw.ui;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.sterl.spring.persistent_tasks_ui.EnableSpringPersistentTasksUI;

@EnableSpringPersistentTasksUI
@Configuration
public class WorkflowUIConfig implements WebMvcConfigurer {
    private static final String BASE = "/pmw-ui";
    private static final String INDEX_HTML = "forward:/pmw-ui/index.html";
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController(BASE).setViewName(INDEX_HTML);

        registry.addViewController(BASE + "/{path:[^\\.]*}")
                .setViewName(INDEX_HTML);
        registry.addViewController(BASE + "/*/{path:[^\\\\.]*}")
                .setViewName(INDEX_HTML);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(BASE + "/assets/**")
                .addResourceLocations("classpath:/static" + BASE + "/assets/")
                .setCacheControl(CacheControl.maxAge(90, TimeUnit.DAYS));

        registry.addResourceHandler(BASE + "/**")
                .addResourceLocations("classpath:/static" + BASE + "/");
    }
}
