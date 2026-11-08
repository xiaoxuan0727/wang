package edu.sustech.xiangqi.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 某一时刻棋局的完整快照。
 *
 * <p>存档系统需要把棋盘、行棋方、状态和着法历史一起保存；
 * 用快照对象传递这些数据，可以避免读档代码直接操作 Board 的内部数组。</p>
 *
 * <p>构造函数会复制数组和列表，因此外部修改原数组不会影响快照。</p>
 */
public final class GameSnapshot {
    private final Piece[][] pieces;
    private final boolean redToMove;
    private final GameStatus status;
    private final String endReason;
    private final List<Move> moveHistory;

    public GameSnapshot(Piece[][] pieces, boolean redToMove, GameStatus status,
                        String endReason, List<Move> moveHistory) {
        if (pieces == null || pieces.length != Board.ROWS
                || pieces[0] == null || pieces[0].length != Board.COLS) {
            throw new IllegalArgumentException("棋盘数组必须是 10 行 9 列");
        }
        this.pieces = copyGrid(pieces);
        this.redToMove = redToMove;
        this.status = status == null ? GameStatus.ONGOING : status;
        this.endReason = endReason == null ? "" : endReason;
        this.moveHistory = new ArrayList<>(moveHistory == null ? List.of() : moveHistory);
    }

    private static Piece[][] copyGrid(Piece[][] source) {
        Piece[][] copy = new Piece[Board.ROWS][Board.COLS];
        for (int row = 0; row < Board.ROWS; row++) {
            if (source[row] == null || source[row].length != Board.COLS) {
                throw new IllegalArgumentException("棋盘数组必须是 10 行 9 列");
            }
            System.arraycopy(source[row], 0, copy[row], 0, Board.COLS);
        }
        return copy;
    }

    public Piece getPieceAt(int row, int col) {
        if (row < 0 || row >= Board.ROWS || col < 0 || col >= Board.COLS) {
            return null;
        }
        return pieces[row][col];
    }

    public Piece[][] copyPieces() {
        return copyGrid(pieces);
    }

    public boolean isRedToMove() {
        return redToMove;
    }

    public GameStatus getStatus() {
        return status;
    }

    public String getEndReason() {
        return endReason;
    }

    public List<Move> getMoveHistory() {
        return List.copyOf(moveHistory);
    }
}
