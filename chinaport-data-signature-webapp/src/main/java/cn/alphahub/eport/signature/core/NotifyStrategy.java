package cn.alphahub.eport.signature.core;

import cn.alphahub.eport.signature.base.enums.NotifyResult;

/**
 * Notify Strategy.
 *
 * @param <T> 通知消息类型
 * @author Julian
 * @version 1.2.0
 */
public interface NotifyStrategy<T> {

    /**
     * 发送通知
     *
     * @param event 事件消息对象
     * @return 结果
     */
    NotifyResult notify(T event);
}
