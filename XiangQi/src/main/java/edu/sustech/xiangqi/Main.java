package edu.sustech.xiangqi;

import edu.sustech.xiangqi.controller.ChessController;
import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.view.ChessFrame;

import javax.swing.SwingUtilities;

// 程序入口
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Board board = new Board();
                ChessFrame frame = new ChessFrame(board);
                ChessController controller = new ChessController(board, frame.getBoardPanel());

                frame.getBoardPanel().addMouseListener(controller);
                frame.setVisible(true);
            }
        });
    }
}
