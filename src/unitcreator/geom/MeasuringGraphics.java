package unitcreator.geom;

import common.system.fake.FakeGraphics;
import common.system.fake.FakeImage;
import common.system.fake.FakeTransform;

public final class MeasuringGraphics implements FakeGraphics {

    public interface AlphaRectProvider {

        float[] alphaRect(FakeImage image);

        default boolean exclude(FakeImage image) { return false; }
    }

    private float m00 = 1f, m01 = 0f, m02 = 0f;
    private float m10 = 0f, m11 = 1f, m12 = 0f;

    private float minX, minY, maxX, maxY;
    private boolean found;

    private float allMinX, allMinY, allMaxX, allMaxY;
    private boolean allFound;

    private final AlphaRectProvider alphaProvider;

    public static final class PartQuad {
        public final float[] pts;
        public final FakeImage image;
        public PartQuad(float[] pts, FakeImage image) {
            this.pts = pts;
            this.image = image;
        }
    }

    private final java.util.List<PartQuad> quads;

    public MeasuringGraphics() {
        this(null);
    }

    public MeasuringGraphics(AlphaRectProvider alphaProvider) {
        this(alphaProvider, false);
    }

    public MeasuringGraphics(AlphaRectProvider alphaProvider, boolean collectQuads) {
        this.alphaProvider = alphaProvider;
        this.quads = collectQuads ? new java.util.ArrayList<PartQuad>() : null;
    }

    public boolean hasBox() { return found || allFound; }
    public float minX() { return found ? minX : allMinX; }
    public float minY() { return found ? minY : allMinY; }
    public float maxX() { return found ? maxX : allMaxX; }
    public float maxY() { return found ? maxY : allMaxY; }

    public java.util.List<PartQuad> quads() {
        return quads == null ? java.util.Collections.<PartQuad>emptyList() : quads;
    }

    @Override
    public void translate(float tx, float ty) {
        m02 = m00 * tx + m01 * ty + m02;
        m12 = m10 * tx + m11 * ty + m12;
    }

    @Override
    public void scale(float sx, float sy) {
        m00 *= sx; m01 *= sy;
        m10 *= sx; m11 *= sy;
    }

    @Override
    public void rotate(float a) {
        float c = (float) Math.cos(a);
        float s = (float) Math.sin(a);
        float n00 = m00 * c + m01 * s;
        float n01 = -m00 * s + m01 * c;
        float n10 = m10 * c + m11 * s;
        float n11 = -m10 * s + m11 * c;
        m00 = n00; m01 = n01; m10 = n10; m11 = n11;
    }

    @Override
    public FakeTransform getTransform() {
        return new Snapshot(m00, m01, m02, m10, m11, m12);
    }

    @Override
    public void setTransform(FakeTransform t) {
        if (t instanceof Snapshot) {
            Snapshot s = (Snapshot) t;
            m00 = s.a; m01 = s.b; m02 = s.c;
            m10 = s.d; m11 = s.e; m12 = s.f;
        }
    }

    @Override
    public void delete(FakeTransform t) {

    }

    @Override
    public void drawImage(FakeImage img, float x, float y) {
        if (img == null) return;
        drawImage(img, x, y, img.getWidth(), img.getHeight());
    }

    @Override
    public void drawImage(FakeImage img, float x, float y, float w, float h) {
        if (w == 0f || h == 0f) return;
        boolean excluded = alphaProvider != null && img != null && alphaProvider.exclude(img);
        float x0 = x, y0 = y, x1 = x + w, y1 = y + h;
        if (alphaProvider != null && img != null) {
            float[] r = alphaProvider.alphaRect(img);
            if (r != null && r.length >= 4) {
                float nx0 = x + w * r[0];
                float ny0 = y + h * r[1];
                float nx1 = x + w * r[2];
                float ny1 = y + h * r[3];
                x0 = nx0; y0 = ny0; x1 = nx1; y1 = ny1;
            }
        }
        accumulate(x0, y0, excluded);
        accumulate(x1, y0, excluded);
        accumulate(x1, y1, excluded);
        accumulate(x0, y1, excluded);

        if (quads != null && !excluded) {
            float[] q = new float[]{
                    projX(x0, y0), projY(x0, y0),
                    projX(x1, y0), projY(x1, y0),
                    projX(x1, y1), projY(x1, y1),
                    projX(x0, y1), projY(x0, y1)};

            float area = Math.abs((q[2] - q[0]) * (q[7] - q[1]) - (q[6] - q[0]) * (q[3] - q[1]));
            if (area > 1e-3f) quads.add(new PartQuad(q, img));
        }
    }

    private float projX(float lx, float ly) { return m00 * lx + m01 * ly + m02; }
    private float projY(float lx, float ly) { return m10 * lx + m11 * ly + m12; }

    private void accumulate(float lx, float ly, boolean excluded) {
        float px = projX(lx, ly);
        float py = projY(lx, ly);
        if (!allFound) {
            allMinX = allMaxX = px;
            allMinY = allMaxY = py;
            allFound = true;
        } else {
            if (px < allMinX) allMinX = px;
            if (px > allMaxX) allMaxX = px;
            if (py < allMinY) allMinY = py;
            if (py > allMaxY) allMaxY = py;
        }
        if (excluded) return;
        if (!found) {
            minX = maxX = px;
            minY = maxY = py;
            found = true;
        } else {
            if (px < minX) minX = px;
            if (px > maxX) maxX = px;
            if (py < minY) minY = py;
            if (py > maxY) maxY = py;
        }
    }

    @Override public void colRect(float a, float b, float c, float d, int e, int f, int g, int h) {}
    @Override public void drawLine(float a, float b, float c, float d) {}
    @Override public void drawOval(float a, float b, float c, float d) {}
    @Override public void drawRect(float a, float b, float c, float d) {}
    @Override public void fillOval(float a, float b, float c, float d) {}
    @Override public void fillRect(float a, float b, float c, float d) {}
    @Override public void gradRect(float a, float b, float c, float d, float e, float f,
                                   int[] g, float h, float i, int[] j) {}
    @Override public void gradRectAlpha(float a, float b, float c, float d, float e, float f,
                                        int g, int[] h, float i, float j, int k, int[] l) {}
    @Override public void setColor(int a) {}
    @Override public void setColor(int a, int b, int c) {}
    @Override public void setComposite(int a, int b, int c) {}
    @Override public void setRenderingHint(int a, int b) {}

    private static final class Snapshot implements FakeTransform {
        final float a, b, c, d, e, f;
        Snapshot(float a, float b, float c, float d, float e, float f) {
            this.a = a; this.b = b; this.c = c; this.d = d; this.e = e; this.f = f;
        }
        @Override public Object getAT() { return null; }
    }
}
