package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.PopupExtensionText;
import autogui.swing.util.SearchTextField;

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
import java.util.Arrays;
import java.util.List;

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
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;

        public PropertyLabel(GuiMappingContext context) {
            this.context = context;
            init();
        }

        public void init() {
            initName();
            initFocus();
            initVisualProperty();
            initBorder();
            initContextUpdate();
            initValue();
            initPopup();
            initDragDrop();
        }

        public void initName() {
            setName(context.getName());
            GuiSwingView.setDescriptionToolTipText(context, this);
        }

        public void initVisualProperty() {
            putClientProperty("html.disable", Boolean.TRUE);
            setText(" "); //initialize preferred size
        }

        public void initFocus() {
            setFocusable(true);
        }

        public void initBorder() {
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10),
                    new FocusBorder(this)));
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initValue() {
            update(context, context.getSource());
        }

        public void initPopup() {
            popup = new PopupExtension(this, new PopupCategorized(this::getSwingStaticMenuItems));
            setInheritsPopupMenu(true);
        }

        public void initDragDrop() {
            GuiSwingView.setupTransferHandler(this, new LabelTransferHandler(this));
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(
                        Arrays.asList(
                                GuiSwingContextInfo.get().getInfoLabel(context),
                                new ContextRefreshAction(context),
                                new LabelJsonCopyAction(this, context),
                                new ToStringCopyAction(this, context),
                                new PopupExtensionText.TextOpenBrowserAction(this))
                );
            }
            return menuItems;
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
            revalidate();
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
            focusColor = SearchTextField.getFocusColor();
        }

        public void initStrokes() {
            strokes = new BasicStroke[3];
            for (int i = 0; i < strokes.length; ++i) {
                strokes[i] = new BasicStroke(strokes.length / 2.0f);
            }
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
            RoundRectangle2D rr = new RoundRectangle2D.Float(x + 1.5f, y + 1.5f, width - 3f, height - 3f, 4, 4);
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
            return new Insets(3, 3, 3, 3);
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

