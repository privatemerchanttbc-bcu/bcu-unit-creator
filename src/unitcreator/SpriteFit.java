package unitcreator;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

final class SpriteFit {

    private static final int ALPHA_MIN = 16;
    private static final float MIN_SAFE_SCALE = 0.18f;
    private static final float MAX_SAFE_SCALE = 6f;
    private static final float MAX_DRAW_SCALE = 20f;

    private SpriteFit() {}

    static int[] clampCut(int[] c, int w, int h) {
        int x = Math.max(0, Math.min(c[0], w - 1));
        int y = Math.max(0, Math.min(c[1], h - 1));
        int cw = Math.max(1, Math.min(c[2], w - x));
        int ch = Math.max(1, Math.min(c[3], h - y));
        return new int[]{x, y, cw, ch};
    }

    static int[] opaqueBox(BufferedImage sheet, int[] cut) {
        int[] c = clampCut(cut, sheet.getWidth(), sheet.getHeight());
        int x0 = c[0] + c[2], y0 = c[1] + c[3], x1 = c[0] - 1, y1 = c[1] - 1;
        for (int y = c[1]; y < c[1] + c[3]; y++) {
            for (int x = c[0]; x < c[0] + c[2]; x++) {
                if ((sheet.getRGB(x, y) >>> 24) > ALPHA_MIN) {
                    if (x < x0) x0 = x;
                    if (x > x1) x1 = x;
                    if (y < y0) y0 = y;
                    if (y > y1) y1 = y;
                }
            }
        }
        if (x1 < x0 || y1 < y0) return c;
        return new int[]{x0, y0, x1 - x0 + 1, y1 - y0 + 1};
    }

    static float[] centroid(BufferedImage sheet, int[] cut) {
        int[] c = clampCut(cut, sheet.getWidth(), sheet.getHeight());
        long sx = 0, sy = 0, cnt = 0;
        for (int y = c[1]; y < c[1] + c[3]; y++) {
            for (int x = c[0]; x < c[0] + c[2]; x++) {
                if ((sheet.getRGB(x, y) >>> 24) > ALPHA_MIN) { sx += x; sy += y; cnt++; }
            }
        }
        if (cnt == 0) return new float[]{c[0] + c[2] / 2f, c[1] + c[3] / 2f};
        return new float[]{(float) sx / cnt, (float) sy / cnt};
    }

    static boolean safeToMatch(BufferedImage aSheet, int[] aCut, BufferedImage bSheet, int[] bCut) {
        if (aSheet == null || bSheet == null || aCut == null || bCut == null) return false;
        float s = matchScale(aSheet, aCut, bSheet, bCut);
        return s >= MIN_SAFE_SCALE && s <= MAX_SAFE_SCALE;
    }

    static boolean drawMatched(Graphics2D g, BufferedImage aSheet, int[] aCut,
                               BufferedImage bSheet, int[] bCut, int slotX, int slotY) {
        return drawMatched(g, aSheet, aCut, bSheet, bCut, slotX, slotY, true);
    }

    static boolean drawMatchedLoose(Graphics2D g, BufferedImage aSheet, int[] aCut,
                                    BufferedImage bSheet, int[] bCut, int slotX, int slotY) {
        return drawMatched(g, aSheet, aCut, bSheet, bCut, slotX, slotY, false);
    }

    private static boolean drawMatched(Graphics2D g, BufferedImage aSheet, int[] aCut,
                                       BufferedImage bSheet, int[] bCut, int slotX, int slotY,
                                       boolean enforceSafeScale) {
        int[] ac = clampCut(aCut, aSheet.getWidth(), aSheet.getHeight());
        int[] ab = opaqueBox(aSheet, ac);
        float[] acen = centroid(aSheet, ac);
        int[] bb = opaqueBox(bSheet, bCut);
        float[] bcen = centroid(bSheet, bb);
        float s = Math.min((float) ab[2] / bb[2], (float) ab[3] / bb[3]);
        if (enforceSafeScale && (s < MIN_SAFE_SCALE || s > MAX_SAFE_SCALE)) return false;
        s = Math.min(s, MAX_DRAW_SCALE);
        int dw = Math.max(1, Math.round(bb[2] * s));
        int dh = Math.max(1, Math.round(bb[3] * s));
        float dx = slotX + (acen[0] - ac[0]) - (bcen[0] - bb[0]) * s;
        float dy = slotY + (acen[1] - ac[1]) - (bcen[1] - bb[1]) * s;
        int ix = Math.round(Math.max(slotX, Math.min(dx, slotX + ac[2] - dw)));
        int iy = Math.round(Math.max(slotY, Math.min(dy, slotY + ac[3] - dh)));
        g.drawImage(bSheet, ix, iy, ix + dw, iy + dh,
                bb[0], bb[1], bb[0] + bb[2], bb[1] + bb[3], null);
        return true;
    }

    private static float matchScale(BufferedImage aSheet, int[] aCut, BufferedImage bSheet, int[] bCut) {
        int[] ac = clampCut(aCut, aSheet.getWidth(), aSheet.getHeight());
        int[] ab = opaqueBox(aSheet, ac);
        int[] bb = opaqueBox(bSheet, bCut);
        return Math.min((float) ab[2] / bb[2], (float) ab[3] / bb[3]);
    }
}
