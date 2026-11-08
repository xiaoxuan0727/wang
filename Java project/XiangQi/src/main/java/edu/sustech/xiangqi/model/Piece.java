package edu.sustech.xiangqi.model;

/**
 * 棋子的抽象父类。
 *
 * <p>棋子只保存“身份”信息（类型、红黑），所在位置由 {@link Board}
 * 的二维数组下标表示。这样做的好处是同一枚棋子被移动时不需要记录坐标，
 * 棋盘数组就是唯一的数据来源。</p>
 */
public abstract class Piece {
    private final PieceType type;
    private final boolean red;

    protected Piece(PieceType type, boolean red) {
        this.type = type;
        this.red = red;
    }

    public PieceType getType() {
        return type;
    }

    public boolean isRed() {
        return red;
    }

    /** 棋子显示名称，例如红方的马和黑方的马都显示“马”。 */
    public String getName() {
        return type.displayName(red);
    }

    /**
     * 判断棋子的局部走法是否合法。
     *
     * <p>这里只检查“这个棋子本身能不能这样走”，不负责以下事情：</p>
     * <ul>
     *   <li>回合是否正确</li>
     *   <li>目标点是否是自己人（由 Board 统一检查）</li>
     *   <li>走完后自己的将帅是否安全（由 Board 模拟后检查）</li>
     * </ul>
     *
     * @return 走法是否符合该棋子的规则
     */
    public abstract boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol);
}
