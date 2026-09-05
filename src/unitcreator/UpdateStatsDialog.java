package unitcreator;

import common.battle.data.CustomUnit;
import common.battle.data.MaskEntity;
import common.battle.data.MaskUnit;
import common.pack.PackData;
import common.util.unit.Form;
import common.util.unit.Unit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

final class UpdateStatsDialog {

    private UpdateStatsDialog() {}

    static void show(Window owner) {
        List<Unit> made = madeUnits();
        if (made.isEmpty()) {
            JOptionPane.showMessageDialog(owner,
                    "The \"UnitCreator\" pack has no units yet.\n"
                            + "Build one with Save as new Unit first.",
                    "Unit Creator", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final JDialog dlg = new JDialog(owner, "Update stats",
                JDialog.ModalityType.APPLICATION_MODAL);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        body.setBackground(UCUI.panel());

        JLabel head = new JLabel("Rebuild a unit's stats from the units it was made of.");
        head.setForeground(UCUI.text());
        head.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        body.add(head, BorderLayout.NORTH);

        final DefaultListModel<Unit> targets = new DefaultListModel<Unit>();
        for (int i = 0; i < made.size(); i++) targets.addElement(made.get(i));
        final JList<Unit> target = new JList<Unit>(targets);
        target.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        target.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i,
                                                          boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, v, i, sel, foc);
                if (v instanceof Unit) setText(unitName((Unit) v));
                return this;
            }
        });
        target.setSelectedIndex(0);

        final JLabel origin = new JLabel(" ");
        origin.setForeground(UCUI.muted());
        origin.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        final Form[] base = {null};
        final List<Donor> parts = new ArrayList<Donor>();
        final DefaultListModel<String> partModel = new DefaultListModel<String>();
        final JList<String> partList = new JList<String>(partModel);

        final JLabel baseLabel = new JLabel("not chosen");
        baseLabel.setForeground(UCUI.muted());

        UCButton pickBase = new UCButton("Choose base unit", UCButton.Kind.NEUTRAL);
        UCButton addPart = new UCButton("Add component", UCButton.Kind.NEUTRAL);
        UCButton dropPart = new UCButton("Remove", UCButton.Kind.NEUTRAL);
        final UCButton apply = new UCButton("Apply", UCButton.Kind.PRIMARY);
        UCButton cancel = new UCButton("Close", UCButton.Kind.NEUTRAL);

        final JCheckBox merge = new JCheckBox(
                "Give it the combined stats of the components", true);
        merge.setOpaque(false);
        merge.setForeground(UCUI.text());

        apply.setEnabled(false);

        final Runnable prefill = () -> {
            Unit u = target.getSelectedValue();
            if (u == null) return;
            OriginStore.Origin o = OriginStore.load(u);
            if (o == null) {
                origin.setText("Not recorded - choose its parts on the right.");
                return;
            }
            base[0] = OriginStore.resolveForm(o.baseKey);
            parts.clear();
            partModel.clear();
            int lost = base[0] == null ? 1 : 0;
            for (int i = 0; i < o.partKeys.size(); i++) {
                Donor d = OriginStore.resolveDonor(o.partKeys.get(i));
                if (d == null) { lost++; continue; }
                parts.add(d);
                partModel.addElement(d.name() + (d.stats() != null ? "" : "   (parts only)"));
            }
            if (base[0] != null) {
                baseLabel.setText(UnitCombiner.displayName(base[0]));
                baseLabel.setForeground(UCUI.text());
                apply.setEnabled(true);
            } else {
                baseLabel.setText("not chosen");
                baseLabel.setForeground(UCUI.muted());
                apply.setEnabled(false);
            }
            origin.setText(lost == 0 ? "Filled in from this unit's own record."
                    : "Recorded, but " + lost + " entry could not be found - check the right side.");
        };
        target.addListSelectionListener(e -> prefill.run());
        prefill.run();

        pickBase.addActionListener(e -> {
            Form f = UnitPickerDialog.pickForm(dlg, "Choose the base unit");
            if (f == null) return;
            base[0] = f;
            baseLabel.setText(UnitCombiner.displayName(f));
            baseLabel.setForeground(UCUI.text());
            apply.setEnabled(true);
        });
        addPart.addActionListener(e -> {
            Donor d = UnitPickerDialog.pickDonor(dlg, "Choose a component");
            if (d == null) return;
            for (int i = 0; i < parts.size(); i++)
                if (parts.get(i).key().equals(d.key())) return;
            parts.add(d);
            partModel.addElement(d.name() + (d.stats() != null ? "" : "   (parts only)"));
        });
        dropPart.addActionListener(e -> {
            int i = partList.getSelectedIndex();
            if (i < 0) return;
            parts.remove(i);
            partModel.remove(i);
        });

        apply.addActionListener(e -> {
            Unit u = target.getSelectedValue();
            if (u == null || base[0] == null) return;
            int ans = JOptionPane.showConfirmDialog(dlg,
                    "This replaces \"" + unitName(u) + "\" stats with a fresh copy of\n"
                            + UnitCombiner.displayName(base[0]) + "'s stats"
                            + (merge.isSelected() ? ", then merges the components in." : ".")
                            + "\n\nAny hand edits you made to this unit's stats are lost.\n"
                            + "Its animation and its name are not touched.\n\nContinue?",
                    "Update stats", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ans != JOptionPane.OK_OPTION) return;
            String err = rebuild(u, base[0], parts, merge.isSelected());
            if (err == null) OriginStore.record(u, base[0], parts);
            if (err != null) {
                JOptionPane.showMessageDialog(dlg, "Could not update: " + err,
                        "Unit Creator", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(dlg,
                    "\"" + unitName(u) + "\" now has "
                            + (merge.isSelected() ? "the combined stats of "
                                    + UnitCombiner.displayName(base[0]) + " and its components."
                                    : "exactly " + UnitCombiner.displayName(base[0]) + "'s stats.")
                            + "\n\nRun this again any time - it always starts from the base unit,\n"
                            + "so the result never drifts.",
                    "Unit Creator", JOptionPane.INFORMATION_MESSAGE);
        });
        cancel.addActionListener(e -> dlg.dispose());

        JPanel left = new JPanel(new BorderLayout(0, 4));
        left.setOpaque(false);
        left.setBorder(UCUI.card("Unit to update"));
        JScrollPane ts = new JScrollPane(target);
        ts.setPreferredSize(new Dimension(240, 150));
        left.add(ts, BorderLayout.CENTER);
        left.add(origin, BorderLayout.SOUTH);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        right.setBorder(UCUI.card("Made from"));

        JPanel baseRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        baseRow.setOpaque(false);
        baseRow.add(pickBase);
        baseRow.add(baseLabel);
        baseRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(baseRow);

        JScrollPane ps = new JScrollPane(partList);
        ps.setPreferredSize(new Dimension(260, 96));
        ps.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(Box.createVerticalStrut(6));
        right.add(ps);

        JPanel partRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        partRow.setOpaque(false);
        partRow.add(addPart);
        partRow.add(dropPart);
        partRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(Box.createVerticalStrut(4));
        right.add(partRow);

        JPanel mid = new JPanel(new GridLayout(1, 2, 10, 0));
        mid.setOpaque(false);
        mid.add(left);
        mid.add(right);
        body.add(mid, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setOpaque(false);

        JPanel mergeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        mergeRow.setOpaque(false);
        mergeRow.add(merge);
        mergeRow.add(UCUI.help("On: the unit takes the best of the base and every component - "
                + "highest health, knockbacks, speed, cost and respawn, attack scaled up to the "
                + "strongest one, and all of their abilities, traits and special effects.<br><br>"
                + "Off: the unit gets the base unit's stats and nothing else.<br><br>"
                + "Either way the stats are rebuilt from the base unit rather than added on top "
                + "of what is there now, so running this twice gives the same result."));
        mergeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        south.add(mergeRow);

        JLabel note = new JLabel("Only stats change. The animation, the icon and the name stay as they are.");
        note.setForeground(UCUI.muted());
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        note.setBorder(BorderFactory.createEmptyBorder(4, 4, 6, 0));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        south.add(note);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.add(cancel);
        buttons.add(apply);
        south.add(buttons);

        body.add(south, BorderLayout.SOUTH);
        dlg.setContentPane(body);
        dlg.pack();
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }

    private static String rebuild(Unit u, Form base, List<Donor> parts, boolean mergeStats) {
        try {
            if (u.forms == null || u.forms.length == 0) return "the unit has no form";
            Form f = u.forms[0];
            if (!(f.du instanceof CustomUnit)) return "this unit's stats are not editable";
            if (base.du == null) return "the base unit has no stats";

            CustomUnit cu = (CustomUnit) f.du;
            cu.importData((MaskEntity) base.du);

            int merged = 0;
            if (mergeStats) {
                for (int i = 0; i < parts.size(); i++) {
                    MaskUnit du = parts.get(i).stats();
                    if (du == null) continue;
                    StatMerger.merge(cu, cu, du);
                    merged++;
                }
            }

            try { f.description.put(SaveDialog.describe(base, parts)); } catch (Throwable ignored) {}
            UnitCreatorPack.save();
            Logger.log("UnitCreator: rebuilt stats for '" + unitName(u) + "' from "
                    + UnitCombiner.displayName(base) + " (stats "
                    + (mergeStats ? "merged" : "kept") + ", " + merged + " stat donors)");
            return null;
        } catch (Throwable t) {
            Logger.err("UnitCreator: stat rebuild failed", t);
            return UCUI.describe(t);
        }
    }

    private static List<Unit> madeUnits() {
        List<Unit> out = new ArrayList<Unit>();
        try {
            PackData.UserPack pack = UnitCreatorPack.get();
            if (pack != null && pack.units != null) {
                for (Unit u : pack.units.getList()) if (u != null) out.add(u);
            }
        } catch (Throwable t) {
            Logger.err("UnitCreator: could not list made units", t);
        }
        return out;
    }

    private static String unitName(Unit u) {
        try { return UnitCombiner.displayName(u.forms[0]); } catch (Throwable t) { return "Unit"; }
    }
}
