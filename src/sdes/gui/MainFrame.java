package sdes.gui;

import sdes.core.SdesAlgorithm;
import sdes.utils.BitUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import javax.swing.SwingWorker;

public class MainFrame extends JFrame {

    private final JTextArea inputArea = new JTextArea(5, 40);
    private final JTextArea outputArea = new JTextArea(5, 40);
    private final JTextField keyField = new JTextField("1010000010", 20);
    private final JComboBox<String> modeComboBox = new JComboBox<>(new String[]{"二进制模式", "ASCII模式"});
    private final JButton encryptButton = new JButton("加密");
    private final JButton decryptButton = new JButton("解密");

    // --- 新增组件和状态变量 ---
    private final JButton copyCiphertextButton = new JButton("复制二进制密文");
    private String lastGeneratedBinaryCiphertext = ""; // 用于存储上一次加密生成的二进制密文

    // 扩展功能：暴力破解
    private final JTextField plainTextFieldBrute = new JTextField("01110010", 15);
    private final JTextField cipherTextFieldBrute = new JTextField(15);
    private final JButton bruteForceButton = new JButton("开始破解");
    private final JLabel bruteForceResultLabel = new JLabel("破解结果: ");

    public MainFrame() {
        super("S-DES 加解密程序");
        initComponents();
        layoutComponents();
        addListeners();

        setSize(550, 550); // 稍微增高以容纳新按钮
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        inputArea.setLineWrap(true);
        copyCiphertextButton.setEnabled(false); // 初始时不可用
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 中心面板：输入、输出、密钥
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        centerPanel.add(new JLabel("模式:"));
        modeComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(modeComboBox);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(new JLabel("密钥 (10-bit):"));
        keyField.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(keyField);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(new JLabel("输入 (明文/密文):"));
        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        inputScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(inputScrollPane);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // --- 输出区域的布局修改 ---
        JPanel outputHeaderPanel = new JPanel(new BorderLayout());
        outputHeaderPanel.add(new JLabel("输出:"), BorderLayout.CENTER);
        outputHeaderPanel.add(copyCiphertextButton, BorderLayout.EAST); // 将复制按钮放在右侧
        outputHeaderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(outputHeaderPanel);

        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(outputScrollPane);

        // 底部面板：操作按钮
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(encryptButton);
        bottomPanel.add(decryptButton);

        // 暴力破解面板
        JPanel bruteForcePanel = new JPanel();
        bruteForcePanel.setLayout(new BoxLayout(bruteForcePanel, BoxLayout.Y_AXIS));
        bruteForcePanel.setBorder(BorderFactory.createTitledBorder("第4/5关：暴力破解与密钥碰撞"));

        JPanel bruteForceTopRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bruteForceTopRow.add(new JLabel("明文(8-bit):"));
        bruteForceTopRow.add(plainTextFieldBrute);
        bruteForceTopRow.add(new JLabel("密文(8-bit):"));
        bruteForceTopRow.add(cipherTextFieldBrute);
        bruteForceTopRow.add(bruteForceButton);

        JPanel bruteForceBottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bruteForceBottomRow.add(bruteForceResultLabel);

        bruteForcePanel.add(bruteForceTopRow);
        bruteForcePanel.add(bruteForceBottomRow);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(mainPanel, BorderLayout.CENTER);
        contentPane.add(bruteForcePanel, BorderLayout.SOUTH);
    }


    private void addListeners() {
        encryptButton.addActionListener(e -> process(true));
        decryptButton.addActionListener(e -> process(false));
        bruteForceButton.addActionListener(e -> runBruteForce());

        // --- 为新按钮添加监听器 ---
        copyCiphertextButton.addActionListener(e -> {
            if (!lastGeneratedBinaryCiphertext.isEmpty()) {
                // 将存储的二进制密文复制到系统剪贴板
                StringSelection stringSelection = new StringSelection(lastGeneratedBinaryCiphertext);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                JOptionPane.showMessageDialog(this, "二进制密文已复制到剪贴板！", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private void process(boolean isEncrypt) {
        String keyText = keyField.getText().trim();
        String inputText = inputArea.getText().trim();
        String selectedMode = (String) modeComboBox.getSelectedItem();

        if (keyText.length() != 10 || !keyText.matches("[01]+")) {
            JOptionPane.showMessageDialog(this, "密钥必须是10位二进制数!", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            boolean[] keyBits = BitUtils.fromBinaryString(keyText);
            SdesAlgorithm sdes = new SdesAlgorithm(keyBits);
            StringBuilder outputBinary = new StringBuilder();

            if (isEncrypt) {
                // --- 加密逻辑 ---
                lastGeneratedBinaryCiphertext = ""; // 重置状态
                copyCiphertextButton.setEnabled(false); // 先禁用复制按钮

                if ("ASCII模式".equals(selectedMode)) {
                    for (char ch : inputText.toCharArray()) {
                        String block = String.format("%8s", Integer.toBinaryString(ch & 0xFF)).replace(' ', '0');
                        boolean[] resultBits = sdes.encrypt(BitUtils.fromBinaryString(block));
                        outputBinary.append(BitUtils.toBinaryString(resultBits));
                    }
                } else { // 二进制模式
                    if (!inputText.matches("[01]+") || inputText.length() % 8 != 0) {
                        JOptionPane.showMessageDialog(this, "二进制模式下，输入必须是8的倍数长度的二进制数!", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    for (int i = 0; i < inputText.length(); i += 8) {
                        String block = inputText.substring(i, i + 8);
                        boolean[] resultBits = sdes.encrypt(BitUtils.fromBinaryString(block));
                        outputBinary.append(BitUtils.toBinaryString(resultBits));
                    }
                }

                // 加密完成，更新状态和UI
                lastGeneratedBinaryCiphertext = outputBinary.toString();
                String asciiGarble = BitUtils.binaryToAscii(lastGeneratedBinaryCiphertext);
                outputArea.setText("密文二进制 (可用于解密):\n" + lastGeneratedBinaryCiphertext + "\n\n字符展示 (乱码):\n" + asciiGarble);
                copyCiphertextButton.setEnabled(true);

            } else {
                // --- 解密逻辑 (更智能) ---
                String binaryToDecrypt;
                // 智能判断：如果输入区为空，且我们刚生成了密文，就自动使用它
                if (inputText.isEmpty() && !lastGeneratedBinaryCiphertext.isEmpty()) {
                    binaryToDecrypt = lastGeneratedBinaryCiphertext;
                    inputArea.setText(binaryToDecrypt); // 帮用户填入输入框，更直观
                } else {
                    binaryToDecrypt = inputText;
                }

                if (!binaryToDecrypt.matches("[01]+") || binaryToDecrypt.length() % 8 != 0) {
                    JOptionPane.showMessageDialog(this,
                            "解密输入必须是8的倍数长度的二进制串！\n（加密后可直接点击解密，或手动粘贴二进制密文）",
                            "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                for (int i = 0; i < binaryToDecrypt.length(); i += 8) {
                    String block = binaryToDecrypt.substring(i, i + 8);
                    boolean[] resultBits = sdes.decrypt(BitUtils.fromBinaryString(block));
                    outputBinary.append(BitUtils.toBinaryString(resultBits));
                }

                if ("ASCII模式".equals(selectedMode)) {
                    outputArea.setText(BitUtils.binaryToAscii(outputBinary.toString()));
                } else {
                    outputArea.setText(outputBinary.toString());
                }
                lastGeneratedBinaryCiphertext = ""; // 解密后清空状态
                copyCiphertextButton.setEnabled(false);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "处理出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // runBruteForce 方法保持不变，这里省略以减少篇幅。
    // 请确保你使用的是之前包含耗时统计和查找所有密钥的版本。
    private void runBruteForce() {
        String plainTextManualInput = plainTextFieldBrute.getText();
        String keyText = keyField.getText();
        String selectedMode = (String) modeComboBox.getSelectedItem();

        String plainTextForBruteForce;
        // 根据当前模式决定如何获取用于破解的8位明文
        if ("ASCII模式".equals(selectedMode)) {
            if (plainTextManualInput.isEmpty()) {
                JOptionPane.showMessageDialog(this, "ASCII模式下，破解用明文不能为空!", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // 自动提取第一个字符，并转换为8位二进制
            char firstChar = plainTextManualInput.charAt(0);
            plainTextForBruteForce = String.format("%8s", Integer.toBinaryString(firstChar & 0xFF)).replace(' ', '0');
            plainTextFieldBrute.setText(plainTextForBruteForce);

        } else { // 二进制模式
            if (plainTextManualInput.length() != 8 || !plainTextManualInput.matches("[01]+")) {
                JOptionPane.showMessageDialog(this, "二进制模式下，破解用明文必须是8位二进制数!", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            plainTextForBruteForce = plainTextManualInput;
        }

        // 检查主密钥是否有效
        if (keyText.length() != 10 || !keyText.matches("[01]+")) {
            JOptionPane.showMessageDialog(this, "主密钥无效，无法生成用于破解的密文对!", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 1. 生成明密文对
        SdesAlgorithm sdesForTest = new SdesAlgorithm(BitUtils.fromBinaryString(keyText));
        boolean[] cipherBits = sdesForTest.encrypt(BitUtils.fromBinaryString(plainTextForBruteForce));
        String cipherText = BitUtils.toBinaryString(cipherBits);
        cipherTextFieldBrute.setText(cipherText);

        // 2. 准备开始破解并给出提示
        JOptionPane.showMessageDialog(this, "已生成明密文对 (" + plainTextForBruteForce + " -> " + cipherText + ")，即将开始破解...", "提示", JOptionPane.INFORMATION_MESSAGE);
        bruteForceResultLabel.setText("正在破解中，请稍候...");

        // 统计耗时
        final long startTime = System.currentTimeMillis();

        final String finalPlainText = plainTextForBruteForce;
        final String finalCipherText = cipherText;

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return SdesAlgorithm.findAllBruteForceKeys(finalPlainText, finalCipherText);
            }

            @Override
            protected void done() {
                try {
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    List<String> foundKeys = get();

                    if (!foundKeys.isEmpty()) {
                        StringBuilder resultText = new StringBuilder("<html><b>破解成功！</b><br>");
                        resultText.append("共找到 ").append(foundKeys.size()).append(" 个可能密钥：<br>");
                        for (int i = 0; i < foundKeys.size(); i++) {
                            resultText.append(foundKeys.get(i));
                            if ((i + 1) % 4 == 0 && i < foundKeys.size() - 1) {
                                resultText.append("<br>");
                            } else {
                                resultText.append("&nbsp;&nbsp;");
                            }
                        }
                        resultText.append("<br><font color='blue'>耗时: ").append(duration).append(" ms</font>");
                        resultText.append("</html>");
                        bruteForceResultLabel.setText(resultText.toString());
                    } else {
                        bruteForceResultLabel.setText("破解失败！未找到任何匹配的密钥。");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    bruteForceResultLabel.setText("<html><font color='red'>破解过程中发生错误。</font></html>");
                }
            }
        }.execute();
    }
}
