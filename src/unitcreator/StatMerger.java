package unitcreator;

import common.battle.data.AtkDataModel;
import common.battle.data.CustomUnit;
import common.battle.data.MaskEntity;
import common.battle.data.MaskUnit;
import common.util.Data;
import common.util.unit.Trait;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

final class StatMerger {

    private static final int PROC_COUNT = 61;
    private static final int WEAK = 8;

    private StatMerger() {}

    static void merge(CustomUnit cu, MaskUnit a, MaskUnit b) {
        try {
            mergeBaseStats(cu, a, b);
            mergeDamage(cu, a, b);
            mergeProcs(cu, b);
            mergeAbilities(cu, b);
        } catch (Throwable t) {
            Logger.err("UnitCreator: stat merge failed", t);
        }
    }

    private static void mergeBaseStats(CustomUnit cu, MaskUnit a, MaskUnit b) {
        cu.hp = Math.max(a.getHp(), b.getHp());
        cu.hb = Math.max(a.getHb(), b.getHb());
        cu.speed = Math.max(a.getSpeed(), b.getSpeed());
        cu.price = Math.max(a.getPrice(), b.getPrice());
        cu.resp = Math.max(a.getRespawn(), b.getRespawn());

    }

    private static void mergeDamage(CustomUnit cu, MaskUnit a, MaskUnit b) {
        int totalA = a.allAtk();
        int totalB = b.allAtk();
        if (totalA <= 0 || totalB <= totalA || cu.atks == null) return;
        double factor = (double) totalB / totalA;
        for (AtkDataModel atk : cu.atks) {
            if (atk != null) atk.atk = (int) Math.round(atk.atk * factor);
        }
        if (cu.rep != null) cu.rep.atk = (int) Math.round(cu.rep.atk * factor);
        Logger.log("UnitCreator: scaled damage x" + String.format("%.2f", factor));
    }

    private static void mergeProcs(CustomUnit cu, MaskUnit b) {
        if (cu.rep == null || cu.rep.proc == null) return;
        Data.Proc dst = cu.rep.proc;
        Data.Proc pb = b.getProc();
        if (pb == null) return;
        cu.common = true;
        int merged = 0;
        for (int i = 0; i < PROC_COUNT; i++) {
            Object a = dst.getArr(i);
            Object bp = pb.getArr(i);
            if (a == null || bp == null) continue;
            boolean ae = exists(a), be = exists(bp);
            if (!be) continue;
            if (!ae || bStronger(i, a, bp)) { copy(a, bp); merged++; }
        }
        Logger.log("UnitCreator: merged " + merged + " procs from B");
    }

    @SuppressWarnings("unchecked")
    private static void mergeAbilities(CustomUnit cu, MaskUnit b) {
        cu.abi |= b.getAbi();
        try {
            List<Trait> bt = b.getTraits();
            if (bt != null && cu.traits != null) {
                for (Trait t : bt) if (t != null && !cu.traits.contains(t)) cu.traits.add(t);
            }
        } catch (Throwable ignored) {}
    }

    private static boolean bStronger(int index, Object a, Object b) {
        if (index == WEAK) {

            Integer ma = field(a, "mult"), mb = field(b, "mult");
            if (ma != null && mb != null && !ma.equals(mb)) return mb < ma;

        }
        Integer pa = field(a, "prob"), pb = field(b, "prob");
        if (pa != null && pb != null && !pa.equals(pb)) return pb > pa;
        return magnitude(b) > magnitude(a);
    }

    private static Integer field(Object item, String name) {
        try {
            Field f = item.getClass().getField(name);
            if (f.getType() == int.class) return f.getInt(item);
        } catch (Throwable ignored) {}
        return null;
    }

    private static long magnitude(Object item) {
        long sum = 0;
        for (Field f : item.getClass().getFields()) {
            if (f.getType() == int.class) {
                try { sum += Math.max(0, f.getInt(item)); } catch (Throwable ignored) {}
            }
        }
        return sum;
    }

    private static boolean exists(Object item) {
        try {
            Method m = item.getClass().getMethod("exists");
            Object r = m.invoke(item);
            return r instanceof Boolean && (Boolean) r;
        } catch (Throwable t) {
            return magnitude(item) > 0;
        }
    }

    private static void copy(Object dst, Object src) {
        try {
            for (Method m : dst.getClass().getMethods()) {
                if (!m.getName().equals("set") || m.getParameterCount() != 1) continue;
                Class<?> pt = m.getParameterTypes()[0];
                if (pt.isPrimitive()) continue;
                if (pt.isInstance(src)) { m.invoke(dst, src); return; }
            }
        } catch (Throwable t) {
            Logger.err("UnitCreator: proc copy failed", t);
        }
    }
}
