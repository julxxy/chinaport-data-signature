package cn.alphahub.eport.signature.base.conf;

import cn.alphahub.eport.signature.base.constant.FrameworkConstant;
import cn.alphahub.eport.signature.base.interceptor.DefaultTraceInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Base Application Configuration.
 *
 * @author Julian
 */
@Configuration
@ComponentScan(basePackages = {FrameworkConstant.BASE_PACKAGE})
public class BaseApplicationConfig implements WebMvcConfigurer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DefaultTraceInterceptor()).addPathPatterns("/**").order(-1);
    }

}
