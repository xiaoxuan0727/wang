package edu.sustech.xiangqi.model;

/**
 * 炮：走直线。
 *
 * <ul>
 *   <li>不吃子时，和车一样：路径上必须没有棋子。</li>
 *   <li>吃子时，起点和终点之间必须恰好隔着一个棋子（炮架）。</li>
 * </ul>
 */
public class CannonPiece extends Piece {
    public CannonPiece(boolean red) {
        super(PieceType.CANNON, red);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        boolean sameRow = fromRow == toRow;
        boolean sameCol = fromCol == toCol;
        if (!sameRow && !sameCol) {
            return false;
        }

        int between = board.countPiecesBetween(fromRow, fromCol, toRow, toCol);
        boolean capture = board.getPieceAt(toRow, toCol) != null;

        // 落点是空位：不能有炮架
        // 落点有棋子（吃子）：必须恰好一个炮架
        return capture ? between == 1 : between == 0;
    }
}
