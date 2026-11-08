package edu.sustech.xiangqi.model;

/**
 * 相 / 象：斜走“田”字（两行两列），不能过河，且“象眼”被堵时不能走。
 */
public class ElephantPiece extends Piece {
    public ElephantPiece(boolean red) {
        super(PieceType.ELEPHANT, red);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        if (Math.abs(toRow - fromRow) != 2 || Math.abs(toCol - fromCol) != 2) {
            return false;
        }

        // 象眼：起点和终点正中间的那个交叉点
        int eyeRow = (fromRow + toRow) / 2;
        int eyeCol = (fromCol + toCol) / 2;
        if (board.getPieceAt(eyeRow, eyeCol) != null) {
            return false;
        }

        // 不能过河：红方在 5~9 行，黑方在 0~4 行
        return isRed() ? toRow >= 5 : toRow <= 4;
    }
}
