package edu.sustech.xiangqi;

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.storage.AuthService;
import edu.sustech.xiangqi.storage.User;
import edu.sustech.xiangqi.view.ChessFrame;
import edu.sustech.xiangqi.view.LoginDialog;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;

/**
 * 程序入口。
 *
 * <p>Swing 是单线程模型：所有界面操作都应在事件分派线程（EDT）上执行，
 * 因此使用 {@link SwingUtilities#invokeLater(Runnable)} 启动程序。</p>
 *
 * <p>启动流程：读取账号文件 → 显示登录窗口 → 登录/游客进入 → 打开棋局窗口。
 * 在棋局窗口切换账号时，会销毁旧窗口并重新显示登录窗口。</p>
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::startApplication);
    }

    private static void startApplication() {
        AuthService authService;
        try {
            authService = new AuthService();
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(null,
                    "无法读取账号数据：\n" + exception.getMessage(),
                    "启动失败", JOptionPane.ERROR_MESSAGE);
            return;
        }
        showLoginAndGame(authService);
    }

    private static void showLoginAndGame(AuthService authService) {
        User user = LoginDialog.showDialog(null, authService);
        if (user == null) {
            System.exit(0); // 用户关闭登录窗口或点击退出
            return;
        }

        Board board = new Board();
        ChessFrame frame = new ChessFrame(
                board,
                user,
                () -> SwingUtilities.invokeLater(() -> showLoginAndGame(authService)));
        frame.setVisible(true);
    }
}
