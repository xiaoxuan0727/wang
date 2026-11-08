package edu.sustech.xiangqi.model;

/**
 * 仕 / 士：每次在九宫内斜走一格。
 */
public class AdvisorPiece extends Piece {
    public AdvisorPiece(boolean red) {
        super(PieceType.ADVISOR, red);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        // 行和列必须都变化一格，不能直走、原地停留或跨越多格
        if (Math.abs(toRow - fromRow) != 1 || Math.abs(toCol - fromCol) != 1) {
            return false;
        }

        // 列必须在九宫的 3~5 列内
        if (toCol < 3 || toCol > 5) {
            return false;
        }

        // 红方九宫是 7~9 行，黑方九宫是 0~2 行
        return isRed() ? toRow >= 7 && toRow <= 9 : toRow >= 0 && toRow <= 2;
    }
}
