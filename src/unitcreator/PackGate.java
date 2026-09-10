package unitcreator;

import common.pack.PackData;

final class PackGate {

    private PackGate() {}

    static boolean decide(boolean userPack, boolean editable, boolean hasPassword, boolean allowAnim) {
        if (!userPack) return true;
        if (editable) return true;
        if (hasPassword) return false;
        return allowAnim;
    }

    static boolean allows(PackData pack) {
        if (pack == null) return false;
        if (!(pack instanceof PackData.UserPack)) return true;
        PackData.UserPack up = (PackData.UserPack) pack;
        try {
            PackData.PackDesc d = up.desc;
            if (d == null) return up.editable;
            byte[] pw = d.parentPassword;
            return decide(true, up.editable, pw != null && pw.length > 0, d.allowAnim);
        } catch (Throwable t) {
            Logger.err("UnitCreator: could not read the permissions of " + name(pack)
                    + ", leaving it out", t);
            return false;
        }
    }

    static String name(PackData pack) {
        try {
            if (pack instanceof PackData.UserPack) {
                PackData.PackDesc d = ((PackData.UserPack) pack).desc;
                if (d != null) {
                    if (d.name != null && !d.name.trim().isEmpty()) return d.name;
                    if (d.id != null && !d.id.trim().isEmpty()) return d.id;
                }
            }
        } catch (Throwable ignored) {
        }
        return "an unnamed pack";
    }
}
