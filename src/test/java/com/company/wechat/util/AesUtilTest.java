package com.company.wechat.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AES加解密工具测试
 */
class AesUtilTest {

    private AesUtil aesUtil;
    private String corpId = "test_corp_id";
    // 43位的EncodingAESKey（实际使用时需要是Base64编码）
    private String encodingAesKey = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

    @BeforeEach
    void setUp() {
        aesUtil = new AesUtil(encodingAesKey, corpId);
    }

    @Test
    void testGetRandomStr() {
        String random1 = AesUtil.getRandomStr();
        String random2 = AesUtil.getRandomStr();
        
        assertNotNull(random1);
        assertNotNull(random2);
        assertEquals(16, random1.length());
        assertEquals(16, random2.length());
        assertNotEquals(random1, random2); // 每次生成应该不同
    }

    @Test
    void testEncryptAndDecrypt() throws Exception {
        String originalText = "<xml><ToUserName><![CDATA[test]]></ToUserName></xml>";
        String randomStr = AesUtil.getRandomStr();
        
        // 加密
        String encrypted = aesUtil.encrypt(randomStr, originalText);
        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        
        // 解密
        String decrypted = aesUtil.decrypt(encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void testDecryptWithWrongCorpId() {
        String wrongCorpId = "wrong_corp_id";
        AesUtil wrongAesUtil = new AesUtil(encodingAesKey, wrongCorpId);
        
        String originalText = "<xml><test>data</test></xml>";
        String randomStr = AesUtil.getRandomStr();
        
        assertThrows(Exception.class, () -> {
            // 使用正确的corpId加密
            String encrypted = aesUtil.encrypt(randomStr, originalText);
            // 使用错误的corpId解密，应该抛出异常
            wrongAesUtil.decrypt(encrypted);
        });
    }

    @Test
    void testEncryptEmptyString() throws Exception {
        String emptyText = "";
        String randomStr = AesUtil.getRandomStr();
        
        String encrypted = aesUtil.encrypt(randomStr, emptyText);
        String decrypted = aesUtil.decrypt(encrypted);
        
        assertEquals(emptyText, decrypted);
    }

    @Test
    void testEncryptLongText() throws Exception {
        // 测试较长的文本
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("This is a test message. ");
        }
        
        String randomStr = AesUtil.getRandomStr();
        String encrypted = aesUtil.encrypt(randomStr, longText.toString());
        String decrypted = aesUtil.decrypt(encrypted);
        
        assertEquals(longText.toString(), decrypted);
    }

    @Test
    void testInvalidEncodingAesKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            // EncodingAESKey长度不是43
            new AesUtil("short_key", corpId);
        });
    }
}

