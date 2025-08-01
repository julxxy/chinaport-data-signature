package cn.alphahub.eport.signature.core.notify;

import cn.alphahub.eport.signature.base.enums.NotifyResult;
import cn.alphahub.eport.signature.core.NotifyStrategy;
import cn.alphahub.eport.signature.core.notify.conf.FeisuNotifyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Email Notify Strategy.
 *
 * @author Julian
 * @version 1.2.0
 * @date 2025/7/31 17:29
 */
@Slf4j
@Component(FeisuNotifyStrategy.NAME)
@EnableConfigurationProperties({FeisuNotifyProperties.class})
public class FeisuNotifyStrategy implements NotifyStrategy<Object> {

    public final static String NAME = "feisuNotifyStrategy";

    @Autowired
    private FeisuNotifyProperties properties;

    @Override
    public NotifyResult notify(Object event) {
        if (null == properties || !properties.isEnabled()) {
            log.warn("飞书通知未启用或属性未设置.");
            return NotifyResult.FAILURE;
        }
        // TODO: Implement the logic to send a notification to Feisu
        return null;
    }
}

