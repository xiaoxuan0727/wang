package edu.sustech.xiangqi.view;

import edu.sustech.xiangqi.storage.AuthService;
import edu.sustech.xiangqi.storage.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;
import java.util.Arrays;

/**
 * 登录 / 注册对话框。支持游客模式。
 *
 * <p>只有登录或注册成功后 {@code showDialog} 才返回 User；
 * 关闭窗口或点击退出时返回 null。</p>
 */
public final class LoginDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final AuthService authService;

    private final JTextField loginUsernameField = new JTextField(16);
    private final JPasswordField loginPasswordField = new JPasswordField(16);
    private final JButton loginButton = new JButton("登录");

    private final JTextField registerUsernameField = new JTextField(16);
    private final JPasswordField registerPasswordField = new JPasswordField(16);
    private final JPasswordField registerConfirmField = new JPasswordField(16);
    private final JButton registerButton = new JButton("注册并登录");

    private User result;

    private LoginDialog(Window owner, AuthService authService) {
        super(owner, "中国象棋 · 登录", ModalityType.APPLICATION_MODAL);
        this.authService = authService;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);

        getRootPane().setDefaultButton(loginButton);
    }

    /** 显示模态对话框并等待用户登录。 */
    public static User showDialog(Component parent, AuthService authService) {
        Window owner = parent instanceof Window window ? window : null;
        LoginDialog dialog = new LoginDialog(owner, authService);
        dialog.setVisible(true);
        return dialog.result;
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

        JPanel header = new JPanel();
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("中国象棋");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 28));
        JLabel subtitle = new JLabel("CS109 项目 Demo · 红方先行");
        subtitle.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        subtitle.setForeground(new Color(110, 100, 90));
        header.add(title);
        header.add(subtitle);
        root.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("登录", createLoginPanel());
        tabs.addTab("注册", createRegisterPanel());
        tabs.addChangeListener(event -> getRootPane().setDefaultButton(
                tabs.getSelectedIndex() == 0 ? loginButton : registerButton));
        root.add(tabs, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        JLabel guestHint = new JLabel("<html>游客可以试玩，但不能保存或读取存档。</html>");
        guestHint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        guestHint.setForeground(new Color(110, 100, 90));

        JPanel buttons = new JPanel();
        JButton guestButton = new JButton("游客进入");
        JButton exitButton = new JButton("退出");
        guestButton.addActionListener(event -> {
            result = User.guest();
            dispose();
        });
        exitButton.addActionListener(event -> {
            result = null;
            dispose();
        });
        buttons.add(guestButton);
        buttons.add(exitButton);

        footer.add(guestHint, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        loginButton.addActionListener(event -> doLogin());
        registerButton.addActionListener(event -> doRegister());
        loginPasswordField.addActionListener(event -> doLogin());
        registerConfirmField.addActionListener(event -> doRegister());

        return root;
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8));
        GridBagConstraints constraints = defaultConstraints();

        addRow(panel, constraints, 0, "用户名", loginUsernameField);
        addRow(panel, constraints, 1, "密码", loginPasswordField);

        constraints.gridy = 2;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        panel.add(loginButton, constraints);
        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        GridBagConstraints constraints = defaultConstraints();

        addRow(panel, constraints, 0, "用户名", registerUsernameField);
        addRow(panel, constraints, 1, "密码", registerPasswordField);
        addRow(panel, constraints, 2, "确认密码", registerConfirmField);

        JLabel hint = new JLabel("<html>用户名 2~20 位字母/数字/下划线，密码至少 4 位。</html>");
        hint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        hint.setForeground(new Color(120, 110, 100));
        constraints.gridy = 3;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        panel.add(hint, constraints);

        constraints.gridy = 4;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        panel.add(registerButton, constraints);
        return panel;
    }

    private static GridBagConstraints defaultConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 6, 5, 6);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        return constraints;
    }

    private static void addRow(JPanel panel, GridBagConstraints constraints,
                               int row, String labelText, JComponent field) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        panel.add(new JLabel(labelText), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        field.setPreferredSize(new Dimension(230, 28));
        field.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        panel.add(field, constraints);
    }

    private void doLogin() {
        String username = loginUsernameField.getText().trim();
        char[] password = loginPasswordField.getPassword();

        try {
            User user = authService.login(username, password);
            if (user == null) {
                JOptionPane.showMessageDialog(this,
                        "用户名或密码错误。",
                        "登录失败", JOptionPane.WARNING_MESSAGE);
                return;
            }
            result = user;
            loginPasswordField.setText("");
            dispose();
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this,
                    "读取账号数据失败：\n" + exception.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void doRegister() {
        String username = registerUsernameField.getText().trim();
        char[] password = registerPasswordField.getPassword();
        char[] confirm = registerConfirmField.getPassword();

        try {
            if (!Arrays.equals(password, confirm)) {
                JOptionPane.showMessageDialog(this,
                        "两次输入的密码不一致。",
                        "注册失败", JOptionPane.WARNING_MESSAGE);
                return;
            }

            User user = authService.register(username, password);
            JOptionPane.showMessageDialog(this,
                    "注册成功，已自动登录。",
                    "成功", JOptionPane.INFORMATION_MESSAGE);
            result = user;
            dispose();
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this,
                    exception.getMessage(),
                    "注册失败", JOptionPane.WARNING_MESSAGE);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this,
                    "保存账号数据失败：\n" + exception.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
    }
}
