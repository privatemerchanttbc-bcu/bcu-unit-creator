package unitcreator;

import common.util.anim.AnimCE;
import common.util.anim.AnimU;
import common.util.unit.Form;
import common.util.unit.Unit;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.FlowLayout;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public final class UnitGraftDialog {

    private static final int UNDO_DEPTH = 10;

    private UnitGraftDialog() {}

    private static java.lang.ref.WeakReference<JFrame> open;

    public static void show(Object mainPage) {
        try {
            JFrame live = open == null ? null : open.get();
            if (live != null && live.isDisplayable()) {
                live.setExtendedState(live.getExtendedState() & ~JFrame.ICONIFIED);
                live.toFront();
                live.requestFocus();
                Logger.log("UnitCreator: window already open, brought to front");
                return;
            }
            GraftSession.cleanupStale();
            Component parent = mainPage instanceof Component ? (Component) mainPage : null;
            UI ui = new UI(parent);
            open = new java.lang.ref.WeakReference<JFrame>(ui.frame);
            ui.frame.setVisible(true);
        } catch (Throwable t) {
            Logger.err("UnitCreator: failed to open graft dialog", t);
        }
    }

    private static final class UI {

        final JFrame frame = new JFrame("Unit Creator - Part Graft");
        final GraftSession session = new GraftSession();
        final GraftPreviewPanel preview = new GraftPreviewPanel();
        final GraftAdjustPanel adjust = new GraftAdjustPanel();
        final Slot slotA = new Slot("Unit 1  (base - keeps its own parts)");
        final Slot slotB = new Slot("Unit 2  (parts source)");
        final JLabel title = new JLabel("Pick a base unit", SwingConstants.CENTER);
        final UCButton save = new UCButton("Save as new Unit", UCButton.Kind.PRIMARY, UCButton.Glyph.CHECK);
        final UCButton update = new UCButton("Update stats", UCButton.Kind.NEUTRAL);
        final UCButton undoBtn = new UCButton("Undo", UCButton.Kind.NEUTRAL);
        Donor donor;

        UI(Component parent) {
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setSize(1500, 900);
            frame.setLocationRelativeTo(parent);

            slotA.tree.enableDropTarget();
            slotB.tree.enableDragSource();

            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
            slotA.setPreferredSize(new Dimension(300, 0));
            slotB.setPreferredSize(new Dimension(300, 0));
            row.add(slotA, BorderLayout.WEST);
            row.add(slotB, BorderLayout.EAST);

            JPanel mid = new JPanel(new BorderLayout(0, 6));
            title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
            title.setForeground(UCUI.GOLD);
            title.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
            mid.add(title, BorderLayout.NORTH);
            mid.add(preview, BorderLayout.CENTER);
            row.add(mid, BorderLayout.CENTER);
            frame.add(row, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout(0, 6));
            bottom.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
            bottom.add(adjust, BorderLayout.CENTER);

            JPanel bar = new JPanel(new BorderLayout(12, 0));
            JLabel hint = new JLabel("Pick a base unit on the left and a source on the right, then drag a part "
                    + "from the right list onto a part of the left list. The whole branch comes with it.");
            hint.setForeground(UCUI.muted());
            bar.add(hint, BorderLayout.CENTER);
            save.setPreferredSize(new Dimension(220, 34));
            save.setEnabled(false);
            JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            acts.setOpaque(false);
            undoBtn.setPreferredSize(new Dimension(110, 34));
            undoBtn.setEnabled(false);
            undoBtn.setToolTipText("Undo the last change, up to " + UNDO_DEPTH
                    + " steps back. Keyboard: Ctrl+Z.");
            undoBtn.addActionListener(e -> doUndo());
            acts.add(undoBtn);
            acts.add(update);
            acts.add(save);
            bar.add(acts, BorderLayout.EAST);
            bottom.add(bar, BorderLayout.SOUTH);
            frame.add(bottom, BorderLayout.SOUTH);

            slotA.button.addActionListener(e -> pickBase());
            slotB.button.addActionListener(e -> pickDonor());
            save.addActionListener(e -> doSave());
            update.setPreferredSize(new Dimension(150, 34));
            update.setToolTipText("Rebuild the stats of a unit you already saved, from the units "
                    + "it was made of.");
            update.addActionListener(e -> UpdateStatsDialog.show(frame));

            KeyStroke ctrlZ = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK);
            frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(ctrlZ, "ucUndo");
            frame.getRootPane().getActionMap().put("ucUndo", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { doUndo(); }
            });

            slotA.tree.selectListener = (t, p) -> { bindToPart(p); updateHighlights(); };
            slotB.tree.selectListener = (t, p) -> updateHighlights();
            slotA.tree.dropListener = (src, sp, tp) -> doGraft(sp, tp);

            preview.pickListener = (left, part) -> {
                if (left) {
                    slotA.tree.select(part);
                    bindToPart(part);
                } else {
                    slotB.tree.select(part);
                }
                updateHighlights();
            };
            preview.dropListener = (sp, tp) -> doGraft(sp, tp);
            preview.typeListener = i -> adjust.setType(i);

            adjust.listener = new GraftAdjustPanel.Listener() {
                @Override public void onChanged(GraftOp op) {
                    mark();
                    session.nudge(op);
                    preview.refreshLeftEntity();
                }
                @Override public void onRemove(GraftOp op) {
                    mark();
                    session.remove(op);
                    afterStructureChange(null);
                }
                @Override public void onSelect(GraftOp op) {
                    if (op != null && op.applied()) slotA.tree.select(op.rootPart);
                    pushSpeedMarks(op);
                    updateHighlights();
                }
                @Override public void onMirror(GraftOp op) {
                    mark();
                    session.setMirror(op);
                    preview.refreshLeftEntity();
                }
                @Override public void onRecopy(GraftOp op) {
                    mark();
                    session.rebuild();
                    afterStructureChange(op);
                }
            };

            preview.start();
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { close(); }
            });
        }

        void pickBase() {
            Form f = UnitPickerDialog.pickForm(frame, "Choose Unit 1 (base)");
            if (f == null) return;
            if (session.dirty()) {
                int ans = JOptionPane.showConfirmDialog(frame,
                        "Changing the base unit discards the parts you already attached. Continue?",
                        "Unit Creator", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ans != JOptionPane.OK_OPTION) return;
            }
            String err = session.setBase(f);
            if (err != null) {
                error("Cannot use this unit as a base: " + err);
                save.setEnabled(false);
                return;
            }
            slotA.set(UnitPickerDialog.iconOf(f), UnitPickerDialog.label(f));
            preview.setRangeSource(new GraftPreviewPanel.RangeSource() {
                @Override public int count() { return session.ldCount(); }
                @Override public int[] get(int i) { return session.ldOf(i); }
                @Override public void set(int i, int near, int far) {
                    session.setLd(i, near, far);
                }
                @Override public void begin() { mark(); }
            });
            title.setText(UnitCombiner.displayName(f));
            save.setEnabled(true);
            afterStructureChange(null);
        }

        void pickDonor() {
            Donor d = UnitPickerDialog.pickDonor(frame, "Choose Unit 2 (parts source)");
            if (d == null) return;
            AnimU<?> a = d.anim();
            if (a == null) {
                error("That entry has no usable animation.");
                return;
            }
            try { a.check(); } catch (Throwable ignored) {}
            if (a.mamodel == null || a.imgcut == null || a.getNum() == null) {
                error("That entry's animation failed to load.");
                return;
            }
            donor = d;
            slotB.set(d.icon(), d.label());
            slotB.tree.setSource(a);
            preview.setRight(d);
            updateHighlights();
        }

        void doGraft(int bRoot, int aParent) {
            if (!session.ready()) { error("Pick a unit for slot 1 first."); return; }
            if (donor == null) { error("Pick a unit for slot 2 first."); return; }
            GraftOp op = new GraftOp(donor, bRoot, aParent);
            op.copyAnim = GraftAdjustPanel.defaultCopyAnim();
            op.donorPartName = slotB.tree.partName(bRoot);
            op.targetPartName = slotA.tree.partName(aParent);
            mark();
            String note = session.add(op);
            afterStructureChange(op.applied() ? op : null);
            if (note != null) {
                if (op.applied()) JOptionPane.showMessageDialog(frame, note,
                        "Unit Creator", JOptionPane.WARNING_MESSAGE);
                else error("Cannot attach that part: " + note);
            }
        }

private final java.util.ArrayDeque<GraftSession.Memento> undo =
                new java.util.ArrayDeque<GraftSession.Memento>();

        void mark() {
            if (!session.ready()) return;
            undo.addLast(session.snapshot());
            while (undo.size() > UNDO_DEPTH) undo.removeFirst();
            refreshUndoLabel();
        }

        void doUndo() {
            if (undo.isEmpty()) {
                error("Nothing to undo.");
                return;
            }
            session.restore(undo.removeLast());
            afterStructureChange(null);
            preview.buildRangeFields();
            preview.refreshLeftEntity();
            refreshUndoLabel();
        }

        void refreshUndoLabel() {
            undoBtn.setEnabled(!undo.isEmpty());
            undoBtn.setText(undo.isEmpty() ? "Undo" : "Undo (" + undo.size() + ")");
        }

        void afterStructureChange(GraftOp select) {
            AnimCE w = session.work();
            if (w == null) {
                slotA.tree.setModel(null);
                preview.setLeft(null);
                adjust.setOps(null, null);
                return;
            }
            slotA.tree.setSource(w);
            preview.setLeft(w);
            adjust.setZRange(AutoFit.zSpan(w.mamodel));
            adjust.setOps(session.ops(), select);
            pushSpeedMarks(adjust.current());
            updateHighlights();
        }

        void pushSpeedMarks(GraftOp op) {
            AnimU<?> b = null, d = null;
            if (op != null && op.donor != null && session.base() != null) {
                try { b = (AnimU<?>) session.base().anim; } catch (Throwable ignored) {}
                d = op.donor.anim();
            }
            if (b == null || d == null) {
                adjust.setSpeedMarks(null, null, null);
                return;
            }
            AnimU.UType[] ts = {AnimU.UType.WALK, AnimU.UType.IDLE, AnimU.UType.ATK};
            String[] ns = {"W", "I", "A"};
            String[] full = {"Walk", "Idle", "Attack"};
            int[] vals = new int[ts.length];
            String[] tags = new String[ts.length];
            StringBuilder detail = new StringBuilder("<html>");
            int n = 0;
            for (int i = 0; i < ts.length; i++) {
                int bl = SubtreeCopier.animLen(b, ts[i]);
                int dl = SubtreeCopier.animLen(d, ts[i]);
                if (bl <= 0 || dl <= 0) continue;
                vals[n] = (int) Math.round(100.0 * bl / dl);
                tags[n] = ns[i];
                n++;
                detail.append(full[i]).append(": base ").append(bl)
                      .append(" frames, source ").append(dl).append(" frames<br>");
            }
            detail.append("</html>");
            if (n == 0) {
                adjust.setSpeedMarks(null, null, null);
                return;
            }
            int[] v = new int[n];
            String[] g = new String[n];
            System.arraycopy(vals, 0, v, 0, n);
            System.arraycopy(tags, 0, g, 0, n);
            adjust.setSpeedMarks(v, g, detail.toString());
        }

        void bindToPart(int part) {
            GraftOp owner = ownerOf(part);
            if (owner == null || owner == adjust.current()) return;
            adjust.select(owner);
            pushSpeedMarks(owner);
        }

        GraftOp ownerOf(int part) {
            AnimCE w = session.work();
            if (w == null || w.mamodel == null || w.mamodel.parts == null) return null;
            int[][] rows = w.mamodel.parts;
            int at = part, guard = 0;
            while (at >= 0 && at < rows.length && guard++ <= rows.length) {
                for (int i = 0; i < session.ops().size(); i++) {
                    GraftOp op = session.ops().get(i);
                    if (!op.applied()) continue;
                    if (at == op.mirrorPart) return op;
                    if (at >= op.partBase && at < op.partBase + op.partCount) return op;
                }
                int[] r = rows[at];
                int par = r != null && r.length > 0 ? r[0] : -1;
                if (par < 0 || par == at) break;
                at = par;
            }
            return null;
        }

        void updateHighlights() {
            int a = slotA.tree.selected();
            preview.setLeftHighlight(a >= 0 ? slotA.tree.subtree(a) : null, a);
            int b = slotB.tree.selected();
            preview.setRightHighlight(b >= 0 ? slotB.tree.subtree(b) : null, b);
        }

        private boolean mergeStats = true;

        void doSave() {
            if (!session.ready()) return;
            if (session.ops().isEmpty()) {
                int ans = JOptionPane.showConfirmDialog(frame,
                        "No parts attached yet. Save a plain copy of unit 1 anyway?",
                        "Unit Creator", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (ans != JOptionPane.OK_OPTION) return;
            }
            SaveDialog.Result r = SaveDialog.show(frame, session.base(), session.donors(), mergeStats);
            if (r == null) return;
            mergeStats = r.mergeStats;
            save.setEnabled(false);
            try {
                Unit u = session.save(r.mergeStats);
                String name = UnitCombiner.displayName(u.forms[0]);
                JOptionPane.showMessageDialog(frame,
                        "Saved \"" + name + "\" to the \"UnitCreator\" pack.\n"
                                + "Its animation is now in the Sprite Editor list.\n"
                                + (r.mergeStats
                                        ? "It carries the combined stats of the units you used.\n"
                                        : "It keeps unit 1's own stats.\n")
                                + "You can keep attaching parts and save again.",
                        "Unit Creator", JOptionPane.INFORMATION_MESSAGE);
            } catch (Throwable t) {
                Logger.err("UnitCreator: save failed", t);
                error("Save failed: " + UCUI.describe(t));
            } finally {
                save.setEnabled(session.ready());
            }
        }

        void close() {
            if (session.dirty()) {
                int ans = JOptionPane.showConfirmDialog(frame,
                        "You have attached parts that are not saved. Close and lose them?",
                        "Unit Creator", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ans != JOptionPane.OK_OPTION) return;
            }
            preview.stop();
            session.dispose();
            open = null;
            frame.dispose();
        }

        void error(String msg) {
            JOptionPane.showMessageDialog(frame, msg, "Unit Creator", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class Slot extends JPanel {
        final IconBox box = new IconBox();
        final UCButton button = new UCButton("Choose Unit", UCButton.Kind.NEUTRAL);
        final PartTree tree = new PartTree();

        Slot(String title) {
            setLayout(new BorderLayout(0, 6));
            setBorder(UCUI.card(title));
            JPanel head = new JPanel(new BorderLayout(0, 6));
            head.setOpaque(false);
            head.add(box, BorderLayout.CENTER);
            head.add(button, BorderLayout.SOUTH);
            add(head, BorderLayout.NORTH);
            add(tree, BorderLayout.CENTER);
        }

        void set(BufferedImage img, String caption) {
            box.set(img, caption);
        }
    }

    private static final class IconBox extends JPanel {
        private BufferedImage img;
        private String caption = "";

        IconBox() { setPreferredSize(new Dimension(240, 170)); }

        void set(BufferedImage i, String c) {
            img = i;
            caption = c == null ? "" : c;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth(), h = getHeight(), area = h - 22;
            g.setColor(UCUI.inset());
            g.fillRect(0, 0, w, area);
            if (img != null) {
                double s = Math.min((w - 20.0) / img.getWidth(), (area - 16.0) / img.getHeight());
                s = Math.min(s, 2.0);
                int dw = (int) (img.getWidth() * s), dh = (int) (img.getHeight() * s);
                g.drawImage(img, (w - dw) / 2, (area - dh) / 2, dw, dh, null);
            } else {
                g.setColor(UCUI.muted());
                g.drawString("(empty)", w / 2 - 24, area / 2);
            }
            g.setColor(UCUI.muted());
            String c = caption.length() > 38 ? caption.substring(0, 38) + "..." : caption;
            g.drawString(c, 6, h - 6);
        }
    }
}
