package edu.sustech.xiangqi.model;

/**
 * 帅 / 将。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>每次在九宫内沿直线走一步（不能斜走，不能出九宫）。</li>
 *   <li>两将不能在同一列上直接照面（中间没有棋子）。这条规则由
 *       {@link Board#isGeneralSafe(boolean)} 统一检查；本类的
 *       canMove 额外支持“飞将”：两将照面时可以直接沿列吃掉对方。</li>
 * </ul>
 */
public class GeneralPiece extends Piece {
    public GeneralPiece(boolean red) {
        super(PieceType.GENERAL, red);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        int rowDistance = Math.abs(toRow - fromRow);
        int colDistance = Math.abs(toCol - fromCol);

        // 九宫内直线走一步
        if (rowDistance + colDistance == 1 && isInsidePalace(toRow, toCol)) {
            return true;
        }

        // 飞将：同一列上无可阻挡的棋子时，可以沿列吃掉对方将帅
        if (fromCol == toCol) {
            Piece target = board.getPieceAt(toRow, toCol);
            if (target != null
                    && target.getType() == PieceType.GENERAL
                    && target.isRed() != isRed()
                    && board.isPathClear(fromRow, fromCol, toRow, toCol)) {
                return true;
            }
        }

        return false;
    }

    /** 判断目标点是否在本方的九宫内。红方在下方（7~9 行），黑方在上方（0~2 行）。 */
    private boolean isInsidePalace(int row, int col) {
        if (col < 3 || col > 5) {
            return false;
        }
        return isRed() ? row >= 7 && row <= 9 : row >= 0 && row <= 2;
    }
}
