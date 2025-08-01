package cn.alphahub.eport.signature.core.notify.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Feisu Notify Properties.
 * <p>
 * This class holds the configuration properties for Feisu notifications.
 * It is enabled only if the property `eport.notify.feisu.enabled` is set to true.
 * </p>
 *
 * @author Julian
 * @version 1.2.0
 * @date 2025/7/31 17:29
 */
@Data
@ConfigurationProperties(prefix = "eport.signature.notify.feisu")
public class FeisuNotifyProperties {
    /**
     * 是否启用飞书通知
     */
    private boolean enabled = false;
    /**
     * Feisu webhook URL
     */
    private String webhookUrl;
    /**
     * Used to mention a user in the message
     */
    private String atUserId;
}
