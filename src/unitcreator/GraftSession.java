package unitcreator;

import common.battle.data.MaskAtk;
import common.pack.Source;
import common.util.AnimGroup;
import common.util.anim.AnimCE;
import common.util.anim.AnimD;
import common.util.anim.AnimU;
import common.util.anim.ImgCut;
import common.util.unit.Form;
import common.util.unit.Unit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

final class GraftSession {

    static final String WORK_ID = "partgraft_work";

    private final Map<Integer, int[]> ld = new LinkedHashMap<Integer, int[]>();

    private final Map<String, Integer> sheetIndex = new LinkedHashMap<String, Integer>();

    private Form base;
    private AnimCE work;
    private final List<GraftOp> ops = new ArrayList<GraftOp>();

    Form base() { return base; }

    AnimCE work() { return work; }

    List<GraftOp> ops() { return ops; }

    boolean ready() { return work != null; }

    boolean dirty() { return !ops.isEmpty(); }

    static final class Memento {
        private final List<GraftOp> ops = new ArrayList<GraftOp>();
        private final Map<Integer, int[]> ld = new LinkedHashMap<Integer, int[]>();
    }

    Memento snapshot() {
        Memento m = new Memento();
        for (int i = 0; i < ops.size(); i++) m.ops.add(ops.get(i).copyTunables());
        for (Map.Entry<Integer, int[]> e : ld.entrySet())
            m.ld.put(e.getKey(), new int[]{e.getValue()[0], e.getValue()[1]});
        return m;
    }

    void restore(Memento m) {
        if (m == null || base == null) return;
        ld.clear();
        for (Map.Entry<Integer, int[]> e : m.ld.entrySet())
            ld.put(e.getKey(), new int[]{e.getValue()[0], e.getValue()[1]});
        ops.clear();
        if (createWork() != null) return;
        for (int i = 0; i < m.ops.size(); i++) {
            GraftOp o = m.ops.get(i).copyTunables();
            String err = SubtreeCopier.copy(work, o, sheetIndex);
            if (err == null) ops.add(o);
            else Logger.log("UnitCreator: dropped graft on undo - " + err);
        }
    }

    Map<Integer, int[]> ldOverrides() { return ld; }

    int[] ldOf(int i) {
        int[] v = ld.get(Integer.valueOf(i));
        if (v != null) return v;
        try {
            MaskAtk[] a = base.du.getAtks();
            if (a != null && i < a.length && a[i] != null)
                return new int[]{a[i].getShortPoint(), a[i].getLongPoint()};
        } catch (Throwable ignored) {}
        return new int[]{0, 0};
    }

    int ldCount() {
        try {
            MaskAtk[] a = base.du.getAtks();
            return a == null ? 0 : a.length;
        } catch (Throwable t) {
            return 0;
        }
    }

    void setLd(int i, int near, int far) {
        ld.put(Integer.valueOf(i), new int[]{near, far});
    }

    String setBase(Form f) {
        base = f;
        ops.clear();
        ld.clear();
        return createWork();
    }

    String add(GraftOp op) {
        if (work == null) return "pick a unit for slot 1 first";

        int alive = GraftCheck.visibleAncestor(work, op.aParent);
        String moved = null;
        if (alive != op.aParent) {
            moved = "\"" + op.targetPartName + "\" is invisible in every animation, so the branch "
                    + "would have been invisible too. It was attached to the nearest visible part above it.";
            Logger.log("UnitCreator: retargeted graft from part " + op.aParent + " to " + alive);
            op.aParent = alive;
        }

        AutoFit.seed(work, op);
        String err = SubtreeCopier.copy(work, op, sheetIndex);
        if (err != null) {
            rebuild();
            return err;
        }
        ops.add(op);

        String why = GraftCheck.diagnose(work, op);
        if (why != null) {
            Logger.log("UnitCreator: graft is not visible - " + why);
            return "attached, but nothing shows: " + why;
        }
        return moved;
    }

    void setMirror(GraftOp op) {
        if (work == null || op == null) return;
        MirrorFit.apply(work, op);
    }

    void nudge(GraftOp op) {
        if (work == null || op == null) return;
        SubtreeCopier.apply(work, op);
    }

    void remove(GraftOp op) {
        if (op == null) return;
        ops.remove(op);
        rebuild();
    }

    void rebuild() {
        if (base == null) return;
        List<GraftOp> old = new ArrayList<GraftOp>(ops);
        ops.clear();
        if (createWork() != null) return;
        for (int i = 0; i < old.size(); i++) {
            GraftOp o = old.get(i);
            o.partBase = -1;
            o.rootPart = -1;
            o.partCount = 0;
            o.origZ = null;
            String err = SubtreeCopier.copy(work, o, sheetIndex);
            if (err == null) ops.add(o);
            else Logger.log("UnitCreator: dropped graft on rebuild - " + err);
        }
    }

    List<Donor> donors() {
        List<Donor> out = new ArrayList<Donor>();
        List<String> seen = new ArrayList<String>();
        for (int i = 0; i < ops.size(); i++) {
            Donor d = ops.get(i).donor;
            if (d == null) continue;
            String k = d.key();
            if (seen.contains(k)) continue;
            seen.add(k);
            out.add(d);
        }
        return out;
    }

    Unit save(boolean mergeStats) {
        return UnitCombiner.saveAsUnit(work, base, donors(), mergeStats, ld);
    }

    void dispose() {
        AnimCE a = work;
        work = null;
        ops.clear();
        base = null;
        drop(a);
    }

    static void cleanupStale() {
        try {
            AnimCE old = AnimCE.map().get(WORK_ID);
            if (old != null) drop(old);
        } catch (Throwable t) {
            Logger.err("UnitCreator: stale work anim cleanup failed", t);
        }
    }

    private String createWork() {
        drop(work);
        work = null;
        if (base == null || base.anim == null) return "pick a unit for slot 1 first";
        try {
            AnimU<?> src = (AnimU<?>) base.anim;
            src.check();
            if (src.imgcut == null || src.mamodel == null || src.getNum() == null || src.anims == null)
                return "unit 1 animation failed to load";
            Source.ResourceLocation rl =
                    new Source.ResourceLocation("_local", WORK_ID, Source.BasePath.ANIM);
            work = new AnimCE(rl, (AnimD<?, ?>) src);
            seedSheetIndex();
            return null;
        } catch (Throwable t) {
            Logger.err("UnitCreator: work animation setup failed", t);
            return UCUI.describe(t);
        }
    }

    private void seedSheetIndex() {
        sheetIndex.clear();
        try {
            ImgCut ic = work.imgcut;
            if (ic == null || ic.cuts == null) return;
            String key = Donor.of(base).key();
            for (int i = 0; i < ic.n && i < ic.cuts.length; i++) {
                int[] c = ic.cuts[i];
                if (c == null || c.length < 4 || c[2] <= 0 || c[3] <= 0) continue;
                sheetIndex.put(SubtreeCopier.sheetKey(key, c), Integer.valueOf(i));
            }
        } catch (Throwable t) {
            Logger.err("UnitCreator: could not index the base sheet", t);
        }
    }

    private static void drop(AnimCE a) {
        if (a == null) return;
        try { a.delete(); } catch (Throwable ignored) {}
        try { AnimCE.map().remove(WORK_ID); } catch (Throwable ignored) {}
        try { AnimGroup.workspaceGroup.renewGroup(); } catch (Throwable ignored) {}
    }
}
