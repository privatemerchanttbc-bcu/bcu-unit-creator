package unitcreator;

import common.system.fake.FakeGraphics;
import common.system.fake.FakeImage;
import common.system.fake.FakeTransform;

import java.awt.Graphics2D;
import java.lang.reflect.Field;

final class SafeFG implements FakeGraphics {

    private static boolean warned;

    private final FakeGraphics g;
    private final Graphics2D awt;

    SafeFG(FakeGraphics g) {
        this.g = g;
        this.awt = unwrap(g);
    }

    private static Graphics2D unwrap(FakeGraphics g) {
        try {
            for (Class<?> c = g.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (!Graphics2D.class.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true);
                    Object o = f.get(g);
                    if (o instanceof Graphics2D) return (Graphics2D) o;
                }
            }
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                Logger.err("UnitCreator: glow blending unavailable, falling back to alpha", t);
            }
        }
        return null;
    }

    @Override
    public void setComposite(int mode, int a, int b) {
        if (mode != BLEND) {
            g.setComposite(mode, a, b);
            return;
        }
        if (awt != null) {
            awt.setComposite(new GlowComposite(a));
            return;
        }
        g.setComposite(TRANS, a, 0);
    }

    @Override public void colRect(float x, float y, float w, float h, int r, int gr, int bl, int al) {
        g.colRect(x, y, w, h, r, gr, bl, al);
    }

    @Override public void delete(FakeTransform t) { g.delete(t); }

    @Override public void drawImage(FakeImage i, float x, float y) { g.drawImage(i, x, y); }

    @Override public void drawImage(FakeImage i, float x, float y, float w, float h) {
        g.drawImage(i, x, y, w, h);
    }

    @Override public void drawLine(float x0, float y0, float x1, float y1) {
        g.drawLine(x0, y0, x1, y1);
    }

    @Override public void drawOval(float x, float y, float w, float h) { g.drawOval(x, y, w, h); }

    @Override public void drawRect(float x, float y, float w, float h) { g.drawRect(x, y, w, h); }

    @Override public void fillOval(float x, float y, float w, float h) { g.fillOval(x, y, w, h); }

    @Override public void fillRect(float x, float y, float w, float h) { g.fillRect(x, y, w, h); }

    @Override public FakeTransform getTransform() { return g.getTransform(); }

    @Override public void gradRect(float x, float y, float w, float h, float sx, float sy,
                                   int[] s, float ex, float ey, int[] e) {
        g.gradRect(x, y, w, h, sx, sy, s, ex, ey, e);
    }

    @Override public void gradRectAlpha(float x, float y, float w, float h, float sx, float sy,
                                        int sa, int[] s, float ex, float ey, int ea, int[] e) {
        g.gradRectAlpha(x, y, w, h, sx, sy, sa, s, ex, ey, ea, e);
    }

    @Override public void rotate(float d) { g.rotate(d); }

    @Override public void scale(float x, float y) { g.scale(x, y); }

    @Override public void setColor(int c) { g.setColor(c); }

    @Override public void setColor(int r, int gr, int b) { g.setColor(r, gr, b); }

    @Override public void setRenderingHint(int k, int v) { g.setRenderingHint(k, v); }

    @Override public void setTransform(FakeTransform t) { g.setTransform(t); }

    @Override public void translate(float x, float y) { g.translate(x, y); }
}
