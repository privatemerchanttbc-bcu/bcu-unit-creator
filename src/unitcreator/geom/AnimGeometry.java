package unitcreator.geom;

import common.CommonStatic;
import common.system.P;
import common.system.fake.FakeImage;
import common.util.anim.EAnimI;
import common.util.anim.EPart;
import common.util.anim.MaModel;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public final class AnimGeometry {

    private static final double[] ONE = {1, 1};

    private AnimGeometry() {}

    public static final class PartBox {
        public final double[] xs = new double[4];
        public final double[] ys = new double[4];
        public double pivotX, pivotY;
        public boolean hasSprite;
    }

    public static AffineTransform partTransform(EPart p, float siz) {
        EPart fa = p.getFa();
        AffineTransform at = fa != null ? partTransform(fa, siz) : new AffineTransform();
        MaModel model = p.getModel();
        double hf = p.getValRaw(13);
        double vf = p.getValRaw(14);
        if (p.getParts()[0] != p) {
            double[] fs = fa != null ? partSize(fa) : ONE;
            at.translate(fs[0] * siz * p.getValRaw(4), fs[1] * siz * p.getValRaw(5));
            at.scale(hf, vf);
        } else {
            if (model.confs.length > 0) {
                int[] data = model.confs[0];
                double[] bs = baseSize(p, false);
                at.translate(-(bs[0] * data[2] * siz * hf), -(bs[1] * data[3] * siz * vf));
            }
            double[] gs = partSize(p);
            at.translate(gs[0] * siz * p.getValRaw(6) * hf, gs[1] * siz * p.getValRaw(7) * vf);
            at.scale(hf, vf);
        }
        double angle = p.getValRaw(11);
        if (angle != 0)
            at.rotate(Math.PI * 2 * angle / model.ints[1]);
        return at;
    }

    public static double[] partSize(EPart p) {
        MaModel model = p.getModel();
        double mi = 1.0 / model.ints[0];
        P sca = p.getSca();
        double g = p.getValRaw(8) * mi * mi;
        double sx = sca.x * g, sy = sca.y * g;
        EPart fa = p.getFa();
        if (fa != null) {
            double[] f = partSize(fa);
            sx *= f[0];
            sy *= f[1];
        }
        return new double[]{sx, sy};
    }

    private static double[] baseSize(EPart p, boolean parent) {
        MaModel model = p.getModel();
        if (model.confs.length == 0)
            return ONE;
        double mi = 1.0 / model.ints[0];
        int ind = p.getInd();
        if (parent) {
            if (p.getFa() != null) {
                double[] f = baseSize(p.getFa(), true);
                return new double[]{f[0] * Math.signum(model.parts[ind][8]), f[1] * Math.signum(model.parts[ind][9])};
            }
            return new double[]{Math.signum(model.parts[ind][8]), Math.signum(model.parts[ind][9])};
        }
        int conf = model.confs[0][0];
        if (conf == -1)
            return new double[]{model.parts[0][8] * mi, model.parts[0][9] * mi};
        if (conf == ind)
            return new double[]{model.parts[conf][8] * mi, model.parts[conf][9] * mi};
        double[] f = baseSize(p.getParts()[conf], true);
        return new double[]{f[0] * model.parts[conf][8] * mi, f[1] * model.parts[conf][9] * mi};
    }

    public static PartBox partBox(EPart p, EAnimI ent, float siz) {
        AffineTransform at = partTransform(p, siz);
        PartBox b = new PartBox();
        Point2D pt = at.transform(new Point2D.Double(0, 0), null);
        b.pivotX = pt.getX();
        b.pivotY = pt.getY();
        int img = (int) p.getValRaw(2);
        FakeImage fimg = img >= 0 ? ent.anim().parts(img) : null;
        if (fimg == null)
            return b;
        b.hasSprite = true;
        double[] sz = partSize(p);
        double tpx = p.getValRaw(6) * sz[0] * siz, tpy = p.getValRaw(7) * sz[1] * siz;
        double scx = fimg.getWidth() * sz[0] * siz, scy = fimg.getHeight() * sz[1] * siz;
        double[][] loc = {{-tpx, -tpy}, {-tpx + scx, -tpy}, {-tpx + scx, -tpy + scy}, {-tpx, -tpy + scy}};
        for (int i = 0; i < 4; i++) {
            pt = at.transform(new Point2D.Double(loc[i][0], loc[i][1]), null);
            b.xs[i] = pt.getX();
            b.ys[i] = pt.getY();
        }
        return b;
    }

    public static double[] toWorld(int mx, int my, int w, int h, float oriX, float oriY) {
        return new double[]{mx - w / 2.0 + oriX, my - h * 3.0 / 4.0 + oriY};
    }

    public static int pick(EAnimI ent, double wx, double wy, float siz) {
        EPart[] order = ent.getOrder();
        if (order == null)
            return -1;
        double dead = CommonStatic.getConfig().deadOpa * 0.01 + 1.0E-5;
        for (int i = order.length - 1; i >= 0; i--) {
            EPart p = order[i];
            if (containsPoint(p, ent, wx, wy, siz, dead))
                return p.getInd();
        }
        return -1;
    }

    public static List<Integer> pickAll(EAnimI ent, double wx, double wy, float siz) {
        List<Integer> res = new ArrayList<Integer>();
        EPart[] order = ent.getOrder();
        if (order == null)
            return res;
        double dead = CommonStatic.getConfig().deadOpa * 0.01 + 1.0E-5;
        for (int i = order.length - 1; i >= 0; i--) {
            EPart p = order[i];
            if (containsPoint(p, ent, wx, wy, siz, dead))
                res.add(p.getInd());
        }
        return res;
    }

    private static boolean containsPoint(EPart p, EAnimI ent, double wx, double wy, float siz, double dead) {
        int img = (int) p.getValRaw(2);
        if (img < 0 || (int) p.getValRaw(1) < 0)
            return false;
        if (p.opa() < dead)
            return false;
        FakeImage fimg = ent.anim().parts(img);
        if (fimg == null)
            return false;
        try {
            AffineTransform at = partTransform(p, siz);
            Point2D loc = at.inverseTransform(new Point2D.Double(wx, wy), null);
            double[] sz = partSize(p);
            double tpx = p.getValRaw(6) * sz[0] * siz, tpy = p.getValRaw(7) * sz[1] * siz;
            double scx = fimg.getWidth() * sz[0] * siz, scy = fimg.getHeight() * sz[1] * siz;
            double x0 = Math.min(-tpx, -tpx + scx), x1 = Math.max(-tpx, -tpx + scx);
            double y0 = Math.min(-tpy, -tpy + scy), y1 = Math.max(-tpy, -tpy + scy);
            return loc.getX() >= x0 && loc.getX() <= x1 && loc.getY() >= y0 && loc.getY() <= y1;
        } catch (NoninvertibleTransformException ignored) {
            return false;
        }
    }
}
