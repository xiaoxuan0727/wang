package edu.sustech.xiangqi.model;

/**
 * 马：走“日”字（先直一格再斜一格），不能“蹩马腿”。
 *
 * <p>蹩马腿的位置，是马先沿直行方向走的那一格。
 * 例如马向上跳两行再左右一格，马腿就在马正上方一格。</p>
 */
public class HorsePiece extends Piece {
    public HorsePiece(boolean red) {
        super(PieceType.HORSE, red);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        int rowDistance = Math.abs(toRow - fromRow);
        int colDistance = Math.abs(toCol - fromCol);

        if (!((rowDistance == 2 && colDistance == 1)
                || (rowDistance == 1 && colDistance == 2))) {
            return false;
        }

        int legRow;
        int legCol;
        if (rowDistance == 2) {
            // 先纵向走一格，马的腿在纵向的那一格
            legRow = fromRow + (toRow > fromRow ? 1 : -1);
            legCol = fromCol;
        } else {
            // 先横向走一格，马的腿在横向的那一格
            legRow = fromRow;
            legCol = fromCol + (toCol > fromCol ? 1 : -1);
        }

        return board.getPieceAt(legRow, legCol) == null;
    }
}
