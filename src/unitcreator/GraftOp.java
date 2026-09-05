package unitcreator;

import common.util.anim.Part;

final class GraftOp {

    static final int TYPES = 4;
    static final String[] TYPE_NAMES = {"Walk", "Idle", "Attack", "Hitback"};

    final Donor donor;
    final int bRoot;
    int aParent;

    final int[] offX = new int[TYPES];
    final int[] offY = new int[TYPES];
    final int[] scale = {1000, 1000, 1000, 1000};
    final int[] angle = new int[TYPES];
    final int[] zOffset = new int[TYPES];
    final int[] speed = {100, 100, 100, 100};

    boolean mirror;
    boolean copyAnim = true;

    int partBase = -1;
    int rootPart = -1;
    int mirrorPart = -1;
    int partCount;
    int origPX, origPY, origSX, origSY, origANG, origPivotX;
    int[] origZ;
    int[] origANGs;
    Part[] angleTracks;
    boolean angleFlipped;

    Part[][] ctrl;
    Part[][] zctrl;

    String donorPartName = "part";
    String targetPartName = "part";

    GraftOp(Donor donor, int bRoot, int aParent) {
        this.donor = donor;
        this.bRoot = bRoot;
        this.aParent = aParent;
    }

    GraftOp copyTunables() {
        GraftOp o = new GraftOp(donor, bRoot, aParent);
        System.arraycopy(offX, 0, o.offX, 0, TYPES);
        System.arraycopy(offY, 0, o.offY, 0, TYPES);
        System.arraycopy(scale, 0, o.scale, 0, TYPES);
        System.arraycopy(angle, 0, o.angle, 0, TYPES);
        System.arraycopy(zOffset, 0, o.zOffset, 0, TYPES);
        System.arraycopy(speed, 0, o.speed, 0, TYPES);
        o.mirror = mirror;
        o.copyAnim = copyAnim;
        o.donorPartName = donorPartName;
        o.targetPartName = targetPartName;
        return o;
    }

    static int typeOf(int animSlot) {
        return animSlot >= 0 && animSlot < TYPES ? animSlot : 1;
    }

    boolean applied() {
        return partBase >= 0 && partCount > 0;
    }

    boolean uniform(int[] v) {
        for (int i = 1; i < v.length; i++) if (v[i] != v[0]) return false;
        return true;
    }

    void setAll(int[] v, int value) {
        for (int i = 0; i < v.length; i++) v[i] = value;
    }

    void addAll(int[] v, int delta) {
        for (int i = 0; i < v.length; i++) v[i] += delta;
    }

    String label() {
        StringBuilder sb = new StringBuilder();
        sb.append(donor.name()).append(" / ").append(disp(donorPartName))
          .append(" -> ").append(disp(targetPartName));
        if (!copyAnim) sb.append("  [sprite only]");
        else if (!uniform(speed)) sb.append("  [speed per anim]");
        else if (speed[0] != 100) sb.append("  [").append(speed[0]).append("%]");
        if (mirror) sb.append("  [mirrored]");
        if (!uniform(offX) || !uniform(offY) || !uniform(scale)
                || !uniform(angle) || !uniform(zOffset)) sb.append("  [per anim]");
        return sb.toString();
    }

    private static String disp(String s) {
        return PartTree.english() ? PartNames.toEnglish(s) : s;
    }
}
