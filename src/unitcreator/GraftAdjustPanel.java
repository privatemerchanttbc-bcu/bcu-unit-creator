package unitcreator;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Hashtable;
import java.util.List;

final class GraftAdjustPanel extends JPanel {

    interface Listener {
        void onChanged(GraftOp op);
        void onRemove(GraftOp op);
        void onSelect(GraftOp op);
        void onRecopy(GraftOp op);
        void onMirror(GraftOp op);
    }

    private static final int POS_RANGE = 1000;
    private static final int ANG_RANGE = 3600;
    private static final int SCALE_MIN = 1, SCALE_MAX = 1000;
    private static final int SPEED_MIN = 50, SPEED_MAX = 200;
    private static final int POS_TYPED = 20000, SCALE_TYPED = 2000, SPEED_TYPED = 1000;

    private static final String H_X = "<b>Move X</b> - slides the attached branch left or right, in the model's own units rather than screen pixels. The branch already follows the part it hangs from, so this is a fine correction on top of that.";
    private static final String H_Y = "<b>Move Y</b> - slides the attached branch up or down, in the model's own units. Negative values move it up.";
    private static final String H_SIZE = "<b>Size</b> - scales the whole branch. 100% is the size picked automatically when you dropped it, which already accounts for the two units being different sizes, so you are adjusting relative to a sensible starting point.";
    private static final String H_ROT = "<b>Rotate</b> - turns the branch around the point where it attaches. A full circle is 3600, so 900 is a quarter turn and negative values turn the other way.";
    private static final String H_DEPTH = "<b>Depth</b> - which layer the branch is drawn on. Lower values sit behind the base unit, higher values in front. It starts just in front of the part you dropped onto.";
    private static final String H_SPEED = "<b>Speed</b> - how fast the branch plays its own motion. 100% fits exactly one of its cycles into one loop of the base unit. Above 100% it cycles more than once per loop. Below 100% it moves slower and repeats before finishing its arc - it is trimmed rather than lengthened, so the base unit never has to stand and wait. The line underneath tells you the value that matches the branch's original pace.";
    private static final String H_COPY = "<b>Copy animation</b> - on, the branch brings its own motion across. Off, only the artwork is copied and the branch simply rides along with the part it hangs from. Turn it off when the source motion fights the base unit.";
    private static final String H_MIRROR = "<b>Mirror</b> - flips the branch horizontally about its attachment point. Both the artwork and its motion are flipped, so a left arm becomes a working right arm.";
    private static final int DEPTH_TYPED = 100000;

    Listener listener;

    private final DefaultListModel<GraftOp> listModel = new DefaultListModel<GraftOp>();
    private final JList<GraftOp> list = new JList<GraftOp>(listModel);
    private final JSlider posX = new JSlider(-POS_RANGE, POS_RANGE, 0);
    private final JSlider posY = new JSlider(-POS_RANGE, POS_RANGE, 0);
    private final JSlider scale = new JSlider(SCALE_MIN, SCALE_MAX, 100);
    private final JSlider angle = new JSlider(-ANG_RANGE, ANG_RANGE, 0);
    private final JSlider depth = new JSlider(-64, 64, 0);
    private final JSlider speed = new JSlider(SPEED_MIN, SPEED_MAX, 100);
    private final JTextField posXv = value(), posYv = value(), scalev = value();
    private final JTextField anglev = value(), depthv = value(), speedv = value();

    private static boolean defaultCopyAnim = true;

    private final UCButton copyAnim = new UCButton("Copy animation", UCButton.Kind.NEUTRAL);
    private final UCButton mirror = new UCButton("Mirror", UCButton.Kind.NEUTRAL);
    private final UCButton remove = new UCButton("Remove graft", UCButton.Kind.NEUTRAL);

    private boolean adjusting;
    private int type = 1;
    private GraftOp current;

    GraftAdjustPanel() {
        setLayout(new BorderLayout(10, 0));
        setBorder(UCUI.card("Attached parts"));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i,
                                                                    boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, v, i, sel, foc);
                if (v instanceof GraftOp) setText(((GraftOp) v).label());
                return this;
            }
        });
        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || adjusting) return;
            bind(list.getSelectedValue());
            if (listener != null) listener.onSelect(current);
        });
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(320, 132));

        JPanel side = new JPanel(new BorderLayout(0, 6));
        side.setOpaque(false);
        side.add(sp, BorderLayout.CENTER);
        JPanel foot = new JPanel(new GridLayout(2, 1, 0, 4));
        foot.setOpaque(false);
        copyAnim.setOn(defaultCopyAnim);
        copyAnim.setToolTipText("On: bring the branch's own motion across. "
                + "Off: copy the artwork only and let it follow the part it hangs from.");
        JPanel caRow = new JPanel(new BorderLayout(4, 0));
        caRow.setOpaque(false);
        caRow.add(copyAnim, BorderLayout.CENTER);
        caRow.add(UCUI.help(H_COPY), BorderLayout.EAST);
        foot.add(caRow);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.setOpaque(false);
        btns.add(mirror);
        btns.add(UCUI.help(H_MIRROR));
        btns.add(remove);
        foot.add(btns);
        side.add(foot, BorderLayout.SOUTH);
        add(side, BorderLayout.WEST);

        JPanel grid = new JPanel(new BorderLayout(0, 2));
        grid.setOpaque(false);
        JPanel plain = new JPanel(new GridLayout(5, 1, 0, 2));
        plain.setOpaque(false);
        plain.add(row("Move X", H_X, posX, posXv));
        plain.add(row("Move Y", H_Y, posY, posYv));
        plain.add(row("Size", H_SIZE, scale, scalev));
        plain.add(row("Rotate", H_ROT, angle, anglev));
        plain.add(row("Depth", H_DEPTH, depth, depthv));
        grid.add(plain, BorderLayout.CENTER);
        speed.setToolTipText("Speed of the copied branch's own motion. "
                + "Above 100% it cycles more than once per base loop; below 100% it moves slower "
                + "and repeats before finishing, so the base unit never has to wait.");
        JPanel speedRow = row("Speed", H_SPEED, speed, speedv);
        speedRow.setPreferredSize(new Dimension(10, 46));
        grid.add(speedRow, BorderLayout.SOUTH);

        speed.setPaintLabels(true);
        add(grid, BorderLayout.CENTER);

        bindField(posXv, posX, -POS_TYPED, POS_TYPED, false);
        bindField(posYv, posY, -POS_TYPED, POS_TYPED, false);
        bindField(scalev, scale, 1, SCALE_TYPED, false);
        bindField(anglev, angle, -ANG_RANGE, ANG_RANGE, false);
        bindField(depthv, depth, -DEPTH_TYPED, DEPTH_TYPED, false);
        bindField(speedv, speed, 5, SPEED_TYPED, true);

        posX.addChangeListener(e -> push());
        posY.addChangeListener(e -> push());
        scale.addChangeListener(e -> push());
        angle.addChangeListener(e -> push());
        depth.addChangeListener(e -> push());
        speed.addChangeListener(e -> {
            if (adjusting || current == null) return;
            current.speed[type] = speed.getValue();
            speedv.setText(speed.getValue() + "%");
            list.repaint();
            if (!speed.getValueIsAdjusting() && current.copyAnim
                    && listener != null) listener.onRecopy(current);
        });
        mirror.addActionListener(e -> {
            if (current == null) return;
            current.mirror = !current.mirror;
            mirror.setOn(current.mirror);
            if (listener != null) listener.onMirror(current);
            bindSliders();
            list.repaint();
        });
        copyAnim.addActionListener(e -> {
            if (current == null) {
                defaultCopyAnim = !defaultCopyAnim;
                copyAnim.setOn(defaultCopyAnim);
                return;
            }
            current.copyAnim = !current.copyAnim;
            defaultCopyAnim = current.copyAnim;
            copyAnim.setOn(current.copyAnim);
            if (listener != null) listener.onRecopy(current);
        });
        remove.addActionListener(e -> {
            if (current != null && listener != null) listener.onRemove(current);
        });

        setEnabledAll(false);
    }

    void setOps(List<GraftOp> ops, GraftOp select) {
        adjusting = true;
        try {
            listModel.clear();
            for (int i = 0; ops != null && i < ops.size(); i++) listModel.addElement(ops.get(i));
            if (select != null && listModel.contains(select)) list.setSelectedValue(select, true);
            else if (!listModel.isEmpty()) list.setSelectedIndex(listModel.size() - 1);
        } finally {
            adjusting = false;
        }
        bind(list.getSelectedValue());
    }

    void setZRange(int[] span) {
        if (span == null || span.length < 2) return;
        adjusting = true;
        try {
            depth.setMinimum(span[0]);
            depth.setMaximum(span[1]);
        } finally {
            adjusting = false;
        }
    }

    static boolean defaultCopyAnim() {
        return defaultCopyAnim;
    }

    GraftOp current() {
        return current;
    }

    void select(GraftOp op) {
        if (op == null || current == op) return;
        adjusting = true;
        try {
            list.setSelectedValue(op, true);
        } finally {
            adjusting = false;
        }
        bind(op);
    }

    void setType(int t) {
        type = t < 0 || t >= GraftOp.TYPES ? 1 : t;
        setBorder(UCUI.card("Attached parts - " + GraftOp.TYPE_NAMES[type]));
        if (current != null) bindSliders();
        repaint();
    }

    private void bind(GraftOp op) {
        current = op;
        setEnabledAll(op != null);
        if (op == null) return;
        bindSliders();
    }

    private void bindSliders() {
        adjusting = true;
        try {
            GraftOp op = current;
            posX.setValue(clamp(op.offX[type], -POS_RANGE, POS_RANGE));
            posY.setValue(clamp(op.offY[type], -POS_RANGE, POS_RANGE));
            scale.setValue(clamp(op.scale[type] / 10, SCALE_MIN, SCALE_MAX));
            angle.setValue(clamp(op.angle[type], -ANG_RANGE, ANG_RANGE));
            int z = op.zOffset[type];
            if (z < depth.getMinimum()) depth.setMinimum(z);
            if (z > depth.getMaximum()) depth.setMaximum(z);
            depth.setValue(z);
            speed.setValue(clamp(op.speed[type], speed.getMinimum(), speed.getMaximum()));
            mirror.setOn(op.mirror);
            copyAnim.setOn(op.copyAnim);
            labels();
        } finally {
            adjusting = false;
        }
    }

    private void push() {
        if (adjusting || current == null) return;
        current.offX[type] = posX.getValue();
        current.offY[type] = posY.getValue();
        current.scale[type] = Math.max(10, scale.getValue() * 10);
        current.angle[type] = angle.getValue();
        current.zOffset[type] = depth.getValue();
        labels();
        fire();
    }

    private void fire() {
        if (listener != null) listener.onChanged(current);
        list.repaint();
    }

    private void labels() {
        posXv.setText(String.valueOf(posX.getValue()));
        posYv.setText(String.valueOf(posY.getValue()));
        scalev.setText(String.valueOf(scale.getValue()));
        anglev.setText(String.valueOf(angle.getValue()));
        depthv.setText(String.valueOf(depth.getValue()));
        speedv.setText(String.valueOf(speed.getValue()));
    }

    void setSpeedMarks(int[] values, String[] tags, String detail) {
        adjusting = true;
        try {
            speed.setToolTipText(detail == null || detail.trim().isEmpty() ? null : detail);
            int lo = SPEED_MIN, hi = SPEED_MAX;
            for (int i = 0; values != null && i < values.length; i++) {
                if (values[i] < lo) lo = Math.max(5, values[i] - 5);
                if (values[i] > hi) hi = Math.min(SPEED_TYPED, values[i] + 5);
            }
            if (current != null) {
                int cs = current.speed[type];
                if (cs < lo) lo = cs;
                if (cs > hi) hi = cs;
            }
            speed.setMinimum(lo);
            speed.setMaximum(hi);

            Hashtable<Integer, JComponent> t = new Hashtable<Integer, JComponent>();
            t.put(Integer.valueOf(lo), tick(lo + "%", false));
            t.put(Integer.valueOf(hi), tick(hi + "%", false));
            if (lo < 100 && hi > 100) t.put(Integer.valueOf(100), tick("100%", false));

            if (values != null && tags != null) {
                Integer[] order = new Integer[values.length];
                for (int i = 0; i < values.length; i++) order[i] = Integer.valueOf(i);
                java.util.Arrays.sort(order, (a, b) -> values[a.intValue()] - values[b.intValue()]);
                int i = 0;
                while (i < order.length) {
                    int j = i, sum = 0;
                    StringBuilder name = new StringBuilder();
                    while (j < order.length
                            && values[order[j].intValue()] - values[order[i].intValue()] <= 4) {
                        sum += values[order[j].intValue()];
                        name.append(tags[order[j].intValue()]);
                        j++;
                    }
                    int at = sum / (j - i);
                    String label = order(name.toString());
                    if (at >= lo && at <= hi)
                        t.put(Integer.valueOf(at), tick("^" + label + " " + at + "%", true));
                    i = j;
                }
            }
            speed.setLabelTable(t);
            speed.setPaintLabels(true);
        } finally {
            adjusting = false;
        }
        speed.revalidate();
        speed.repaint();
    }

    private static String order(String tags) {
        StringBuilder sb = new StringBuilder();
        String canon = "WIA";
        for (int i = 0; i < canon.length(); i++)
            if (tags.indexOf(canon.charAt(i)) >= 0) sb.append(canon.charAt(i));
        for (int i = 0; i < tags.length(); i++)
            if (canon.indexOf(tags.charAt(i)) < 0) sb.append(tags.charAt(i));
        return sb.toString();
    }

    private static JLabel tick(String text, boolean mark) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.SANS_SERIF, mark ? Font.BOLD : Font.PLAIN, 10));
        l.setForeground(mark ? UCUI.GOLD : UCUI.muted());
        if (mark) l.setToolTipText("Set Speed here to play the copied branch "
                + "at the pace it has in its own unit");
        return l;
    }

    private void bindField(JTextField f, JSlider s, int lo, int hi, boolean recopy) {
        java.awt.event.ActionListener commit = e -> applyField(f, s, lo, hi, recopy);
        f.addActionListener(commit);
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                applyField(f, s, lo, hi, recopy);
            }
        });
    }

    private void applyField(JTextField f, JSlider s, int lo, int hi, boolean recopy) {
        if (adjusting || current == null) { labels(); return; }
        int v;
        try {
            String t = f.getText().replaceAll("[^0-9-]", "");
            if (t.isEmpty() || t.equals("-")) { labels(); return; }
            v = Integer.parseInt(t);
        } catch (Throwable t) {
            labels();
            return;
        }
        v = clamp(v, lo, hi);
        if (v < s.getMinimum()) s.setMinimum(v);
        if (v > s.getMaximum()) s.setMaximum(v);
        if (s.getValue() == v) { labels(); return; }
        s.setValue(v);
        if (recopy && current != null && current.copyAnim && listener != null)
            listener.onRecopy(current);
    }

    private void setEnabledAll(boolean b) {
        posX.setEnabled(b);
        posY.setEnabled(b);
        scale.setEnabled(b);
        angle.setEnabled(b);
        depth.setEnabled(b);
        speed.setEnabled(b);
        posXv.setEnabled(b);
        posYv.setEnabled(b);
        scalev.setEnabled(b);
        anglev.setEnabled(b);
        depthv.setEnabled(b);
        speedv.setEnabled(b);
        mirror.setEnabled(b);
        remove.setEnabled(b);
        copyAnim.setEnabled(true);
        if (!b) copyAnim.setOn(defaultCopyAnim);
    }

    private static JPanel row(String title, String help, JSlider s, JTextField v) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(title);
        l.setPreferredSize(new Dimension(56, 18));
        l.setForeground(UCUI.muted());
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        head.setOpaque(false);
        head.setPreferredSize(new Dimension(82, 20));
        head.add(l);
        head.add(UCUI.help(help));
        s.setOpaque(false);
        p.add(head, BorderLayout.WEST);
        p.add(s, BorderLayout.CENTER);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    private static JTextField value() {
        JTextField f = new JTextField("0");
        f.setPreferredSize(new Dimension(54, 20));
        f.setHorizontalAlignment(SwingConstants.RIGHT);
        f.setFont(f.getFont().deriveFont(Font.PLAIN, 12f));
        f.setToolTipText("Type an exact value and press Enter");
        return f;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
