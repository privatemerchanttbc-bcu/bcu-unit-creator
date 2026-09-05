package unitcreator;

import java.util.Random;

public final class NameCombiner {

    private NameCombiner() {}

    private static final String VOWELS = "aeiouyAEIOUY";
    private static final int MAX_LEN = 16;
    private static final String[] PREFIXES = {"Neo ", "Proto ", "Ultra ", "Omega "};
    private static final String[] SUFFIXES = {"oid", "us", "ex"};

    public static String combine(String a, String b, Random rng) {
        if (rng == null) rng = new Random();
        a = clean(a);
        b = clean(b);
        if (a.isEmpty() && b.isEmpty()) return "Hybrid";
        if (a.isEmpty()) return capWords(b);
        if (b.isEmpty()) return capWords(a);

        String[] wa = a.split("\\s+");
        String[] wb = b.split("\\s+");
        boolean multi = wa.length > 1 || wb.length > 1;

        int pattern = rng.nextInt(multi ? 4 : 3);
        String result;
        switch (pattern) {
            case 0: result = head(a) + tail(b); break;
            case 1: result = head(b) + tail(a); break;
            case 2: result = portmanteau(a, b); break;
            default: result = blendWords(wa, wb); break;
        }

        if (result == null || result.trim().isEmpty()) result = head(a) + tail(b);
        result = dedupeSeam(result.trim());
        result = capWords(result);

        int flavour = rng.nextInt(10);
        if (flavour == 0) result = PREFIXES[rng.nextInt(PREFIXES.length)] + result;
        else if (flavour == 1) result = stripTrailingVowel(result) + SUFFIXES[rng.nextInt(SUFFIXES.length)];

        return clampLen(result);
    }

    private static int splitIndex(String s) {
        if (s.length() <= 2) return Math.max(1, s.length() / 2);
        int mid = s.length() / 2;
        int best = -1, bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < s.length(); i++) {
            if (isVowel(s.charAt(i))) {
                int d = Math.abs(i - mid);
                if (d < bestDist) { bestDist = d; best = i; }
            }
        }
        if (best < 0) return mid;
        return Math.min(s.length(), best + 1);
    }

    private static String head(String s) {
        String w = firstWord(s);
        return w.substring(0, splitIndex(w));
    }

    private static String tail(String s) {
        String w = lastWord(s);
        int i = splitIndex(w);
        return i >= w.length() ? w : w.substring(i);
    }

    private static String portmanteau(String a, String b) {
        String wa = firstWord(a), wb = lastWord(b);
        int la = lastVowel(wa);
        int fb = firstVowel(wb);
        String left = la >= 0 ? wa.substring(0, la) : wa;
        String right = fb >= 0 ? wb.substring(fb) : wb;
        if (left.isEmpty()) left = wa;
        if (right.isEmpty()) right = wb;
        return left + right;
    }

    private static String blendWords(String[] wa, String[] wb) {
        return wa[0] + " " + wb[wb.length - 1];
    }

    private static boolean isVowel(char c) { return VOWELS.indexOf(c) >= 0; }

    private static int firstVowel(String s) {
        for (int i = 0; i < s.length(); i++) if (isVowel(s.charAt(i))) return i;
        return -1;
    }

    private static int lastVowel(String s) {
        for (int i = s.length() - 1; i >= 0; i--) if (isVowel(s.charAt(i))) return i;
        return -1;
    }

    private static String firstWord(String s) {
        int sp = s.indexOf(' ');
        return sp < 0 ? s : s.substring(0, sp);
    }

    private static String lastWord(String s) {
        int sp = s.lastIndexOf(' ');
        return sp < 0 ? s : s.substring(sp + 1);
    }

    private static String clean(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean lastSpace = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) { sb.append(c); lastSpace = false; }
            else if ((c == ' ' || c == '-' || c == '_') && sb.length() > 0 && !lastSpace) {
                sb.append(' '); lastSpace = true;
            }
        }
        return sb.toString().trim();
    }

    private static String dedupeSeam(String s) {
        StringBuilder sb = new StringBuilder();
        int run = 1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i > 0 && Character.toLowerCase(c) == Character.toLowerCase(s.charAt(i - 1))) {
                run++;
                boolean vowel = isVowel(c);
                if (run >= 3 || (vowel && run >= 2)) continue;
            } else {
                run = 1;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String stripTrailingVowel(String s) {
        int end = s.length();
        while (end > 1 && isVowel(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }

    private static String capWords(String s) {
        String[] parts = s.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0)));
            sb.append(p.substring(1));
        }
        return sb.length() == 0 ? s : sb.toString();
    }

    private static String clampLen(String s) {
        if (s.length() <= MAX_LEN) return s;
        String cut = s.substring(0, MAX_LEN).trim();

        return cut.isEmpty() ? s.substring(0, MAX_LEN) : cut;
    }
}
