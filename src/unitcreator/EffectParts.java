package unitcreator;

import common.util.anim.AnimU;
import common.util.anim.EAnimU;
import common.util.anim.EPart;
import common.util.anim.MaModel;

import java.util.Arrays;
import java.util.Locale;

final class EffectParts {

    private static final String[] TOKENS = {
            "eff", "fx", "slash", "aura", "beam", "flash",
            "spark", "smoke", "shockwave", "impact", "muzzle", "afterimage"
    };
    private static final int GLOW_COL = 12;
    private static final float VISIBLE_OPACITY = 0.01f;
    private static final int MAX_IDLE_FRAMES = 120;

    private EffectParts() {}

    static boolean[] markEffects(AnimU<?> anim) {
        MaModel m = anim == null ? null : anim.mamodel;
        if (m == null || m.parts == null) return new boolean[0];
        int n = Math.min(m.n, m.parts.length);
        boolean[] name = markByName(m);
        boolean[] idle = markByIdleVisibility(anim);
        boolean[] eff = new boolean[n];
        for (int i = 0; i < n; i++) {
            boolean glow = m.parts[i].length > GLOW_COL && m.parts[i][GLOW_COL] != 0;
            boolean inv = idle != null && i < idle.length && idle[i];
            boolean nm = i < name.length && name[i];
            eff[i] = glow || inv || nm;
        }
        return eff;
    }

    static boolean[] markByIdleVisibility(AnimU<?> anim) {
        try {
            EAnimU e = anim.getEAnim(AnimU.UType.IDLE);
            if (e == null) return null;
            EPart[] ent = e.ent;
            if (ent == null || ent.length == 0) return null;

            boolean[] visible = new boolean[ent.length];
            int len = 0;
            try { len = e.len(); } catch (Throwable ignored) {}
            if (len <= 0) len = 1;
            if (len > MAX_IDLE_FRAMES) len = MAX_IDLE_FRAMES;
            try { e.update(false); } catch (Throwable ignored) {}
            for (int f = 0; f <= len; f++) {
                try { e.setTime(f); } catch (Throwable ignored) {}
                for (int i = 0; i < ent.length; i++) {
                    if (ent[i] == null) continue;
                    float op;
                    try { op = ent[i].opa(); } catch (Throwable t) { op = 1f; }
                    if (op > VISIBLE_OPACITY) visible[i] = true;
                }
            }
            int vis = 0;
            for (boolean b : visible) if (b) vis++;
            if (vis == 0) return null;
            boolean[] eff = new boolean[ent.length];
            for (int i = 0; i < ent.length; i++) eff[i] = !visible[i];
            return eff;
        } catch (Throwable t) {
            return null;
        }
    }

    static boolean[] allowedCuts(AnimU<?> bAnim, int cutCount) {
        boolean[] allowed = new boolean[cutCount];
        MaModel model = bAnim == null ? null : bAnim.mamodel;
        if (model == null || model.parts == null) { Arrays.fill(allowed, true); return allowed; }
        boolean[] eff = markEffects(bAnim);
        int n = Math.min(model.n, model.parts.length);
        boolean any = false;
        for (int k = 0; k < n; k++) {
            if (k < eff.length && eff[k]) continue;
            int[] p = model.parts[k];
            int img = p.length > 2 ? p[2] : -1;
            if (img >= 0 && img < cutCount) { allowed[img] = true; any = true; }
        }
        if (!any) Arrays.fill(allowed, true);
        return allowed;
    }

    private static boolean[] markByName(MaModel model) {
        int n = Math.min(model.n, model.parts.length);
        String[] names = model.strs0;
        boolean[] self = new boolean[n];
        for (int i = 0; i < n; i++) self[i] = nameIsEffect(names, i);
        boolean[] eff = new boolean[n];
        for (int i = 0; i < n; i++) {
            int p = i, guard = 0;
            while (p >= 0 && p < n && guard++ < n + 2) {
                if (self[p]) { eff[i] = true; break; }
                int par = model.parts[p].length > 0 ? model.parts[p][0] : -1;
                if (par == p || par < 0 || par >= n) break;
                p = par;
            }
        }
        return eff;
    }

    private static boolean nameIsEffect(String[] names, int i) {
        if (names == null || i >= names.length || names[i] == null) return false;
        String s = names[i].toLowerCase(Locale.ROOT);
        for (String t : TOKENS) if (s.contains(t)) return true;
        return false;
    }
}
