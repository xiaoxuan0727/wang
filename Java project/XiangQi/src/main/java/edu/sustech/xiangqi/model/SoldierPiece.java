package edu.sustech.xiangqi.model;

/**
 * 兵 / 卒。
 *
 * <ul>
 *   <li>过河前：只能向前走一格。</li>
 *   <li>过河后：可以向前，也可以向左或向右走一格。</li>
 *   <li>永远不能后退，也不能斜走。</li>
 * </ul>
 *
 * <p>注意：兵“过河”指已经站在对方半场。红方向上走（行号减小），
 * 黑方向下走（行号增大）。</p>
 */
public class SoldierPiece extends Piece {
    public SoldierPiece(boolean red) {
        super(PieceType.SOLDIER, red);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        int forward = isRed() ? -1 : 1;

        // 向前一格
        if (toRow == fromRow + forward && toCol == fromCol) {
            return true;
        }

        // 过河后才能横向移动一格
        boolean crossedRiver = isRed() ? fromRow <= 4 : fromRow >= 5;
        return crossedRiver && toRow == fromRow && Math.abs(toCol - fromCol) == 1;
    }
}
