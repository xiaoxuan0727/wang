package edu.sustech.xiangqi.model;

import java.util.Objects;

/**
 * 棋盘上的一个交叉点。行 0 在最上方（黑方底线），
 * 行 9 在最下方（红方底线）；列 0 在最左边。
 *
 * <p>使用不可变对象表示坐标，可以安全地放进 List、Set，
 * 也能避免“传引用后被意外修改”的问题。</p>
 */
public final class Position {
    private final int row;
    private final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Position position)) {
            return false;
        }
        return row == position.row && col == position.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}
