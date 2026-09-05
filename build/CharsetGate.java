import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CharsetGate {

    private static final char[] BANNED = {
        '\u2014', '\u2013', '\u2012', '\u2015', '\u2212', '\u00ad',
        '\u2018', '\u2019', '\u201a', '\u201b',
        '\u201c', '\u201d', '\u201e', '\u201f',
        '\u2026', '\u00a0', '\u2007', '\u202f', '\ufeff'
    };

    private static final String[] SKIP = { "/out/" };

    public static void main(String[] args) throws IOException {
        Path root = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        List<String> hits = new ArrayList<String>();
        int scanned = 0;

        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> files = new ArrayList<Path>();
            walk.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(files::add);

            for (Path p : files) {
                String rel = root.relativize(p).toString().replace(File.separatorChar, '/');
                if (skip("/" + rel)) {
                    continue;
                }
                scanned++;
                inspect(p, rel, hits);
            }
        }

        if (hits.isEmpty()) {
            System.out.println("[charset-gate] OK: " + scanned + " files, no typographic characters");
            return;
        }

        System.out.println("[charset-gate] FAILED: " + hits.size() + " violation(s)");
        for (String hit : hits) {
            System.out.println("  " + hit);
        }
        System.out.println();
        System.out.println("[charset-gate] Replace with plain ASCII: - for dashes, \" and ' for quotes.");
        System.exit(1);
    }

    private static boolean skip(String rel) {
        for (String s : SKIP) {
            if (rel.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private static void inspect(Path p, String rel, List<String> hits) throws IOException {
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            for (char bad : BANNED) {
                int at = line.indexOf(bad);
                if (at >= 0) {
                    hits.add(rel + ":" + (i + 1) + " U+"
                        + String.format("%04X", (int) bad) + " at column " + (at + 1));
                    break;
                }
            }
        }
    }
}
