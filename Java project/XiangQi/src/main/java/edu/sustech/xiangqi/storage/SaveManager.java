package edu.sustech.xiangqi.storage;

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.GameSnapshot;
import edu.sustech.xiangqi.model.GameStatus;
import edu.sustech.xiangqi.model.Move;
import edu.sustech.xiangqi.model.Piece;
import edu.sustech.xiangqi.model.PieceFactory;
import edu.sustech.xiangqi.model.PieceType;
import edu.sustech.xiangqi.model.Position;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 棋局存档的保存与读取。
 *
 * <p>每个注册用户有 3 个存档位，只能读写自己目录下的存档。
 * 文件使用简单、可读的文本格式（UTF-8），便于学习和检查：</p>
 *
 * <pre>
 * xiangqi-save-v1
 * turn=red
 * status=ONGOING
 * reason=
 * moves=1
 * move=6,4,5,4,S,-
 * board:
 * r h e a g a e h r
 * . . . . . . . . .
 * ...
 * </pre>
 *
 * <p>读档时会逐行校验：文件头、字段、棋盘行数/列数、棋子数量、
 * 棋子位置、将帅安全以及当前方是否还有棋可走。任何不合法的文件
 * 都会抛出 {@link InvalidSaveException}，由界面提示用户。</p>
 */
public final class SaveManager {
    public static final int MAX_SLOTS = 3;

    private static final String FORMAT_HEADER = "xiangqi-save-v1";
    private static final Pattern SAFE_USERNAME = Pattern.compile("[A-Za-z0-9_]{2,20}");
    private static final int MAX_MOVES = 10_000;
    private static final long MAX_FILE_BYTES = 1_000_000L;

    private final Path baseDir;

    public SaveManager() {
        this(StoragePaths.baseDir());
    }

    public SaveManager(Path baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * 保存当前对局。只允许保存进行中的对局，保证读档后可以继续下棋。
     */
    public void save(String username, int slot, Board board) throws IOException {
        Path file = getSavePath(username, slot);

        if (board.getStatus() != GameStatus.ONGOING) {
            throw new IOException("只能保存进行中的对局");
        }

        String content = serialize(board);
        Path parent = file.getParent();
        Files.createDirectories(parent);

        // 先写临时文件再替换，避免中途失败留下半个存档
        Path temp = Files.createTempFile(parent, "slot" + slot + "-", ".tmp");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temp, file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /**
     * 读取存档并覆盖目标棋盘。
     *
     * <p>只有全部内容校验通过后才会修改 target，因此读档失败时
     * 当前对局仍保持原样。</p>
     */
    public void loadInto(String username, int slot, Board target)
            throws IOException, InvalidSaveException {
        Path file = getSavePath(username, slot);

        if (!Files.exists(file)) {
            throw new FileNotFoundException("存档不存在：" + file);
        }
        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new InvalidSaveException("存档文件过大，可能已损坏");
        }

        String content = Files.readString(file, StandardCharsets.UTF_8);
        GameSnapshot snapshot = parse(content);
        target.restore(snapshot);
    }

    public Path getSavePath(String username, int slot) {
        if (username == null || !SAFE_USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException("非法的用户名");
        }
        if (slot < 1 || slot > MAX_SLOTS) {
            throw new IllegalArgumentException("存档位必须在 1~" + MAX_SLOTS + " 之间");
        }
        return StoragePaths.saveFile(baseDir, username, slot);
    }

    // ------------------------------------------------------------------
    // 写存档
    // ------------------------------------------------------------------

    private static String serialize(Board board) {
        GameSnapshot snapshot = board.toSnapshot();
        StringBuilder text = new StringBuilder();

        text.append(FORMAT_HEADER).append('\n');
        text.append("turn=").append(snapshot.isRedToMove() ? "red" : "black").append('\n');
        text.append("status=").append(snapshot.getStatus().name()).append('\n');
        text.append("reason=").append(snapshot.getEndReason()).append('\n');

        List<Move> moves = snapshot.getMoveHistory();
        text.append("moves=").append(moves.size()).append('\n');
        for (Move move : moves) {
            text.append("move=")
                    .append(move.getFrom().getRow()).append(',')
                    .append(move.getFrom().getCol()).append(',')
                    .append(move.getTo().getRow()).append(',')
                    .append(move.getTo().getCol()).append(',')
                    .append(move.getPieceType().symbol(move.isRed())).append(',')
                    .append(move.getCapturedType() == null
                            ? "-"
                            : move.getCapturedType().symbol(!move.isRed()))
                    .append('\n');
        }

        text.append("board:\n");
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Piece piece = snapshot.getPieceAt(row, col);
                if (piece == null) {
                    text.append('.');
                } else {
                    text.append(piece.getType().symbol(piece.isRed()));
                }
                if (col < Board.COLS - 1) {
                    text.append(' ');
                }
            }
            text.append('\n');
        }
        return text.toString();
    }

    // ------------------------------------------------------------------
    // 读存档与校验
    // ------------------------------------------------------------------

    private static GameSnapshot parse(String content) throws InvalidSaveException {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        String[] rawLines = normalized.split("\n", -1);
        List<String> lines = new ArrayList<>();
        for (String line : rawLines) {
            lines.add(line);
        }
        // 允许文件末尾有空行
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }
        if (lines.isEmpty()) {
            throw new InvalidSaveException("存档为空");
        }

        int index = 0;
        String header = lines.get(index++).replace("﻿", "").trim();
        if (!FORMAT_HEADER.equals(header)) {
            throw invalid(1, "无法识别的文件头，应为 " + FORMAT_HEADER);
        }

        boolean redToMove;
        String turnValue = readValue(lines, index++, "turn");
        if ("red".equals(turnValue)) {
            redToMove = true;
        } else if ("black".equals(turnValue)) {
            redToMove = false;
        } else {
            throw invalid(index, "turn 只能是 red 或 black");
        }

        String statusValue = readValue(lines, index++, "status");
        if (!"ONGOING".equals(statusValue)) {
            throw invalid(index, "只能载入进行中的对局（status=ONGOING）");
        }

        String reason = readValue(lines, index++, "reason");
        if (reason.length() > 100) {
            throw invalid(index, "reason 字段过长");
        }

        String movesValue = readValue(lines, index++, "moves");
        int moveCount = parseInt(movesValue, index, "moves");
        if (moveCount < 0 || moveCount > MAX_MOVES) {
            throw invalid(index, "着法数量超出范围");
        }

        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < moveCount; i++) {
            String value = readValue(lines, index, "move");
            index++;
            moves.add(parseMove(value, moves.size() + 1));
        }

        String boardTag = requireLine(lines, index).trim();
        if (!"board:".equals(boardTag)) {
            throw invalid(index + 1, "缺少 board: 标记");
        }
        index++;

        Piece[][] grid = new Piece[Board.ROWS][Board.COLS];
        int[][] counts = new int[2][PieceType.values().length];
        for (int row = 0; row < Board.ROWS; row++) {
            String line = requireLine(lines, index);
            index++;
            String[] tokens = line.trim().split("\\s+");
            if (tokens.length != Board.COLS) {
                throw invalid(index, "棋盘第 " + row + " 行必须有 9 个格子，实际为 " + tokens.length);
            }
            for (int col = 0; col < Board.COLS; col++) {
                String token = tokens[col];
                if (".".equals(token)) {
                    continue;
                }
                if (token.length() != 1) {
                    throw invalid(index, "非法的棋子代码：" + token);
                }
                char symbol = token.charAt(0);
                PieceType type = PieceType.fromSymbol(symbol);
                if (type == null) {
                    throw invalid(index, "非法的棋子代码：" + symbol);
                }
                boolean red = Character.isUpperCase(symbol);
                grid[row][col] = PieceFactory.create(type, red);
                counts[red ? 0 : 1][type.ordinal()]++;
            }
        }

        if (index != lines.size()) {
            throw invalid(index + 1, "棋盘后存在多余内容");
        }

        validatePieceCountsAndPositions(grid, counts);

        GameSnapshot snapshot = new GameSnapshot(grid, redToMove, GameStatus.ONGOING, reason, moves);

        // 更进一步检查“这确实是一个下棋过程中可能出现的局面”
        Board probe = Board.fromSnapshot(snapshot);
        boolean notToMove = !redToMove;
        if (probe.isInCheck(notToMove)) {
            throw new InvalidSaveException("局面非法：非行棋的一方正在被将军");
        }
        if (!probe.hasAnyLegalMove(redToMove)) {
            throw new InvalidSaveException("局面非法：行棋方已无合法着法，对局本应结束");
        }

        return snapshot;
    }

    private static void validatePieceCountsAndPositions(Piece[][] grid, int[][] counts)
            throws InvalidSaveException {
        if (counts[0][PieceType.GENERAL.ordinal()] != 1
                || counts[1][PieceType.GENERAL.ordinal()] != 1) {
            throw new InvalidSaveException("局面非法：红黑双方必须各有一个将帅");
        }

        for (PieceType type : PieceType.values()) {
            int max = maxCount(type);
            if (counts[0][type.ordinal()] > max || counts[1][type.ordinal()] > max) {
                throw new InvalidSaveException("局面非法：棋子数量超过上限（" + type + "）");
            }
        }

        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Piece piece = grid[row][col];
                if (piece == null) {
                    continue;
                }
                PieceType type = piece.getType();
                if ((type == PieceType.GENERAL || type == PieceType.ADVISOR)
                        && !Board.isInsidePalace(row, col, piece.isRed())) {
                    throw new InvalidSaveException("局面非法：将帅或仕士位于九宫之外");
                }
                if (type == PieceType.ELEPHANT) {
                    boolean onOwnSide = piece.isRed() ? row >= 5 : row <= 4;
                    if (!onOwnSide) {
                        throw new InvalidSaveException("局面非法：象/相位于河对岸");
                    }
                }
            }
        }
    }

    private static int maxCount(PieceType type) {
        return switch (type) {
            case GENERAL -> 1;
            case ADVISOR, ELEPHANT, HORSE, ROOK, CANNON -> 2;
            case SOLDIER -> 5;
        };
    }

    private static Move parseMove(String value, int moveNumber) throws InvalidSaveException {
        String[] parts = value.split(",", -1);
        if (parts.length != 6) {
            throw new InvalidSaveException("第 " + moveNumber + " 条着法格式错误，应为 6 个字段");
        }

        int fromRow = parseInt(parts[0], moveNumber, "着法起点行");
        int fromCol = parseInt(parts[1], moveNumber, "着法起点列");
        int toRow = parseInt(parts[2], moveNumber, "着法终点行");
        int toCol = parseInt(parts[3], moveNumber, "着法终点列");

        if (!insideBoard(fromRow, fromCol) || !insideBoard(toRow, toCol)) {
            throw new InvalidSaveException("第 " + moveNumber + " 条着法坐标越界");
        }
        if (fromRow == toRow && fromCol == toCol) {
            throw new InvalidSaveException("第 " + moveNumber + " 条着法起点与终点相同");
        }

        char pieceSymbol = singleSymbol(parts[4], moveNumber, "着法棋子");
        PieceType pieceType = PieceType.fromSymbol(pieceSymbol);
        if (pieceType == null) {
            throw new InvalidSaveException("第 " + moveNumber + " 条着法包含未知棋子");
        }
        boolean red = Character.isUpperCase(pieceSymbol);

        PieceType capturedType = null;
        if (!"-".equals(parts[5])) {
            char capturedSymbol = singleSymbol(parts[5], moveNumber, "被吃棋子");
            capturedType = PieceType.fromSymbol(capturedSymbol);
            if (capturedType == null) {
                throw new InvalidSaveException("第 " + moveNumber + " 条着法包含未知被吃棋子");
            }
            if (Character.isUpperCase(capturedSymbol) == red) {
                throw new InvalidSaveException("第 " + moveNumber + " 条着法被吃棋子颜色错误");
            }
        }

        return new Move(new Position(fromRow, fromCol), new Position(toRow, toCol),
                pieceType, red, capturedType);
    }

    private static char singleSymbol(String value, int context, String name)
            throws InvalidSaveException {
        if (value.length() != 1) {
            throw new InvalidSaveException("第 " + context + " 条着法的" + name + "应为单个字符");
        }
        return value.charAt(0);
    }

    private static boolean insideBoard(int row, int col) {
        return row >= 0 && row < Board.ROWS && col >= 0 && col < Board.COLS;
    }

    private static String readValue(List<String> lines, int index, String key)
            throws InvalidSaveException {
        String line = requireLine(lines, index);
        String prefix = key + "=";
        if (!line.startsWith(prefix)) {
            throw invalid(index + 1, "缺少字段 " + key + "=");
        }
        return line.substring(prefix.length());
    }

    private static String requireLine(List<String> lines, int index) throws InvalidSaveException {
        if (index < 0 || index >= lines.size()) {
            throw new InvalidSaveException("存档在读取第 " + (index + 1) + " 行时意外结束");
        }
        return lines.get(index);
    }

    private static int parseInt(String value, int context, String name) throws InvalidSaveException {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new InvalidSaveException("第 " + context + " 行的" + name + "不是整数：" + value);
        }
    }

    private static InvalidSaveException invalid(int lineNumber, String message) {
        return new InvalidSaveException("存档第 " + lineNumber + " 行：" + message);
    }
}
