package edu.sustech.xiangqi.model;

/**
 * 根据棋子类型创建具体对象。
 *
 * <p>把“创建哪个子类”集中在一个地方，棋盘初始化、
 * 读档恢复都可以复用，避免到处写 if/else。</p>
 */
public final class PieceFactory {
    private PieceFactory() {
        // 工具类不需要实例
    }

    public static Piece create(PieceType type, boolean red) {
        return switch (type) {
            case GENERAL -> new GeneralPiece(red);
            case ADVISOR -> new AdvisorPiece(red);
            case ELEPHANT -> new ElephantPiece(red);
            case HORSE -> new HorsePiece(red);
            case ROOK -> new RookPiece(red);
            case CANNON -> new CannonPiece(red);
            case SOLDIER -> new SoldierPiece(red);
        };
    }
}
