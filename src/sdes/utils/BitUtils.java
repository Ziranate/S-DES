package sdes.utils;

import java.nio.charset.StandardCharsets;

/**
 * 二进制处理工具类
 */
public class BitUtils {

    /**
     * 将布尔数组转换为二进制字符串
     */
    public static String toBinaryString(boolean[] bits) {
        StringBuilder sb = new StringBuilder(bits.length);
        for (boolean bit : bits) {
            sb.append(bit ? '1' : '0');
        }
        return sb.toString();
    }

    /**
     * 将二进制字符串转换为布尔数组
     */
    public static boolean[] fromBinaryString(String binaryString) {
        boolean[] bits = new boolean[binaryString.length()];
        for (int i = 0; i < binaryString.length(); i++) {
            if (binaryString.charAt(i) != '0' && binaryString.charAt(i) != '1') {
                throw new IllegalArgumentException("输入的不是有效的二进制字符串！");
            }
            bits[i] = binaryString.charAt(i) == '1';
        }
        return bits;
    }

    /**
     * 将ASCII字符串转换为二进制字符串
     */
    public static String asciiToBinary(String asciiString) {
        byte[] bytes = asciiString.getBytes(StandardCharsets.US_ASCII);
        StringBuilder binary = new StringBuilder();
        for (byte b : bytes) {
            // 每个ASCII字符转为8位二进制
            String binStr = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            binary.append(binStr);
        }
        return binary.toString();
    }

    /**
     * 将二进制字符串转换为ASCII字符串（可能产生乱码）
     */
    public static String binaryToAscii(String binaryString) {
        if (binaryString.length() % 8 != 0) {
            throw new IllegalArgumentException("二进制字符串长度必须是8的倍数。");
        }
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < binaryString.length(); i += 8) {
            String byteString = binaryString.substring(i, i + 8);
            int charCode = Integer.parseInt(byteString, 2);
            ascii.append((char) charCode);
        }
        return ascii.toString();
    }
}
