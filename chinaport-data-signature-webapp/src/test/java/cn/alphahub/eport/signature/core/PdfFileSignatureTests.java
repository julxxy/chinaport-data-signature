package cn.alphahub.eport.signature.core;

import cn.alphahub.eport.signature.entity.UkeyRequest;
import cn.alphahub.eport.signature.entity.UkeyResponse.Args;
import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * PDF 签名.
 *
 * @version 1.0.0
 * @date 8/15/26 16:20
 */
@Slf4j
@SpringBootTest
class PdfFileSignatureTests {

    /**
     * 实际下发的 ukey 方法
     */
    static final String PDF_ADD_SIGN_METHOD = "pdf-sign_PdfAddSign";
    /**
     * 实际下发的 ukey 方法
     */
    static final String PDF_CHECK_SIGN_METHOD = "pdf-sign_PdfCheckSign";
    /**
     * 随附单据类型编码，按实际业务调整
     */
    static final String EDOC_CODE = "00000001";
    /**
     * u-key默认密码8个8
     */
    private static final String DEFAULT_PASSWORD = "88888888";
    /**
     * 根据实际情况替换
     */
    final String PDF_PATH = "doc/sign-pdf-test.pdf";

    @Autowired
    CertificateHandler certHandler;

    @Autowired
    SignHandler signHandler;

    @Test
    @DisplayName("海关PDF文件加签+验证签名结果")
    void signPdfFile() {
        Path pdfFile = resolvePdfFile();
        String destDir = pdfFile.getParent().toString();
        String fileName = pdfFile.getFileName().toString();
        String datetiem = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Map<String, Object> signArgsMap = new LinkedHashMap<>();
        signArgsMap.put("passwd", DEFAULT_PASSWORD);
        signArgsMap.put("filePath", destDir);
        signArgsMap.put("fileName", fileName);
        signArgsMap.put("datetiem", datetiem);
        signArgsMap.put("Edoc_code", EDOC_CODE);

        Args signArgs = signHandler.getUkeyResponseArgs(new UkeyRequest(PDF_ADD_SIGN_METHOD, signArgsMap));
        System.err.println("PDF加签结果: " + JSONUtil.toJsonStr(signArgs));
        System.err.println("X509Certificate: " + certHandler.getX509Certificate(CertificateHandler.SING_DATA_METHOD));

        Map<String, Object> verifyArgsMap = new LinkedHashMap<>();
        verifyArgsMap.put("filePath", pdfFile.toString());
        Args verifyArgs = signHandler.getUkeyResponseArgs(new UkeyRequest(PDF_CHECK_SIGN_METHOD, verifyArgsMap));
        System.err.println("PDF验签结果: " + JSONUtil.toJsonStr(verifyArgs));
    }

    @Test
    @DisplayName("海关PDF Base64内容加签+验证签名结果")
    void signPdfFileContent() throws Exception {
        Path pdfFile = resolvePdfFile();
        String fileContent = Base64.encode(Files.readAllBytes(pdfFile));
        String datetiem = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Map<String, Object> signArgsMap = new LinkedHashMap<>();
        signArgsMap.put("fileContent", fileContent);
        signArgsMap.put("datetiem", datetiem);
        signArgsMap.put("Edoc_code", EDOC_CODE);
        signArgsMap.put("passwd", DEFAULT_PASSWORD);

        Args signArgs = signHandler.getUkeyResponseArgs(new UkeyRequest(PDF_ADD_SIGN_METHOD, signArgsMap));
        System.err.println("PDF加签结果: " + JSONUtil.toJsonStr(signArgs));
        System.err.println("X509Certificate: " + certHandler.getX509Certificate(CertificateHandler.SING_DATA_METHOD));

        String signedPdfBase64 = signArgs.getData().isEmpty() ? fileContent : signArgs.getData().get(0);
        Map<String, Object> verifyArgsMap = new LinkedHashMap<>();
        verifyArgsMap.put("fileContent", signedPdfBase64);
        Args verifyArgs = signHandler.getUkeyResponseArgs(new UkeyRequest(PDF_CHECK_SIGN_METHOD, verifyArgsMap));
        System.err.println("PDF验签结果: " + JSONUtil.toJsonStr(verifyArgs));
    }

    private Path resolvePdfFile() {
        Path fromCwd = Path.of(PDF_PATH);
        if (Files.isRegularFile(fromCwd)) {
            return fromCwd.toAbsolutePath().normalize();
        }
        Path fromModuleParent = Path.of("..").resolve(PDF_PATH);
        if (Files.isRegularFile(fromModuleParent)) {
            return fromModuleParent.toAbsolutePath().normalize();
        }
        throw new IllegalStateException("未找到测试 PDF: " + PDF_PATH);
    }
}
