package unitcreator;

import common.pack.PackData;
import common.pack.Source;
import common.pack.UserProfile;

public final class UnitCreatorPack {

    public static final String PACK_ID = "UnitCreator";

    private UnitCreatorPack() {}

    public static PackData.UserPack get() {
        PackData.UserPack pack = UserProfile.getUserPack(PACK_ID);
        if (pack != null) return pack;
        pack = new PackData.UserPack(PACK_ID);
        try {
            pack.desc.names.put("Unit Creator");
        } catch (Throwable ignored) {}
        UserProfile.profile().packmap.put(PACK_ID, pack);
        try { UserProfile.profile().packlist.add(pack); } catch (Throwable ignored) {}
        Logger.log("UnitCreator: created hybrid pack '" + PACK_ID + "'");
        return pack;
    }

    public static void save() {
        try {
            Source.Workspace.saveLocalAnimations();
            Source.Workspace.saveWorkspace();
            Logger.log("UnitCreator: workspace saved");
        } catch (Throwable t) {
            Logger.err("UnitCreator: workspace save failed", t);
        }
    }
}
