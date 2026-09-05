package unitcreator;

import common.util.anim.AnimCE;
import common.util.anim.AnimU;
import common.util.anim.EAnimU;
import common.util.anim.EPart;
import common.util.anim.MaModel;

final class GraftCheck {

    private static final AnimU.UType[] TYPES = {
            AnimU.UType.WALK, AnimU.UType.IDLE, AnimU.UType.ATK, AnimU.UType.HB
    };
    private static final float ALIVE = 0.01f;
    private static final int MAX_FRAMES = 80;
    private static final int STEP = 2;

    private GraftCheck() {}

    static int visibleAncestor(AnimCE work, int part) {
        MaModel m = work == null ? null : work.mamodel;
        if (m == null || m.parts == null || part < 0 || part >= m.parts.length) return part;
        int at = part, guard = 0;
        while (at >= 0 && at < m.parts.length && guard++ <= m.parts.length) {
            if (chainAlive(work, at)) return at;
            int par = m.parts[at].length > 0 ? m.parts[at][0] : -1;
            if (par < 0 || par >= m.parts.length || par == at) break;
            at = par;
        }
        return 0;
    }

    static boolean chainAlive(AnimCE work, int part) {
        for (int t = 0; t < TYPES.length; t++) {
            EAnimU e = prime(work, TYPES[t]);
            if (e == null || e.ent == null || part < 0 || part >= e.ent.length) continue;
            int len = 1;
            try { len = Math.max(1, Math.min(e.len(), MAX_FRAMES)); } catch (Throwable ignored) {}
            for (int f = 0; f <= len; f += STEP) {
                try { e.setTime(f); } catch (Throwable ignored) {}
                EPart p = e.ent[part];
                if (p == null) continue;
                try {
                    if (p.opa() > ALIVE) return true;
                } catch (Throwable ignored) {}
            }
        }
        return false;
    }

    static String diagnose(AnimCE work, GraftOp op) {
        if (work == null || op == null || !op.applied()) return null;
        int drawn = 0, noImg = 0, noId = 0, clear = 0, missing = 0;
        for (int t = 0; t < TYPES.length; t++) {
            EAnimU e = prime(work, TYPES[t]);
            if (e == null || e.ent == null) continue;
            int len = 1;
            try { len = Math.max(1, Math.min(e.len(), MAX_FRAMES)); } catch (Throwable ignored) {}
            for (int f = 0; f <= len; f += STEP) {
                try { e.setTime(f); } catch (Throwable ignored) {}
                for (int i = 0; i < op.partCount; i++) {
                    int idx = op.partBase + i;
                    if (idx < 0 || idx >= e.ent.length || e.ent[idx] == null) continue;
                    EPart p = e.ent[idx];
                    int img, id;
                    float o;
                    try {
                        img = (int) p.getValRaw(2);
                        id = (int) p.getValRaw(1);
                        o = p.opa();
                    } catch (Throwable x) {
                        continue;
                    }
                    if (img < 0) { noImg++; continue; }
                    if (id < 0) { noId++; continue; }
                    if (o <= ALIVE) { clear++; continue; }
                    boolean has = false;
                    try { has = work.parts(img) != null; } catch (Throwable ignored) {}
                    if (!has) { missing++; continue; }
                    drawn++;
                }
            }
            if (drawn > 0) return null;
        }
        if (drawn > 0) return null;
        if (clear > 0 && clear >= noId && clear >= noImg && clear >= missing)
            return "the part it hangs from is fully transparent in every animation, "
                    + "so the branch inherits that and stays invisible";
        if (noId > 0 && noId >= noImg && noId >= missing)
            return "the copied parts are marked as not drawn in the source unit";
        if (missing > 0)
            return "the copied sprites did not make it into the new sheet";
        if (noImg > 0)
            return "the copied parts carry no sprite";
        return "the copied parts never become visible";
    }

    private static EAnimU prime(AnimCE work, AnimU.UType t) {
        try {
            EAnimU e = work.getEAnim(t);
            if (e == null) return null;
            e.update(false);
            return e;
        } catch (Throwable x) {
            return null;
        }
    }
}
