package cn.alphahub.eport.signature.core.notify;

import cn.alphahub.dtt.plus.util.JacksonUtil;
import cn.alphahub.eport.signature.base.enums.NotifyResult;
import cn.alphahub.eport.signature.base.enums.UkeyError;
import cn.alphahub.eport.signature.config.EmailProperties;
import cn.alphahub.eport.signature.config.UkeyAccessClientProperties.Command;
import cn.alphahub.eport.signature.config.UkeyHealthHelper;
import cn.alphahub.eport.signature.core.NotifyStrategy;
import cn.alphahub.eport.signature.core.notify.model.EmailNotifyRecord;
import cn.alphahub.eport.signature.entity.ConsoleOutput;
import cn.alphahub.eport.signature.entity.UkeyResponse;
import cn.alphahub.eport.signature.entity.WebSocketWrapper;
import cn.alphahub.multiple.email.EmailTemplate;
import cn.alphahub.multiple.email.EmailTemplate.SimpleMailMessageDomain;
import cn.alphahub.multiple.email.annotation.Email;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Email Notify Strategy.
 *
 * @author Julian
 * @version 1.2.0
 * @date 2025/7/31 17:29
 */
@Slf4j
@Component("emailNotifyStrategy")
public class EmailNotifyStrategy implements NotifyStrategy<EmailNotifyRecord> {

    @Autowired
    private EmailTemplate emailTemplate;
    @Autowired
    private EmailProperties emailProperties;
    @Autowired
    private UkeyHealthHelper ukeyHealthHelper;

    @Override
    public NotifyResult notify(EmailNotifyRecord event) {
        if (event == null) {
            log.warn("电子口岸u-key加签数据失败，通知事件对象为空");
            return NotifyResult.FAILURE;
        }
        sendAlertWhenFailure(event);
        return NotifyResult.SUCCESS;
    }

    /**
     * 处理加签失败的逻辑，发送邮件通知，由于u-key自身硬件问题导致的加签失败，如：
     * <pre>
     * {
     *   "_id": 1,
     *   "_method": "cus-sec_SpcSignDataAsPEM",
     *   "_status": "00",
     *   "_args": {
     *     "Result": false,
     *     "Data": [],
     *     "Error": [
     *       "[读卡器底层库]复位读卡器失败:错误码：50070",
     *       "Err:Custom50070"
     *     ]
     *   }
     * }
     * </pre>
     * <p>
     * <figure>
     * <img src="https://weasley.oss-cn-shanghai.aliyuncs.com/Photos/iShot_2023-08-10_13.56.34.png" alt="邮件通知效果">
     * <figcaption>加签失败邮件通知</figcaption>
     * </figure>
     * </p>
     *
     * @param event 电子口岸u-key加签失败通知事件
     * @implNote [读卡器底层库]复位读卡器失败会自动重启u-key的Windows进程，希望能提升自我容灾机制
     * @since 2023-06-10
     */
    @Email
    public void sendAlertWhenFailure(EmailNotifyRecord event) {

        UkeyResponse ukeyResponse = event.ukeyResponse();
        WebSocketWrapper webSocketWrapper = event.webSocketWrapper();

        String messagePayload = getUkeySignPayload(webSocketWrapper);
        String payload = StringUtils.defaultIfBlank(messagePayload, "");
        String errorMessage = JacksonUtil.toPrettyJson(ukeyResponse);

        String subject = "电子口岸 U-Key 加签失败提醒";

        if (emailProperties.getEnable().equals(true)) {
            log.warn("电子口岸u-key加签数据失败，发送邮件通知: {}, {}", subject, payload);
            SimpleMailMessageDomain message = new SimpleMailMessageDomain();
            message.setTo(emailProperties.getTo());
            message.setCc(emailProperties.getCcEmails());
            message.setSentDate(LocalDateTime.now());
            message.setSubject(subject);
            message.setText("");
            boolean shouldBreak = false;
            boolean existsError = false;
            if (CollectionUtils.isNotEmpty(ukeyResponse.get_args().getError())) {
                for (String errorText : ukeyResponse.get_args().getError()) {
                    for (UkeyError ukeyError : UkeyError.values()) {
                        if (Strings.CS.contains(errorText, ukeyError.getErr())) {
                            restartUkeyWindowsWebsocketClient(message);
                            shouldBreak = true;
                            existsError = true;
                            break;
                        }
                    }
                    if (shouldBreak) {
                        break;
                    }
                }
            }
            StringBuilder text = new StringBuilder()
                    .append("原始错误:\n").append(errorMessage).append("\n")
                    .append("------------------------------\n")
                    .append("数据载荷:\n").append(payload).append("\n");
            if (existsError) {
                message.setText(text.append("提示，如遇 “[读卡器底层库]复位读卡器失败” 等错误，程序自动重启客户端后如果还是不能加签，请手动重启加签exe客户端程序。").toString());
            } else {
                message.setText(text.toString());
            }
            emailTemplate.send(message);
        }
    }

    /**
     * 处理加签失败的逻辑，重启u-key的Windows客户端，由于u-key自身硬件问题导致的加签失败
     *
     * @since 1.0.9
     */
    public void restartUkeyWindowsWebsocketClient(SimpleMailMessageDomain message) {
        ConsoleOutput output = ukeyHealthHelper.fixUkey(Command.RESTART);
        String restartLog = message.getText()
                .concat("\n\n加签程序【chinaport-data-signature】重启Windows Websocket客户端，cmd终端信息:\n")
                .concat(StringUtils.defaultIfBlank(JacksonUtil.toJson(output), ""));
        message.setText(restartLog);
    }

    /**
     * 获取u-key加签的消息负载
     *
     * @since 1.2.0
     */
    private String getUkeySignPayload(WebSocketWrapper wrapper) {
        if (wrapper == null || wrapper.getRequest() == null) {
            return "Request is null";
        }
        String ukeyPayload = Objects.toString(wrapper.getPayload(), "Payload is null");
        String requestData = Objects.toString(wrapper.getRequest().getData(), "Request data is null");
        return """
                Ukey加签负载：
                %s
                应用请求负载：
                %s
                """.formatted(ukeyPayload, requestData);
    }

}

