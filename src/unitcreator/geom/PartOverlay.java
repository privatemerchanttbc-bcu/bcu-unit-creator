package unitcreator.geom;

import common.system.fake.FakeGraphics;
import common.util.anim.EAnimI;
import common.util.anim.EPart;

public final class PartOverlay {

    private static final float HR = 6f;
    private static final float HS = 5f;
    private static final int CIRCLE_SEGS = 14;

    private PartOverlay() {}

    private static void circle(FakeGraphics g, float cx, float cy, float r) {
        float px = cx + r, py = cy;
        for (int i = 1; i <= CIRCLE_SEGS; i++) {
            double a = Math.PI * 2 * i / CIRCLE_SEGS;
            float nx = cx + (float) (Math.cos(a) * r);
            float ny = cy + (float) (Math.sin(a) * r);
            g.drawLine(px, py, nx, ny);
            px = nx;
            py = ny;
        }
    }

    public static void drawHover(FakeGraphics g, EAnimI ent, int partInd, float siz) {
        if (partInd < 0 || ent.ent == null || partInd >= ent.ent.length)
            return;
        EPart p = ent.ent[partInd];
        AnimGeometry.PartBox b = AnimGeometry.partBox(p, ent, siz);
        if (!b.hasSprite)
            return;
        g.setColor(185, 185, 185);
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            g.drawLine((float) b.xs[i], (float) b.ys[i], (float) b.xs[j], (float) b.ys[j]);
        }
    }

    public static void draw(FakeGraphics g, EAnimI ent, int partInd, float siz,
                            boolean showRotate, boolean showScale) {
        if (partInd < 0 || ent.ent == null || partInd >= ent.ent.length)
            return;
        EPart p = ent.ent[partInd];
        AnimGeometry.PartBox b = AnimGeometry.partBox(p, ent, siz);
        if (b.hasSprite) {
            g.setColor(0, 255, 128);
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                g.drawLine((float) b.xs[i], (float) b.ys[i], (float) b.xs[j], (float) b.ys[j]);
            }
            if (showRotate) {
                for (int i = 0; i < 4; i++) {
                    float cx = (float) b.xs[i], cy = (float) b.ys[i];
                    g.setColor(FakeGraphics.WHITE);
                    circle(g, cx, cy, HR + 1);
                    g.setColor(0, 255, 128);
                    circle(g, cx, cy, HR);
                }
            }
            if (showScale) {
                for (int i = 0; i < 4; i++) {
                    float cx = (float) b.xs[i], cy = (float) b.ys[i];
                    g.setColor(FakeGraphics.WHITE);
                    g.fillRect(cx - HS, cy - HS, HS * 2, HS * 2);
                    g.setColor(0, 160, 255);
                    g.drawRect(cx - HS, cy - HS, HS * 2, HS * 2);
                }
                for (int i = 0; i < 4; i++) {
                    int j = (i + 1) % 4;
                    float mx = (float) ((b.xs[i] + b.xs[j]) / 2);
                    float my = (float) ((b.ys[i] + b.ys[j]) / 2);
                    g.setColor(FakeGraphics.WHITE);
                    g.fillRect(mx - HS, my - HS, HS * 2, HS * 2);
                    g.setColor(0, 160, 255);
                    g.drawRect(mx - HS, my - HS, HS * 2, HS * 2);
                }
            }
        }
        float px = (float) b.pivotX, py = (float) b.pivotY;
        g.setColor(FakeGraphics.RED);
        g.fillRect(px - 3, py - 3, 6, 6);
        g.drawLine(px - 10, py, px + 10, py);
        g.drawLine(px, py - 10, px, py + 10);
    }
}
