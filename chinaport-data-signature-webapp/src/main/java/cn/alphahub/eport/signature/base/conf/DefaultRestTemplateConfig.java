package cn.alphahub.eport.signature.base.conf;

import cn.alphahub.eport.signature.base.interceptor.RestTemplateTraceInterceptor;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类
 *
 * @author Julian
 */
@Configuration
public class DefaultRestTemplateConfig implements InitializingBean {

    private final List<RestTemplate> restTemplates;

    public DefaultRestTemplateConfig(List<RestTemplate> restTemplates) {
        this.restTemplates = restTemplates;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (null != this.restTemplates) {
            this.restTemplates.forEach(restTemplate -> restTemplate.getInterceptors().add(new RestTemplateTraceInterceptor()));
        }
    }

}
