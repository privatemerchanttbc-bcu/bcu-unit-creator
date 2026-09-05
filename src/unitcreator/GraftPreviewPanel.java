package unitcreator;

import common.system.P;
import common.system.fake.FakeGraphics;
import common.system.fake.FakeImage;
import common.system.fake.ImageBuilder;
import common.util.anim.AnimCE;
import common.util.anim.AnimU;
import common.util.anim.EAnimU;
import unitcreator.geom.PartOverlay;
import unitcreator.geom.MeasuringGraphics;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import common.battle.data.MaskAtk;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.Cursor;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

final class GraftPreviewPanel extends JPanel {

    interface PickListener { void onPick(boolean left, int part); }

    interface CanvasDropListener { void onDrop(int sourcePart, int targetPart); }

    interface TypeListener { void onType(int index); }

    private static final AnimU.UType[] TYPES = {
            AnimU.UType.WALK, AnimU.UType.IDLE, AnimU.UType.ATK, AnimU.UType.HB
    };
    private static final String[] NAMES = {"Walk", "Idle", "Attack", "Hitback"};

    private final View view = new View();
    private final Timer timer;
    private final JSlider zoom = new JSlider(20, 800, 100);
    private final UCButton pause = new UCButton("Pause", UCButton.Kind.NEUTRAL);
    private final UCButton fps = new UCButton("60 fps", UCButton.Kind.NEUTRAL);
    private final UCButton rangeBtn = new UCButton("Range", UCButton.Kind.NEUTRAL);
    private int fpsMode = 60;

    PickListener pickListener;
    CanvasDropListener dropListener;
    TypeListener typeListener;

    GraftPreviewPanel() {
        setLayout(new BorderLayout(4, 4));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 4));
        bar.setOpaque(false);
        final UCButton[] tabs = new UCButton[TYPES.length];
        for (int i = 0; i < TYPES.length; i++) {
            final int idx = i;
            UCButton b = new UCButton(NAMES[i], UCButton.Kind.NEUTRAL);
            b.setOn(i == 1);
            b.addActionListener(e -> {
                for (int j = 0; j < tabs.length; j++) tabs[j].setOn(j == idx);
                view.setType(TYPES[idx]);
                if (typeListener != null) typeListener.onType(idx);
            });
            tabs[i] = b;
            bar.add(b);
        }

        pause.addActionListener(e -> {
            view.paused = !view.paused;
            pause.setOn(view.paused);
            pause.setText(view.paused ? "Play" : "Pause");
        });
        bar.add(pause);
        UCButton back = new UCButton("<", UCButton.Kind.GHOST);
        UCButton fwd = new UCButton(">", UCButton.Kind.GHOST);
        back.addActionListener(e -> view.step(-1f));
        fwd.addActionListener(e -> view.step(1f));
        bar.add(back);
        bar.add(fwd);
        fps.setToolTipText("Preview refresh rate. BC animations are authored at 30 frames per "
                + "second, so 60 fps shows interpolated in-between poses at the same speed. "
                + "Drop to 30 if heavy glow effects make the preview stutter.");
        fps.addActionListener(e -> setFps(fpsMode == 60 ? 30 : 60));
        bar.add(fps);
        rangeBtn.setToolTipText("Show unit 1's impact range - the LD Point Near and Far of "
                + "each attack. The bar runs from the unit root the way the unit faces. The gap "
                + "before the near end is the blind spot the attack cannot reach.");
        rangeBtn.addActionListener(e -> {
            view.showRange = !view.showRange;
            rangeBtn.setText(view.showRange ? "Range" : "Range off");
        });
        bar.add(rangeBtn);

        JLabel zl = new JLabel("  Zoom");
        zl.setForeground(UCUI.muted());
        bar.add(zl);
        zoom.setPreferredSize(new Dimension(140, 20));
        zoom.setOpaque(false);
        zoom.addChangeListener(e -> view.setZoom(zoom.getValue() / 100f));
        bar.add(zoom);
        UCButton reset = new UCButton("Reset", UCButton.Kind.GHOST);
        reset.addActionListener(e -> { view.resetView(); zoom.setValue(100); });
        bar.add(reset);

        add(view, BorderLayout.CENTER);
        buildRangeRow();
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(bar, BorderLayout.NORTH);
        south.add(rangeRow, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        view.onZoomChange = z -> zoom.setValue(Math.round(z * 100));
        view.owner = this;
        timer = new Timer(16, e -> view.tick());
    }

    void setFps(int mode) {
        fpsMode = mode == 30 ? 30 : 60;
        if (fpsMode == 60) {
            timer.setDelay(16);
            view.frameStep = 0.5f;
        } else {
            timer.setDelay(33);
            view.frameStep = 1f;
        }
        fps.setText(fpsMode + " fps");
    }

    void setLeft(AnimCE anim) { view.leftAnim = anim; view.reloadLeft(); }

    interface RangeSource {
        int count();
        int[] get(int i);
        void set(int i, int near, int far);
        void begin();
    }

    private final JPanel rangeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
    private final JComboBox<String> atkPick = new JComboBox<String>();
    private final JTextField nearF = new JTextField(5);
    private final JTextField farF = new JTextField(5);
    private final JLabel rangeNote = new JLabel();
    private boolean rangeSync;

    void setRangeSource(RangeSource src) {
        view.rangeSrc = src;
        buildRangeFields();
    }

    void buildRangeFields() {
        RangeSource src = view.rangeSrc;
        int n = src == null ? 0 : src.count();
        rangeSync = true;
        try {
            atkPick.removeAllItems();
            for (int i = 0; i < n; i++) {
                int[] v = src.get(i);
                boolean has = v != null && v[1] > v[0];
                atkPick.addItem("Attack " + (i + 1) + (has ? "" : "   (no range)"));
            }
        } finally {
            rangeSync = false;
        }
        atkPick.setEnabled(n > 0);
        nearF.setEnabled(n > 0);
        farF.setEnabled(n > 0);
        if (n > 0) {
            atkPick.setSelectedIndex(Math.min(view.rangeSel, n - 1));
            view.rangeSel = atkPick.getSelectedIndex();
        } else {
            view.rangeSel = 0;
        }
        rangeNote.setText(n == 0 ? "this unit has no attacks" : "");
        syncRangeFields();
    }

    private void buildRangeRow() {
        rangeRow.setOpaque(false);
        JLabel head = new JLabel("Impact range");
        head.setForeground(UCUI.muted());
        head.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        rangeRow.add(head);
        rangeRow.add(UCUI.help("The near and far edge of one attack's damage, in game distance. "
                + "Pick the attack here and only that one is drawn on the preview. Drag either "
                + "black handle on the bar, or type exact numbers. This changes the unit you are "
                + "about to save, never the base unit."));
        atkPick.setPreferredSize(new Dimension(150, 22));
        atkPick.addActionListener(e -> {
            if (rangeSync) return;
            view.rangeSel = Math.max(0, atkPick.getSelectedIndex());
            syncRangeFields();
        });
        rangeRow.add(atkPick);
        rangeRow.add(new JLabel("  Near"));
        nearF.setHorizontalAlignment(JTextField.RIGHT);
        rangeRow.add(nearF);
        rangeRow.add(new JLabel("Far"));
        farF.setHorizontalAlignment(JTextField.RIGHT);
        rangeRow.add(farF);
        rangeNote.setForeground(UCUI.muted());
        rangeRow.add(rangeNote);

        java.awt.event.ActionListener go = e -> commitRange();
        nearF.addActionListener(go);
        farF.addActionListener(go);
        java.awt.event.FocusAdapter fa = new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { commitRange(); }
        };
        nearF.addFocusListener(fa);
        farF.addFocusListener(fa);
    }

    private void commitRange() {
        RangeSource src = view.rangeSrc;
        if (rangeSync || src == null || src.count() == 0) return;
        int i = view.rangeSel;
        int[] cur = src.get(i);
        if (cur == null) return;
        int near = parse(nearF, cur[0]);
        int far = parse(farF, cur[1]);
        if (near < 0) near = 0;
        if (far <= near) far = near + 1;
        if (near == cur[0] && far == cur[1]) return;
        src.begin();
        src.set(i, near, far);
        syncRangeFields();
    }

    private static int parse(JTextField t, int fallback) {
        try { return Integer.parseInt(t.getText().trim()); } catch (Throwable x) { return fallback; }
    }

    void syncRangeFields() {
        RangeSource src = view.rangeSrc;
        if (src == null || src.count() == 0) { nearF.setText(""); farF.setText(""); return; }
        int[] v = src.get(view.rangeSel);
        if (v == null) return;
        rangeSync = true;
        try {
            if (!nearF.hasFocus()) nearF.setText(String.valueOf(v[0]));
            if (!farF.hasFocus()) farF.setText(String.valueOf(v[1]));
        } finally {
            rangeSync = false;
        }
    }

    void setRight(Donor d) { view.donor = d; view.reloadRight(); }

    void reloadLeft() { view.reloadLeft(); }

    void refreshLeftEntity() { view.refreshLeftEntity(); }

    void reloadRight() { view.reloadRight(); }

    void setLeftHighlight(boolean[] sub, int root) { view.left.hi = sub; view.left.hiRoot = root; }

    void setRightHighlight(boolean[] sub, int root) { view.right.hi = sub; view.right.hiRoot = root; }

    void start() { timer.start(); }

    void stop() { timer.stop(); }

    private static final class Side {
        EAnimU anim;
        float frame;
        float bxMin, bxMax, byMin, byMax;
        boolean haveBox;
        float ox, oy;
        boolean[] hi;
        int hiRoot = -1;
    }

    private static final class View extends JPanel {
        private float frameStep = 0.5f;
        RangeSource rangeSrc;
        boolean showRange = true;
        private int[] marks;
        int rangeSel;
        private int[] rangeGrab;
        private static final int MAX_HOVER = 48;
        private static final Color INK = new Color(16, 16, 16);
        private static final Color FILL = new Color(0, 0, 0, 52);
        private static final int HANDLE_DY = 17;
        private static final Color NEAR_C = new Color(10, 10, 10);
        private static final Color FAR_C = new Color(120, 120, 120);
        private static final long SLOW_TICK_MS = 120;

        java.util.function.Consumer<Float> onZoomChange;
        GraftPreviewPanel owner;

        AnimU.UType type = AnimU.UType.IDLE;
        AnimCE leftAnim;
        Donor donor;
        final Side left = new Side();
        final Side right = new Side();

        boolean paused;
        private FakeImage canvas;
        private BufferedImage bimg;
        private BufferedImage bg;
        private int bgw, bgh;
        private boolean drawFailed;
        private boolean loadFailed;
        private long slowLogged;
        private int cw, ch;
        private float zoom = 1f, panX = 0, panY = 0;
        private Point drag;

        View() {
            setBackground(UCUI.inset());
            setBorder(BorderFactory.createLineBorder(UCUI.line()));
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    rangeGrab = hitMark(e.getX(), e.getY());
                    if (rangeGrab != null) {
                        if (rangeSrc != null) rangeSrc.begin();
                        return;
                    }
                    drag = e.getPoint();
                    pickAt(e.getX(), e.getY());
                }
                @Override public void mouseReleased(MouseEvent e) { drag = null; rangeGrab = null; }
                @Override public void mouseMoved(MouseEvent e) {
                    setCursor(hitMark(e.getX(), e.getY()) != null
                            ? Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
                            : Cursor.getDefaultCursor());
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (rangeGrab != null) { dragMark(e.getX()); return; }
                    if (drag == null) return;
                    panX += e.getX() - drag.x;
                    panY += e.getY() - drag.y;
                    drag = e.getPoint();
                    repaint();
                }
                @Override public void mouseWheelMoved(MouseWheelEvent e) {
                    float f = e.getWheelRotation() < 0 ? 1.15f : 1f / 1.15f;
                    zoom = Math.max(0.2f, Math.min(8f, zoom * f));
                    if (onZoomChange != null) onZoomChange.accept(Float.valueOf(zoom));
                    repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(ma);
            setTransferHandler(new TransferHandler() {
                @Override public boolean canImport(TransferSupport s) {
                    return s.isDrop() && s.isDataFlavorSupported(PartTree.FLAVOR);
                }
                @Override public boolean importData(TransferSupport s) {
                    if (!canImport(s)) return false;
                    try {
                        Object o = s.getTransferable().getTransferData(PartTree.FLAVOR);
                        if (!(o instanceof PartTree.PartRef)) return false;
                        Point pt = s.getDropLocation().getDropPoint();
                        int target = pickSide(left, pt.x, pt.y);
                        if (target < 0 || owner == null || owner.dropListener == null) return false;
                        owner.dropListener.onDrop(((PartTree.PartRef) o).part, target);
                        return true;
                    } catch (Throwable t) {
                        return false;
                    }
                }
            });
        }

        void setType(AnimU.UType t) {
            type = t;
            reloadLeft();
            reloadRight();
        }

        void setZoom(float z) { zoom = z; repaint(); }

        void resetView() { panX = panY = 0; zoom = 1f; repaint(); }

        void step(float d) {
            paused = true;
            if (owner != null) {
                owner.pause.setOn(true);
                owner.pause.setText("Play");
            }
            advance(left, d);
            advance(right, d);
            repaint();
        }

        void reloadLeft() {
            load(left, leftAnim == null ? null : safeEAnim(leftAnim), true);
        }

        void refreshLeftEntity() {
            load(left, leftAnim == null ? null : safeEAnim(leftAnim), false);
        }

        void reloadRight() {
            load(right, donor == null ? null : donor.eanim(type), true);
        }

        private EAnimU safeEAnim(AnimCE a) {
            try { return a.getEAnim(type); } catch (Throwable t) { return null; }
        }

        private void load(Side s, EAnimU a, boolean remeasure) {
            float keep = s.frame;
            s.anim = null;
            if (remeasure) {
                s.haveBox = false;
                s.frame = 0f;
            }
            if (a == null) return;
            try {
                a.sele = -1;
                a.update(false);
                if (remeasure) {
                    MeasuringGraphics mg = new MeasuringGraphics();
                    a.draw(mg, new P(0f, 0f), 1f);
                    if (mg.hasBox()) {
                        s.bxMin = mg.minX(); s.bxMax = mg.maxX();
                        s.byMin = mg.minY(); s.byMax = mg.maxY();
                        s.haveBox = true;
                    }
                } else {
                    s.frame = keep;
                    try { a.setTime(keep); } catch (Throwable ignored) {}
                }
                s.anim = a;
            } catch (Throwable t) {
                s.anim = null;
                if (!loadFailed) {
                    loadFailed = true;
                    Logger.err("UnitCreator: preview entity build failed", t);
                }
            }
        }

        private void ensureCanvas() {
            int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
            if (canvas != null && bimg != null && w == cw && h == ch) return;
            try {
                if (ImageBuilder.builder == null) return;
                canvas = ImageBuilder.builder.build(w, h);
                Object bi = canvas.bimg();
                bimg = bi instanceof BufferedImage ? (BufferedImage) bi : null;
                cw = w; ch = h;
            } catch (Throwable ignored) {
                canvas = null; bimg = null;
            }
        }

        void tick() {
            if (!onScreen()) return;
            ensureCanvas();
            if (canvas == null || bimg == null) { repaint(); return; }
            long t0 = System.nanoTime();
            try {
                ensureBg(cw, ch);
                Graphics2D clr = bimg.createGraphics();
                try {
                    if (bg != null) {
                        clr.setComposite(AlphaComposite.Src);
                        clr.drawImage(bg, 0, 0, null);
                    } else {
                        clr.setComposite(AlphaComposite.Src);
                        clr.setColor(UCUI.inset());
                        clr.fillRect(0, 0, cw, ch);
                    }
                } finally {
                    clr.dispose();
                }

                if (!paused) {
                    advance(left, frameStep);
                    advance(right, frameStep);
                }
                float siz = fitScale() * zoom;
                layout(siz);
                FakeGraphics fg = new SafeFG(canvas.getGraphics());
                paintSide(fg, left, siz);
                paintSide(fg, right, siz);
                drawRange(siz);
            } catch (Throwable ignored) {
            } finally {
                repaint();
            }
            reportSlow((System.nanoTime() - t0) / 1000000L);
        }

        private void reportSlow(long ms) {
            if (ms < SLOW_TICK_MS) return;
            long now = System.currentTimeMillis();
            if (now - slowLogged < 3000) return;
            slowLogged = now;
            Logger.log("UnitCreator: slow preview tick " + ms + "ms type=" + type
                    + " leftParts=" + parts(left) + " rightParts=" + parts(right)
                    + " leftHover=" + hover(left) + " rightHover=" + hover(right));
        }

        private static int parts(Side s) {
            return s.anim == null || s.anim.ent == null ? 0 : s.anim.ent.length;
        }

        private static int hover(Side s) {
            if (s.hi == null) return 0;
            int c = 0;
            for (int i = 0; i < s.hi.length; i++) if (s.hi[i]) c++;
            return c;
        }

        private boolean onScreen() {
            try {
                Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
                if (w == null || !w.isShowing()) return false;
                if (w instanceof Frame
                        && (((Frame) w).getExtendedState() & Frame.ICONIFIED) != 0) return false;
                return true;
            } catch (Throwable t) {
                return true;
            }
        }

private void drawRange(float siz) {
            if (!showRange || rangeSrc == null || left.anim == null || bimg == null) return;
            Graphics2D g2 = bimg.createGraphics();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                float ppu = 0.32f * siz;
                float y = left.oy;
                float x0 = left.ox;
                marks = null;
                int n = rangeSrc.count();
                if (rangeSel < 0 || rangeSel >= n) return;

                int[] v = rangeSrc.get(rangeSel);
                if (v == null || v[1] <= v[0]) {
                    g2.setColor(INK);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                    g2.drawString("attack " + (rangeSel + 1) + " has no impact range",
                            Math.round(x0) - 170, Math.round(y) + 14);
                    return;
                }
                float ly = y + 12;
                band(g2, x0, ly, v[0] * ppu, v[1] * ppu,
                        "atk " + (rangeSel + 1) + "   near " + v[0] + "   far " + v[1]);
                marks = new int[]{Math.round(x0 - v[0] * ppu),
                        Math.round(x0 - v[1] * ppu), Math.round(ly)};
            } catch (Throwable ignored) {
            } finally {
                g2.dispose();
            }
        }

        private void band(Graphics2D g2, float x0, float y, float near, float far,
                          String label) {
            int a = Math.round(x0 - far), b = Math.round(x0 - near);
            int yy = Math.round(y);
            if (b <= a) return;
            g2.setColor(FILL);
            g2.fillRect(a, yy - 4, b - a, 9);
            g2.setColor(INK);
            g2.drawLine(a, yy, b, yy);
            g2.drawLine(a, yy - 5, a, yy + 5);
            g2.drawLine(b, yy - 5, b, yy + 5);
            int hy = yy + HANDLE_DY;
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(FAR_C);
            g2.drawLine(a, yy + 5, a, hy - 7);
            g2.drawRect(a - 4, hy - 7, 9, 15);
            g2.setColor(NEAR_C);
            g2.drawLine(b, yy + 5, b, hy - 7);
            g2.drawRect(b - 4, hy - 7, 9, 15);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(INK);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            int tw = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, a - tw - 9, yy + 4);
        }

        private int[] hitMark(int mx, int my) {
            if (!showRange || marks == null) return null;
            if (Math.abs(my - (marks[2] + HANDLE_DY)) > 10) return null;
            if (Math.abs(mx - marks[0]) <= 6) return new int[]{rangeSel, 0};
            if (Math.abs(mx - marks[1]) <= 6) return new int[]{rangeSel, 1};
            return null;
        }

        private void dragMark(int mx) {
            if (rangeGrab == null || rangeSrc == null) return;
            float ppu = 0.32f * fitScale() * zoom;
            if (ppu <= 0.0001f) return;
            int world = Math.max(0, Math.round((left.ox - mx) / ppu));
            int[] v = rangeSrc.get(rangeGrab[0]);
            if (v == null) return;
            int near = v[0], far = v[1];
            if (rangeGrab[1] == 0) near = Math.min(world, far - 1);
            else far = Math.max(world, near + 1);
            rangeSrc.set(rangeGrab[0], Math.max(0, near), Math.max(1, far));
            if (owner != null) owner.syncRangeFields();
        }

        private void advance(Side s, float d) {
            if (s.anim == null) return;
            int len = 0;
            try { len = s.anim.len(); } catch (Throwable ignored) {}
            float nf = s.frame + d;
            if (len > 0) {
                nf = nf % len;
                if (nf < 0) nf += len;
            } else if (nf < 0) {
                nf = 0;
            }
            s.frame = nf;
            try { s.anim.setTime(s.frame); } catch (Throwable ignored) {}
        }

        private float fitScale() {
            float bw = Math.max(boxW(left), boxW(right));
            float bh = Math.max(boxH(left), boxH(right));
            if (bw <= 1f && bh <= 1f) return 1f;
            float s = Math.min((cw * 0.40f) / Math.max(1f, bw), (ch * 0.72f) / Math.max(1f, bh));
            return Math.max(0.05f, Math.min(8f, s));
        }

        private static float boxW(Side s) { return s.haveBox ? Math.max(1f, s.bxMax - s.bxMin) : 1f; }

        private static float boxH(Side s) { return s.haveBox ? Math.max(1f, s.byMax - s.byMin) : 1f; }

        private void layout(float siz) {
            float gap = (boxW(left) + boxW(right)) * siz / 2f + 48f;
            float mx = cw / 2f + panX, my = ch / 2f + panY;
            left.ox = mx - gap / 2f - siz * cx(left);
            left.oy = my - siz * cy(left);
            right.ox = mx + gap / 2f - siz * cx(right);
            right.oy = my - siz * cy(right);
        }

        private static float cx(Side s) { return s.haveBox ? (s.bxMin + s.bxMax) / 2f : 0f; }

        private static float cy(Side s) { return s.haveBox ? (s.byMin + s.byMax) / 2f : 0f; }

        private void paintSide(FakeGraphics g, Side s, float siz) {
            if (s.anim == null) return;
            s.anim.sele = -1;
            try {
                s.anim.draw(g, new P(s.ox, s.oy), siz);
            } catch (Throwable t) {
                if (!drawFailed) {
                    drawFailed = true;
                    Logger.err("UnitCreator: preview draw failed", t);
                }
            }
            if (s.hi == null || s.hiRoot < 0) return;
            g.translate(s.ox, s.oy);
            try {
                int drawn = 0;
                for (int i = 0; i < s.hi.length && drawn < MAX_HOVER; i++) {
                    if (!s.hi[i] || i == s.hiRoot) continue;
                    PartOverlay.drawHover(g, s.anim, i, siz);
                    drawn++;
                }
                PartOverlay.draw(g, s.anim, s.hiRoot, siz, false, false);
            } catch (Throwable ignored) {
            } finally {
                g.translate(-s.ox, -s.oy);
            }
        }

        private void pickAt(int mx, int my) {
            if (owner == null || owner.pickListener == null) return;
            int p = pickSide(left, mx, my);
            if (p >= 0) { owner.pickListener.onPick(true, p); return; }
            p = pickSide(right, mx, my);
            if (p >= 0) owner.pickListener.onPick(false, p);
        }

        private int pickSide(Side s, int mx, int my) {
            if (s.anim == null) return -1;
            try {
                float siz = fitScale() * zoom;
                return unitcreator.geom.AnimGeometry.pick(s.anim, mx - s.ox, my - s.oy, siz);
            } catch (Throwable t) {
                return -1;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bimg != null) {
                g.drawImage(bimg, 0, 0, null);
                return;
            }
            int w = getWidth(), h = getHeight();
            ensureBg(w, h);
            if (bg != null) g.drawImage(bg, 0, 0, null);
        }

        private void ensureBg(int w, int h) {
            if (w <= 0 || h <= 0) return;
            if (bg != null && bgw == w && bgh == h) return;
            BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = b.createGraphics();
            try {
                Color edge = UCUI.inset();
                Color center = mix(edge, UCUI.TEAL, 0.22f);
                g2.setPaint(new java.awt.RadialGradientPaint(
                        new java.awt.geom.Point2D.Float(w * 0.5f, h * 0.32f),
                        Math.max(1f, Math.max(w, h) * 0.8f),
                        new float[]{0f, 1f}, new Color[]{center, edge}));
                g2.fillRect(0, 0, w, h);
                g2.setColor(UCUI.line());
                for (int x = w / 2 % 32; x < w; x += 32) g2.drawLine(x, 0, x, h);
                for (int y = h / 2 % 32; y < h; y += 32) g2.drawLine(0, y, w, y);
            } finally {
                g2.dispose();
            }
            bg = b;
            bgw = w;
            bgh = h;
        }

        private static Color mix(Color a, Color b, float t) {
            return new Color(
                    Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                    Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
        }
    }
}
