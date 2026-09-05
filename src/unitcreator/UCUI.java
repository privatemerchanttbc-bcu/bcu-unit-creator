package unitcreator;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class UCUI {

    static final Color GOLD        = new Color(248, 187, 0);
    static final Color GOLD_HI     = new Color(255, 212, 81);
    static final Color GOLD_BORDER = new Color(169, 127, 0);
    static final Color GREEN_HI    = new Color(143, 194, 87);
    static final Color GREEN_LO    = new Color(95, 154, 58);
    static final Color GREEN_BORDER= new Color(60, 107, 34);
    static final Color RED         = new Color(239, 154, 154);
    static final Color TEAL        = new Color(98, 117, 127);
    static final Color INK         = new Color(22, 24, 10);

    private UCUI() {}

    static boolean isDark() {
        try {
            Color c = UIManager.getColor("control");
            if (c == null) c = UIManager.getColor("Panel.background");
            if (c == null) return true;
            double lum = 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue();
            return lum < 128;
        } catch (Throwable t) {
            return true;
        }
    }

    static Color text()          { return isDark() ? new Color(245, 245, 245) : new Color(58, 58, 58); }
    static Color muted()         { return isDark() ? new Color(174, 174, 174) : new Color(138, 138, 138); }
    static Color neutralTop()    { return isDark() ? new Color(79, 79, 79)    : new Color(251, 251, 251); }
    static Color neutralBot()    { return isDark() ? new Color(43, 43, 43)    : new Color(220, 220, 220); }
    static Color neutralBorder() { return isDark() ? new Color(20, 20, 20)    : new Color(168, 168, 168); }
    static Color panel()         { return isDark() ? new Color(72, 72, 72)    : new Color(245, 245, 245); }
    static Color inset()         { return isDark() ? new Color(38, 38, 38)    : new Color(201, 201, 201); }
    static Color line()          { return isDark() ? new Color(0, 0, 0, 128)  : new Color(0, 0, 0, 56); }
    static Color select()        { return isDark() ? new Color(98, 117, 127)  : new Color(187, 222, 251); }

    static String describe(Throwable t) {
        if (t == null) return "unknown error";
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        StringBuilder sb = new StringBuilder(root.getClass().getSimpleName());
        if (root.getMessage() != null && !root.getMessage().isEmpty()) {
            sb.append(": ").append(root.getMessage());
        }
        StackTraceElement[] st = root.getStackTrace();
        if (st != null && st.length > 0) {
            sb.append("  (at ").append(st[0].getClassName()).append('.')
              .append(st[0].getMethodName()).append(':').append(st[0].getLineNumber()).append(')');
        }
        return sb.toString();
    }

    static final int HELP_HOLD_MS = 60000;

    static JLabel help(String text) {
        final JLabel l = new JLabel("[?]");
        l.setForeground(muted());
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        l.setToolTipText("<html><body style='width:340px;padding:2px'>" + text + "</body></html>");
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            private int savedDismiss = -1;
            private int savedInitial = -1;

            @Override public void mouseEntered(MouseEvent e) {
                ToolTipManager m = ToolTipManager.sharedInstance();
                if (savedDismiss < 0) {
                    savedDismiss = m.getDismissDelay();
                    savedInitial = m.getInitialDelay();
                }
                m.setDismissDelay(HELP_HOLD_MS);
                m.setInitialDelay(150);
                l.setForeground(GOLD);
            }

            @Override public void mouseExited(MouseEvent e) {
                ToolTipManager m = ToolTipManager.sharedInstance();
                if (savedDismiss >= 0) {
                    m.setDismissDelay(savedDismiss);
                    m.setInitialDelay(savedInitial);
                }
                l.setForeground(muted());
            }
        });
        return l;
    }

    static Border card(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(line()), title);
        tb.setTitleColor(muted());
        tb.setTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        return BorderFactory.createCompoundBorder(tb,
                BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }
}
