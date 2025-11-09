package com.company.wechat.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 签名验证工具测试
 */
class WechatSignUtilTest {

    @Test
    void testComputeSignature() {
        String token = "test_token";
        String timestamp = "1234567890";
        String nonce = "test_nonce";
        String encrypt = "test_encrypt";

        String signature = WechatSignUtil.computeSignature(token, timestamp, nonce, encrypt);
        
        assertNotNull(signature);
        assertEquals(40, signature.length()); // SHA-1结果是40位16进制字符串
    }

    @Test
    void testVerifySignature() {
        String token = "test_token";
        String timestamp = "1234567890";
        String nonce = "test_nonce";
        String encrypt = "test_encrypt";

        // 先计算签名
        String signature = WechatSignUtil.computeSignature(token, timestamp, nonce, encrypt);
        
        // 验证签名
        boolean valid = WechatSignUtil.verifySignature(token, timestamp, nonce, encrypt, signature);
        
        assertTrue(valid);
    }

    @Test
    void testVerifySignatureFailed() {
        String token = "test_token";
        String timestamp = "1234567890";
        String nonce = "test_nonce";
        String encrypt = "test_encrypt";
        String wrongSignature = "wrong_signature";

        boolean valid = WechatSignUtil.verifySignature(token, timestamp, nonce, encrypt, wrongSignature);
        
        assertFalse(valid);
    }

    @Test
    void testSignatureConsistency() {
        String token = "test_token";
        String timestamp = "1234567890";
        String nonce = "test_nonce";
        String encrypt = "test_encrypt";

        // 多次计算应该得到相同的结果
        String signature1 = WechatSignUtil.computeSignature(token, timestamp, nonce, encrypt);
        String signature2 = WechatSignUtil.computeSignature(token, timestamp, nonce, encrypt);
        
        assertEquals(signature1, signature2);
    }
}

