package unitcreator;

import common.battle.data.MaskUnit;
import common.system.VImg;
import common.system.fake.FakeImage;
import common.util.anim.AnimU;
import common.util.anim.EAnimU;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import common.util.unit.Form;

import java.awt.image.BufferedImage;

final class Donor {

    private final Form form;
    private final Enemy enemy;

    private Donor(Form f, Enemy e) {
        form = f;
        enemy = e;
    }

    static Donor of(Form f) {
        return f == null ? null : new Donor(f, null);
    }

    static Donor of(Enemy e) {
        return e == null ? null : new Donor(null, e);
    }

    boolean isEnemy() {
        return enemy != null;
    }

    Form form() {
        return form;
    }

    Object source() {
        return form != null ? (Object) form : (Object) enemy;
    }

    String label() {
        return form != null ? UnitPickerDialog.label(form) : enemyLabel();
    }

    String name() {
        if (form != null) return UnitCombiner.displayName(form);
        String n = null;
        try { n = MultiLangCont.get(enemy); } catch (Throwable ignored) {}
        if (n == null || n.trim().isEmpty()) {
            try { n = enemy.names == null ? null : enemy.names.toString(); } catch (Throwable ignored) {}
        }
        if (n == null || n.trim().isEmpty()) n = enemy.name;
        return n == null || n.trim().isEmpty() ? "Enemy" : n.trim();
    }

    AnimU<?> anim() {
        try {
            Object a = form != null ? form.anim : enemy.anim;
            return a instanceof AnimU ? (AnimU<?>) a : null;
        } catch (Throwable t) {
            return null;
        }
    }

    EAnimU eanim(AnimU.UType t) {
        try {
            return form != null ? form.getEAnim(t) : enemy.getEAnim(t);
        } catch (Throwable e) {
            return null;
        }
    }

    MaskUnit stats() {
        try {
            return form != null ? form.du : null;
        } catch (Throwable t) {
            return null;
        }
    }

    BufferedImage icon() {
        try {
            if (form != null) return UnitPickerDialog.iconOf(form);
            VImg v = enemy.getIcon();
            FakeImage fi = v == null ? null : v.getImg();
            Object o = fi == null ? null : fi.bimg();
            return o instanceof BufferedImage ? (BufferedImage) o : null;
        } catch (Throwable t) {
            return null;
        }
    }

    String key() {
        try {
            if (form != null) return "u:" + form.unit.id.pack + ":" + form.unit.id.id + ":" + form.fid;
            return "e:" + enemy.id.pack + ":" + enemy.id.id;
        } catch (Throwable t) {
            return String.valueOf(source());
        }
    }

    private String enemyLabel() {
        String pack = "";
        try { pack = enemy.id.pack + "-" + enemy.id.id; } catch (Throwable ignored) {}
        return "[" + pack + " e] " + name();
    }
}
