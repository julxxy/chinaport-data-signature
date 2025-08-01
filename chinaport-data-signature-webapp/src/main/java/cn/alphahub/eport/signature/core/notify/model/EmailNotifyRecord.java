package cn.alphahub.eport.signature.core.notify.model;

import cn.alphahub.eport.signature.entity.UkeyResponse;
import cn.alphahub.eport.signature.entity.WebSocketWrapper;

/**
 * 电子口岸u-key加签失败通知记录
 *
 * @param webSocketWrapper 请求消息负载
 * @param ukeyResponse     Ukey错误响应
 */
public record EmailNotifyRecord(WebSocketWrapper webSocketWrapper, UkeyResponse ukeyResponse) {
}
