package unitcreator;

import common.battle.data.CustomUnit;
import common.battle.data.MaskEntity;
import common.battle.data.MaskUnit;
import common.pack.Identifier;
import common.pack.PackData;
import common.pack.Source;
import common.util.AnimGroup;
import common.util.anim.AnimCE;
import common.util.anim.AnimD;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import common.util.unit.Unit;

import java.util.List;
import java.util.Random;

public final class UnitCombiner {

    private UnitCombiner() {}

    static Unit saveAsUnit(AnimCE work, Form base, List<Donor> donors, boolean mergeStats,
                           java.util.Map<Integer, int[]> ld) {
        if (work == null || base == null || base.du == null)
            throw new IllegalArgumentException("save: base unit is not ready");

        String name = pickName(base, donors);
        String animId = AnimCE.getAvailable(name, Source.BasePath.ANIM);
        Source.ResourceLocation rl =
                new Source.ResourceLocation("_local", animId, Source.BasePath.ANIM);
        AnimCE out = new AnimCE(rl, (AnimD<?, ?>) work);

        IconMaker.makeIcon(out);
        try {
            out.history.clear();
            out.unSave("graft base");
        } catch (Throwable t) {
            Logger.err("UnitCreator: history baseline reset failed", t);
        }

        PackData.UserPack pack = UnitCreatorPack.get();
        Identifier<Unit> id = pack.getNextID(Unit.class);
        CustomUnit cu = new CustomUnit();
        Unit u = new Unit(id, out, cu);
        cu.importData((MaskEntity) base.du);

        int merged = 0;
        if (mergeStats) {
            for (int i = 0; donors != null && i < donors.size(); i++) {
                MaskUnit du = donors.get(i).stats();
                if (du == null) continue;
                StatMerger.merge(cu, cu, du);
                merged++;
            }
        }

        if (ld != null && !ld.isEmpty() && cu.atks != null) {
            for (java.util.Map.Entry<Integer, int[]> e : ld.entrySet()) {
                int i = e.getKey().intValue();
                if (i < 0 || i >= cu.atks.length || cu.atks[i] == null) continue;
                cu.atks[i].ld0 = e.getValue()[0];
                cu.atks[i].ld1 = e.getValue()[1];
            }
            Logger.log("UnitCreator: applied " + ld.size() + " impact range overrides");
        }

        try { u.forms[0].names.put(name); } catch (Throwable ignored) {}
        try { u.forms[0].description.put(SaveDialog.describe(base, donors)); } catch (Throwable ignored) {}
        pack.units.add(u);

        OriginStore.record(u, base, donors);

        try { out.save(); } catch (Throwable t) { Logger.err("UnitCreator: anim save failed", t); }
        UnitCreatorPack.save();
        try { AnimGroup.writeAnimGroup(); } catch (Throwable t) {
            Logger.err("UnitCreator: anim group save failed", t);
        }

        Logger.log("UnitCreator: saved graft unit '" + name + "' as " + id
                + " (anim _local/" + animId + ", stats " + (mergeStats ? "merged" : "kept")
                + ", " + merged + " stat donors)");
        return u;
    }

    private static String pickName(Form base, List<Donor> donors) {
        String a = displayName(base);
        String b = null;
        for (int i = 0; donors != null && i < donors.size(); i++) {
            if (donors.get(i) != null) { b = donors.get(i).name(); break; }
        }
        String name;
        if (b == null) {
            name = a + " Graft";
        } else {
            try {
                name = NameCombiner.combine(a, b, new Random());
            } catch (Throwable t) {
                Logger.err("UnitCreator: name combine failed", t);
                name = a + " Graft";
            }
        }
        if (name == null || name.trim().isEmpty()) name = "Graft";
        return name.trim();
    }

    static String displayName(Form f) {
        try {
            String n = MultiLangCont.get(f);
            if (n != null && !n.trim().isEmpty()) return n.trim();
        } catch (Throwable ignored) {}
        try {
            if (f.names != null) {
                String n = f.names.toString();
                if (n != null && !n.trim().isEmpty()) return n.trim();
            }
        } catch (Throwable ignored) {}
        return f.name == null || f.name.trim().isEmpty() ? "Unit" : f.name.trim();
    }
}
