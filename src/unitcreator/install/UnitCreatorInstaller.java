package unitcreator.install;

import unitcreator.transform.MainPageTransformer;

import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class UnitCreatorInstaller {

    private static final String MARKER = "unitcreator/unitcreator.install";
    private static final String ORIGINAL = "unitcreator/MainPage.class.orig";
    private static final String TARGET = "page/MainPage.class";
    private static final String BACKUP_SUFFIX = ".unitcreator-backup";
    private static final String OWNED_PREFIX = "unitcreator/";

    private static final String FEATURE_DIR = "bcu-features";
    private static final String REGISTRATION = "200-unit-creator.jvmargs";
    private static final String LAUNCHER = "BCU New Features.bat";
    private static final String HIDDEN = "BCU New Features (No Console).vbs";
    private static final String AGENT = "unit-creator.jar";

    private static final String INSTALLER_PREFIX = "unitcreator/install/";
    private static final String TRANSFORM_PREFIX = "unitcreator/transform/";
    private static final String AGENT_PREFIX = "unitcreator/agent/";

    public static void main(String[] args) {
        if (System.getProperty("unitcreator.log") == null)
            System.setProperty("unitcreator.log",
                    new File(System.getProperty("java.io.tmpdir"), "unit-creator-install.log")
                            .getAbsolutePath());
        try {
            boolean uninstall = false;
            boolean register = false;
            boolean unregister = false;
            String path = null;
            for (String a : args) {
                if (a.equals("--uninstall") || a.equals("-u")) uninstall = true;
                else if (a.equals("--register")) register = true;
                else if (a.equals("--unregister")) unregister = true;
                else if (!a.startsWith("-")) path = a;
            }
            File bcu = path != null ? new File(path) : autoFindBcu();
            if (bcu != null) bcu = bcu.getAbsoluteFile();
            if (bcu == null || !bcu.isFile())
                fail("Could not find the BCU jar. Put this installer folder inside your\n"
                        + "BCU folder, or pass the BCU jar path as an argument.");

            if (register || unregister) {
                File root = bcu.getParentFile();
                if (root == null) fail("Could not work out the BCU folder.");
                if (register) {
                    String made = register(root);
                    logLine("registered with the shared launcher in " + root.getName());
                    success("Unit Creator added to the shared launcher.\n\n"
                            + "Start BCU with \"" + LAUNCHER + "\" in:\n" + root.getAbsolutePath()
                            + "\n\nYour BCU jar is not modified." + made);
                } else {
                    String gone = unregister(root);
                    logLine("removed the shared launcher registration in " + root.getName());
                    success("Unit Creator removed from the shared launcher.\n\n"
                            + "Other features registered there are untouched." + gone);
                }
                return;
            }

            if (uninstall) {
                if (!jarHasEntry(bcu, MARKER)) {
                    success("Unit Creator is not installed in:\n" + bcu.getName()
                            + "\n\nNothing to remove.");
                    return;
                }
                int removed = uninstall(bcu);
                logLine("uninstalled from " + bcu.getName() + " (" + removed + " entries removed)");
                success("Unit Creator removed from:\n" + bcu.getName()
                        + "\n\nOnly the entries this installer added were touched."
                        + "\nEverything else in the jar is unchanged."
                        + "\n\nUnits you already made stay in your BCU packs.");
                return;
            }

            if (jarHasEntry(bcu, MARKER)) {
                success("Unit Creator is already installed in:\n" + bcu.getName()
                        + "\n\nNothing to do. Run the uninstaller first if you want"
                        + "\nto reinstall a newer build.");
                return;
            }

            int added = install(bcu);
            logLine("installed into " + bcu.getName() + " (" + added + " classes added)");
            success("Unit Creator installed into:\n" + bcu.getName()
                    + "\n\nStart BCU as usual - the main menu now has a"
                    + "\n\"Unit Creator\" button. A backup was kept as:\n"
                    + bcu.getName() + BACKUP_SUFFIX);
        } catch (InstallError e) {
            fail(e.getMessage());
        } catch (Throwable t) {
            fail("Unexpected error: " + t);
        }
    }

    private static int install(File bcu) throws Exception {
        Map<String, byte[]> out = new LinkedHashMap<String, byte[]>();
        byte[] original = null;
        byte[] patched = null;

        ZipFile zf = new ZipFile(bcu);
        try {
            java.util.Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                String name = e.getName();
                if (isSignatureFile(name)) continue;
                byte[] data = readAll(zf.getInputStream(e));
                if (name.equals(TARGET)) {
                    original = data;
                    byte[] t = apply(name, data);
                    if (t != null) {
                        patched = t;
                        data = t;
                    }
                }
                out.put(name, data);
            }
        } finally {
            zf.close();
        }

        if (original == null)
            throw new InstallError("This jar does not contain " + TARGET + ".\n"
                    + "It may not be a BCU jar. Nothing was changed.");
        if (patched == null || !MainPageTransformer.hooksComplete())
            throw new InstallError("The BCU main menu class could not be patched -\n"
                    + "this BCU build may be too different.\nNothing was changed.");

        List<String> addedNames = new ArrayList<String>();
        int n = copyRuntimeClasses(out, addedNames);
        if (n == 0)
            throw new InstallError("Internal error: the Unit Creator runtime classes were not\n"
                    + "found inside the installer. Please rebuild the installer.");

        out.put(ORIGINAL, original);
        StringBuilder mk = new StringBuilder("Unit Creator static install\n");
        mk.append(new Date()).append("\n");
        for (int i = 0; i < addedNames.size(); i++) mk.append(addedNames.get(i)).append("\n");
        out.put(MARKER, mk.toString().getBytes(StandardCharsets.UTF_8));

        File backup = new File(bcu.getParentFile(), bcu.getName() + BACKUP_SUFFIX);
        Files.copy(bcu.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        write(bcu, out);
        return n;
    }

    private static int uninstall(File bcu) throws Exception {
        Map<String, byte[]> out = new LinkedHashMap<String, byte[]>();
        byte[] original = null;
        int removed = 0;

        List<String> owned = readMarkerList(bcu);

        ZipFile zf = new ZipFile(bcu);
        try {
            java.util.Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                String name = e.getName();
                if (isSignatureFile(name)) continue;
                byte[] data = readAll(zf.getInputStream(e));
                if (name.equals(ORIGINAL)) {
                    original = data;
                    removed++;
                    continue;
                }
                if (name.startsWith(OWNED_PREFIX) || owned.contains(name)) {
                    removed++;
                    continue;
                }
                out.put(name, data);
            }
        } finally {
            zf.close();
        }

        if (original == null)
            throw new InstallError("The stored original main menu class is missing from the jar,\n"
                    + "so Unit Creator cannot be removed cleanly.\n"
                    + "Restore " + bcu.getName() + BACKUP_SUFFIX + " by hand instead.");
        out.put(TARGET, original);
        write(bcu, out);
        return removed;
    }

    private static List<String> readMarkerList(File bcu) {
        List<String> list = new ArrayList<String>();
        ZipFile zf = null;
        try {
            zf = new ZipFile(bcu);
            ZipEntry e = zf.getEntry(MARKER);
            if (e == null) return list;
            String text = new String(readAll(zf.getInputStream(e)), StandardCharsets.UTF_8);
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String s = lines[i].trim();
                if (s.endsWith(".class")) list.add(s);
            }
        } catch (Throwable ignored) {
        } finally {
            if (zf != null) try { zf.close(); } catch (Exception ignored) {}
        }
        return list;
    }

    private static void write(File bcu, Map<String, byte[]> out) throws Exception {
        File tmp = File.createTempFile("bcu-unitcreator", ".jar", bcu.getParentFile());
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmp));
        try {
            for (Map.Entry<String, byte[]> en : out.entrySet()) {
                zos.putNextEntry(new ZipEntry(en.getKey()));
                zos.write(en.getValue());
                zos.closeEntry();
            }
        } finally {
            zos.close();
        }
        try {
            Files.move(tmp.toPath(), bcu.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Throwable ignored) {
            Files.move(tmp.toPath(), bcu.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static byte[] apply(String entryName, byte[] data) {
        try {
            String className = entryName.substring(0, entryName.length() - ".class".length());
            return new MainPageTransformer()
                    .transform(null, className, null, null, data);
        } catch (Throwable t) {
            return null;
        }
    }

    private static int copyRuntimeClasses(Map<String, byte[]> out, List<String> added) throws Exception {
        Map<String, byte[]> mine = new LinkedHashMap<String, byte[]>();
        File self = new File(UnitCreatorInstaller.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        if (self.isFile()) {
            ZipFile zf = new ZipFile(self);
            try {
                java.util.Enumeration<? extends ZipEntry> en = zf.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    if (e.isDirectory()) continue;
                    String name = e.getName();
                    if (isRuntimeClass(name)) mine.put(name, readAll(zf.getInputStream(e)));
                }
            } finally {
                zf.close();
            }
        } else if (self.isDirectory()) {
            collect(self, "", mine);
        }

        int n = 0;
        for (Map.Entry<String, byte[]> en : mine.entrySet()) {
            String name = en.getKey();
            if (!out.containsKey(name)) added.add(name);
            out.put(name, en.getValue());
            n++;
        }
        return n;
    }

    private static void collect(File root, String prefix, Map<String, byte[]> out) throws Exception {
        File[] kids = root.listFiles();
        if (kids == null) return;
        for (File k : kids) {
            String name = prefix + k.getName();
            if (k.isDirectory()) collect(k, name + "/", out);
            else if (isRuntimeClass(name)) out.put(name, Files.readAllBytes(k.toPath()));
        }
    }

    private static boolean isRuntimeClass(String name) {
        if (!name.endsWith(".class")) return false;
        if (!name.startsWith(OWNED_PREFIX)) return false;
        return !name.startsWith(INSTALLER_PREFIX) && !name.startsWith(TRANSFORM_PREFIX)
                && !name.startsWith(AGENT_PREFIX);
    }

    private static File autoFindBcu() {
        File dir;
        try {
            dir = new File(UnitCreatorInstaller.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParentFile();
        } catch (Exception e) {
            dir = new File(".");
        }
        File found = pickBcu(dir);
        if (found == null) found = pickBcu(new File("."));
        if (found == null && dir != null) found = pickBcu(dir.getParentFile());
        return found;
    }

    private static File pickBcu(File dir) {
        if (dir == null || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        File best = null;
        for (File f : files) {
            String n = f.getName().toLowerCase();
            if (!n.endsWith(".jar")) continue;
            if (!n.startsWith("bcu")) continue;
            if (n.contains("installer")
                    || n.contains("backup") || n.contains("directedit")
                    || n.contains("speedscale") || n.contains("unitcreator")) continue;
            if (best == null || n.startsWith("bcu-0")) best = f;
        }
        return best;
    }

    private static boolean jarHasEntry(File jar, String entry) {
        ZipFile zf = null;
        try {
            zf = new ZipFile(jar);
            return zf.getEntry(entry) != null;
        } catch (Exception e) {
            return false;
        } finally {
            if (zf != null) try { zf.close(); } catch (Exception ignored) {}
        }
    }

    private static boolean isSignatureFile(String name) {
        String u = name.toUpperCase();
        return u.startsWith("META-INF/") && (u.endsWith(".SF") || u.endsWith(".RSA")
                || u.endsWith(".DSA") || u.endsWith(".EC"));
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) bo.write(buf, 0, r);
        in.close();
        return bo.toByteArray();
    }

    private static void logLine(String msg) {
        try {
            File file = new File(installerDir(), "install.log");
            java.io.PrintWriter w = new java.io.PrintWriter(new java.io.FileWriter(file, true), true);
            w.println(new Date() + "  " + msg);
            w.close();
        } catch (Throwable ignored) {
        }
    }

private static String register(File root) throws Exception {
        File features = new File(root, FEATURE_DIR);
        if (!features.isDirectory() && !features.mkdirs())
            throw new InstallError("Could not create:\n" + features.getAbsolutePath());

        String module = moduleDir(root);
        StringBuilder reg = new StringBuilder();
        reg.append("# One JVM argument per line. Files are loaded alphabetically.\n");
        reg.append("\"-Dunitcreator.log=!BCU_ROOT!\\").append(module)
                .append("\\unit-creator.log\"\n");
        reg.append("\"-javaagent:!BCU_ROOT!\\").append(module).append("\\")
                .append(AGENT).append("\"\n");
        write(new File(features, REGISTRATION), reg.toString());

        StringBuilder note = new StringBuilder();
        File bat = new File(root, LAUNCHER);
        if (!bat.isFile()) {
            write(bat, sharedLauncher());
            write(new File(root, HIDDEN), hiddenLauncher());
            note.append("\n\nThe shared launcher was not there yet, so it was created.");
        } else {
            note.append("\n\nThe existing shared launcher was left as it is.");
        }
        return note.toString();
    }

    private static String unregister(File root) throws Exception {
        File features = new File(root, FEATURE_DIR);
        File mine = new File(features, REGISTRATION);
        if (!mine.isFile()) return "\n\nThere was no registration to remove.";
        if (!mine.delete())
            throw new InstallError("Could not delete:\n" + mine.getAbsolutePath());

        String[] rest = features.list(new java.io.FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return name.endsWith(".jvmargs");
            }
        });
        if (rest != null && rest.length > 0)
            return "\n\n" + rest.length + " other feature is still registered, so the"
                    + "\nshared launcher was kept.";

        new File(root, LAUNCHER).delete();
        new File(root, HIDDEN).delete();
        features.delete();
        return "\n\nNothing else was registered, so the shared launcher was removed too.";
    }

    private static String moduleDir(File root) {
        try {
            File dir = installerDir();
            if (dir != null && dir.getParentFile() != null
                    && dir.getParentFile().getCanonicalFile().equals(root.getCanonicalFile()))
                return dir.getName();
        } catch (Throwable ignored) {
        }
        return "Unit Creator";
    }

    private static void write(File file, String text) throws Exception {
        Files.write(file.toPath(), text.replace("\n", "\r\n")
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String sharedLauncher() {
        return "@echo off\nsetlocal enabledelayedexpansion\n"
                + "set \"BCU_ROOT=%~dp0\"\n"
                + "if \"!BCU_ROOT:~-1!\"==\"\\\" set \"BCU_ROOT=!BCU_ROOT:~0,-1!\"\n"
                + "set \"FEATURE_DIR=!BCU_ROOT!\\" + FEATURE_DIR + "\"\n"
                + "set \"FEATURE_ARGS=\"\n"
                + "if exist \"!FEATURE_DIR!\\*.jvmargs\" for /f \"delims=\" %%g in "
                + "('dir /b /a-d /on \"!FEATURE_DIR!\\*.jvmargs\"') do (\n"
                + "  for /f \"usebackq delims=\" %%a in (\"!FEATURE_DIR!\\%%g\") do (\n"
                + "    set \"ARG=%%a\"\n"
                + "    if defined ARG if not \"!ARG:~0,1!\"==\"#\" set \"FEATURE_ARGS=!FEATURE_ARGS! !ARG!\"\n"
                + "  )\n"
                + ")\n"
                + "if not defined FEATURE_ARGS (echo [ERROR] No feature registrations found in "
                + "!FEATURE_DIR!& pause& exit /b 1)\n"
                + "for /f \"usebackq delims=\" %%f in (`powershell -NoProfile -ExecutionPolicy Bypass "
                + "-Command \"$d='!BCU_ROOT!'; Get-ChildItem -LiteralPath $d -Filter 'BCU*.jar' "
                + "^| Where-Object { $_.Name -notmatch 'installer|attack-ld|manual-control|backup|patch' } "
                + "^| Sort-Object LastWriteTimeUtc -Descending ^| Select-Object -First 1 -ExpandProperty FullName\"`) "
                + "do set \"BCU_JAR=%%f\"\n"
                + "if not defined BCU_JAR (echo [ERROR] No BCU jar found in !BCU_ROOT!& pause& exit /b 1)\n"
                + "cd /d \"!BCU_ROOT!\"\n"
                + "java !FEATURE_ARGS! -jar \"!BCU_JAR!\" %*\n"
                + "set \"CODE=!errorlevel!\"\n"
                + "if not \"!CODE!\"==\"0\" if not defined BCU_FEATURES_HIDDEN pause\n"
                + "endlocal & exit /b %CODE%\n";
    }

    private static String hiddenLauncher() {
        return "Option Explicit\nDim sh, env, fso, root, runFile, command, code\n"
                + "Set sh = CreateObject(\"WScript.Shell\")\n"
                + "Set env = sh.Environment(\"Process\")\n"
                + "Set fso = CreateObject(\"Scripting.FileSystemObject\")\n"
                + "root = fso.GetParentFolderName(WScript.ScriptFullName)\n"
                + "runFile = fso.BuildPath(root, \"" + LAUNCHER + "\")\n"
                + "env(\"BCU_FEATURES_HIDDEN\") = \"1\"\n"
                + "command = \"cmd.exe /d /s /c \" & Chr(34) & Chr(34) & runFile & Chr(34) & Chr(34)\n"
                + "code = sh.Run(command, 0, True)\n"
                + "If code <> 0 Then MsgBox \"BCU New Features exited with code \" & code, "
                + "vbExclamation, \"BCU New Features\"\n"
                + "WScript.Quit code\n";
    }

    private static File installerDir() {
        try {
            return new File(UnitCreatorInstaller.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParentFile();
        } catch (Exception e) {
            return new File(".");
        }
    }

    private static void success(String msg) {
        System.out.println("[Unit Creator Installer] OK\n" + msg);
        if (!GraphicsEnvironment.isHeadless() && !Boolean.getBoolean("unitcreator.nogui"))
            JOptionPane.showMessageDialog(null, msg, "Unit Creator",
                    JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    private static void fail(String msg) {
        System.err.println("[Unit Creator Installer] FAILED\n" + msg);
        if (!GraphicsEnvironment.isHeadless() && !Boolean.getBoolean("unitcreator.nogui"))
            JOptionPane.showMessageDialog(null, msg, "Unit Creator - Not Installed",
                    JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private static final class InstallError extends Exception {
        InstallError(String m) { super(m); }
    }

    private UnitCreatorInstaller() {}
}
