package edu.sustech.xiangqi.model;

/**
 * 七种棋子类型，以及它们在红/黑两方的显示名称。
 *
 * <p>保存棋局时使用字母代码（G/A/E/H/R/C/S）表示棋子：
 * 大写代表红方，小写代表黑方。例如 R=红车，r=黑车。</p>
 */
public enum PieceType {
    GENERAL('G', "帅", "将"),
    ADVISOR('A', "仕", "士"),
    ELEPHANT('E', "相", "象"),
    HORSE('H', "马", "马"),
    ROOK('R', "车", "车"),
    CANNON('C', "炮", "炮"),
    SOLDIER('S', "兵", "卒");

    private final char code;
    private final String redName;
    private final String blackName;

    PieceType(char code, String redName, String blackName) {
        this.code = code;
        this.redName = redName;
        this.blackName = blackName;
    }

    public char getCode() {
        return code;
    }

    public String displayName(boolean red) {
        return red ? redName : blackName;
    }

    /** 返回该类型在指定一方使用的存档字符，例如红车为 R，黑车为 r。 */
    public char symbol(boolean red) {
        return red ? Character.toUpperCase(code) : Character.toLowerCase(code);
    }

    /**
     * 根据存档字符查找棋子类型。
     *
     * @return 找不到时返回 null，由调用者决定如何处理非法数据
     */
    public static PieceType fromSymbol(char symbol) {
        char upper = Character.toUpperCase(symbol);
        for (PieceType type : values()) {
            if (type.code == upper) {
                return type;
            }
        }
        return null;
    }
}
