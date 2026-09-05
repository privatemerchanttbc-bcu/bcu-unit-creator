package unitcreator;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

final class UCButton extends JButton {

    enum Kind { NEUTRAL, ACCENT, PRIMARY, GHOST }
    enum Glyph { NONE, DICE, WAND, CHECK, SPARK }

    private final Kind kind;
    private final Glyph glyph;
    private boolean hover, pressed, on;

    UCButton(String text, Kind kind, Glyph glyph) {
        super(text);
        this.kind = kind;
        this.glyph = glyph;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(fg(kind));
        setFont(getFont().deriveFont(java.awt.Font.BOLD, 13f));
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
        });
    }

    UCButton(String text, Kind kind) { this(text, kind, Glyph.NONE); }

    void setOn(boolean b) { if (on != b) { on = b; setForeground(fg(effectiveKind())); repaint(); } }
    boolean isOn() { return on; }

    private Kind effectiveKind() { return on ? Kind.ACCENT : kind; }

    @Override public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = Math.max(d.height, 30);
        d.width += glyph == Glyph.NONE ? 14 : 30;
        return d;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        try {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Kind k = effectiveKind();
            float b = pressed ? 0.9f : hover ? 1.08f : 1f;

            RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1.5f, h - 1.5f, 12, 12);
            if (k == Kind.GHOST) {
                g.setColor(UCUI.line());
                g.draw(r);
            } else {
                Color top = adj(topColor(k), b), bot = adj(botColor(k), b);
                g.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
                g.fill(r);
                g.setColor(borderColor(k));
                g.draw(r);

                g.setColor(new Color(255, 255, 255, 30));
                g.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3.5f, h - 3.5f, 10, 10));
            }
            if (hasFocus()) {
                g.setColor(UCUI.GOLD);
                g.draw(new RoundRectangle2D.Float(2f, 2f, w - 5f, h - 5f, 9, 9));
            }

            Color fg = fg(k);
            g.setColor(fg);
            java.awt.FontMetrics fm = g.getFontMetrics(getFont());
            String t = getText();
            int tw = fm.stringWidth(t);
            int gx = glyph == Glyph.NONE ? 0 : 20;
            int x = (w - tw - gx) / 2 + gx;
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            if (glyph != Glyph.NONE) drawGlyph(g, x - gx, h / 2, fg);
            g.setFont(getFont());
            g.drawString(t, x, y);
            g.dispose();
        } catch (Throwable t) {
            super.paintComponent(g0);
        }
    }

    private void drawGlyph(Graphics2D g, int cx, int cy, Color c) {
        g.setColor(c);
        g.setStroke(new java.awt.BasicStroke(1.7f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        int s = 6;
        switch (glyph) {
            case DICE:
                g.drawRoundRect(cx, cy - s, 2 * s, 2 * s, 4, 4);
                dot(g, cx + 4, cy - s + 4); dot(g, cx + 2 * s - 4, cy - s + 4);
                dot(g, cx + 4, cy + s - 4); dot(g, cx + 2 * s - 4, cy + s - 4);
                break;
            case WAND:
                g.drawLine(cx, cy + s, cx + 2 * s, cy - s);
                star(g, cx + 2 * s, cy - s, 3);
                break;
            case CHECK:
                g.drawPolyline(new int[]{cx, cx + s - 1, cx + 2 * s}, new int[]{cy, cy + s, cy - s}, 3);
                break;
            case SPARK:
                star(g, cx + s, cy, s);
                break;
            default:
        }
    }

    private void dot(Graphics2D g, int x, int y) { g.fillOval(x - 1, y - 1, 2, 2); }

    private void star(Graphics2D g, int cx, int cy, int r) {
        g.drawLine(cx - r, cy, cx + r, cy);
        g.drawLine(cx, cy - r, cx, cy + r);
    }

    private static Color topColor(Kind k) {
        switch (k) {
            case ACCENT:  return UCUI.GOLD_HI;
            case PRIMARY: return UCUI.GREEN_HI;
            default:      return UCUI.neutralTop();
        }
    }
    private static Color botColor(Kind k) {
        switch (k) {
            case ACCENT:  return UCUI.GOLD;
            case PRIMARY: return UCUI.GREEN_LO;
            default:      return UCUI.neutralBot();
        }
    }
    private static Color borderColor(Kind k) {
        switch (k) {
            case ACCENT:  return UCUI.GOLD_BORDER;
            case PRIMARY: return UCUI.GREEN_BORDER;
            default:      return UCUI.neutralBorder();
        }
    }
    private static Color fg(Kind k) {
        switch (k) {
            case ACCENT:
            case PRIMARY: return UCUI.INK;
            case GHOST:   return UCUI.muted();
            default:      return UCUI.text();
        }
    }

    private static Color adj(Color c, float f) {
        int r = clamp((int) (c.getRed() * f)), g = clamp((int) (c.getGreen() * f)), b = clamp((int) (c.getBlue() * f));
        return new Color(r, g, b, c.getAlpha());
    }
    private static int clamp(int v) { return v < 0 ? 0 : v > 255 ? 255 : v; }
}
