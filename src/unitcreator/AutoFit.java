package unitcreator;

import common.system.P;
import common.util.anim.AnimCE;
import common.util.anim.AnimU;
import common.util.anim.EAnimU;
import common.util.anim.EPart;
import common.util.anim.MaModel;
import unitcreator.geom.AnimGeometry;
import unitcreator.geom.MeasuringGraphics;

final class AutoFit {

    private static final int MIN_SCALE = 50, MAX_SCALE = 20000;
    private static final double MIN_BODY = 0.05, MAX_BODY = 20.0;
    private static final double EPS = 1.0E-6;

    private AutoFit() {}

    static void seed(AnimCE work, GraftOp op) {
        double s = 1000.0 * chain(work, op) * body(work, op);
        op.setAll(op.scale, clamp((int) Math.round(s), MIN_SCALE, MAX_SCALE));
        op.setAll(op.zOffset, zSeed(work, op));
        op.setAll(op.offX, 0);
        op.setAll(op.offY, 0);
        op.setAll(op.angle, 0);
        op.mirror = false;
    }

    static int[] zSpan(MaModel m) {
        int lo = 0, hi = 0;
        if (m != null && m.parts != null) {
            boolean first = true;
            for (int i = 0; i < m.parts.length; i++) {
                int[] r = m.parts[i];
                if (r == null || r.length <= SubtreeCopier.Z) continue;
                int z = r[SubtreeCopier.Z];
                if (first) { lo = z; hi = z; first = false; }
                else { if (z < lo) lo = z; if (z > hi) hi = z; }
            }
        }
        int pad = Math.max(8, hi - lo + 4);
        return new int[]{lo - pad, hi + pad};
    }

    private static double chain(AnimCE work, GraftOp op) {
        try {
            EAnimU we = prime(work.getEAnim(AnimU.UType.IDLE));
            EAnimU de = prime(op.donor.eanim(AnimU.UType.IDLE));
            EPart target = part(we, op.aParent);
            EPart root = part(de, op.bRoot);
            if (target == null || root == null) return 1.0;
            double[] ts = AnimGeometry.partSize(target);
            EPart fa = root.getFa();
            double[] ds = fa == null ? new double[]{1.0, 1.0} : AnimGeometry.partSize(fa);
            double t = Math.abs(ts[0]), d = Math.abs(ds[0]);
            if (t < EPS || d < EPS) return 1.0;
            return d / t;
        } catch (Throwable e) {
            return 1.0;
        }
    }

    private static double body(AnimCE work, GraftOp op) {
        double a = height(safeEAnim(work));
        double b = height(op.donor.eanim(AnimU.UType.IDLE));
        if (a <= 0 || b <= 0) return 1.0;
        double r = a / b;
        if (r < MIN_BODY) return MIN_BODY;
        if (r > MAX_BODY) return MAX_BODY;
        return r;
    }

    private static EAnimU safeEAnim(AnimCE work) {
        try {
            return work.getEAnim(AnimU.UType.IDLE);
        } catch (Throwable t) {
            return null;
        }
    }

    private static double height(EAnimU e) {
        try {
            if (e == null) return 0;
            e.update(false);
            MeasuringGraphics mg = new MeasuringGraphics();
            e.draw(mg, new P(0f, 0f), 1f);
            return mg.hasBox() ? mg.maxY() - mg.minY() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int zSeed(AnimCE work, GraftOp op) {
        try {
            AnimU<?> d = op.donor.anim();
            int tz = zOf(work.mamodel, op.aParent);
            int rz = zOf(d == null ? null : d.mamodel, op.bRoot);
            return tz + 1 - rz;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int zOf(MaModel m, int i) {
        if (m == null || m.parts == null || i < 0 || i >= m.parts.length) return 0;
        int[] r = m.parts[i];
        return r != null && r.length > SubtreeCopier.Z ? r[SubtreeCopier.Z] : 0;
    }

    private static EPart part(EAnimU e, int i) {
        return e != null && e.ent != null && i >= 0 && i < e.ent.length ? e.ent[i] : null;
    }

    private static EAnimU prime(EAnimU e) {
        if (e == null) return null;
        try {
            e.update(false);
            e.setTime(0f);
        } catch (Throwable ignored) {}
        return e;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
