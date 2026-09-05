package unitcreator;

import page.JBTN;
import page.Page;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class UnitCreatorHooks {

    private static final Map<Object, JBTN> BUTTONS =
            Collections.synchronizedMap(new WeakHashMap<Object, JBTN>());

    private static final String ENABLED = "unitcreator.enabled";

    private UnitCreatorHooks() {}

    public static void onMainPageBuilt(final Object mainPage) {
        if (!Boolean.parseBoolean(System.getProperty(ENABLED, "true"))) return;
        try {
            if (BUTTONS.containsKey(mainPage)) return;
            if (alreadyPresent((Container) mainPage)) {
                Logger.log("UnitCreator: MainPage button already present, skipping");
                return;
            }
            JBTN btn = new JBTN("Unit Creator");
            btn.setLnr((Consumer<ActionEvent>) new Consumer<ActionEvent>() {
                @Override public void accept(ActionEvent e) { onClicked(mainPage); }
            });
            ((Container) mainPage).add(btn);
            BUTTONS.put(mainPage, btn);
            Logger.log("UnitCreator: MainPage button added");
        } catch (Throwable t) {
            Logger.err("UnitCreator: failed to add MainPage button", t);
        }
    }

    public static void onMainPageResized(Object mainPage, int w, int h) {
        JBTN btn = BUTTONS.get(mainPage);
        if (btn == null) return;
        try {
            Page.set(btn, w, h, 1500, 680, 200, 50);
        } catch (Throwable t) {
            Logger.err("UnitCreator: failed to place MainPage button", t);
        }
    }

    private static boolean alreadyPresent(Container c) {
        try {
            for (int i = 0; i < c.getComponentCount(); i++) {
                java.awt.Component k = c.getComponent(i);
                if (k instanceof JBTN && "Unit Creator".equals(((JBTN) k).getText())) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    static void onClicked(Object mainPage) {
        try {
            UnitGraftDialog.show(mainPage);
        } catch (Throwable t) {
            Logger.err("UnitCreator: dialog failed", t);
        }
    }
}
