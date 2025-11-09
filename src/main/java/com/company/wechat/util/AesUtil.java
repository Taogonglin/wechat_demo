package com.company.wechat.util;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/**
 * AES加解密工具类
 * 用于企业微信消息的加解密
 * 
 * @author Company
 */
public class AesUtil {

    private static final String ALGORITHM = "AES/CBC/NoPadding";

    private final byte[] aesKey;
    private final String corpId;

    public AesUtil(String encodingAesKey, String corpId) {
        if (encodingAesKey.length() != 43) {
            throw new IllegalArgumentException("EncodingAESKey长度必须为43字符");
        }
        this.corpId = corpId;
        this.aesKey = Base64.decodeBase64(encodingAesKey + "=");
    }

    /**
     * 解密消息
     */
    public String decrypt(String encryptedMsg) throws Exception {
        byte[] original = Base64.decodeBase64(encryptedMsg);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decrypted = cipher.doFinal(original);

        // 去除补位字符
        byte[] bytes = decode(decrypted);

        // 分离16位随机字符串,网络字节序和corpId
        byte[] networkOrder = Arrays.copyOfRange(bytes, 16, 20);

        int xmlLength = bytesToInt(networkOrder);

        String xmlContent = new String(Arrays.copyOfRange(bytes, 20, 20 + xmlLength), StandardCharsets.UTF_8);
        String fromCorpId = new String(Arrays.copyOfRange(bytes, 20 + xmlLength, bytes.length), StandardCharsets.UTF_8);

        // 校验corpId
        if (!corpId.equals(fromCorpId)) {
            throw new IllegalArgumentException("CorpId校验失败");
        }

        return xmlContent;
    }

    /**
     * 加密消息
     */
    public String encrypt(String randomStr, String text) throws Exception {
        byte[] randomBytes = randomStr.getBytes(StandardCharsets.UTF_8);
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] corpIdBytes = corpId.getBytes(StandardCharsets.UTF_8);
        byte[] networkBytesOrder = intToBytes(textBytes.length);

        byte[] unencrypted = new byte[randomBytes.length + networkBytesOrder.length + textBytes.length + corpIdBytes.length];
        System.arraycopy(randomBytes, 0, unencrypted, 0, randomBytes.length);
        System.arraycopy(networkBytesOrder, 0, unencrypted, randomBytes.length, networkBytesOrder.length);
        System.arraycopy(textBytes, 0, unencrypted, randomBytes.length + networkBytesOrder.length, textBytes.length);
        System.arraycopy(corpIdBytes, 0, unencrypted, randomBytes.length + networkBytesOrder.length + textBytes.length, corpIdBytes.length);

        // 补位
        byte[] padded = encode(unencrypted);

        // 加密
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encrypted = cipher.doFinal(padded);

        return Base64.encodeBase64String(encrypted);
    }

    /**
     * 随机生成16位字符串
     */
    public static String getRandomStr() {
        String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    /**
     * 将整数转换为4字节网络字节序
     */
    private byte[] intToBytes(int number) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (number >> 24 & 0xFF);
        bytes[1] = (byte) (number >> 16 & 0xFF);
        bytes[2] = (byte) (number >> 8 & 0xFF);
        bytes[3] = (byte) (number & 0xFF);
        return bytes;
    }

    /**
     * 4字节网络字节序转换为整数
     */
    private int bytesToInt(byte[] bytes) {
        int number = 0;
        for (int i = 0; i < 4; i++) {
            number <<= 8;
            number |= (bytes[i] & 0xFF);
        }
        return number;
    }

    /**
     * 补位
     */
    private byte[] encode(byte[] text) {
        int blockSize = 32;
        int textLength = text.length;
        int amountToPad = blockSize - (textLength % blockSize);
        if (amountToPad == 0) {
            amountToPad = blockSize;
        }
        byte[] padChr = new byte[amountToPad];
        for (int i = 0; i < amountToPad; i++) {
            padChr[i] = (byte) amountToPad;
        }
        byte[] result = new byte[textLength + amountToPad];
        System.arraycopy(text, 0, result, 0, textLength);
        System.arraycopy(padChr, 0, result, textLength, amountToPad);
        return result;
    }

    /**
     * 去除补位
     */
    private byte[] decode(byte[] decrypted) {
        int pad = decrypted[decrypted.length - 1];
        if (pad < 1 || pad > 32) {
            pad = 0;
        }
        return Arrays.copyOfRange(decrypted, 0, decrypted.length - pad);
    }
}
