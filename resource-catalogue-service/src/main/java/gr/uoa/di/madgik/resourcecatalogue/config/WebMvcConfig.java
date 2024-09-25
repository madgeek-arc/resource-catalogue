package gr.uoa.di.madgik.resourcecatalogue.config;

import gr.uoa.di.madgik.resourcecatalogue.utils.MatomoInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@ComponentScan({
        "gr.uoa.di.madgik.registry.controllers",
        "gr.uoa.di.madgik.resourcecatalogue.controllers",
        "gr.uoa.di.madgik.resourcecatalogue.recdb.controllers"})
//@EnableWebMvc
@EnableAspectJAutoProxy
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedMethods("*").allowedOrigins("*");
    }

    @Autowired
    MatomoInterceptor matomoInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(matomoInterceptor);
    }
}

