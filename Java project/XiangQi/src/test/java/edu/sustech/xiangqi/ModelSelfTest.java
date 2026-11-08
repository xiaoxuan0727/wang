package edu.sustech.xiangqi;

import edu.sustech.xiangqi.model.AdvisorPiece;
import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.CannonPiece;
import edu.sustech.xiangqi.model.ElephantPiece;
import edu.sustech.xiangqi.model.GameSnapshot;
import edu.sustech.xiangqi.model.GameStatus;
import edu.sustech.xiangqi.model.GeneralPiece;
import edu.sustech.xiangqi.model.HorsePiece;
import edu.sustech.xiangqi.model.Move;
import edu.sustech.xiangqi.model.Piece;
import edu.sustech.xiangqi.model.PieceType;
import edu.sustech.xiangqi.model.RookPiece;
import edu.sustech.xiangqi.model.SoldierPiece;
import edu.sustech.xiangqi.storage.AuthService;
import edu.sustech.xiangqi.storage.InvalidSaveException;
import edu.sustech.xiangqi.storage.SaveManager;
import edu.sustech.xiangqi.storage.User;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 不依赖 JUnit 的规则自测程序。
 *
 * <p>运行方式（项目根目录下）：</p>
 * <pre>
 * javac -encoding UTF-8 -d out/test $(find src/main/java src/test/java -name "*.java")
 * java -cp out/test edu.sustech.xiangqi.ModelSelfTest
 * </pre>
 *
 * <p>全部通过时退出码为 0，否则为 1。</p>
 */
public final class ModelSelfTest {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        testInitialBoard();
        testTurnOrderAndCapture();
        testAdvisor();
        testElephant();
        testHorse();
        testRook();
        testCannon();
        testSoldier();
        testGeneral();
        testCheckAndFacing();
        testCheckmateByMove();
        testStalemateByMove();
        testResignAndDraw();
        testAuthAndSaveLoad();

        System.out.println();
        System.out.println("======================================");
        System.out.println("通过：" + passed + "，失败：" + failed);
        System.out.println("======================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------
    // 基础局面 / 回合
    // ------------------------------------------------------------------

    private static void testInitialBoard() {
        Board board = new Board();
        check("初始棋子共 32 枚", countPieces(board) == 32);
        check("红帅在 (9,4)", isType(board.getPieceAt(9, 4), PieceType.GENERAL, true));
        check("黑将在 (0,4)", isType(board.getPieceAt(0, 4), PieceType.GENERAL, false));
        check("红方先行", board.isRedToMove());
        check("第一步之前没有上一步", board.getLastMove() == null);
        check("初始局面没有将军", !board.isInCheck(true) && !board.isInCheck(false));
    }

    private static void testTurnOrderAndCapture() {
        Board board = new Board();

        check("红方不能先走黑棋", !board.tryMove(0, 0, 1, 0));
        check("红兵前进合法", board.tryMove(6, 4, 5, 4));
        check("红方不能连走两步", !board.tryMove(6, 0, 5, 0));
        check("轮到黑方", !board.isRedToMove());
        check("黑卒前进合法", board.tryMove(3, 4, 4, 4));
        check("轮到红方", board.isRedToMove());
        check("着法记录共 2 条", board.getMoveHistory().size() == 2);

        Move lastMove = board.getLastMove();
        check("上一步描述包含棋子和箭头", lastMove != null
                && lastMove.describe().contains("黑")
                && lastMove.describe().contains("→"));

        Piece[][] grid = emptyGrid();
        grid[5][0] = new RookPiece(true);
        grid[5][4] = new SoldierPiece(false);
        Board captureBoard = boardWith(grid, true);

        check("红车吃黑卒成功", captureBoard.tryMove(5, 0, 5, 4));
        check("吃子后目标点变成红车", isType(captureBoard.getPieceAt(5, 4), PieceType.ROOK, true));
        check("吃子记录保留被吃棋子",
                captureBoard.getLastMove().getCapturedType() == PieceType.SOLDIER);
    }

    // ------------------------------------------------------------------
    // 每种棋子的走法
    // ------------------------------------------------------------------

    private static void testAdvisor() {
        Piece[][] grid = emptyGrid();
        grid[9][4] = new AdvisorPiece(true);
        Board board = boardWith(grid, true);

        check("红仕斜走一格合法", board.isLegalMove(9, 4, 8, 3));
        check("仕不能直走", !board.isLegalMove(9, 4, 8, 4));
        check("仕不能一次斜走两格", !board.isLegalMove(9, 4, 7, 2));

        Piece[][] blackGrid = emptyGrid();
        blackGrid[0][3] = new AdvisorPiece(false);
        Board blackBoard = boardWith(blackGrid, false);

        check("黑士斜走一格合法", blackBoard.isLegalMove(0, 3, 1, 4));
        check("黑士不能出九宫", !blackBoard.isLegalMove(0, 3, 1, 2));
    }

    private static void testElephant() {
        Piece[][] grid = emptyGrid();
        grid[9][2] = new ElephantPiece(true);
        Board board = boardWith(grid, true);
        check("红相走田字合法", board.isLegalMove(9, 2, 7, 0));

        grid[8][1] = new SoldierPiece(true);
        Board blocked = boardWith(grid, true);
        check("象眼被堵时不能走", !blocked.isLegalMove(9, 2, 7, 0));

        Piece[][] crossing = emptyGrid();
        crossing[5][2] = new ElephantPiece(true);
        Board crossingBoard = boardWith(crossing, true);
        check("红相不能过河", !crossingBoard.isLegalMove(5, 2, 3, 0));

        Piece[][] blackCrossing = emptyGrid();
        blackCrossing[4][2] = new ElephantPiece(false);
        Board blackBoard = boardWith(blackCrossing, false);
        check("黑象不能过河", !blackBoard.isLegalMove(4, 2, 6, 0));
    }

    private static void testHorse() {
        Piece[][] grid = emptyGrid();
        grid[9][1] = new HorsePiece(true);
        Board board = boardWith(grid, true);
        check("马走日字合法", board.isLegalMove(9, 1, 7, 2));

        grid[8][1] = new SoldierPiece(true);
        Board blocked = boardWith(grid, true);
        check("蹩马腿时不能走", !blocked.isLegalMove(9, 1, 7, 2));

        Piece[][] grid2 = emptyGrid();
        grid2[5][5] = new HorsePiece(true);
        Board board2 = boardWith(grid2, true);
        check("马向另一方向走日字合法", board2.isLegalMove(5, 5, 3, 4));
    }

    private static void testRook() {
        Piece[][] grid = emptyGrid();
        grid[5][0] = new RookPiece(true);
        Board board = boardWith(grid, true);
        check("车直线移动合法", board.isLegalMove(5, 0, 5, 4));

        grid[5][2] = new SoldierPiece(false);
        Board blocked = boardWith(grid, true);
        check("车不能穿过棋子", !blocked.isLegalMove(5, 0, 5, 4));

        Piece[][] capture = emptyGrid();
        capture[5][0] = new RookPiece(true);
        capture[5][4] = new SoldierPiece(false);
        Board captureBoard = boardWith(capture, true);
        check("车可以吃子", captureBoard.isLegalMove(5, 0, 5, 4));

        Piece[][] friendly = emptyGrid();
        friendly[5][0] = new RookPiece(true);
        friendly[5][4] = new SoldierPiece(true);
        Board friendlyBoard = boardWith(friendly, true);
        check("车不能吃己方棋子", !friendlyBoard.isLegalMove(5, 0, 5, 4));
    }

    private static void testCannon() {
        Piece[][] empty = emptyGrid();
        empty[5][0] = new CannonPiece(true);
        empty[5][4] = new SoldierPiece(false);
        Board noScreen = boardWith(empty, true);
        check("炮没有炮架不能吃子", !noScreen.isLegalMove(5, 0, 5, 4));

        Piece[][] nonCapture = emptyGrid();
        nonCapture[5][0] = new CannonPiece(true);
        Board moveBoard = boardWith(nonCapture, true);
        check("炮不吃子时走直线合法", moveBoard.isLegalMove(5, 0, 5, 4));

        Piece[][] withScreen = emptyGrid();
        withScreen[5][0] = new CannonPiece(true);
        withScreen[5][2] = new SoldierPiece(true);
        withScreen[5][4] = new SoldierPiece(false);
        Board screenBoard = boardWith(withScreen, true);
        check("炮隔一个炮架吃子合法", screenBoard.isLegalMove(5, 0, 5, 4));
        check("有炮架时不能移动到空位", !screenBoard.isLegalMove(5, 0, 5, 3));

        Piece[][] twoScreens = emptyGrid();
        twoScreens[5][0] = new CannonPiece(true);
        twoScreens[5][2] = new SoldierPiece(true);
        twoScreens[5][3] = new SoldierPiece(true);
        twoScreens[5][4] = new SoldierPiece(false);
        Board twoScreenBoard = boardWith(twoScreens, true);
        check("隔两个炮架不能吃子", !twoScreenBoard.isLegalMove(5, 0, 5, 4));
    }

    private static void testSoldier() {
        Piece[][] grid = emptyGrid();
        grid[6][4] = new SoldierPiece(true);
        Board board = boardWith(grid, true);
        check("红兵过河前只能前进", board.isLegalMove(6, 4, 5, 4));
        check("红兵过河前不能横走", !board.isLegalMove(6, 4, 6, 3));
        check("兵不能后退", !board.isLegalMove(6, 4, 7, 4));

        Piece[][] crossed = emptyGrid();
        crossed[4][4] = new SoldierPiece(true);
        Board crossedBoard = boardWith(crossed, true);
        check("红兵过河后可以横走", crossedBoard.isLegalMove(4, 4, 4, 3));
        check("兵仍然不能后退", !crossedBoard.isLegalMove(4, 4, 5, 4));

        Piece[][] black = emptyGrid();
        black[3][4] = new SoldierPiece(false);
        Board blackBoard = boardWith(black, false);
        check("黑卒过河前不能横走", !blackBoard.isLegalMove(3, 4, 3, 3));
        check("黑卒前进合法", blackBoard.isLegalMove(3, 4, 4, 4));

        Piece[][] blackCrossed = emptyGrid();
        blackCrossed[5][4] = new SoldierPiece(false);
        Board blackCrossedBoard = boardWith(blackCrossed, false);
        check("黑卒过河后可以横走", blackCrossedBoard.isLegalMove(5, 4, 5, 5));
    }

    private static void testGeneral() {
        Piece[][] grid = emptyGrid();
        Board board = boardWith(grid, true);
        check("帅在九宫内直走一格合法", board.isLegalMove(9, 3, 8, 3));
        check("帅不能斜走", !board.isLegalMove(9, 3, 8, 4));
        check("帅不能走出九宫", !board.isLegalMove(9, 3, 9, 2));
    }

    private static void testCheckAndFacing() {
        // 黑车在 (9,8) 沿第 9 行将军红帅
        Piece[][] grid = emptyGrid();
        grid[9][8] = new RookPiece(false);
        grid[7][0] = new SoldierPiece(true);
        Board board = boardWith(grid, true);

        check("红帅被黑车将军", board.isInCheck(true));
        check("被将军时不能走无关的棋", !board.isLegalMove(7, 0, 6, 0));
        check("帅可以走开应将", board.isLegalMove(9, 3, 8, 3));
        check("走开应将后对局继续", board.tryMove(9, 3, 8, 3));
        check("红方解除将军后轮到黑方", !board.isRedToMove());

        // 两将照面：红兵移开后会让两帅同一列直接相对
        Piece[][] facing = new Piece[Board.ROWS][Board.COLS];
        facing[9][4] = new GeneralPiece(true);
        facing[0][4] = new GeneralPiece(false);
        facing[5][4] = new SoldierPiece(true);
        Board facingBoard = boardWith(facing, true);
        check("两将之间有棋子时双方安全",
                facingBoard.isGeneralSafe(true) && facingBoard.isGeneralSafe(false));
        check("不能走开挡住两将的兵", !facingBoard.isLegalMove(5, 4, 5, 3));

        Piece[][] noBlocker = new Piece[Board.ROWS][Board.COLS];
        noBlocker[9][4] = new GeneralPiece(true);
        noBlocker[0][4] = new GeneralPiece(false);
        Board noBlockerBoard = boardWith(noBlocker, true);
        check("两将直接照面视为不安全", !noBlockerBoard.isGeneralSafe(true)
                && !noBlockerBoard.isGeneralSafe(false));
    }

    private static void testCheckmateByMove() {
        Piece[][] grid = new Piece[Board.ROWS][Board.COLS];
        grid[0][4] = new GeneralPiece(false);
        grid[9][3] = new GeneralPiece(true);
        grid[2][4] = new HorsePiece(true); // 控制 0,3 和 0,5
        grid[1][0] = new RookPiece(true);  // 控制第 1 行
        grid[5][8] = new RookPiece(true);  // 走到 0,8 后将军
        Board board = boardWith(grid, true);

        check("红车走到 (0,8) 是合法着法", board.tryMove(5, 8, 0, 8));
        check("将军后黑方被将死", board.getStatus() == GameStatus.RED_WIN);
        check("结束原因为将死", "将死".equals(board.getEndReason()));
        check("红方获胜", board.getStatus() == GameStatus.RED_WIN);
    }

    private static void testStalemateByMove() {
        Piece[][] grid = new Piece[Board.ROWS][Board.COLS];
        grid[0][3] = new GeneralPiece(false);
        grid[9][4] = new GeneralPiece(true);
        grid[2][5] = new HorsePiece(true); // 控制 0,4 和 1,3
        grid[1][0] = new RookPiece(true);  // 控制第 1 行
        grid[9][0] = new RookPiece(true);  // 用来走一步无害的棋
        Board board = boardWith(grid, true);

        check("走一步无害的棋是合法的", board.tryMove(9, 0, 9, 1));
        check("黑方无棋可走且未被将军 → 困毙判负",
                board.getStatus() == GameStatus.RED_WIN
                        && board.getEndReason().startsWith("困毙"));
        check("红方获胜（困毙）", board.getStatus() == GameStatus.RED_WIN);
    }

    private static void testResignAndDraw() {
        Board resignBoard = new Board();
        check("红方认输成功", resignBoard.resign(true));
        check("红方认输后黑方胜", resignBoard.getStatus() == GameStatus.BLACK_WIN);
        check("认输后不能继续走棋", !resignBoard.tryMove(6, 4, 5, 4));

        Board drawBoard = new Board();
        check("双方同意和棋成功", drawBoard.agreeDraw());
        check("和棋状态正确", drawBoard.getStatus() == GameStatus.DRAW);
        check("和棋后不能继续走棋", !drawBoard.tryMove(6, 4, 5, 4));
    }

    // ------------------------------------------------------------------
    // 登录与存档
    // ------------------------------------------------------------------

    private static void testAuthAndSaveLoad() throws Exception {
        Path tempDir = Files.createTempDirectory("xiangqi-selftest-");
        try {
            AuthService authService = new AuthService(tempDir);
            User registered = authService.register("Alice", "secret".toCharArray());
            check("注册成功且用户名保留大小写", "Alice".equals(registered.getUsername()));
            check("新用户不是游客", !registered.isGuest());

            expectThrows("用户名重复（不区分大小写）",
                    IllegalArgumentException.class,
                    () -> authService.register("alice", "other".toCharArray()));
            expectThrows("非法用户名被拒绝",
                    IllegalArgumentException.class,
                    () -> authService.register("坏名字!", "secret".toCharArray()));

            // 模拟关闭程序后重新打开：新的 AuthService 从同一目录读取
            AuthService reopened = new AuthService(tempDir);
            check("重启程序后仍能登录", reopened.login("alice", "secret".toCharArray()) != null);
            check("错误密码无法登录", reopened.login("alice", "wrong".toCharArray()) == null);
            check("不存在的用户无法登录", reopened.login("bob", "secret".toCharArray()) == null);

            Board board = new Board();
            board.tryMove(6, 4, 5, 4); // 红兵
            board.tryMove(3, 4, 4, 4); // 黑卒
            board.tryMove(9, 1, 7, 2); // 红马
            board.tryMove(0, 1, 2, 2); // 黑马

            SaveManager saveManager = new SaveManager(tempDir);
            saveManager.save("Alice", 1, board);
            check("存档文件已创建", Files.exists(saveManager.getSavePath("Alice", 1)));

            Board loaded = new Board();
            saveManager.loadInto("Alice", 1, loaded);
            check("读档后仍然轮到红方", loaded.isRedToMove());
            check("读档后着法记录完整", loaded.getMoveHistory().size() == 4);
            check("读档后棋子位置一致",
                    isType(loaded.getPieceAt(5, 4), PieceType.SOLDIER, true)
                            && isType(loaded.getPieceAt(4, 4), PieceType.SOLDIER, false)
                            && isType(loaded.getPieceAt(7, 2), PieceType.HORSE, true));
            check("读档后上一步正确", loaded.getLastMove() != null
                    && loaded.getLastMove().getPieceType() == PieceType.HORSE);

            expectThrows("不能读取别人的存档",
                    FileNotFoundException.class,
                    () -> new SaveManager(tempDir).loadInto("Bob", 1, new Board()));

            Path saveFile = saveManager.getSavePath("Alice", 1);
            String validContent = Files.readString(saveFile, StandardCharsets.UTF_8);
            check("存档中包含棋盘标记", validContent.contains("board:"));
            check("存档中包含着法记录", validContent.contains("moves=4"));

            Files.writeString(saveFile, "这不是一个合法存档\n", StandardCharsets.UTF_8);
            expectThrows("损坏的存档被拒绝",
                    InvalidSaveException.class,
                    () -> saveManager.loadInto("Alice", 1, new Board()));

            Files.writeString(saveFile, validContent.replace(" G ", " . "), StandardCharsets.UTF_8);
            expectThrows("缺少将帅的存档被拒绝",
                    InvalidSaveException.class,
                    () -> saveManager.loadInto("Alice", 1, new Board()));
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ------------------------------------------------------------------
    // 辅助方法
    // ------------------------------------------------------------------

    /** 只放双方将帅的棋盘，两个将帅不在同一列。 */
    private static Piece[][] emptyGrid() {
        Piece[][] grid = new Piece[Board.ROWS][Board.COLS];
        grid[9][3] = new GeneralPiece(true);
        grid[0][5] = new GeneralPiece(false);
        return grid;
    }

    private static Board boardWith(Piece[][] grid, boolean redToMove) {
        GameSnapshot snapshot = new GameSnapshot(
                grid, redToMove, GameStatus.ONGOING, "", List.of());
        return Board.fromSnapshot(snapshot);
    }

    private static boolean isType(Piece piece, PieceType type, boolean red) {
        return piece != null && piece.getType() == type && piece.isRed() == red;
    }

    private static int countPieces(Board board) {
        int count = 0;
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                if (board.getPieceAt(row, col) != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private static <T extends Throwable> void expectThrows(
            String name, Class<T> expectedType, ThrowingAction action) {
        try {
            action.run();
            check(name + "（没有抛出异常）", false);
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                check(name, true);
            } else {
                check(name + "（抛出了 " + throwable.getClass().getSimpleName() + "）", false);
            }
        }
    }

    private static void deleteRecursively(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(file -> {
                        try {
                            Files.deleteIfExists(file);
                        } catch (Exception ignored) {
                            // 测试清理失败不影响测试结果
                        }
                    });
        }
    }
}
