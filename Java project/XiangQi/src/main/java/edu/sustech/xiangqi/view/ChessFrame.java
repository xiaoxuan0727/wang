package edu.sustech.xiangqi.view;

import edu.sustech.xiangqi.controller.ChessController;
import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.GameStatus;
import edu.sustech.xiangqi.model.Move;
import edu.sustech.xiangqi.storage.InvalidSaveException;
import edu.sustech.xiangqi.storage.SaveManager;
import edu.sustech.xiangqi.storage.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 主窗口：组合棋盘、侧边信息栏、菜单和所有对局操作。
 *
 * <p>窗口负责“应用层动作”（新局、认输、和棋、存读档），
 * 棋盘点击交给 {@link ChessController}，规则判断交给 {@link Board}。</p>
 */
public final class ChessFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final Color RED = new Color(176, 36, 32);
    private static final Color BLACK = new Color(45, 45, 45);
    private static final Color HINT_COLOR = new Color(110, 100, 90);
    private static final Color CHECK_COLOR = new Color(190, 85, 20);
    private static final Color BOARD_BG = new Color(92, 63, 38);
    private static final Color SIDE_BG = new Color(250, 244, 232);

    private final Board board;
    private final User user;
    private final Runnable onSwitchAccount;
    private final BoardPanel boardPanel;
    private final ChessController controller;
    private final SaveManager saveManager = new SaveManager();

    private final JLabel userLabel = new JLabel();
    private final JLabel turnLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JTextArea lastMoveArea = new JTextArea(2, 22);
    private final JTextArea historyArea = new JTextArea();
    private final JButton resignButton = new JButton("认输");
    private final JButton drawButton = new JButton("提议和棋");
    private final JButton saveButton = new JButton("保存");
    private final JButton loadButton = new JButton("载入");

    /** 防止同一局结束消息重复弹窗。 */
    private String announcedResult;

    public ChessFrame(Board board, User user, Runnable onSwitchAccount) {
        super("中国象棋 · " + user);
        this.board = Objects.requireNonNull(board);
        this.user = Objects.requireNonNull(user);
        this.onSwitchAccount = Objects.requireNonNull(onSwitchAccount);

        this.boardPanel = new BoardPanel(board);
        this.controller = new ChessController(board, boardPanel, this::refreshView);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setJMenuBar(buildMenuBar());
        setContentPane(buildContent());
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);

        refreshView();
    }

    // ------------------------------------------------------------------
    // 界面搭建
    // ------------------------------------------------------------------

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout());

        JPanel boardWrapper = new JPanel(new GridBagLayout());
        boardWrapper.setBackground(BOARD_BG);
        boardWrapper.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        boardWrapper.add(boardPanel);
        root.add(boardWrapper, BorderLayout.CENTER);

        root.add(buildSidePanel(), BorderLayout.EAST);

        JLabel hint = new JLabel(
                "点击己方棋子，绿色圆点/圆环表示合法落点；橙色框表示上一步。",
                SwingConstants.CENTER);
        hint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        hint.setForeground(HINT_COLOR);
        hint.setOpaque(true);
        hint.setBackground(new Color(246, 235, 215));
        hint.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
        root.add(hint, BorderLayout.SOUTH);

        return root;
    }

    private JPanel buildSidePanel() {
        JPanel side = new JPanel(new BorderLayout(0, 12));
        side.setPreferredSize(new Dimension(340, 0));
        side.setBackground(SIDE_BG);
        side.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        userLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        userLabel.setForeground(HINT_COLOR);
        userLabel.setAlignmentX(LEFT_ALIGNMENT);

        turnLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 27));
        turnLabel.setAlignmentX(LEFT_ALIGNMENT);

        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lastMoveTitle = new JLabel("上一步");
        lastMoveTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        lastMoveTitle.setAlignmentX(LEFT_ALIGNMENT);

        lastMoveArea.setEditable(false);
        lastMoveArea.setOpaque(false);
        lastMoveArea.setLineWrap(true);
        lastMoveArea.setWrapStyleWord(true);
        lastMoveArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lastMoveArea.setForeground(new Color(75, 65, 55));
        lastMoveArea.setAlignmentX(LEFT_ALIGNMENT);
        lastMoveArea.setBorder(null);

        info.add(userLabel);
        info.add(Box.createVerticalStrut(10));
        info.add(turnLabel);
        info.add(Box.createVerticalStrut(5));
        info.add(statusLabel);
        info.add(Box.createVerticalStrut(12));
        info.add(new JSeparator());
        info.add(Box.createVerticalStrut(9));
        info.add(lastMoveTitle);
        info.add(Box.createVerticalStrut(3));
        info.add(lastMoveArea);
        side.add(info, BorderLayout.NORTH);

        historyArea.setEditable(false);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
        historyArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        historyArea.setForeground(new Color(60, 55, 50));
        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyScroll.setBorder(BorderFactory.createTitledBorder("着法记录（行,列）"));
        side.add(historyScroll, BorderLayout.CENTER);

        side.add(buildActionPanel(), BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildActionPanel() {
        JPanel actions = new JPanel(new GridLayout(2, 3, 8, 8));
        actions.setOpaque(false);

        JButton newGameButton = new JButton("新开一局");
        JButton switchButton = new JButton("切换账号");

        Font buttonFont = new Font("Microsoft YaHei", Font.PLAIN, 13);
        for (JButton button : new JButton[]{
                newGameButton, resignButton, drawButton, saveButton, loadButton, switchButton}) {
            button.setFont(buttonFont);
        }

        newGameButton.addActionListener(event -> newGame());
        resignButton.addActionListener(event -> resign());
        drawButton.addActionListener(event -> agreeDraw());
        saveButton.addActionListener(event -> saveGame());
        loadButton.addActionListener(event -> loadGame());
        switchButton.addActionListener(event -> switchAccount());

        actions.add(newGameButton);
        actions.add(resignButton);
        actions.add(drawButton);
        actions.add(saveButton);
        actions.add(loadButton);
        actions.add(switchButton);
        return actions;
    }

    private JMenuBar buildMenuBar() {
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JMenuBar menuBar = new JMenuBar();

        JMenu gameMenu = new JMenu("游戏");
        JMenuItem newItem = new JMenuItem("新开一局");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutMask));
        newItem.addActionListener(event -> newGame());

        JMenuItem resignItem = new JMenuItem("认输");
        resignItem.addActionListener(event -> resign());
        JMenuItem drawItem = new JMenuItem("提议和棋");
        drawItem.addActionListener(event -> agreeDraw());
        JMenuItem saveItem = new JMenuItem("保存棋局…");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask));
        saveItem.addActionListener(event -> saveGame());
        JMenuItem loadItem = new JMenuItem("载入棋局…");
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutMask));
        loadItem.addActionListener(event -> loadGame());
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(event -> System.exit(0));

        gameMenu.add(newItem);
        gameMenu.add(resignItem);
        gameMenu.add(drawItem);
        gameMenu.addSeparator();
        gameMenu.add(saveItem);
        gameMenu.add(loadItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        JMenu accountMenu = new JMenu("账号");
        JMenuItem currentUserItem = new JMenuItem("当前用户：" + user);
        currentUserItem.setEnabled(false);
        JMenuItem switchItem = new JMenuItem("切换账号…");
        switchItem.addActionListener(event -> switchAccount());
        accountMenu.add(currentUserItem);
        accountMenu.add(switchItem);

        JMenu helpMenu = new JMenu("帮助");
        JMenuItem rulesItem = new JMenuItem("象棋规则速查");
        rulesItem.addActionListener(event -> showRules());
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(event -> showAbout());
        helpMenu.add(rulesItem);
        helpMenu.add(aboutItem);

        menuBar.add(gameMenu);
        menuBar.add(accountMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    // ------------------------------------------------------------------
    // 刷新界面
    // ------------------------------------------------------------------

    public void refreshView() {
        boolean ongoing = board.getStatus() == GameStatus.ONGOING;
        boolean redTurn = board.isRedToMove();

        userLabel.setText(user.isGuest()
                ? "游客模式 · 存档功能已关闭"
                : "当前玩家：" + user.getUsername());

        if (ongoing) {
            if (board.isInCheck(redTurn)) {
                turnLabel.setText((redTurn ? "红方" : "黑方") + "行棋 · 被将军！");
                turnLabel.setForeground(CHECK_COLOR);
                statusLabel.setText("请立即应将。");
                statusLabel.setForeground(CHECK_COLOR);
            } else {
                turnLabel.setText((redTurn ? "红方" : "黑方") + "行棋");
                turnLabel.setForeground(redTurn ? RED : BLACK);
                statusLabel.setText("点击棋子，绿色标记为合法落点。");
                statusLabel.setForeground(HINT_COLOR);
            }
        } else {
            turnLabel.setText("对局结束");
            turnLabel.setForeground(new Color(70, 70, 70));
            statusLabel.setText(resultText());
            statusLabel.setForeground(winnerColor());
        }

        Move lastMove = board.getLastMove();
        lastMoveArea.setText(lastMove == null ? "—" : lastMove.describe());
        lastMoveArea.setCaretPosition(0);

        historyArea.setText(buildHistoryText());
        historyArea.setCaretPosition(0);

        resignButton.setEnabled(ongoing);
        drawButton.setEnabled(ongoing);
        saveButton.setEnabled(ongoing && !user.isGuest());
        loadButton.setEnabled(!user.isGuest());
        saveButton.setToolTipText(user.isGuest() ? "游客模式不能保存" : "保存当前对局");
        loadButton.setToolTipText(user.isGuest() ? "游客模式不能读档" : "读取存档");

        boardPanel.repaint();
        announceResultIfNeeded();
    }

    private String buildHistoryText() {
        List<Move> history = board.getMoveHistory();
        if (history.isEmpty()) {
            return "尚无着法。";
        }

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            text.append(String.format("%2d. ", i + 1))
                    .append(history.get(i).describe())
                    .append('\n');
        }
        return text.toString();
    }

    private String resultText() {
        return switch (board.getStatus()) {
            case RED_WIN -> "红方胜 · " + board.getEndReason();
            case BLACK_WIN -> "黑方胜 · " + board.getEndReason();
            case DRAW -> "和棋 · " + board.getEndReason();
            case ONGOING -> "对局进行中";
        };
    }

    private Color winnerColor() {
        return switch (board.getStatus()) {
            case RED_WIN -> RED;
            case BLACK_WIN -> BLACK;
            default -> new Color(55, 90, 140);
        };
    }

    private void announceResultIfNeeded() {
        if (board.getStatus() == GameStatus.ONGOING) {
            announcedResult = null;
            return;
        }
        if (!isShowing()) {
            return;
        }

        String result = resultText();
        if (!result.equals(announcedResult)) {
            announcedResult = result;
            JOptionPane.showMessageDialog(this, result, "对局结束",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ------------------------------------------------------------------
    // 对局操作
    // ------------------------------------------------------------------

    private void newGame() {
        boolean hasProgress = board.getStatus() == GameStatus.ONGOING
                && !board.getMoveHistory().isEmpty();
        if (hasProgress && !confirm("当前对局尚未结束，确定重新开始吗？未保存的进度将丢失。")) {
            return;
        }

        board.reset();
        controller.clearSelection();
        announcedResult = null;
        refreshView();
    }

    private void resign() {
        if (board.isGameOver()) {
            return;
        }
        boolean red = board.isRedToMove();
        if (!confirm((red ? "红方" : "黑方") + "确定认输吗？")) {
            return;
        }

        board.resign(red);
        controller.clearSelection();
        refreshView();
    }

    private void agreeDraw() {
        if (board.isGameOver()) {
            return;
        }
        if (!confirm("双方是否同意和棋？")) {
            return;
        }

        board.agreeDraw();
        controller.clearSelection();
        refreshView();
    }

    private void saveGame() {
        if (user.isGuest()) {
            showInfo("游客模式不能保存存档，请注册并登录后再使用。");
            return;
        }
        if (board.isGameOver()) {
            showInfo("只能保存进行中的对局。");
            return;
        }

        Integer slot = chooseSlot("保存棋局", "选择保存位置：");
        if (slot == null) {
            return;
        }

        try {
            saveManager.save(user.getUsername(), slot, board);
            showInfo("保存成功：\n" + saveManager.getSavePath(user.getUsername(), slot));
        } catch (IOException | IllegalArgumentException exception) {
            showError("保存失败：\n" + exception.getMessage());
        }
    }

    private void loadGame() {
        if (user.isGuest()) {
            showInfo("游客模式不能读取存档，请注册并登录后再使用。");
            return;
        }

        Integer slot = chooseSlot("载入棋局", "选择要载入的存档：");
        if (slot == null) {
            return;
        }

        if (board.getStatus() == GameStatus.ONGOING && !board.getMoveHistory().isEmpty()
                && !confirm("载入存档会覆盖当前对局，确定继续吗？")) {
            return;
        }

        try {
            saveManager.loadInto(user.getUsername(), slot, board);
            controller.clearSelection();
            announcedResult = null;
            refreshView();
            showInfo("读档成功。");
        } catch (FileNotFoundException exception) {
            showInfo("这个存档位还没有存档。");
        } catch (InvalidSaveException exception) {
            showError("存档无效或已损坏，无法读取：\n\n" + exception.getMessage());
        } catch (IOException | IllegalArgumentException exception) {
            showError("读取存档失败：\n" + exception.getMessage());
        }
    }

    private void switchAccount() {
        if (!confirm("确定退出当前账号并返回登录界面吗？未保存的进度将丢失。")) {
            return;
        }
        dispose();
        onSwitchAccount.run();
    }

    private Integer chooseSlot(String title, String message) {
        String[] options = new String[SaveManager.MAX_SLOTS];
        for (int i = 0; i < options.length; i++) {
            options[i] = (i + 1) + " 号存档";
        }

        String selected = (String) JOptionPane.showInputDialog(
                this, message, title,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (selected == null) {
            return null;
        }
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(selected)) {
                return i + 1;
            }
        }
        return null;
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(
                this, message, "请确认",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "提示",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "错误",
                JOptionPane.ERROR_MESSAGE);
    }

    private void showRules() {
        String rules = """
                中国象棋基本规则
                ====================
                1. 红方先行，双方轮流走一枚棋子，不能停着。
                2. 帅/将：九宫内直走一格；两将不能在同一列照面。
                3. 仕/士：九宫内斜走一格。
                4. 相/象：走“田”字，象眼被堵不能走，不能过河。
                5. 马：走“日”字，蹩马腿时不能走。
                6. 车：沿直线走任意格，路径上不能有棋子。
                7. 炮：不吃子时同车；吃子时必须恰好隔一个棋子（炮架）。
                8. 兵/卒：过河前只能前进；过河后可左右移动，永远不能后退。
                9. 被将军时必须应将，不能走出让己方将帅被攻击的棋。
                10. 无子可走的一方判负：被将军时是将死，否则是困毙。
                11. 可以认输，或双方同意和棋。

                操作：点击己方棋子，再点击绿色标记的交叉点落子。
                """;
        JTextArea area = new JTextArea(rules, 20, 46);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(520, 380));
        JOptionPane.showMessageDialog(this, scroll, "象棋规则速查",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "中国象棋 · CS109 项目基础 Demo\n\n"
                        + "Swing 图形界面 · 完整走子规则 · 将军/将死/困毙\n"
                        + "注册登录 · 本地存档 · 游客模式",
                "关于", JOptionPane.INFORMATION_MESSAGE);
    }
}
