package edu.sustech.xiangqi.view;

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.Move;
import edu.sustech.xiangqi.model.Piece;
import edu.sustech.xiangqi.model.Position;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.Collections;
import java.util.List;

/**
 * 棋盘视图：只负责“把 Board 画出来”和“把鼠标坐标转换成棋盘交叉点”。
 *
 * <p>它不修改棋盘数据，选中状态和合法落点由控制器通过
 * {@link #showSelection(int, int, List)} 传进来。</p>
 */
public final class BoardPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final int CELL_SIZE = 54;
    private static final int MARGIN = 46;
    private static final int PIECE_RADIUS = 22;
    private static final int HIT_RADIUS = 25;

    private static final Color GRID_COLOR = new Color(104, 72, 42);
    private static final Color RED_COLOR = new Color(176, 36, 32);
    private static final Color BLACK_COLOR = new Color(45, 45, 45);
    private static final Color SELECT_COLOR = new Color(22, 116, 126);
    private static final Color TARGET_COLOR = new Color(46, 139, 87);
    private static final Color LAST_MOVE_COLOR = new Color(214, 132, 35);

    private final Board board;

    private int selectedRow = -1;
    private int selectedCol = -1;
    private List<Position> legalTargets = Collections.emptyList();

    public BoardPanel(Board board) {
        this.board = board;
        setOpaque(true);
        setBackground(new Color(239, 218, 179));
        setPreferredSize(new Dimension(
                MARGIN * 2 + CELL_SIZE * (Board.COLS - 1),
                MARGIN * 2 + CELL_SIZE * (Board.ROWS - 1)));
    }

    /**
     * 把窗口中的像素坐标转换为最近的棋盘交叉点。
     *
     * @return 距离交叉点太远或超出棋盘时返回 null
     */
    public Point getIntersectionAt(int x, int y) {
        int col = (int) Math.round((x - MARGIN) / (double) CELL_SIZE);
        int row = (int) Math.round((y - MARGIN) / (double) CELL_SIZE);

        if (!board.isInside(row, col)) {
            return null;
        }

        int dx = x - screenX(col);
        int dy = y - screenY(row);
        if (dx * dx + dy * dy > HIT_RADIUS * HIT_RADIUS) {
            return null;
        }
        return new Point(col, row);
    }

    /** 显示选中棋子及其全部合法落点；传 (-1,-1) 可清除。 */
    public void showSelection(int row, int col, List<Position> targets) {
        selectedRow = row;
        selectedCol = col;
        legalTargets = targets == null ? Collections.emptyList() : targets;
        repaint();
    }

    public void clearSelection() {
        showSelection(-1, -1, Collections.emptyList());
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            paintBackground(g);
            paintBoard(g);
            paintCoordinates(g);
            paintLastMove(g);
            paintLegalTargets(g);
            paintPieces(g);
            paintSelection(g);
        } finally {
            g.dispose();
        }
    }

    private void paintBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(
                0, 0, new Color(246, 228, 192),
                0, getHeight(), new Color(226, 198, 150)));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void paintBoard(Graphics2D g) {
        int left = screenX(0);
        int right = screenX(Board.COLS - 1);
        int top = screenY(0);
        int bottom = screenY(Board.ROWS - 1);

        // 棋盘外的双线边框
        g.setColor(new Color(132, 92, 54));
        g.setStroke(new BasicStroke(3f));
        g.drawRect(left - 9, top - 9, right - left + 18, bottom - top + 18);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(left - 14, top - 14, right - left + 28, bottom - top + 28);

        g.setColor(GRID_COLOR);
        g.setStroke(new BasicStroke(1.4f));

        // 横线：10 条完整的线
        for (int row = 0; row < Board.ROWS; row++) {
            g.drawLine(screenX(0), screenY(row), screenX(Board.COLS - 1), screenY(row));
        }

        // 竖线：最左、最右两条完整，其余在楚河汉界处断开
        for (int col = 0; col < Board.COLS; col++) {
            if (col == 0 || col == Board.COLS - 1) {
                g.drawLine(screenX(col), screenY(0), screenX(col), screenY(Board.ROWS - 1));
            } else {
                g.drawLine(screenX(col), screenY(0), screenX(col), screenY(4));
                g.drawLine(screenX(col), screenY(5), screenX(col), screenY(Board.ROWS - 1));
            }
        }

        // 九宫对角线
        g.drawLine(screenX(3), screenY(0), screenX(5), screenY(2));
        g.drawLine(screenX(5), screenY(0), screenX(3), screenY(2));
        g.drawLine(screenX(3), screenY(7), screenX(5), screenY(9));
        g.drawLine(screenX(5), screenY(7), screenX(3), screenY(9));

        // 楚河汉界
        g.setColor(new Color(126, 84, 44));
        g.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        int riverY = (screenY(4) + screenY(5)) / 2;
        drawCenteredText(g, "楚 河", screenX(2), riverY);
        drawCenteredText(g, "汉 界", screenX(6), riverY);
    }

    private void paintCoordinates(Graphics2D g) {
        g.setColor(new Color(120, 96, 68, 180));
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));

        for (int col = 0; col < Board.COLS; col++) {
            drawCenteredText(g, Integer.toString(col), screenX(col), 22);
        }
        for (int row = 0; row < Board.ROWS; row++) {
            drawCenteredText(g, Integer.toString(row), 20, screenY(row));
        }
    }

    private void paintLastMove(Graphics2D g) {
        Move lastMove = board.getLastMove();
        if (lastMove == null) {
            return;
        }

        g.setColor(withAlpha(LAST_MOVE_COLOR, 110));
        int fromX = screenX(lastMove.getFrom().getCol());
        int fromY = screenY(lastMove.getFrom().getRow());
        g.fillOval(fromX - 7, fromY - 7, 14, 14);

        g.setColor(withAlpha(LAST_MOVE_COLOR, 210));
        g.setStroke(new BasicStroke(3f));
        int toX = screenX(lastMove.getTo().getCol());
        int toY = screenY(lastMove.getTo().getRow());
        int radius = PIECE_RADIUS + 4;
        g.drawRoundRect(toX - radius, toY - radius, radius * 2, radius * 2, 12, 12);
    }

    private void paintLegalTargets(Graphics2D g) {
        for (Position target : legalTargets) {
            int x = screenX(target.getCol());
            int y = screenY(target.getRow());
            Piece piece = board.getPieceAt(target.getRow(), target.getCol());

            if (piece == null) {
                g.setColor(withAlpha(TARGET_COLOR, 170));
                g.fillOval(x - 7, y - 7, 14, 14);
            } else {
                // 可以吃子的目标：画一个绿色圆环
                g.setColor(withAlpha(TARGET_COLOR, 220));
                g.setStroke(new BasicStroke(3f));
                int radius = PIECE_RADIUS + 5;
                g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            }
        }
    }

    private void paintPieces(Graphics2D g) {
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Piece piece = board.getPieceAt(row, col);
                if (piece == null) {
                    continue;
                }

                int x = screenX(col);
                int y = screenY(row);

                // 阴影
                g.setColor(new Color(75, 52, 30, 55));
                g.fillOval(x - PIECE_RADIUS + 2, y - PIECE_RADIUS + 4,
                        PIECE_RADIUS * 2, PIECE_RADIUS * 2);

                // 棋子主体
                g.setColor(new Color(255, 248, 226));
                g.fillOval(x - PIECE_RADIUS, y - PIECE_RADIUS,
                        PIECE_RADIUS * 2, PIECE_RADIUS * 2);

                g.setColor(new Color(120, 79, 45));
                g.setStroke(new BasicStroke(2f));
                g.drawOval(x - PIECE_RADIUS, y - PIECE_RADIUS,
                        PIECE_RADIUS * 2, PIECE_RADIUS * 2);
                g.setStroke(new BasicStroke(1f));
                g.drawOval(x - PIECE_RADIUS + 4, y - PIECE_RADIUS + 4,
                        (PIECE_RADIUS - 4) * 2, (PIECE_RADIUS - 4) * 2);

                g.setColor(piece.isRed() ? RED_COLOR : BLACK_COLOR);
                g.setFont(new Font("Microsoft YaHei", Font.BOLD, 27));
                drawCenteredText(g, piece.getName(), x, y);
            }
        }
    }

    private void paintSelection(Graphics2D g) {
        if (selectedRow < 0 || selectedCol < 0) {
            return;
        }
        int x = screenX(selectedCol);
        int y = screenY(selectedRow);
        g.setColor(SELECT_COLOR);
        g.setStroke(new BasicStroke(3f));
        int radius = PIECE_RADIUS + 6;
        g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private void drawCenteredText(Graphics2D g, String text, int centerX, int centerY) {
        FontMetrics metrics = g.getFontMetrics();
        int baseline = centerY + (metrics.getAscent() - metrics.getDescent()) / 2;
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }

    private int screenX(int col) {
        return MARGIN + col * CELL_SIZE;
    }

    private int screenY(int row) {
        return MARGIN + row * CELL_SIZE;
    }
}
