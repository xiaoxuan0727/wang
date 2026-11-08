package edu.sustech.xiangqi.model;

/**
 * 车：沿同一行或同一列走任意格，路径上不能有棋子。
 */
public class RookPiece extends Piece {
    public RookPiece(boolean red) {
        super(PieceType.ROOK, red);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        boolean sameRow = fromRow == toRow;
        boolean sameCol = fromCol == toCol;
        if (!sameRow && !sameCol) {
            return false;
        }

        return board.isPathClear(fromRow, fromCol, toRow, toCol);
    }
}
