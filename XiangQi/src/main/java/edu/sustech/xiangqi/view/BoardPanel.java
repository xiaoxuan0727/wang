package edu.sustech.xiangqi.view;

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.Piece;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

// 绘制棋盘、棋子和选中效果
public final class BoardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int CELL_SIZE = 52;
    private static final int MARGIN = 36;
    private static final int PIECE_RADIUS = 21;
    private static final int HIT_RADIUS = 23;

    private final Board board;
    private int highlightedRow = -1;
    private int highlightedCol = -1;

    public BoardPanel(Board board) {
        this.board = board;
        setBackground(new Color(239, 218, 179));
        setPreferredSize(new Dimension(
                MARGIN * 2 + CELL_SIZE * (Board.COLS - 1),
                MARGIN * 2 + CELL_SIZE * (Board.ROWS - 1)));
    }

    // 返回交叉点坐标
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

    public void showSelection(int row, int col) {
        highlightedRow = row;
        highlightedCol = col;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBoard(g);
        drawPieces(g);
        g.dispose();
    }

    private void drawBoard(Graphics2D g) {
        g.setColor(new Color(117, 83, 51));
        g.setStroke(new BasicStroke(1.5f));

        for (int row = 0; row < Board.ROWS; row++) {
            drawGridLine(g, row, 0, row, Board.COLS - 1);
        }
        for (int col = 0; col < Board.COLS; col++) {
            if (col == 0 || col == Board.COLS - 1) {
                drawGridLine(g, 0, col, Board.ROWS - 1, col);
            } else {
                drawGridLine(g, 0, col, 4, col);
                drawGridLine(g, 5, col, Board.ROWS - 1, col);
            }
        }

        // 上下九宫各有两条对角线。
        drawGridLine(g, 0, 3, 2, 5);
        drawGridLine(g, 0, 5, 2, 3);
        drawGridLine(g, 7, 3, 9, 5);
        drawGridLine(g, 7, 5, 9, 3);

        g.setFont(new Font(Font.SERIF, Font.BOLD, 23));
        int riverY = (screenY(4) + screenY(5)) / 2;
        drawCenteredText(g, "楚 河", screenX(2), riverY);
        drawCenteredText(g, "汉 界", screenX(6), riverY);
    }

    private void drawPieces(Graphics2D g) {
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Piece piece = board.getPieceAt(row, col);
                if (piece == null) {
                    continue;
                }

                int x = screenX(col);
                int y = screenY(row);
                if (row == highlightedRow && col == highlightedCol) {
                    g.setColor(new Color(31, 120, 119));
                    g.setStroke(new BasicStroke(3));
                    int radius = PIECE_RADIUS + 5;
                    g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
                }

                g.setColor(new Color(255, 248, 228));
                g.fillOval(x - PIECE_RADIUS, y - PIECE_RADIUS, PIECE_RADIUS * 2, PIECE_RADIUS * 2);
                g.setColor(new Color(120, 79, 45));
                g.setStroke(new BasicStroke(2));
                g.drawOval(x - PIECE_RADIUS, y - PIECE_RADIUS, PIECE_RADIUS * 2, PIECE_RADIUS * 2);
                g.setColor(piece.isRed() ? new Color(177, 43, 39) : new Color(43, 43, 43));
                g.setFont(new Font(Font.SERIF, Font.BOLD, 28));
                drawCenteredText(g, piece.getName(), x, y);
            }
        }
    }

    private void drawGridLine(Graphics2D g, int fromRow, int fromCol, int toRow, int toCol) {
        g.drawLine(screenX(fromCol), screenY(fromRow), screenX(toCol), screenY(toRow));
    }

    private void drawCenteredText(Graphics2D g, String text, int x, int y) {
        FontMetrics metrics = g.getFontMetrics();
        int baseline = y + (metrics.getAscent() - metrics.getDescent()) / 2;
        g.drawString(text, x - metrics.stringWidth(text) / 2, baseline);
    }

    private int screenX(int col) {
        return MARGIN + col * CELL_SIZE;
    }

    private int screenY(int row) {
        return MARGIN + row * CELL_SIZE;
    }
}
