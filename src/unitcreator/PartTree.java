package unitcreator;

import common.util.anim.AnimU;
import common.util.anim.MaModel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;

final class PartTree extends JPanel {

    static final DataFlavor FLAVOR = new DataFlavor(PartRef.class, "BCU animation part");

    static final class PartRef {
        final PartTree tree;
        final int part;
        PartRef(PartTree tree, int part) { this.tree = tree; this.part = part; }
    }

    interface SelectListener { void onSelect(PartTree tree, int part); }

    interface DropListener { void onDrop(PartTree source, int sourcePart, int targetPart); }

    private static final List<PartTree> LIVE = new ArrayList<PartTree>();
    private static boolean english = true;

    static boolean english() { return english; }

    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Body");
    private final DefaultTreeModel model = new DefaultTreeModel(root);
    private final JTree tree;
    private final UCButton lang = new UCButton("EN", UCButton.Kind.NEUTRAL);
    private DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[0];

    private MaModel mamodel;
    private boolean[] fx = new boolean[0];
    private boolean adjusting;

    SelectListener selectListener;
    DropListener dropListener;

    PartTree() {
        setLayout(new BorderLayout());
        tree = new JTree(model) {
            @Override
            public String convertValueToText(Object value, boolean sel, boolean exp,
                                             boolean leaf, int row, boolean focus) {
                Object o = value instanceof DefaultMutableTreeNode
                        ? ((DefaultMutableTreeNode) value).getUserObject() : value;
                if (o instanceof Integer) return labelOf(((Integer) o).intValue());
                return String.valueOf(o);
            }
        };
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(e -> {
            if (adjusting || selectListener == null) return;
            selectListener.onSelect(PartTree.this, selected());
        });
        JScrollPane sp = new JScrollPane(tree);
        sp.setBorder(null);
        add(sp, BorderLayout.CENTER);

        lang.setOn(english);
        lang.setToolTipText("Switch between the original part names and an English reading");
        lang.addActionListener(e -> setEnglishAll(!english));
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        head.setOpaque(false);
        head.add(lang);
        add(head, BorderLayout.NORTH);
        LIVE.add(this);
    }

    @Override
    public void removeNotify() {
        LIVE.remove(this);
        super.removeNotify();
    }

    private static void setEnglishAll(boolean b) {
        english = b;
        for (int i = 0; i < LIVE.size(); i++) {
            PartTree t = LIVE.get(i);
            t.lang.setOn(b);
            t.lang.setText(b ? "EN" : "JP");
            t.rebuild();
        }
    }

    void setSource(AnimU<?> anim) {
        mamodel = anim == null ? null : anim.mamodel;
        fx = new boolean[0];
        try { if (anim != null) fx = EffectParts.markEffects(anim); } catch (Throwable ignored) {}
        rebuild();
    }

    void setModel(MaModel m) {
        mamodel = m;
        fx = new boolean[0];
        rebuild();
    }

    MaModel mamodel() {
        return mamodel;
    }

    int count() {
        if (mamodel == null || mamodel.parts == null) return 0;
        return Math.min(mamodel.n, mamodel.parts.length);
    }

    int selected() {
        TreePath p = tree.getSelectionPath();
        if (p == null) return -1;
        Object o = p.getLastPathComponent();
        if (!(o instanceof DefaultMutableTreeNode)) return -1;
        Object u = ((DefaultMutableTreeNode) o).getUserObject();
        return u instanceof Integer ? ((Integer) u).intValue() : -1;
    }

    void select(int part) {
        if (part < 0 || part >= nodes.length || nodes[part] == null) return;
        adjusting = true;
        try {
            TreePath path = new TreePath(model.getPathToRoot(nodes[part]));
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        } finally {
            adjusting = false;
        }
    }

    boolean[] subtree(int part) {
        int n = count();
        boolean[] keep = new boolean[n];
        if (part < 0 || part >= n) return keep;
        keep[part] = true;
        try { mamodel.getChild(keep); } catch (Throwable ignored) {}
        return keep;
    }

    String partName(int i) {
        if (mamodel == null || mamodel.strs0 == null) return "part " + i;
        if (i < 0 || i >= mamodel.strs0.length || mamodel.strs0[i] == null
                || mamodel.strs0[i].trim().isEmpty()) return "part " + i;
        return mamodel.strs0[i].trim();
    }

    void enableDragSource() {
        tree.setDragEnabled(true);
        tree.setTransferHandler(new TransferHandler() {
            @Override public int getSourceActions(JComponent c) { return COPY; }
            @Override protected Transferable createTransferable(JComponent c) {
                int p = selected();
                return p < 0 ? null : new Ref(new PartRef(PartTree.this, p));
            }
        });
    }

    void enableDropTarget() {
        tree.setTransferHandler(new TransferHandler() {
            @Override public boolean canImport(TransferSupport s) {
                return s.isDrop() && s.isDataFlavorSupported(FLAVOR) && targetOf(s) >= 0;
            }
            @Override public boolean importData(TransferSupport s) {
                if (!canImport(s)) return false;
                int target = targetOf(s);
                try {
                    Object o = s.getTransferable().getTransferData(FLAVOR);
                    if (!(o instanceof PartRef)) return false;
                    PartRef ref = (PartRef) o;
                    if (ref.tree == PartTree.this || dropListener == null) return false;
                    dropListener.onDrop(ref.tree, ref.part, target);
                    return true;
                } catch (Throwable t) {
                    return false;
                }
            }
        });
    }

    private int targetOf(TransferHandler.TransferSupport s) {
        try {
            JTree.DropLocation dl = (JTree.DropLocation) s.getDropLocation();
            TreePath path = dl == null ? null : dl.getPath();
            if (path == null) return -1;
            Object u = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            return u instanceof Integer ? ((Integer) u).intValue() : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    private void rebuild() {
        int keep = selected();
        root.removeAllChildren();
        int n = count();
        nodes = new DefaultMutableTreeNode[n];
        if (n == 0) {
            model.reload();
            return;
        }
        int[][] parts = mamodel.parts;
        for (int i = 0; i < n; i++) nodes[i] = new DefaultMutableTreeNode(Integer.valueOf(i));
        for (int i = 0; i < n; i++) {
            int par = parts[i] != null && parts[i].length > 0 ? parts[i][0] : -1;
            if (par < 0 || par >= n || par == i || !reachesRoot(parts, i, n)) root.add(nodes[i]);
            else nodes[par].add(nodes[i]);
        }
        model.reload();
        for (int i = 0; i < n; i++) tree.expandPath(new TreePath(model.getPathToRoot(nodes[i])));
        if (keep >= 0 && keep < n) select(keep);
    }

    private static boolean reachesRoot(int[][] parts, int i, int n) {
        int p = i, guard = 0;
        while (guard++ <= n) {
            int par = parts[p] != null && parts[p].length > 0 ? parts[p][0] : -1;
            if (par < 0 || par >= n || par == p) return true;
            p = par;
        }
        return false;
    }

    private String labelOf(int i) {
        StringBuilder sb = new StringBuilder();
        String raw = partName(i);
        sb.append(i).append(": ").append(english ? PartNames.toEnglish(raw) : raw);
        int[] r = mamodel != null && mamodel.parts != null && i < mamodel.parts.length
                ? mamodel.parts[i] : null;
        if (r != null && r.length > SubtreeCopier.SPR && r[SubtreeCopier.SPR] < 0) sb.append("  [no sprite]");
        if (i < fx.length && fx[i]) sb.append("  [FX]");
        return sb.toString();
    }

    private static final class Ref implements Transferable {
        private final PartRef ref;
        Ref(PartRef ref) { this.ref = ref; }
        @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{FLAVOR}; }
        @Override public boolean isDataFlavorSupported(DataFlavor f) { return FLAVOR.equals(f); }
        @Override public Object getTransferData(DataFlavor f) throws UnsupportedFlavorException {
            if (!FLAVOR.equals(f)) throw new UnsupportedFlavorException(f);
            return ref;
        }
    }
}
