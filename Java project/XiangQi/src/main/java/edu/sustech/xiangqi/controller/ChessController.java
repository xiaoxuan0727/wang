package edu.sustech.xiangqi.controller;

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.Piece;
import edu.sustech.xiangqi.model.Position;
import edu.sustech.xiangqi.view.BoardPanel;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 棋盘交互控制器：把鼠标点击翻译成“选中棋子 / 尝试走棋”。
 *
 * <p>真正的规则全部在 {@link Board} 中，控制器不包含任何走法判断，
 * 这样界面与规则互相独立，改界面不会破坏规则。</p>
 */
public class ChessController extends MouseAdapter {
    private final Board board;
    private final BoardPanel boardPanel;
    private final Runnable onBoardChanged;

    private Position selected;

    public ChessController(Board board, BoardPanel boardPanel, Runnable onBoardChanged) {
        this.board = board;
        this.boardPanel = boardPanel;
        this.onBoardChanged = onBoardChanged;
        boardPanel.addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (!SwingUtilities.isLeftMouseButton(event)) {
            return;
        }

        Point intersection = boardPanel.getIntersectionAt(event.getX(), event.getY());
        if (intersection != null) {
            handleClick(intersection.y, intersection.x);
        }
    }

    private void handleClick(int row, int col) {
        if (board.isGameOver()) {
            return;
        }

        // 再次点击同一个棋子：取消选择
        if (selected != null && selected.getRow() == row && selected.getCol() == col) {
            clearSelection();
            return;
        }

        Piece clickedPiece = board.getPieceAt(row, col);

        if (selected == null) {
            // 只能选择当前行棋方的棋子
            if (clickedPiece != null && clickedPiece.isRed() == board.isRedToMove()) {
                select(row, col);
            }
            return;
        }

        Piece selectedPiece = board.getPieceAt(selected.getRow(), selected.getCol());
        if (selectedPiece == null) {
            clearSelection();
            return;
        }

        // 点击另一枚己方棋子：切换选择
        if (clickedPiece != null && clickedPiece.isRed() == selectedPiece.isRed()) {
            select(row, col);
            return;
        }

        // 尝试移动。非法移动时保留选择，方便用户重新选择目标
        boolean moved = board.tryMove(selected.getRow(), selected.getCol(), row, col);
        if (moved) {
            clearSelection();
            onBoardChanged.run();
        }
    }

    private void select(int row, int col) {
        selected = new Position(row, col);
        List<Position> targets = board.getLegalTargets(row, col);
        boardPanel.showSelection(row, col, targets);
    }

    /** 清除选中状态。重新开局或读档后由窗口调用。 */
    public void clearSelection() {
        selected = null;
        boardPanel.clearSelection();
    }
}
