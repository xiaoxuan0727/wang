package edu.sustech.xiangqi.controller;

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.Piece;
import edu.sustech.xiangqi.view.BoardPanel;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// 处理点击和选中状态
public class ChessController extends MouseAdapter {
    private final Board board;
    private final BoardPanel boardPanel;

    // 默认 -1 没有选中任何棋子
    private int selectedRow = -1;
    private int selectedCol = -1;

    public ChessController(Board board, BoardPanel boardPanel) {
        this.board = board;
        this.boardPanel = boardPanel;
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (!SwingUtilities.isLeftMouseButton(event)) {
            return;
        }

        Point position = boardPanel.getIntersectionAt(event.getX(), event.getY());
        if (position != null) {
            handleClick(position.y, position.x);
        }
    }

    private void handleClick(int row, int col) {
        if (row == selectedRow && col == selectedCol) {
            clearSelection();
            return;
        }

        Piece clickedPiece = board.getPieceAt(row, col);
        if (selectedRow == -1) {
            if (clickedPiece != null) {
                select(row, col);
            }
            return;
        }

        // 点击另一枚己方棋子时切换选择
        Piece selectedPiece = board.getPieceAt(selectedRow, selectedCol);
        if (clickedPiece != null && selectedPiece != null
                && clickedPiece.isRed() == selectedPiece.isRed()) {
            select(row, col);
            return;
        }

        // 移动失败时保留选择，用户可以继续选择其他落点
        if (board.tryMove(selectedRow, selectedCol, row, col)) {
            clearSelection();
        }
    }

    private void select(int row, int col) {
        selectedRow = row;
        selectedCol = col;
        boardPanel.showSelection(row, col);
    }

    private void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
        boardPanel.showSelection(-1, -1);
    }
}
