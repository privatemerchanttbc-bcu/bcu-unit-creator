package unitcreator;

import common.util.unit.Form;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.List;

final class SaveDialog {

    private static final int ICON = 34;

    static final class Result {
        final boolean mergeStats;
        Result(boolean mergeStats) { this.mergeStats = mergeStats; }
    }

    private SaveDialog() {}

    static Result show(Window owner, Form base, List<Donor> donors, boolean mergeDefault) {
        final boolean[] ok = {false};
        final JDialog dlg = new JDialog(owner, "Save new unit", JDialog.ModalityType.APPLICATION_MODAL);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        body.setBackground(UCUI.panel());

        JLabel head = new JLabel("These units were used to build the new unit:");
        head.setForeground(UCUI.text());
        head.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        body.add(head, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBackground(UCUI.inset());

        int statDonors = 0;
        rows.add(row(iconOf(base), baseName(base), "Base - skeleton, animation and stats"));
        for (int i = 0; donors != null && i < donors.size(); i++) {
            Donor d = donors.get(i);
            if (d == null) continue;
            boolean stats = d.stats() != null;
            if (stats) statDonors++;
            rows.add(row(d.icon(), d.name(),
                    stats ? "Parts + stats" : "Parts only - enemies have no unit stats"));
        }

        JScrollPane sc = new JScrollPane(rows,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(BorderFactory.createLineBorder(UCUI.line()));
        sc.getVerticalScrollBar().setUnitIncrement(16);
        sc.setPreferredSize(new Dimension(430, Math.min(260, 18 + ICON + 10
                + (donors == null ? 0 : donors.size()) * (ICON + 10))));
        body.add(sc, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setOpaque(false);

        final JCheckBox merge = new JCheckBox(
                "Give the new unit the combined stats of these units", mergeDefault && statDonors > 0);
        merge.setOpaque(false);
        merge.setForeground(UCUI.text());
        merge.setEnabled(statDonors > 0);

        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        line.setOpaque(false);
        line.add(merge);
        line.add(UCUI.help(statDonors > 0
                ? "On: the new unit takes the best of every unit listed above - highest health, "
                + "knockbacks, speed, cost and respawn, attack scaled up to the strongest donor, "
                + "and every ability, trait and special effect they own.<br><br>"
                + "Off: the new unit keeps unit 1's stats exactly and only gains the new body parts."
                : "No unit donors were used - only enemy parts were attached, and enemies carry no "
                + "unit stats. The new unit keeps unit 1's stats."));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        south.add(line);

        JLabel note = new JLabel("The list above is also written into the new unit's description.");
        note.setForeground(UCUI.muted());
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        note.setBorder(BorderFactory.createEmptyBorder(4, 4, 6, 0));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        south.add(note);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        UCButton cancel = new UCButton("Cancel", UCButton.Kind.NEUTRAL);
        UCButton save = new UCButton("Save", UCButton.Kind.PRIMARY);
        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> { ok[0] = true; dlg.dispose(); });
        buttons.add(cancel);
        buttons.add(save);
        south.add(Box.createVerticalStrut(2));
        south.add(buttons);

        body.add(south, BorderLayout.SOUTH);
        dlg.setContentPane(body);
        dlg.getRootPane().setDefaultButton(save);
        dlg.pack();
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);

        return ok[0] ? new Result(merge.isSelected()) : null;
    }

    static String describe(Form base, List<Donor> donors) {
        StringBuilder sb = new StringBuilder("Made in Unit Creator from: ");
        sb.append(baseName(base)).append(" (base)");
        for (int i = 0; donors != null && i < donors.size(); i++) {
            Donor d = donors.get(i);
            if (d == null) continue;
            sb.append(", ").append(d.name());
            sb.append(d.stats() != null ? " (parts + stats)" : " (parts)");
        }
        return sb.toString();
    }

    private static JPanel row(BufferedImage img, String name, String role) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));

        JLabel ic = new JLabel();
        ic.setPreferredSize(new Dimension(ICON, ICON));
        if (img != null) {
            int w = img.getWidth(), h = img.getHeight();
            double s = Math.min((double) ICON / Math.max(1, w), (double) ICON / Math.max(1, h));
            int nw = Math.max(1, (int) Math.round(w * s));
            int nh = Math.max(1, (int) Math.round(h * s));
            ic.setIcon(new ImageIcon(img.getScaledInstance(nw, nh, Image.SCALE_SMOOTH)));
        }
        p.add(ic, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel n = new JLabel(name);
        n.setForeground(UCUI.text());
        n.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        JLabel r = new JLabel(role);
        r.setForeground(UCUI.muted());
        r.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        n.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(n);
        text.add(r);
        p.add(text, BorderLayout.CENTER);
        return p;
    }

    private static String baseName(Form base) {
        try { return UnitCombiner.displayName(base); } catch (Throwable t) { return "Unit 1"; }
    }

    private static BufferedImage iconOf(Form base) {
        try { return UnitPickerDialog.iconOf(base); } catch (Throwable t) { return null; }
    }
}
