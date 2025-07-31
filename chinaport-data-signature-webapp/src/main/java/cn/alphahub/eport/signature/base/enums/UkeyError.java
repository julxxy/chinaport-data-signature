package cn.alphahub.eport.signature.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 电子口岸 u-key 发生错误，需要重启“客户端控件”的情况
 */
@Getter
@AllArgsConstructor
public enum UkeyError {
    RESETTING_CARD_READER_FAILED(50070, "[读卡器底层库]复位读卡器失败:错误码=50070", "Custom50070"),
    OPEN_THE_CARD_READER_FAILED(50200, "[读卡器底层库]打开读卡器失败：错误码=50200", "Custom50200"),
    ;
    /**
     * 错误码
     */
    private final int code;
    /**
     * 描述
     */
    private final String desc;
    /**
     * Err
     */
    private final String err;
}
