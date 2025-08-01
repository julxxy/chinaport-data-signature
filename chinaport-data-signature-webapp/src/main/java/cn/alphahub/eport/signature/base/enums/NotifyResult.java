package cn.alphahub.eport.signature.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知结果.
 *
 * @author Julian
 * @since 1.2.0
 */
@Getter
@AllArgsConstructor
public enum NotifyResult {
    SUCCESS,
    FAILURE,
    SKIP;
}
