package sdes.core;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * S-DES 核心算法实现类
 */
public class SdesAlgorithm {

    // 2.3.1 密钥扩展置换
    private static final int[] P10 = {3, 5, 2, 7, 4, 10, 1, 9, 8, 6};
    private static final int[] P8 = {6, 3, 7, 4, 8, 5, 10, 9};

    // 2.3.2 初始置换盒
    private static final int[] IP = {2, 6, 3, 1, 4, 8, 5, 7};
    // 2.3.3 最终置换盒
    private static final int[] IP_INV = {4, 1, 3, 5, 7, 2, 8, 6};

    // 2.3.4 轮函数F
    private static final int[] EP = {4, 1, 2, 3, 2, 3, 4, 1};
    private static final int[] P4 = {2, 4, 3, 1};
    private static final int[][][] S_BOX = {
            {{1, 0, 3, 2}, {3, 2, 1, 0}, {0, 2, 1, 3}, {3, 1, 0, 2}}, // S0
            {{0, 1, 2, 3}, {2, 3, 1, 0}, {3, 0, 1, 2}, {2, 1, 0, 3}}  // S1
    };

    private boolean[] key1;
    private boolean[] key2;

    /**
     * 构造函数，需要一个10位的密钥来初始化。
     * @param key 10位密钥的布尔数组表示。
     */
    public SdesAlgorithm(boolean[] key) {
        if (key.length != 10) {
            throw new IllegalArgumentException("密钥长度必须为10位！");
        }
        generateKeys(key);
    }

    // --- 公共方法 ---

    /**
     * 加密一个8位的块。
     * @param plaintext 8位明文的布尔数组。
     * @return 8位密文的布尔数组。
     */
    public boolean[] encrypt(boolean[] plaintext) {
        if (plaintext.length != 8) {
            throw new IllegalArgumentException("明文分组长度必须为8位！");
        }
        boolean[] temp = permute(plaintext, IP);
        temp = functionFk(temp, key1);
        temp = switchHalves(temp);
        temp = functionFk(temp, key2);
        return permute(temp, IP_INV);
    }

    /**
     * 解密一个8位的块。
     * @param ciphertext 8位密文的布尔数组。
     * @return 8位明文的布尔数组。
     */
    public boolean[] decrypt(boolean[] ciphertext) {
        if (ciphertext.length != 8) {
            throw new IllegalArgumentException("密文分组长度必须为8位！");
        }
        boolean[] temp = permute(ciphertext, IP);
        temp = functionFk(temp, key2); // 注意：解密时先用k2
        temp = switchHalves(temp);
        temp = functionFk(temp, key1); // 再用k1
        return permute(temp, IP_INV);
    }

    // --- 内部核心算法步骤 ---

    /**
     * 密钥生成过程。
     * $k_i = P_8(Shift^i(P_{10}(K)))$
     * @param key 10位原始密钥。
     */
    private void generateKeys(boolean[] key) {
        // P10置换
        boolean[] p10Key = permute(key, P10);

        // 分割成左右两部分
        boolean[] left = Arrays.copyOfRange(p10Key, 0, 5);
        boolean[] right = Arrays.copyOfRange(p10Key, 5, 10);

        // 生成k1
        boolean[] ls1Left = leftShift(left, 1);
        boolean[] ls1Right = leftShift(right, 1);
        boolean[] combined1 = combine(ls1Left, ls1Right);
        this.key1 = permute(combined1, P8);

        // 生成k2
        boolean[] ls2Left = leftShift(ls1Left, 2); // 注意是对ls1的结果进行二次移位
        boolean[] ls2Right = leftShift(ls1Right, 2);
        boolean[] combined2 = combine(ls2Left, ls2Right);
        this.key2 = permute(combined2, P8);
    }

    /**
     * 轮函数 F_k
     * @param data 8位输入数据
     * @param subKey 8位子密钥
     * @return 8位输出数据
     */
    private boolean[] functionFk(boolean[] data, boolean[] subKey) {
        boolean[] left = Arrays.copyOfRange(data, 0, 4);
        boolean[] right = Arrays.copyOfRange(data, 4, 8);
        boolean[] resultOfF = functionF(right, subKey);
        boolean[] newLeft = xor(left, resultOfF);
        return combine(newLeft, right);
    }

    /**
     * 轮函数中的 F 部分 (E/P -> S-Box -> P4)
     */
    private boolean[] functionF(boolean[] rightHalf, boolean[] subKey) {
        // E/P 扩展置换
        boolean[] expanded = permute(rightHalf, EP);
        // 与子密钥异或
        boolean[] xored = xor(expanded, subKey);

        // 分割成两部分，送入S-Box
        boolean[] s0Input = Arrays.copyOfRange(xored, 0, 4);
        boolean[] s1Input = Arrays.copyOfRange(xored, 4, 8);

        // S-Box查找
        boolean[] s0Output = sBoxLookup(s0Input, S_BOX[0]);
        boolean[] s1Output = sBoxLookup(s1Input, S_BOX[1]);

        // 合并S-Box输出
        boolean[] combinedS = combine(s0Output, s1Output);

        // P4置换
        return permute(combinedS, P4);
    }

    /**
     * S-Box 查找
     * @param input 4位输入
     * @param sbox 2x4的S-Box矩阵
     * @return 2位输出
     */
    private boolean[] sBoxLookup(boolean[] input, int[][] sbox) {
        int row = toDecimal(new boolean[]{input[0], input[3]});
        int col = toDecimal(new boolean[]{input[1], input[2]});
        int val = sbox[row][col];
        return toBinaryArray(val, 2);
    }

    /**
     * 交换左右4位。
     */
    private boolean[] switchHalves(boolean[] data) {
        boolean[] left = Arrays.copyOfRange(data, 0, 4);
        boolean[] right = Arrays.copyOfRange(data, 4, 8);
        return combine(right, left);
    }

    // --- 辅助工具方法 ---

    private boolean[] permute(boolean[] input, int[] table) {
        boolean[] output = new boolean[table.length];
        for (int i = 0; i < table.length; i++) {
            // 置换表的索引是从1开始的，数组是从0开始的
            output[i] = input[table[i] - 1];
        }
        return output;
    }

    /**
     * 循环左移。
     * n=1代表LS-1，n=2代表LS-2。
     */
    private boolean[] leftShift(boolean[] input, int n) {
        boolean[] output = new boolean[input.length];
        // 修正n的值，使其在数组长度范围内
        int shift = n % input.length;
        for (int i = 0; i < input.length; i++) {
            output[i] = input[(i + shift) % input.length];
        }
        return output;
    }

    private boolean[] xor(boolean[] a, boolean[] b) {
        boolean[] result = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] ^ b[i];
        }
        return result;
    }

    private boolean[] combine(boolean[] left, boolean[] right) {
        boolean[] result = new boolean[left.length + right.length];
        System.arraycopy(left, 0, result, 0, left.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }

    private int toDecimal(boolean[] bits) {
        int decimal = 0;
        for (int i = 0; i < bits.length; i++) {
            if (bits[i]) {
                decimal += Math.pow(2, bits.length - 1 - i);
            }
        }
        return decimal;
    }

    private boolean[] toBinaryArray(int number, int length) {
        boolean[] binary = new boolean[length];
        for (int i = length - 1; i >= 0; i--) {
            binary[i] = (number & 1) == 1;
            number >>= 1;
        }
        return binary;
    }

    // --- 静态方法用于暴力破解 ---

    /**
     * 暴力破解密钥（多线程）
     * @param plainText 已知明文 (8位)
     * @param cipherText 已知密文 (8位)
     * @return 找到的10位密钥，如果找不到则返回null
     */
    public static String bruteForce(String plainText, String cipherText) {
        final boolean[] plainBits = toBinaryArray(plainText);
        final boolean[] cipherBits = toBinaryArray(cipherText);
        final AtomicReference<String> foundKey = new AtomicReference<>(null);

        // 10位密钥空间为 2^10 = 1024
        int totalKeys = 1024;

        // 使用多线程提升效率
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < totalKeys; i++) {
            final int keyCandidate = i;
            executor.submit(() -> {
                // 如果已经找到密钥，就不再继续执行
                if (foundKey.get() != null) {
                    return;
                }

                String keyStr = String.format("%10s", Integer.toBinaryString(keyCandidate)).replace(' ', '0');
                boolean[] keyBits = toBinaryArray(keyStr);

                SdesAlgorithm sdes = new SdesAlgorithm(keyBits);
                boolean[] encrypted = sdes.encrypt(plainBits);

                if (Arrays.equals(encrypted, cipherBits)) {
                    // 使用AtomicReference确保线程安全
                    foundKey.compareAndSet(null, keyStr);
                }
            });
        }

        executor.shutdown();
        try {
            // 等待所有任务完成
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return foundKey.get();
    }


    private static boolean[] toBinaryArray(String binaryString) {
        boolean[] bits = new boolean[binaryString.length()];
        for (int i = 0; i < binaryString.length(); i++) {
            bits[i] = binaryString.charAt(i) == '1';
        }
        return bits;
    }

    /**
     * 暴力破解，查找并返回所有可能的密钥（多线程）
     * @param plainText 已知明文 (8位二进制字符串)
     * @param cipherText 已知密文 (8位二进制字符串)
     * @return 一个包含所有匹配密钥的字符串列表。如果找不到，列表为空。
     */
    public static List<String> findAllBruteForceKeys(String plainText, String cipherText) {
        final boolean[] plainBits = toBinaryArray(plainText);
        final boolean[] cipherBits = toBinaryArray(cipherText);

        // 使用线程安全的列表来收集所有找到的密钥
        final List<String> foundKeys = new CopyOnWriteArrayList<>();

        int totalKeys = 1024; // 2^10
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < totalKeys; i++) {
            final int keyCandidate = i;
            executor.submit(() -> {
                String keyStr = String.format("%10s", Integer.toBinaryString(keyCandidate)).replace(' ', '0');
                boolean[] keyBits = toBinaryArray(keyStr);

                // 注意：每次循环都要创建一个新的SdesAlgorithm实例
                // 因为它的子密钥是和实例绑定的
                SdesAlgorithm sdes = new SdesAlgorithm(keyBits);
                boolean[] encrypted = sdes.encrypt(plainBits);

                if (Arrays.equals(encrypted, cipherBits)) {
                    foundKeys.add(keyStr); // 找到一个就添加到列表
                }
            });
        }

        executor.shutdown();
        try {
            // 等待所有线程执行完毕
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                System.err.println("暴力破解超时！");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return foundKeys;
    }

}
