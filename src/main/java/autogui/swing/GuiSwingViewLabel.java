package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.PopupExtensionText;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.RoundRectangle2D;

/**
 * <h3>representation</h3>
 * {@link GuiReprValue},
 *   a fallback view.
 *
 * <h3>{@link PropertyLabel#getSwingViewValue()}</h3>
 * latest set value: (read-only) Object.
 *   {@link PropertyLabel#setSwingViewValueWithUpdate(Object)} just causes GUI update without updating the context's value.
 *
 *  <h3>history-value</h3>
 *  unsupported.
 *
 *  <h3>string-transfer</h3>
 *  {@link LabelTransferHandler}.
 *   exporting-only {@link autogui.base.mapping.GuiRepresentation#toHumanReadableString(GuiMappingContext, Object)}
 */
public class GuiSwingViewLabel implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyLabel label = new PropertyLabel(context);
        if (context.isTypeElementProperty()) {
            return label.wrapNamed();
        } else {
            return label;
        }
    }

    public static class PropertyLabel extends JLabel
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object> {
        protected GuiMappingContext context;
        protected Object value;
        protected PopupExtension popup;

        public PropertyLabel(GuiMappingContext context) {
            this.context = context;
            putClientProperty("html.disable", Boolean.TRUE);
            setMinimumSize(new Dimension(100, 20));

            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10),
                    new FocusBorder(this)));

            //context update
            context.addSourceUpdateListener(this);
            //initial update
            update(context, context.getSource());

            //popup
            JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
            ContextRefreshAction refreshAction = new ContextRefreshAction(context);

            PopupExtensionText.TextOpenBrowserAction browserAction = new PopupExtensionText.TextOpenBrowserAction(this);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.accept(info);
                menu.accept(refreshAction);
                menu.accept(browserAction);
                menu.accept(new LabelJsonCopyAction(this, context));
                menu.accept(new ToStringCopyAction(this, context));
            });
            setInheritsPopupMenu(true);

            //drag
            GuiSwingView.setupTransferHandler(this, new LabelTransferHandler(this));
            setFocusable(true);

        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return value;
        }

        @Override
        public void setSwingViewValue(Object value) {
            GuiReprValue label = (GuiReprValue) context.getRepresentation();
            this.value = value;
            setText("" + label.toUpdateValue(context, value));
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            setSwingViewValue(value);
        }

        public String getValueAsString() {
            GuiReprValue label = (GuiReprValue) context.getRepresentation();
            return "" + label.toHumanReadableString(context, getSwingViewValue());
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }
    }

    public static class FocusBorder implements Border {
        protected Color focusColor;
        protected BasicStroke[] strokes;

        public FocusBorder(JComponent target) {
            target.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    target.repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    target.repaint();
                }
            });
        }

        public void initFocusColor() {
            focusColor = UIManager.getColor("Focus.color");
            if (focusColor == null) {
                focusColor = new Color(150, 150, 150);
            }
        }

        public void initStrokes() {
            strokes = new BasicStroke[3];
            for (int i = 0; i < strokes.length; ++i) {
                strokes[i] = new BasicStroke(strokes.length / 2.0f);
            }
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (focusColor == null) {
                initFocusColor();
            }
            if (strokes == null){
                initStrokes();
            }

            if (c.hasFocus()) {
                paintStrokes(g, x, y, width, height);
            }
        }

        public void paintStrokes(Graphics g, int x, int y, int width, int height) {
            RoundRectangle2D rr = new RoundRectangle2D.Float(x, y, width - 1, height - 1, 3, 3);
            Graphics2D g2 = (Graphics2D) g;
            Color color2 = new Color(focusColor.getRed(), focusColor.getGreen(), focusColor.getBlue(), 150);
            g2.setColor(color2);
            for (BasicStroke s : strokes) {
                g2.setStroke(s);
                g2.draw(rr);
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    /** special handling for any type of value as string */
    public static class LabelJsonCopyAction extends GuiSwingJsonTransfer.JsonCopyAction {
        public LabelJsonCopyAction(ValuePane<?> component, GuiMappingContext context) {
            super(component, context);
        }

        @Override
        public Object toCopiedJson(Object value) {
            if (value instanceof GuiReprValue.NamedValue) {
                return ((GuiReprValue.NamedValue) value).toJson(toCopiedJson(((GuiReprValue.NamedValue) value).value));
            } else {
                return "" + value;
            }
        }
    }

    public static class LabelTransferHandler extends  TransferHandler {
        protected PropertyLabel pane;

        public LabelTransferHandler(PropertyLabel pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return false;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection(pane.getValueAsString());
        }
    }

}

