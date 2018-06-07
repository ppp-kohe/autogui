package autogui.swing.util;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;

public class UIManagerUtil {
    static UIManagerUtil instance;

    protected Font consoleFont;
    protected int iconSize = -1;

    public static UIManagerUtil getInstance() {
        if (instance == null) {
            instance = new UIManagerUtil();
        }
        return instance;
    }

    public Font getConsoleFont() {
        if (consoleFont == null) {
            String os = System.getProperty("os.name", "").toLowerCase();
            Font base = UIManager.getFont("List.font");
            int size = (base == null ? 12 : base.getSize());

            Font f = null;

            if (os.contains("mac")) {
                f = new Font("Menlo", Font.PLAIN, size); //macOS Sierra introduced "SF Mono" font but it seems not available
            } else if (os.contains("windows")) {
                f = new Font("Consolas", Font.PLAIN, size); //from Vista
            }
            if (f == null || f.getFamily().equals(Font.DIALOG)) { //free font
                f = new Font("DejaVu Sans Mono", Font.PLAIN, size);
            }
            if (f.getFamily().equals(Font.DIALOG)) { //if the font didn't find, the family becomes "Dialog"
                f = new Font(Font.MONOSPACED, Font.PLAIN, size);
            }
            consoleFont = f;
        }
        return consoleFont;
    }

    public Font getLabelFont() {
        Font font = UIManager.getFont("Label.font");
        if (font == null) {
            font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        return font;
    }

    public Font getEditorPaneFont() {
        Font font = UIManager.getFont("EditorPane.font");
        if (font == null) {
            font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        return font;
    }

    public Border getTableFocusCellHighlightBorder() {
        Border b = UIManager.getBorder("Table.focusCellHighlightBorder");
        if (b == null) {
            b = BorderFactory.createEmptyBorder();
        }
        return b;
    }

    public Color getTextPaneSelectionBackground() {
        Color color = UIManager.getColor("TextPane.selectionBackground");
        if (color == null) {
            color = new Color(160, 200, 250);
        }
        return color;
    }

    public Color getFocusColor() {
        Color color = UIManager.getColor("Focus.color");
        if (color == null) {
            color = new Color(150, 150, 150);
        }
        return color;
    }

    /**
     * @return 32 or label font height x2.1
     */
    public int getIconSize() {
        if (iconSize < 0) {
            try {
                BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_3BYTE_BGR);
                Graphics2D g = img.createGraphics();
                {
                    Font font = getLabelFont();
                    TextLayout l = new TextLayout("M", font, g.getFontRenderContext());
                    float h = l.getAscent() + l.getDescent();
                    iconSize = (int) (h * 2.1f); //in macOS HS Lucida Grande 13pt
                }
                g.dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                iconSize = 32;
            }
        }
        return iconSize;
    }

    public int getScaledSizeInt(int n) {
        //macOS Retina env automatically scales the size:
        //    e.g. with x2 PPI, texts with size 14 Font will be rendered as size 28. border size 5 will be handled as 10.
        // however GTK (and Windows too?) physically increases UI resource sizes, and we need to supply x2 sizes for x2 PPI.
        return (int) getScaledSizeFloat(n);
    }

    public float getScaledSizeFloat(float n) {
        return ((float) getIconSize()) / 32f * n;
    }

}
