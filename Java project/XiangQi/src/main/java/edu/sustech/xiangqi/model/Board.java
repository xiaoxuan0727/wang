package edu.sustech.xiangqi.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 棋盘模型：保存所有棋子，并统一检查、执行移动。
 *
 * <p>坐标约定：行 0 在上方（黑方底线），行 9 在下方（红方底线）；
 * 列 0 在最左边，列 8 在最右边。</p>
 *
 * <p>职责划分：</p>
 * <ul>
 *   <li>具体棋子类只判断自己的局部走法。</li>
 *   <li>Board 检查回合、己方占位、路径、走完后是否被将军等全局规则。</li>
 *   <li>界面只负责显示和转发点击，不直接修改棋盘数组。</li>
 * </ul>
 */
public class Board {
    public static final int ROWS = 10;
    public static final int COLS = 9;

    private final Piece[][] pieces = new Piece[ROWS][COLS];

    /** true 表示轮到红方走棋。红方先行。 */
    private boolean redToMove;
    private GameStatus status;
    private String endReason;
    private final List<Move> moveHistory = new ArrayList<>();

    public Board() {
        reset();
    }

    /** 开始一局新棋：清空棋盘，摆好初始棋子，红方先行。 */
    public final void reset() {
        clearBoard();
        setUpInitialPieces();
        redToMove = true;
        status = GameStatus.ONGOING;
        endReason = "";
        moveHistory.clear();
    }

    private void clearBoard() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                pieces[row][col] = null;
            }
        }
    }

    private void setUpInitialPieces() {
        placeBackRank(0, false); // 黑方底线
        placeBackRank(9, true);  // 红方底线

        // 炮
        pieces[2][1] = new CannonPiece(false);
        pieces[2][7] = new CannonPiece(false);
        pieces[7][1] = new CannonPiece(true);
        pieces[7][7] = new CannonPiece(true);

        // 兵 / 卒
        for (int col : new int[]{0, 2, 4, 6, 8}) {
            pieces[3][col] = new SoldierPiece(false);
            pieces[6][col] = new SoldierPiece(true);
        }
    }

    /** 摆放底线九子：车马象士将士象马车。 */
    private void placeBackRank(int row, boolean red) {
        pieces[row][0] = new RookPiece(red);
        pieces[row][1] = new HorsePiece(red);
        pieces[row][2] = new ElephantPiece(red);
        pieces[row][3] = new AdvisorPiece(red);
        pieces[row][4] = new GeneralPiece(red);
        pieces[row][5] = new AdvisorPiece(red);
        pieces[row][6] = new ElephantPiece(red);
        pieces[row][7] = new HorsePiece(red);
        pieces[row][8] = new RookPiece(red);
    }

    public static boolean isInsidePalace(int row, int col, boolean red) {
        if (col < 3 || col > 5) {
            return false;
        }
        return red ? row >= 7 && row <= 9 : row >= 0 && row <= 2;
    }

    public boolean isInside(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    public Piece getPieceAt(int row, int col) {
        if (!isInside(row, col)) {
            return null;
        }
        return pieces[row][col];
    }

    /**
     * 检查同一行或同一列上，起点和终点之间是否有棋子。
     * 起点与终点本身不算在“中间”。
     */
    public boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow != toRow && fromCol != toCol) {
            return false;
        }

        int rowStep = Integer.signum(toRow - fromRow);
        int colStep = Integer.signum(toCol - fromCol);

        int row = fromRow + rowStep;
        int col = fromCol + colStep;
        while (row != toRow || col != toCol) {
            if (pieces[row][col] != null) {
                return false;
            }
            row += rowStep;
            col += colStep;
        }
        return true;
    }

    /**
     * 数一数直线路径上起点和终点之间有多少个棋子（炮架数量）。
     * 只应在同一行或同一列时调用。
     */
    public int countPiecesBetween(int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow != toRow && fromCol != toCol) {
            return -1;
        }

        int rowStep = Integer.signum(toRow - fromRow);
        int colStep = Integer.signum(toCol - fromCol);

        int count = 0;
        int row = fromRow + rowStep;
        int col = fromCol + colStep;
        while (row != toRow || col != toCol) {
            if (pieces[row][col] != null) {
                count++;
            }
            row += rowStep;
            col += colStep;
        }
        return count;
    }

    /**
     * 只判断“从 from 到 to 这步棋是否合法”，不执行移动。
     *
     * <p>检查顺序：坐标、起止点、己方占位、棋子局部规则、
     * 走完后自己是否被将军（包括将帅照面）。</p>
     *
     * <p>该方法不检查回合，因此也可用于“当前局面下某一方是否还有棋可走”。</p>
     */
    public boolean isLegalMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (!isInside(fromRow, fromCol) || !isInside(toRow, toCol)) {
            return false;
        }
        if (fromRow == toRow && fromCol == toCol) {
            return false;
        }

        Piece piece = pieces[fromRow][fromCol];
        if (piece == null) {
            return false;
        }

        Piece target = pieces[toRow][toCol];
        if (target != null && target.isRed() == piece.isRed()) {
            return false; // 不能吃自己的棋子
        }

        if (!piece.canMove(this, fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        return wouldKeepGeneralSafe(piece, fromRow, fromCol, toRow, toCol);
    }

    /**
     * 在棋盘上临时执行一步，检查走棋方将帅是否安全，然后恢复原状。
     *
     * <p>这是实现“不能送将”“不能自己送吃”“将帅不能照面”的关键技巧：
     * 先真正移动，检查，再回滚。</p>
     */
    private boolean wouldKeepGeneralSafe(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        Piece captured = pieces[toRow][toCol];

        pieces[fromRow][fromCol] = null;
        pieces[toRow][toCol] = piece;

        boolean safe;
        try {
            safe = isGeneralSafe(piece.isRed());
        } finally {
            // 无论检查过程是否抛异常，都恢复棋盘
            pieces[toRow][toCol] = captured;
            pieces[fromRow][fromCol] = piece;
        }
        return safe;
    }

    /**
     * 判断某一方的将帅是否安全（没有被将军，也没有和对方将帅照面）。
     */
    public boolean isGeneralSafe(boolean red) {
        Position general = findGeneral(red);
        if (general == null) {
            return false; // 将帅已经不在了
        }

        Position enemyGeneral = findGeneral(!red);
        if (enemyGeneral != null
                && general.getCol() == enemyGeneral.getCol()
                && isPathClear(general.getRow(), general.getCol(),
                               enemyGeneral.getRow(), enemyGeneral.getCol())) {
            return false; // 两将照面
        }

        // 检查对方每一个棋子是否正在攻击我方将帅
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Piece enemy = pieces[row][col];
                if (enemy == null || enemy.isRed() == red) {
                    continue;
                }
                if (enemy.canMove(this, row, col, general.getRow(), general.getCol())) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 某一方是否正在被将军。 */
    public boolean isInCheck(boolean red) {
        return !isGeneralSafe(red);
    }

    private Position findGeneral(boolean red) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Piece piece = pieces[row][col];
                if (piece != null && piece.getType() == PieceType.GENERAL && piece.isRed() == red) {
                    return new Position(row, col);
                }
            }
        }
        return null;
    }

    /**
     * 尝试执行一步棋。只有轮到走棋的一方、走法全面合法时才会成功。
     *
     * @return 是否成功移动
     */
    public boolean tryMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (status != GameStatus.ONGOING) {
            return false;
        }

        Piece piece = getPieceAt(fromRow, fromCol);
        if (piece == null || piece.isRed() != redToMove) {
            return false; // 回合检查：只能动自己的棋子
        }

        if (!isLegalMove(fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        Piece captured = pieces[toRow][toCol];

        pieces[fromRow][fromCol] = null;
        pieces[toRow][toCol] = piece;

        moveHistory.add(new Move(
                new Position(fromRow, fromCol),
                new Position(toRow, toCol),
                piece.getType(),
                piece.isRed(),
                captured == null ? null : captured.getType()));

        // 特殊情况：直接吃掉对方将帅
        if (captured != null && captured.getType() == PieceType.GENERAL) {
            status = piece.isRed() ? GameStatus.RED_WIN : GameStatus.BLACK_WIN;
            endReason = "将帅被擒";
            return true;
        }

        // 落子后换边
        redToMove = !redToMove;

        // 对方无棋可走：被将死或困毙，均判负
        if (!hasAnyLegalMove(redToMove)) {
            status = redToMove ? GameStatus.BLACK_WIN : GameStatus.RED_WIN;
            endReason = isInCheck(redToMove) ? "将死" : "困毙（无子可走）";
        }
        return true;
    }

    /** 某一方是否至少存在一步合法走法。 */
    public boolean hasAnyLegalMove(boolean red) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Piece piece = pieces[row][col];
                if (piece == null || piece.isRed() != red) {
                    continue;
                }
                for (int toRow = 0; toRow < ROWS; toRow++) {
                    for (int toCol = 0; toCol < COLS; toCol++) {
                        if (isLegalMove(row, col, toRow, toCol)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 返回某枚棋子的所有合法落点，供界面高亮显示。
     * 只返回当前行棋方的棋子；游戏结束后返回空列表。
     */
    public List<Position> getLegalTargets(int row, int col) {
        if (status != GameStatus.ONGOING || !isInside(row, col)) {
            return Collections.emptyList();
        }

        Piece piece = pieces[row][col];
        if (piece == null || piece.isRed() != redToMove) {
            return Collections.emptyList();
        }

        List<Position> targets = new ArrayList<>();
        for (int toRow = 0; toRow < ROWS; toRow++) {
            for (int toCol = 0; toCol < COLS; toCol++) {
                if (isLegalMove(row, col, toRow, toCol)) {
                    targets.add(new Position(toRow, toCol));
                }
            }
        }
        return Collections.unmodifiableList(targets);
    }

    /** 当前行棋方认输。 */
    public boolean resign(boolean red) {
        if (status != GameStatus.ONGOING) {
            return false;
        }
        status = red ? GameStatus.BLACK_WIN : GameStatus.RED_WIN;
        endReason = "认输";
        return true;
    }

    /** 双方同意和棋。 */
    public boolean agreeDraw() {
        if (status != GameStatus.ONGOING) {
            return false;
        }
        status = GameStatus.DRAW;
        endReason = "双方同意和棋";
        return true;
    }

    public boolean isRedToMove() {
        return redToMove;
    }

    public GameStatus getStatus() {
        return status;
    }

    public boolean isGameOver() {
        return status != GameStatus.ONGOING;
    }

    public String getEndReason() {
        return endReason;
    }

    public List<Move> getMoveHistory() {
        return Collections.unmodifiableList(moveHistory);
    }

    /** 上一步棋；还没有人走棋时返回 null。 */
    public Move getLastMove() {
        return moveHistory.isEmpty() ? null : moveHistory.get(moveHistory.size() - 1);
    }

    /** 生成用于保存的完整快照。 */
    public GameSnapshot toSnapshot() {
        return new GameSnapshot(pieces, redToMove, status, endReason, moveHistory);
    }

    /** 用快照覆盖当前棋盘，主要用于读档。调用前应确认快照内容合法。 */
    public void restore(GameSnapshot snapshot) {
        Piece[][] saved = snapshot.copyPieces();
        for (int row = 0; row < ROWS; row++) {
            System.arraycopy(saved[row], 0, pieces[row], 0, COLS);
        }
        redToMove = snapshot.isRedToMove();
        status = snapshot.getStatus();
        endReason = snapshot.getEndReason();
        moveHistory.clear();
        moveHistory.addAll(snapshot.getMoveHistory());
    }

    /** 根据快照创建棋盘，主要用于测试和读取自定义局面。 */
    public static Board fromSnapshot(GameSnapshot snapshot) {
        Board board = new Board();
        board.restore(snapshot);
        return board;
    }
}
