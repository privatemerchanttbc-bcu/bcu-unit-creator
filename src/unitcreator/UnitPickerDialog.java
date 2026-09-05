package unitcreator;

import common.pack.PackData;
import common.pack.UserProfile;
import common.system.fake.FakeImage;
import common.util.anim.AnimU;
import common.util.unit.Enemy;
import common.util.unit.Form;
import common.util.unit.Unit;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class UnitPickerDialog {

    private static final String[] FORM_TAGS = {"f", "c", "s", "u"};

    private UnitPickerDialog() {}

    static Form pickForm(Component parent, String title) {
        Donor d = pick(parent, title, false);
        return d == null ? null : d.form();
    }

    static Donor pickDonor(Component parent, String title) {
        return pick(parent, title, true);
    }

    private static Donor pick(Component parent, String title, boolean allowEnemies) {
        final List<Donor> units = collectUnits();
        final List<Donor> enemies = allowEnemies ? collectEnemies() : new ArrayList<Donor>();
        if (units.isEmpty() && enemies.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Nothing found (packs may still be loading).",
                    "Unit Creator", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        final ArrayList<Donor> visible = new ArrayList<Donor>();
        final DefaultTableModel model = new DefaultTableModel(new Object[]{"Name"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        final JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Font cell = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
        table.setFont(cell);
        table.setRowHeight(28);
        table.getTableHeader().setFont(cell.deriveFont(Font.BOLD, 16f));

        final JTextField search = new JTextField();
        search.setFont(cell);
        search.setToolTipText("Type to filter by name or pack id");

        final JRadioButton srcUnit = new JRadioButton("Units", true);
        final JRadioButton srcEnemy = new JRadioButton("Enemies");
        ButtonGroup grp = new ButtonGroup();
        grp.add(srcUnit);
        grp.add(srcEnemy);

        final IconView icon = new IconView();

        final Runnable refilter = new Runnable() {
            @Override public void run() {
                String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
                List<Donor> pool = srcEnemy.isSelected() ? enemies : units;
                model.setRowCount(0);
                visible.clear();
                for (int i = 0; i < pool.size(); i++) {
                    Donor d = pool.get(i);
                    String lbl = d.label();
                    if (!q.isEmpty() && !lbl.toLowerCase(Locale.ROOT).contains(q)) continue;
                    model.addRow(new Object[]{lbl});
                    visible.add(d);
                }
                if (model.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
                else icon.set(null);
            }
        };
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refilter.run(); }
            @Override public void removeUpdate(DocumentEvent e) { refilter.run(); }
            @Override public void changedUpdate(DocumentEvent e) { refilter.run(); }
        });
        srcUnit.addActionListener(e -> refilter.run());
        srcEnemy.addActionListener(e -> refilter.run());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int r = table.getSelectedRow();
            icon.set(r >= 0 && r < visible.size() ? visible.get(r) : null);
        });

        refilter.run();
        if (!visible.isEmpty()) icon.set(visible.get(0));

        JPanel top = new JPanel(new BorderLayout(6, 4));
        if (allowEnemies) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            row.add(srcUnit);
            row.add(srcEnemy);
            top.add(row, BorderLayout.NORTH);
        }
        top.add(search, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(top, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(460, 520));
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(icon, BorderLayout.EAST);

        int ok = JOptionPane.showConfirmDialog(parent, panel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return null;
        int row = table.getSelectedRow();
        return row >= 0 && row < visible.size() ? visible.get(row) : null;
    }

    private static final class IconView extends JPanel {
        private BufferedImage img;
        IconView() {
            setPreferredSize(new Dimension(140, 140));
            setBorder(BorderFactory.createTitledBorder("Preview"));
        }
        void set(Donor d) {
            img = d == null ? null : d.icon();
            repaint();
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            java.awt.Insets in = getInsets();
            int w = getWidth() - in.left - in.right, h = getHeight() - in.top - in.bottom;
            g.setColor(new Color(46, 52, 60));
            g.fillRect(in.left, in.top, w, h);
            if (img != null) {
                double s = Math.min((w - 12.0) / img.getWidth(), (h - 12.0) / img.getHeight());
                s = Math.min(s, 2.0);
                int dw = (int) (img.getWidth() * s), dh = (int) (img.getHeight() * s);
                g.drawImage(img, in.left + (w - dw) / 2, in.top + (h - dh) / 2, dw, dh, null);
            }
        }
    }

    static BufferedImage iconOf(Form f) {
        try {
            if (f == null || f.anim == null) return null;
            AnimU<?> anim = (AnimU<?>) f.anim;
            FakeImage fi = anim.getUni() == null ? null : anim.getUni().getImg();
            Object o = fi == null ? null : fi.bimg();
            return o instanceof BufferedImage ? (BufferedImage) o : null;
        } catch (Throwable t) {
            return null;
        }
    }

    static List<Donor> collectUnits() {
        ArrayList<Donor> out = new ArrayList<Donor>();
        Set<String> seen = new HashSet<String>();
        try {
            for (PackData pack : UserProfile.getAllPacks()) {
                if (pack == null || pack.units == null) continue;
                List<Unit> units;
                try { units = pack.units.getList(); } catch (Throwable t) { continue; }
                if (units == null) continue;
                for (Unit u : units) {
                    if (u == null || u.id == null || u.forms == null) continue;
                    if (!seen.add(u.id.pack + ":" + u.id.id)) continue;
                    for (Form f : u.forms) {
                        if (f != null && f.du != null && f.unit != null) out.add(Donor.of(f));
                    }
                }
            }
        } catch (Throwable t) {
            Logger.err("UnitCreator: unit enumeration failed", t);
        }
        return out;
    }

    static List<Donor> collectEnemies() {
        ArrayList<Donor> out = new ArrayList<Donor>();
        Set<String> seen = new HashSet<String>();
        try {
            for (PackData pack : UserProfile.getAllPacks()) {
                if (pack == null || pack.enemies == null) continue;
                List<Enemy> es;
                try { es = pack.enemies.getList(); } catch (Throwable t) { continue; }
                if (es == null) continue;
                for (Enemy e : es) {
                    if (e == null || e.id == null || e.anim == null) continue;
                    if (!seen.add(e.id.pack + ":" + e.id.id)) continue;
                    out.add(Donor.of(e));
                }
            }
        } catch (Throwable t) {
            Logger.err("UnitCreator: enemy enumeration failed", t);
        }
        return out;
    }

    static String label(Form f) {
        String name = null;
        try {
            name = common.util.lang.MultiLangCont.get(f);
            if (name == null || name.trim().isEmpty()) name = f.names != null ? f.names.toString() : null;
            if (name == null || name.trim().isEmpty()) name = f.name;
        } catch (Throwable ignored) {}
        if (name == null || name.trim().isEmpty()) name = String.valueOf(f);
        String tag = f.fid >= 0 && f.fid < FORM_TAGS.length ? FORM_TAGS[f.fid] : "?";
        String pack = "";
        try { pack = f.unit.id.pack + "-" + f.unit.id.id; } catch (Throwable ignored) {}
        return "[" + pack + " " + tag + "] " + name;
    }
}
