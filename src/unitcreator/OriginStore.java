package unitcreator;

import common.CommonStatic;
import common.util.unit.Form;
import common.util.unit.Unit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class OriginStore {

    private static final String PATH = UnitCreatorPack.PACK_ID + "/origins.txt";

    static final class Origin {
        final String baseKey;
        final List<String> partKeys = new ArrayList<String>();
        Origin(String baseKey) { this.baseKey = baseKey; }
    }

    private OriginStore() {}

    static void record(Unit u, Form base, List<Donor> donors) {
        String id = idOf(u);
        if (id == null) return;
        try {
            Map<String, String> all = readAll();
            StringBuilder sb = new StringBuilder(keyOf(base));
            for (int i = 0; donors != null && i < donors.size(); i++) {
                Donor d = donors.get(i);
                if (d == null) continue;
                sb.append(' ').append(d.key());
            }
            all.put(id, sb.toString());
            writeAll(all);
            Logger.log("UnitCreator: recorded origin for unit " + id);
        } catch (Throwable t) {
            Logger.err("UnitCreator: could not record origin", t);
        }
    }

    static Origin load(Unit u) {
        String id = idOf(u);
        if (id == null) return null;
        try {
            String line = readAll().get(id);
            if (line == null) return null;
            String[] parts = line.trim().split(" +");
            if (parts.length == 0 || parts[0].length() == 0) return null;
            Origin o = new Origin(parts[0]);
            for (int i = 1; i < parts.length; i++)
                if (parts[i].length() > 0) o.partKeys.add(parts[i]);
            return o;
        } catch (Throwable t) {
            Logger.err("UnitCreator: could not read origin", t);
            return null;
        }
    }

    static Form resolveForm(String key) {
        Donor d = resolveDonor(key);
        return d == null ? null : d.form();
    }

    static Donor resolveDonor(String key) {
        if (key == null || key.length() == 0) return null;
        try {
            List<Donor> pool = key.startsWith("e:")
                    ? UnitPickerDialog.collectEnemies()
                    : UnitPickerDialog.collectUnits();
            for (int i = 0; i < pool.size(); i++)
                if (key.equals(pool.get(i).key())) return pool.get(i);
        } catch (Throwable t) {
            Logger.err("UnitCreator: could not resolve " + key, t);
        }
        return null;
    }

    private static String keyOf(Form f) {
        try {
            return "u:" + f.unit.id.pack + ":" + f.unit.id.id + ":" + f.fid;
        } catch (Throwable t) {
            return "";
        }
    }

    private static String idOf(Unit u) {
        try {
            return String.valueOf(u.id.id);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Map<String, String> readAll() throws Exception {
        Map<String, String> map = new LinkedHashMap<String, String>();
        File f = file();
        if (f == null || !f.isFile()) return map;
        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i).trim();
            if (s.length() == 0 || s.startsWith("#")) continue;
            int at = s.indexOf('=');
            if (at <= 0) continue;
            map.put(s.substring(0, at).trim(), s.substring(at + 1).trim());
        }
        return map;
    }

    private static void writeAll(Map<String, String> map) throws Exception {
        File f = file();
        if (f == null) return;
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory()) parent.mkdirs();
        Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8);
        try {
            w.write("# unit id = base key, then one key per component\n");
            for (Map.Entry<String, String> e : map.entrySet())
                w.write(e.getKey() + "=" + e.getValue() + "\n");
        } finally {
            w.close();
        }
    }

    private static File file() {
        try {
            return CommonStatic.ctx.getWorkspaceFile(PATH);
        } catch (Throwable t) {
            Logger.err("UnitCreator: origin file path unavailable", t);
            return null;
        }
    }
}
