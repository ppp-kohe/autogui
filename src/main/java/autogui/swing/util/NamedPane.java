package autogui.swing.util;

import javax.naming.NameParser;
import javax.swing.*;

public class NamedPane extends JComponent {
    protected String displayName;
    protected JLabel label;
    protected JComponent contentPane;

    public NamedPane() { }

    public NamedPane(String displayName) {
        this.displayName = displayName;
        init();
    }

    public NamedPane(String displayName, JComponent contentPane) {
        this(displayName);
        this.contentPane = contentPane;
        if (contentPane != null) {
            setContentPane(contentPane);
        }
    }

    public JComponent getContentPane() {
        return contentPane;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        if (label != null) {
            label.setText(displayName + ":");
        }
    }

    public void init() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        ResizableFlowLayout layout = new ResizableFlowLayout(true, 5);
        setLayout(layout);

        label = new JLabel(displayName + ":");
        label.setHorizontalAlignment(JLabel.RIGHT);
        ResizableFlowLayout.add(this, label, false);
    }

    public void setContentPane(JComponent contentPane) {
        if (this.contentPane != null) {
            ResizableFlowLayout.remove(this, this.contentPane);
        }
        this.contentPane = contentPane;
        ResizableFlowLayout.add(this, contentPane, true);
    }
}
