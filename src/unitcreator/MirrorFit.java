package unitcreator;

import common.util.anim.AnimCE;
import common.util.anim.AnimU;
import common.util.anim.EAnimU;
import common.util.anim.EPart;
import unitcreator.geom.AnimGeometry;

final class MirrorFit {

    private static final double EPS = 1.0E-6;

    private MirrorFit() {}

    static void apply(AnimCE work, GraftOp op) {
        if (work == null || op == null || !op.applied()) return;
        try {
            double before = centerX(work, op);
            SubtreeCopier.apply(work, op);
            double after = centerX(work, op);
            if (Double.isNaN(before) || Double.isNaN(after)) return;

            double s = pivotScaleX(work, op);
            if (Math.abs(s) < EPS) return;

            int shift = (int) Math.round(-(after - before) / s);
            if (shift != 0) {
                op.addAll(op.offX, shift);
                SubtreeCopier.apply(work, op);
            }
        } catch (Throwable t) {
            SubtreeCopier.apply(work, op);
        }
    }

    private static double centerX(AnimCE work, GraftOp op) {
        EAnimU e = prime(work);
        if (e == null || e.ent == null) return Double.NaN;
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        int end = op.partBase + op.partCount;
        for (int i = op.partBase; i < end && i < e.ent.length; i++) {
            EPart p = e.ent[i];
            if (p == null) continue;
            AnimGeometry.PartBox b;
            try {
                b = AnimGeometry.partBox(p, e, 1f);
            } catch (Throwable t) {
                continue;
            }
            if (!b.hasSprite) continue;
            for (int k = 0; k < 4; k++) {
                if (b.xs[k] < lo) lo = b.xs[k];
                if (b.xs[k] > hi) hi = b.xs[k];
            }
        }
        return hi < lo ? Double.NaN : (lo + hi) / 2.0;
    }

    private static double pivotScaleX(AnimCE work, GraftOp op) {
        EAnimU e = prime(work);
        if (e == null || e.ent == null) return Double.NaN;
        int i = op.mirrorPart;
        if (i < 0 || i >= e.ent.length || e.ent[i] == null) return Double.NaN;
        try {
            return AnimGeometry.partSize(e.ent[i])[0];
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static EAnimU prime(AnimCE work) {
        try {
            EAnimU e = work.getEAnim(AnimU.UType.IDLE);
            if (e == null) return null;
            e.update(false);
            e.setTime(0f);
            return e;
        } catch (Throwable t) {
            return null;
        }
    }
}
