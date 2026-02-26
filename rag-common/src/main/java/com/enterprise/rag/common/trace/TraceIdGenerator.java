package com.enterprise.rag.common.trace;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TraceId 生成器
 * 生成唯一的请求追踪标识符
 */
public final class TraceIdGenerator {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private TraceIdGenerator() {
        // Utility class, prevent instantiation
    }

    /**
     * 生成唯一的 TraceId
     * 格式: 时间戳(13位) + 随机数(19位) = 32位十六进制字符串
     * 
     * @return 32位十六进制字符串格式的 TraceId
     */
    public static String generate() {
        long timestamp = System.currentTimeMillis();
        long random = ThreadLocalRandom.current().nextLong();
        
        StringBuilder sb = new StringBuilder(32);
        
        // 添加时间戳部分 (13位十六进制)
        appendHex(sb, timestamp, 13);
        
        // 添加随机数部分 (16位十六进制)
        appendHex(sb, random, 16);
        
        // 补充3位随机字符确保32位
        int extra = ThreadLocalRandom.current().nextInt(0x1000);
        appendHex(sb, extra, 3);
        
        return sb.toString();
    }

    /**
     * 生成基于 UUID 的 TraceId
     * 
     * @return 32位十六进制字符串格式的 TraceId (无连字符的 UUID)
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 验证 TraceId 格式是否有效
     * 
     * @param traceId 待验证的 TraceId
     * @return true 如果格式有效
     */
    public static boolean isValid(String traceId) {
        if (traceId == null || traceId.length() != 32) {
            return false;
        }
        for (int i = 0; i < traceId.length(); i++) {
            char c = traceId.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    private static void appendHex(StringBuilder sb, long value, int length) {
        char[] result = new char[length];
        for (int i = length - 1; i >= 0; i--) {
            result[i] = HEX_CHARS[(int) (value & 0xF)];
            value >>>= 4;
        }
        sb.append(result);
    }
}
