package unitcreator;

import common.system.fake.FakeImage;
import common.system.fake.ImageBuilder;
import common.util.anim.AnimCE;
import common.util.anim.AnimU;
import common.util.anim.ImgCut;
import common.util.anim.MaAnim;
import common.util.anim.MaModel;
import common.util.anim.Part;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class SubtreeCopier {

    static final int PAR = 0, SPR = 2, Z = 3, PX = 4, PY = 5, PVX = 6, SX = 8, SY = 9, ANG = 10;
    static final int OPA = 11;
    static final int MAX_SHEET = 4096;
    private static final int ROW_COLS = 14;
    private static final int MIN_STRIP_WIDTH = 256;
    private static final int MIN_GAP = 8;
    private static final int MAX_FREE = 400;
    private static final int MAX_Z_TRACK_PARTS = 40;
    private static final int ANG_MODIF = 11;
    private static final int[] CTRL_MODIFS = {PX, PY, 8, ANG_MODIF};

    private SubtreeCopier() {}

    static String copy(AnimCE work, GraftOp op) {
        return copy(work, op, new LinkedHashMap<String, Integer>());
    }

    static String copy(AnimCE work, GraftOp op, Map<String, Integer> shared) {
        AnimU<?> donor = op.donor == null ? null : op.donor.anim();
        if (donor == null) return "source animation is not loaded";
        try { donor.check(); } catch (Throwable ignored) {}

        MaModel am = work.mamodel, bm = donor.mamodel;
        if (am == null || bm == null || am.parts == null || bm.parts == null) return "model data missing";
        int bn = Math.min(bm.n, bm.parts.length);
        int an = Math.min(am.n, am.parts.length);
        if (op.bRoot < 0 || op.bRoot >= bn) return "invalid source part";
        if (op.aParent < 0 || op.aParent >= an) return "invalid target part";

        boolean[] keep = new boolean[bn];
        keep[op.bRoot] = true;
        try { bm.getChild(keep); } catch (Throwable ignored) {}
        List<Integer> src = new ArrayList<Integer>();
        for (int k = 0; k < bn; k++) if (keep[k]) src.add(Integer.valueOf(k));
        if (src.isEmpty()) return "source part has no data";

        ImgCut bic = donor.imgcut;
        if (bic == null || bic.cuts == null) return "source image cut data missing";

        LinkedHashMap<Integer, Integer> want = new LinkedHashMap<Integer, Integer>();
        for (int i = 0; i < src.size(); i++) {
            int[] row = bm.parts[src.get(i).intValue()];
            if (row != null && row.length > SPR) addCut(want, bic, row[SPR]);
        }
        if (donor.anims != null) {
            for (MaAnim ma : donor.anims) {
                if (ma == null || ma.parts == null) continue;
                for (Part tr : ma.parts) {
                    if (tr == null || tr.ints == null || tr.ints.length < 2 || tr.ints[1] != 2) continue;
                    int t = tr.ints[0];
                    if (t < 0 || t >= bn || !keep[t] || tr.moves == null) continue;
                    for (int[] mv : tr.moves) if (mv != null && mv.length > 1) addCut(want, bic, mv[1]);
                }
            }
        }

        BufferedImage aSheet = asBuffered(work.getNum());
        BufferedImage bSheet = asBuffered(donor.getNum());
        if (aSheet == null || bSheet == null) return "sprite sheet missing";
        int aW = aSheet.getWidth(), aH = aSheet.getHeight();

        String dkey = op.donor == null ? "?" : op.donor.key();
        LinkedHashMap<Integer, Integer> cutMap = new LinkedHashMap<Integer, Integer>();
        List<int[]> rects = new ArrayList<int[]>();
        List<String> freshKeys = new ArrayList<String>();
        int reused = 0;
        for (Integer c : want.keySet()) {
            int[] r = SpriteFit.clampCut(bic.cuts[c.intValue()], bSheet.getWidth(), bSheet.getHeight());
            String k = sheetKey(dkey, r);
            Integer have = shared.get(k);
            if (have != null) {
                cutMap.put(c, have);
                reused++;
                continue;
            }
            int at = freshKeys.indexOf(k);
            if (at >= 0) {
                cutMap.put(c, Integer.valueOf(-1 - at));
                continue;
            }
            cutMap.put(c, Integer.valueOf(-1 - rects.size()));
            freshKeys.add(k);
            rects.add(r);
        }

        int[] grow = new int[2];
        int[][] slot = place(rects, work.imgcut, aW, aH, grow);
        int newW = Math.max(aW, grow[0]), newH = Math.max(aH, grow[1]);
        if (newW > MAX_SHEET || newH > MAX_SHEET)
            return "sprite sheet would grow past " + MAX_SHEET + " pixels - remove a graft first";

        FakeImage canvas = null;
        if (!rects.isEmpty()) {
            if (ImageBuilder.builder == null) return "image builder unavailable";
            canvas = ImageBuilder.builder.build(newW, newH);
            BufferedImage dst = asBuffered(canvas);
            if (dst == null) return "canvas allocation failed";
            Graphics2D g = dst.createGraphics();
            try {
                g.drawImage(aSheet, 0, 0, null);
                for (int i = 0; i < rects.size(); i++) {
                    int[] r = rects.get(i), s = slot[i];
                    g.drawImage(bSheet, s[0], s[1], s[0] + s[2], s[1] + s[3],
                            r[0], r[1], r[0] + r[2], r[1] + r[3], null);
                }
            } finally {
                g.dispose();
            }
        }

        ImgCut aic = work.imgcut;
        int cutBase = aic.n;
        if (!rects.isEmpty()) {
            aic.cuts = Arrays.copyOf(aic.cuts, cutBase + rects.size());
            aic.strs = Arrays.copyOf(aic.strs, cutBase + rects.size());
            for (int i = 0; i < rects.size(); i++) {
                int[] s = slot[i];
                aic.cuts[cutBase + i] = new int[]{s[0], s[1], s[2], s[3]};
                aic.strs[cutBase + i] = "graft" + (cutBase + i);
                shared.put(freshKeys.get(i), Integer.valueOf(cutBase + i));
            }
            aic.n = cutBase + rects.size();
        }
        for (java.util.Map.Entry<Integer, Integer> e : cutMap.entrySet())
            if (e.getValue().intValue() < 0)
                e.setValue(Integer.valueOf(cutBase + (-1 - e.getValue().intValue())));
        for (Integer v : cutMap.values())
            if (v.intValue() < 0 || v.intValue() >= aic.n)
                return "internal error: sprite index " + v + " is outside the sheet";
        if (reused > 0)
            Logger.log("UnitCreator: reused " + reused + " sprite cuts already in the sheet, added "
                    + rects.size());

        int pivot = am.parts.length;
        int base = pivot + 1;
        int[] map = new int[bn];
        Arrays.fill(map, -1);
        for (int i = 0; i < src.size(); i++) map[src.get(i).intValue()] = base + i;

        am.parts = Arrays.copyOf(am.parts, base + src.size());
        am.strs0 = Arrays.copyOf(am.strs0, base + src.size());
        op.origZ = new int[src.size()];
        op.origANGs = new int[src.size()];
        op.angleTracks = null;
        op.angleFlipped = false;

        int unit = scaleUnit(am), oUnit = opaUnit(am);
        int[] mrow = new int[ROW_COLS];
        mrow[PAR] = op.aParent;
        mrow[SPR] = -1;
        mrow[SX] = unit;
        mrow[SY] = unit;
        mrow[OPA] = oUnit;
        am.parts[pivot] = mrow;
        am.strs0[pivot] = "graft pivot";
        op.mirrorPart = pivot;

        for (int i = 0; i < src.size(); i++) {
            int k = src.get(i).intValue();
            int[] row = pad(bm.parts[k].clone());
            int par = row[PAR];
            row[PAR] = (k == op.bRoot || par < 0 || par >= bn || map[par] < 0) ? pivot : map[par];
            Integer sl = row[SPR] >= 0 ? cutMap.get(Integer.valueOf(row[SPR])) : null;
            row[SPR] = sl == null ? -1 : sl.intValue();
            op.origZ[i] = row[Z];
            op.origANGs[i] = row[ANG];
            am.parts[base + i] = row;
            am.strs0[base + i] = partName(bm.strs0, k);
        }
        am.n = am.parts.length;
        normalize(am);
        op.partBase = base;
        op.partCount = src.size();
        op.rootPart = map[op.bRoot];

        int[] rr = am.parts[op.rootPart];
        op.origPX = rr[PX];
        op.origPY = rr[PY];
        op.origSX = rr[SX];
        op.origSY = rr[SY];
        op.origANG = rr[ANG];
        op.origPivotX = rr[PVX];

        if (op.copyAnim) copyTracks(work, donor, op, map, bn, cutMap);
        createControls(work, op);

        if (canvas != null) work.setNum(canvas);
        try {
            work.parts = work.imgcut.cut(work.getNum());
        } catch (Throwable t) {
            Logger.err("UnitCreator: cut refresh failed", t);
        }
        if (work.anims != null) for (MaAnim ma : work.anims) if (ma != null) ma.validate();

        apply(work, op);
        report(work, op, pivot, cutBase, aic.n);
        return null;
    }

    private static void report(AnimCE work, GraftOp op, int pivot, int cutLo, int cutHi) {
        try {
            MaModel am = work.mamodel;
            Logger.log("UnitCreator: graft '" + op.donorPartName + "' parts " + op.partBase
                    + ".." + (op.partBase + op.partCount - 1) + " pivot=" + pivot
                    + " cuts " + cutLo + ".." + (cutHi - 1)
                    + " scale=" + java.util.Arrays.toString(op.scale)
                    + " z=" + java.util.Arrays.toString(op.zOffset)
                    + " speed=" + java.util.Arrays.toString(op.speed)
                    + " copyAnim=" + op.copyAnim
                    + " zTracks=" + (op.zctrl != null));
            int lim = Math.min(op.partCount, 12);
            for (int i = 0; i < lim; i++) {
                int idx = op.partBase + i;
                if (idx < 0 || idx >= am.parts.length) continue;
                int[] r = am.parts[idx];
                if (r == null) continue;
                int sp = r[SPR];
                String img = "none";
                try {
                    FakeImage f = sp >= 0 ? work.parts(sp) : null;
                    img = f == null ? "NULL" : f.getWidth() + "x" + f.getHeight();
                } catch (Throwable ignored) {}
                Logger.log("   part " + idx + " par=" + r[PAR] + " spr=" + sp + " id=" + r[1]
                        + " z=" + r[Z] + " opa=" + r[OPA] + " sca=" + r[SX] + "/" + r[SY]
                        + " pos=" + r[PX] + "," + r[PY] + " piv=" + r[6] + "," + r[7]
                        + " img=" + img);
            }
        } catch (Throwable t) {
            Logger.err("UnitCreator: graft report failed", t);
        }
    }

    static void apply(AnimCE work, GraftOp op) {
        if (work == null || op == null || !op.applied()) return;
        MaModel am = work.mamodel;
        if (am == null || am.parts == null) return;
        int unit = scaleUnit(am);

        if (op.mirrorPart >= 0 && op.mirrorPart < am.parts.length) {
            int[] mp = am.parts[op.mirrorPart];
            if (mp != null && mp.length > OPA) {
                mp[PX] = op.origPX;
                mp[PY] = op.origPY;
                mp[SX] = op.mirror ? -unit : unit;
                mp[SY] = unit;
                mp[ANG] = 0;
            }
        }
        if (op.rootPart >= 0 && op.rootPart < am.parts.length) {
            int[] rr = am.parts[op.rootPart];
            if (rr != null && rr.length > ANG) {
                rr[PX] = 0;
                rr[PY] = 0;
                rr[SX] = op.origSX;
                rr[SY] = op.origSY;
            }
        }
        for (int i = 0; i < op.partCount; i++) {
            int idx = op.partBase + i;
            if (idx < 0 || idx >= am.parts.length) continue;
            int[] row = am.parts[idx];
            if (row == null) continue;
            if (op.origZ != null && i < op.origZ.length && row.length > Z)
                row[Z] = op.origZ[i] + op.zOffset[1];
            if (op.origANGs != null && i < op.origANGs.length && row.length > ANG)
                row[ANG] = op.mirror ? -op.origANGs[i] : op.origANGs[i];
        }

        if (op.angleTracks != null && op.angleFlipped != op.mirror) {
            for (int i = 0; i < op.angleTracks.length; i++) negateMoves(op.angleTracks[i]);
            op.angleFlipped = op.mirror;
        }

        if (op.ctrl == null) return;
        for (int t = 0; t < op.ctrl.length; t++) {
            int ti = GraftOp.typeOf(t);
            Part[] c = op.ctrl[t];
            if (c != null && c.length >= 4) {
                setConst(c[0], op.offX[ti]);
                setConst(c[1], op.offY[ti]);
                setConst(c[2], op.scale[ti]);
                setConst(c[3], op.mirror ? -op.angle[ti] : op.angle[ti]);
            }
            if (op.zctrl != null && t < op.zctrl.length && op.zctrl[t] != null && op.origZ != null) {
                Part[] z = op.zctrl[t];
                for (int i = 0; i < z.length && i < op.origZ.length; i++)
                    setConst(z[i], op.origZ[i] + op.zOffset[ti]);
            }
        }
    }

    private static void negateMoves(Part p) {
        if (p == null || p.moves == null) return;
        for (int k = 0; k < p.moves.length; k++)
            if (p.moves[k] != null && p.moves[k].length > 1) p.moves[k][1] = -p.moves[k][1];
    }

    private static void setConst(Part p, int v) {
        if (p == null || p.moves == null) return;
        for (int k = 0; k < p.moves.length; k++)
            if (p.moves[k] != null && p.moves[k].length > 1) p.moves[k][1] = v;
    }

    private static Part constTrack(int target, int modif, int len, int value) {
        Part p = new Part();
        p.ints = new int[]{target, modif, 1, 0, 0};
        p.name = "ctrl";
        p.moves = new int[][]{{0, value, 0, 0}, {Math.max(1, len), value, 0, 0}};
        p.n = 2;
        p.off = 0;
        try { p.validate(); } catch (Throwable ignored) {}
        return p;
    }

    private static void createControls(AnimCE work, GraftOp op) {
        if (work.anims == null || op.mirrorPart < 0) return;
        int slots = work.anims.length;
        op.ctrl = new Part[slots][];
        boolean useZ = op.partCount <= MAX_Z_TRACK_PARTS;
        op.zctrl = useZ ? new Part[slots][] : null;
        for (int t = 0; t < slots; t++) {
            MaAnim ma = work.anims[t];
            if (ma == null) continue;
            int len = Math.max(1, maxFrame(ma));
            Part[] c = new Part[CTRL_MODIFS.length];
            for (int k = 0; k < CTRL_MODIFS.length; k++)
                c[k] = constTrack(op.mirrorPart, CTRL_MODIFS[k], len, 0);
            op.ctrl[t] = c;
            Part[] z = null;
            if (useZ) {
                z = new Part[op.partCount];
                for (int i = 0; i < op.partCount; i++)
                    z[i] = constTrack(op.partBase + i, Z, len, 0);
                op.zctrl[t] = z;
            }
            int extra = c.length + (z == null ? 0 : z.length);
            int old = ma.parts == null ? 0 : ma.parts.length;
            Part[] np = ma.parts == null ? new Part[extra] : Arrays.copyOf(ma.parts, old + extra);
            int w = old;
            for (int k = 0; k < c.length; k++) np[w++] = c[k];
            if (z != null) for (int k = 0; k < z.length; k++) np[w++] = z[k];
            ma.parts = np;
            ma.n = np.length;
        }
    }

    private static void copyTracks(AnimCE work, AnimU<?> donor, GraftOp op, int[] map, int bn,
                                   LinkedHashMap<Integer, Integer> cutMap) {
        if (work.anims == null || donor.anims == null) return;
        List<Part> angles = new ArrayList<Part>();
        for (int t = 0; t < work.anims.length; t++) {
            MaAnim aMa = work.anims[t];
            MaAnim bMa = matchAnim(work, donor, t);
            if (aMa == null || bMa == null || bMa.parts == null) continue;

            int aLen = maxFrame(aMa), bLen = maxFrame(bMa);
            double tf = (aLen > 0 && bLen > 0) ? (double) aLen / bLen : 1.0;
            int sp = op.speed[GraftOp.typeOf(t)];
            double k = sp <= 0 ? 1.0 : sp / 100.0;
            tf /= k;
            List<Part> extra = new ArrayList<Part>();
            for (Part p0 : bMa.parts) {
                if (p0 == null || p0.ints == null || p0.ints.length == 0) continue;
                int target = p0.ints[0];
                if (target < 0 || target >= bn || map[target] < 0) continue;
                Part p = p0.clone();
                p.ints[0] = map[target];
                boolean imgSwap = p.ints.length > 1 && p.ints[1] == 2;
                if (p.moves != null) {
                    if (tf != 1.0) retime(p.moves, tf);
                    if (imgSwap) {
                        for (int[] mv : p.moves) {
                            if (mv == null || mv.length < 2) continue;
                            Integer sl = cutMap.get(Integer.valueOf(mv[1]));
                            mv[1] = sl == null ? -1 : sl.intValue();
                        }
                    }
                }
                if (sp != 100 && aLen > 0) {
                    p.ints[2] = -1;
                    trim(p, aLen);
                }
                try { p.validate(); } catch (Throwable ignored) {}
                if (p.ints.length > 1 && p.ints[1] == ANG_MODIF) angles.add(p);
                extra.add(p);
            }
            if (extra.isEmpty()) continue;
            int old = aMa.parts == null ? 0 : aMa.parts.length;
            Part[] np = aMa.parts == null
                    ? new Part[extra.size()]
                    : Arrays.copyOf(aMa.parts, old + extra.size());
            for (int j = 0; j < extra.size(); j++) np[old + j] = extra.get(j);
            aMa.parts = np;
            aMa.n = np.length;
        }
        op.angleTracks = angles.toArray(new Part[angles.size()]);
    }

    private static void trim(Part p, int limit) {
        if (p.moves == null || p.moves.length == 0 || limit <= 0) return;
        int keep = 0;
        while (keep < p.moves.length && p.moves[keep] != null
                && p.moves[keep].length > 0 && p.moves[keep][0] <= limit) keep++;
        if (keep >= p.moves.length) return;

        boolean step = p.ints != null && p.ints.length > 1 && p.ints[1] == SPR;
        if (keep == 0) {
            int v = p.moves[0][1];
            p.moves = new int[][]{{0, v, 0, 0}, {limit, v, 0, 0}};
            p.n = 2;
            return;
        }
        int[] a = p.moves[keep - 1];
        int[] b = p.moves[keep];
        int v;
        if (step || b[0] == a[0]) v = a[1];
        else v = a[1] + (int) Math.round((double) (b[1] - a[1]) * (limit - a[0]) / (b[0] - a[0]));

        int[][] mv = new int[a[0] == limit ? keep : keep + 1][];
        for (int i = 0; i < keep; i++) mv[i] = p.moves[i];
        if (mv.length > keep) mv[keep] = new int[]{limit, v, 0, 0};
        if (mv.length < 2) mv = new int[][]{mv[0], {limit, mv[0][1], 0, 0}};
        p.moves = mv;
        p.n = mv.length;
    }

    private static void retime(int[][] moves, double tf) {
        for (int[] mv : moves) {
            if (mv == null || mv.length == 0) continue;
            mv[0] = (int) Math.round(mv[0] * tf);
        }
    }

    private static MaAnim matchAnim(AnimCE work, AnimU<?> donor, int t) {
        try {
            if (work.types != null && t < work.types.length && donor.types != null) {
                Object at = work.types[t];
                for (int i = 0; i < donor.types.length && i < donor.anims.length; i++) {
                    if (at != null && at.equals(donor.types[i])) return donor.anims[i];
                }
                return null;
            }
        } catch (Throwable ignored) {}
        return t < donor.anims.length ? donor.anims[t] : null;
    }


    private static int[][] place(List<int[]> rects, ImgCut aic, int aW, int aH, int[] grow) {
        int n = rects.size();
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = Integer.valueOf(i);
        final List<int[]> rs = rects;
        Arrays.sort(order, new Comparator<Integer>() {
            @Override public int compare(Integer a, Integer b) {
                return rs.get(b.intValue())[3] - rs.get(a.intValue())[3];
            }
        });

        int[][] slot = new int[n][];
        List<int[]> taken = occupied(aic, aW, aH);
        List<int[]> free = gaps(aW, aH, taken);
        List<Integer> left = new ArrayList<Integer>();

        for (int oi = 0; oi < n; oi++) {
            int i = order[oi].intValue();
            int[] r = rects.get(i);
            int hit = -1;
            long bestArea = Long.MAX_VALUE;
            for (int k = 0; k < free.size(); k++) {
                int[] f = free.get(k);
                if (f[2] < r[2] || f[3] < r[3]) continue;
                long area = (long) f[2] * f[3];
                if (area < bestArea) { bestArea = area; hit = k; }
            }
            if (hit < 0) { left.add(order[oi]); continue; }
            int[] f = free.get(hit);
            int[] put = new int[]{f[0], f[1], r[2], r[3]};
            slot[i] = put;
            taken.add(put);
            free.remove(hit);
            carve(free, put);
        }

        int maxRow = Math.max(aW, MIN_STRIP_WIDTH);
        int cx = 0, cy = 0, rowH = 0, stripW = 0, stripH = 0;
        for (int oi = 0; oi < left.size(); oi++) {
            int i = left.get(oi).intValue();
            int[] r = rects.get(i);
            if (cx > 0 && cx + r[2] > maxRow) { cx = 0; cy += rowH; rowH = 0; }
            slot[i] = new int[]{cx, aH + cy, r[2], r[3]};
            cx += r[2];
            if (r[3] > rowH) rowH = r[3];
            if (cx > stripW) stripW = cx;
            if (cy + rowH > stripH) stripH = cy + rowH;
        }

        if (!verify(slot, aic, aW, aH)) {
            Logger.log("UnitCreator: gap packing rejected by overlap check, appending instead");
            return strip(rects, aW, aH, grow);
        }
        grow[0] = Math.max(aW, stripW);
        grow[1] = aH + stripH;
        return slot;
    }

    private static int[][] strip(List<int[]> rects, int aW, int aH, int[] grow) {
        int maxRow = Math.max(aW, MIN_STRIP_WIDTH);
        int[][] slot = new int[rects.size()][];
        int cx = 0, cy = 0, rowH = 0, stripW = 0, stripH = 0;
        for (int i = 0; i < rects.size(); i++) {
            int[] r = rects.get(i);
            if (cx > 0 && cx + r[2] > maxRow) { cx = 0; cy += rowH; rowH = 0; }
            slot[i] = new int[]{cx, aH + cy, r[2], r[3]};
            cx += r[2];
            if (r[3] > rowH) rowH = r[3];
            if (cx > stripW) stripW = cx;
            if (cy + rowH > stripH) stripH = cy + rowH;
        }
        grow[0] = Math.max(aW, stripW);
        grow[1] = aH + stripH;
        return slot;
    }

    private static List<int[]> occupied(ImgCut aic, int aW, int aH) {
        List<int[]> out = new ArrayList<int[]>();
        if (aic == null || aic.cuts == null) return out;
        for (int i = 0; i < aic.n && i < aic.cuts.length; i++) {
            int[] c = aic.cuts[i];
            if (c == null || c.length < 4 || c[2] <= 0 || c[3] <= 0) continue;
            int x = Math.max(0, Math.min(c[0], aW));
            int y = Math.max(0, Math.min(c[1], aH));
            int w = Math.min(c[2], aW - x);
            int h = Math.min(c[3], aH - y);
            if (w > 0 && h > 0) out.add(new int[]{x, y, w, h});
        }
        return out;
    }

    private static List<int[]> gaps(int aW, int aH, List<int[]> taken) {
        List<int[]> free = new ArrayList<int[]>();
        free.add(new int[]{0, 0, aW, aH});
        for (int i = 0; i < taken.size(); i++) {
            carve(free, taken.get(i));
            if (free.size() > MAX_FREE) {
                sortByArea(free);
                while (free.size() > MAX_FREE) free.remove(free.size() - 1);
            }
        }
        sortByArea(free);
        return free;
    }

    private static void carve(List<int[]> free, int[] occ) {
        List<int[]> next = new ArrayList<int[]>(free.size() + 4);
        for (int i = 0; i < free.size(); i++) {
            int[] f = free.get(i);
            if (!hits(f, occ)) { next.add(f); continue; }
            if (occ[1] > f[1]) add(next, f[0], f[1], f[2], occ[1] - f[1]);
            if (occ[1] + occ[3] < f[1] + f[3])
                add(next, f[0], occ[1] + occ[3], f[2], f[1] + f[3] - occ[1] - occ[3]);
            int ty = Math.max(f[1], occ[1]);
            int by = Math.min(f[1] + f[3], occ[1] + occ[3]);
            if (by > ty) {
                if (occ[0] > f[0]) add(next, f[0], ty, occ[0] - f[0], by - ty);
                if (occ[0] + occ[2] < f[0] + f[2])
                    add(next, occ[0] + occ[2], ty, f[0] + f[2] - occ[0] - occ[2], by - ty);
            }
        }
        free.clear();
        free.addAll(next);
    }

    private static void add(List<int[]> out, int x, int y, int w, int h) {
        if (w >= MIN_GAP && h >= MIN_GAP) out.add(new int[]{x, y, w, h});
    }

    private static void sortByArea(List<int[]> free) {
        Collections.sort(free, new Comparator<int[]>() {
            @Override public int compare(int[] a, int[] b) {
                long x = (long) a[2] * a[3], y = (long) b[2] * b[3];
                return x == y ? 0 : (x < y ? -1 : 1);
            }
        });
    }

    private static boolean hits(int[] a, int[] b) {
        return a[0] < b[0] + b[2] && b[0] < a[0] + a[2]
                && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
    }

    private static boolean verify(int[][] slot, ImgCut aic, int aW, int aH) {
        List<int[]> old = occupied(aic, aW, aH);
        for (int i = 0; i < slot.length; i++) {
            if (slot[i] == null) return false;
            if (slot[i][1] >= aH) continue;
            for (int k = 0; k < old.size(); k++)
                if (hits(slot[i], old.get(k))) return false;
        }
        for (int i = 0; i < slot.length; i++)
            for (int k = i + 1; k < slot.length; k++)
                if (hits(slot[i], slot[k])) return false;
        return true;
    }

    static String sheetKey(String donorKey, int[] r) {
        return donorKey + "@" + r[0] + "," + r[1] + "," + r[2] + "," + r[3];
    }

    private static void addCut(LinkedHashMap<Integer, Integer> map, ImgCut ic, int idx) {
        if (idx < 0 || idx >= ic.n || idx >= ic.cuts.length) return;
        int[] c = ic.cuts[idx];
        if (c == null || c.length < 4 || c[2] <= 0 || c[3] <= 0) return;
        Integer key = Integer.valueOf(idx);
        if (map.containsKey(key)) return;
        map.put(key, Integer.valueOf(map.size()));
    }

    static int animLen(AnimU<?> a, AnimU.UType t) {
        try {
            if (a == null) return 0;
            a.check();
            MaAnim ma = a.getMaAnim(t);
            return ma == null ? 0 : maxFrame(ma);
        } catch (Throwable e) {
            return 0;
        }
    }

    private static int maxFrame(MaAnim ma) {
        int mf = 0;
        if (ma == null || ma.parts == null) return 0;
        for (Part p : ma.parts) {
            if (p == null || p.moves == null) continue;
            for (int[] mv : p.moves) if (mv != null && mv.length > 0 && mv[0] > mf) mf = mv[0];
        }
        return mf;
    }

    private static String partName(String[] names, int i) {
        if (names != null && i < names.length && names[i] != null && !names[i].isEmpty()) return names[i];
        return "graft";
    }

    private static int[] pad(int[] row) {
        return row.length >= ROW_COLS ? row : Arrays.copyOf(row, ROW_COLS);
    }

    private static void normalize(MaModel m) {
        if (m == null || m.parts == null) return;
        for (int i = 0; i < m.parts.length; i++) {
            if (m.parts[i] == null) m.parts[i] = new int[ROW_COLS];
            else if (m.parts[i].length < ROW_COLS) m.parts[i] = Arrays.copyOf(m.parts[i], ROW_COLS);
        }
    }

    private static int scaleUnit(MaModel m) {
        return m != null && m.ints != null && m.ints.length > 0 && m.ints[0] != 0 ? m.ints[0] : 1000;
    }

    private static int opaUnit(MaModel m) {
        return m != null && m.ints != null && m.ints.length > 2 && m.ints[2] != 0 ? m.ints[2] : 1000;
    }

    private static BufferedImage asBuffered(FakeImage fi) {
        if (fi == null) return null;
        Object o = fi.bimg();
        return o instanceof BufferedImage ? (BufferedImage) o : null;
    }
}
