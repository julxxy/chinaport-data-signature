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
        if (null == properties) {
            log.warn("飞书通知属性未设置.");
            return NotifyResult.SKIP;
        }
        if (!properties.isEnabled()) {
            log.info("飞书通知已禁用，跳过发送，如需启用请设置 eport.signature.notify.feisu.enabled=true");
            return NotifyResult.SKIP;
        }
        // TODO: Implement the logic to send a notification to Feisu
        return null;
    }
}

