package edu.sustech.xiangqi.model;

/**
 * 一条着法记录：谁、哪个棋子、从哪里走到哪里、是否吃子。
 *
 * <p>记录着法既可以显示在界面上，也可以写入存档，读档后恢复着法列表。</p>
 */
public final class Move {
    private final Position from;
    private final Position to;
    private final PieceType pieceType;
    private final boolean red;
    private final PieceType capturedType; // null 表示没有吃子

    public Move(Position from, Position to, PieceType pieceType, boolean red, PieceType capturedType) {
        this.from = from;
        this.to = to;
        this.pieceType = pieceType;
        this.red = red;
        this.capturedType = capturedType;
    }

    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    public PieceType getPieceType() {
        return pieceType;
    }

    public boolean isRed() {
        return red;
    }

    /** @return 被吃棋子的类型；没有吃子时返回 null */
    public PieceType getCapturedType() {
        return capturedType;
    }

    /** 生成人类可读的着法，例如：红 车 (9,0) → (7,0)，吃 黑 卒。 */
    public String describe() {
        StringBuilder text = new StringBuilder();
        text.append(red ? "红" : "黑").append(' ')
                .append(pieceType.displayName(red)).append(' ')
                .append(from).append(" → ").append(to);
        if (capturedType != null) {
            text.append("，吃 ").append(capturedType.displayName(!red));
        }
        return text.toString();
    }
}
