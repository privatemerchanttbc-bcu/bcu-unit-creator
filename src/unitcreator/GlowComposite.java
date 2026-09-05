package unitcreator;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

final class GlowComposite implements Composite, CompositeContext {

    private final float opa;

    GlowComposite(int opacity) {
        int o = opacity < 0 ? 0 : (opacity > 255 ? 255 : opacity);
        this.opa = o / 255f;
    }

    @Override
    public CompositeContext createContext(ColorModel src, ColorModel dst, RenderingHints hints) {
        return this;
    }

    @Override
    public void dispose() {}

    @Override
    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
        if (packed(src) && packed(dstIn) && packed(dstOut)) {
            composePacked(src, dstIn, dstOut);
            return;
        }
        composeGeneric(src, dstIn, dstOut);
    }

    private static boolean packed(Raster r) {
        return r.getTransferType() == DataBuffer.TYPE_INT && r.getNumBands() >= 3;
    }

    private void composePacked(Raster src, Raster dstIn, WritableRaster dstOut) {
        int w = Math.min(Math.min(src.getWidth(), dstIn.getWidth()), dstOut.getWidth());
        int h = Math.min(Math.min(src.getHeight(), dstIn.getHeight()), dstOut.getHeight());
        if (w <= 0 || h <= 0) return;
        boolean srcAlpha = src.getNumBands() > 3;

        int[] sRow = new int[w];
        int[] dRow = new int[w];
        int sx = src.getMinX(), sy = src.getMinY();
        int dx = dstIn.getMinX(), dy = dstIn.getMinY();
        int ox = dstOut.getMinX(), oy = dstOut.getMinY();

        for (int y = 0; y < h; y++) {
            src.getDataElements(sx, sy + y, w, 1, sRow);
            dstIn.getDataElements(dx, dy + y, w, 1, dRow);
            for (int x = 0; x < w; x++) {
                int sp = sRow[x];
                int a = srcAlpha ? (sp >>> 24) : 255;
                if (a == 0) continue;
                float f = a * opa * (1f / 255f);
                int dp = dRow[x];
                int r = ((dp >> 16) & 255) + (int) (((sp >> 16) & 255) * f + 0.5f);
                int g = ((dp >> 8) & 255) + (int) (((sp >> 8) & 255) * f + 0.5f);
                int b = (dp & 255) + (int) ((sp & 255) * f + 0.5f);
                int na = (dp >>> 24) + (int) (a * opa + 0.5f);
                if (r > 255) r = 255;
                if (g > 255) g = 255;
                if (b > 255) b = 255;
                if (na > 255) na = 255;
                dRow[x] = (na << 24) | (r << 16) | (g << 8) | b;
            }
            dstOut.setDataElements(ox, oy + y, w, 1, dRow);
        }
    }

    private void composeGeneric(Raster src, Raster dstIn, WritableRaster dstOut) {
        int w = Math.min(Math.min(src.getWidth(), dstIn.getWidth()), dstOut.getWidth());
        int h = Math.min(Math.min(src.getHeight(), dstIn.getHeight()), dstOut.getHeight());
        if (w <= 0 || h <= 0) return;

        int sb = src.getNumBands();
        int db = dstIn.getNumBands();
        int ob = dstOut.getNumBands();
        int nb = Math.min(db, ob);
        if (sb < 1 || nb < 1) return;

        int[] sRow = new int[w * sb];
        int[] dRow = new int[w * db];
        int[] oRow = db == ob ? dRow : new int[w * ob];

        int sx = src.getMinX(), sy = src.getMinY();
        int dx = dstIn.getMinX(), dy = dstIn.getMinY();
        int ox = dstOut.getMinX(), oy = dstOut.getMinY();

        for (int y = 0; y < h; y++) {
            src.getPixels(sx, sy + y, w, 1, sRow);
            dstIn.getPixels(dx, dy + y, w, 1, dRow);
            if (oRow != dRow) System.arraycopy(dRow, 0, oRow, 0, Math.min(dRow.length, oRow.length));

            for (int x = 0; x < w; x++) {
                int si = x * sb;
                int oi = x * ob;
                int a = sb > 3 ? sRow[si + 3] : 255;
                if (a == 0) continue;
                float f = a / 255f * opa;
                int colors = Math.min(3, Math.min(sb, nb));
                for (int c = 0; c < colors; c++) {
                    int v = oRow[oi + c] + Math.round(sRow[si + c] * f);
                    oRow[oi + c] = v > 255 ? 255 : v;
                }
                if (ob > 3) {
                    int na = oRow[oi + 3] + Math.round(a * opa);
                    oRow[oi + 3] = na > 255 ? 255 : na;
                }
            }
            dstOut.setPixels(ox, oy + y, w, 1, oRow);
        }
    }
}
