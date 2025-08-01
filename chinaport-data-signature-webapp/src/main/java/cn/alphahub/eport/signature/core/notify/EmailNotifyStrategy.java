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
import cn.alphahub.eport.signature.entity.SignRequest;
import cn.alphahub.eport.signature.entity.UkeyResponse;
import cn.alphahub.multiple.email.EmailTemplate;
import cn.alphahub.multiple.email.EmailTemplate.MimeMessageDomain;
import jakarta.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 电子口岸U-Key加签失败邮件通知策略
 *
 * @author Julian
 * @version 1.2.0
 * @date 2025/7/31 17:29
 */
@Slf4j
@Component(EmailNotifyStrategy.NAME)
public class EmailNotifyStrategy implements NotifyStrategy<EmailNotifyRecord> {
    public final static String NAME = "emailNotifyStrategy";

    @Autowired
    private EmailTemplate emailTemplate;
    @Autowired
    private EmailProperties emailProperties;
    @Autowired
    private UkeyHealthHelper ukeyHealthHelper;
    /**
     * 应用名称，用于邮件通知主题
     */
    @Value("${spring.application.name:电子口岸数据签名服务}")
    private String applicationName;

    @Override
    public NotifyResult notify(EmailNotifyRecord event) {
        if (event == null) {
            log.warn("U-Key 加签失败，通知事件为空");
            return NotifyResult.FAILURE;
        }
        try {
            notifyWhenFailure(event);
            return NotifyResult.SUCCESS;
        } catch (Exception e) {
            log.error("U-Key 加签失败，发送邮件通知异常: {}", e.getMessage(), e);
            return NotifyResult.FAILURE;
        }
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
     *
     * @param event 电子口岸u-key加签失败通知事件
     * @throws MessagingException 邮件发送异常
     * @implNote [读卡器底层库]复位读卡器失败会自动重启u-key的Windows进程，希望能提升自我容灾机制
     * @since 1.2.0
     */
    private void notifyWhenFailure(EmailNotifyRecord event) throws MessagingException {
        UkeyResponse ukeyErrResponse = event.ukeyResponse();
        SignRequest signRequest = event.webSocketWrapper().getRequest();
        String traceId = event.webSocketWrapper().getSessionId(); // WebSocket会话ID为traceId

        String subject = "【重要提醒】电子口岸 U-Key 加签失败";

        String errorItemsHtml;
        if (ukeyErrResponse != null && ukeyErrResponse.get_args() != null &&
                CollectionUtils.isNotEmpty(ukeyErrResponse.get_args().getError())) {
            errorItemsHtml = ukeyErrResponse.get_args().getError().stream()
                    .map(e -> "<li>" + e + "</li>")
                    .collect(Collectors.joining());
        } else {
            errorItemsHtml = "<li>未获取到详细错误信息。</li>";
        }

        String htmlBody = """
                <div>
                    <b>您好，</b><br><br>
                    您的 U-Key 加签操作失败，主要信息如下：<br><br>
                    <div style="color:#888; margin-bottom:10px;">
                        追踪ID/TraceID：%s
                    </div>
                    <br>
                    <b style='color:#d32f2f;'>错误原因：</b>
                    <ul>
                        %s
                    </ul>
                    <br>
                    <b>建议操作：</b>
                    <ol>
                        <li>检查卡介质是否已插入并重新插拔。</li>
                        <li>更换 USB 接口或使用原装数据线。</li>
                        <li>重启电脑或更换其他电脑尝试。</li>
                        <li>如无效，设备可能损坏，请联系数据分中心。</li>
                        <li>联系方式：<a href="https://www.singlewindow.cn/rahotline">全国通关一体化服务热线</a></li>
                    </ol>
                    <br>
                    <b>原始错误数据见附件，方便进一步排查。</b><br>
                </div>
                """.formatted(traceId, errorItemsHtml);

        MultipartFile attachment = null;
        String rawErrJson = JacksonUtil.toPrettyJson(ukeyErrResponse);
        String requestData = signRequest.getData();
        String attachmentText = """
                ================== 原始U-Key错误诊断详细内容 ==================
                
                %s
                
                ================== 报文原始请求内容 ==================
                
                %s
                """.formatted(StringUtils.defaultString(rawErrJson), requestData);
        try {
            attachment = new MockMultipartFile(
                    "error-details",
                    "错误详情.txt",
                    "text/plain",
                    attachmentText.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ignored) {
        }

        MimeMessageDomain message = new MimeMessageDomain();
        message.setTo(emailProperties.getTo());
        message.setCc(emailProperties.getCcEmails());
        message.setSubject(subject);
        message.setText(htmlBody);
        message.setSentDate(LocalDateTime.now());
        message.setFilepath(null);

        // 是否有可自动修复的错误
        boolean hasAutoFixableError = false;
        if (ukeyErrResponse != null
                && ukeyErrResponse.get_args() != null
                && CollectionUtils.isNotEmpty(ukeyErrResponse.get_args().getError())) {

            hasAutoFixableError = ukeyErrResponse.get_args().getError().stream()
                    .anyMatch(errorText -> Arrays.stream(UkeyError.values())
                            .anyMatch(ukeyError -> Strings.CS.contains(errorText, ukeyError.getErr()))
                    );

            if (hasAutoFixableError) {
                log.info("检测到可自动修复的U-Key错误，准备重启客户端...");
                restartUkeyWindowsWebsocketClient(message);
            }
        }

        if (hasAutoFixableError) {
            String extraTip = """
                    
                    <div style="margin-top: 10px; padding: 10px; background: #fff3cd; color: #856404; border: 1px solid #ffeeba; border-radius: 4px;">
                        <b>提示：</b>如遇 “[读卡器底层库]复位读卡器失败” 等错误，程序已自动重启客户端。<br>
                        如果还是不能加签，请手动重启加签 exe 客户端程序。
                    </div>
                    
                    """;
            message.setText(htmlBody + extraTip);
        } else {
            message.setText(htmlBody);
        }

        emailTemplate.send(message, attachment);
    }

    /**
     * 处理加签失败的逻辑，重启u-key的Windows客户端，由于u-key自身硬件问题导致的加签失败
     *
     * @since 1.0.9
     */
    public void restartUkeyWindowsWebsocketClient(MimeMessageDomain message) {
        ConsoleOutput output = ukeyHealthHelper.fixUkey(Command.RESTART);
        String restartInfoHtml = """
                
                <div style="margin-top:18px; padding:12px; background:#fff3cd; border:1px solid #ffeeba; border-radius:4px; color:#856404;">
                    <b>已自动重启 U-Key Windows WebSocket 客户端：</b>
                    <div style="margin-top:6px;">
                        程序名称：<b style="color:#333;">%s</b>
                    </div>
                </div>
                <div style="margin-top:12px;">
                    <b>CMD 终端信息：</b>
                    <pre style="padding:10px;background:#f8f9fa;border:1px solid #e0e0e0;border-radius:4px;color:#212529;">%s</pre>
                </div>
                
                """.formatted(applicationName, StringUtils.defaultIfBlank(JacksonUtil.toJson(output), "（无终端输出）"));
        message.setText(message.getText() + restartInfoHtml);
    }

}

