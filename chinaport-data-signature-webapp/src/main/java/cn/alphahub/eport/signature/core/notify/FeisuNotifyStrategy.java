package cn.alphahub.eport.signature.core.notify;

import cn.alphahub.eport.signature.base.enums.NotifyResult;
import cn.alphahub.eport.signature.core.NotifyStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Email Notify Strategy.
 *
 * @author Julian
 * @version 1.2.0
 * @date 2025/7/31 17:29
 */
@Slf4j
@Component("feisuNotifyStrategy")
public class FeisuNotifyStrategy implements NotifyStrategy<Object> {

    @Override
    public NotifyResult notify(Object event) {
        return null;
    }
}

