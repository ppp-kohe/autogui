package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.mapping.GuiTaskClock;
import autogui.swing.util.*;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * a fallback swing view for {@link GuiReprValue}
 *
 * <h3>swing-value</h3>
 * {@link PropertyLabel#getSwingViewValue()}
 * latest set value as (read-only) Object.
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
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        PropertyLabel label = new PropertyLabel(context, new SpecifierManagerDefault(parentSpecifier));
        if (context.isTypeElementProperty()) {
            return label.wrapSwingNamed();
        } else {
            return label;
        }
    }

    public static class PropertyLabel extends JLabel
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object> {
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected Object value;
        protected PopupExtension popup;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected MenuBuilder.MenuLabel infoLabel;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);
        protected boolean selected;
        protected Color currentBackground;
        protected Color currentForeground;

        public PropertyLabel(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
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
            initKeyBindingsForStaticMenuItems();
        }

        public void initName() {
            setName(context.getName());
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
            GuiSwingView.setDescriptionToolTipText(context, this);
        }

        public void initVisualProperty() {
            putClientProperty("html.disable", Boolean.TRUE);
            setText(" "); //initialize preferred size
            setSelected(false);
        }

        public void initFocus() {
            setFocusable(true);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    requestFocusInWindow();
                }
            });
        }

        public void initBorder() {
            int hr = UIManagerUtil.getInstance().getScaledSizeInt(10);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, hr, 0, hr),
                    new FocusBorder(this)));
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initValue() {
            update(context, context.getSource().getValue(), context.getContextClock().copy());
        }

        public void initPopup() {
            popup = new PopupExtension(this, new PopupCategorized(this::getSwingStaticMenuItems));
            setInheritsPopupMenu(true);
        }

        public void initKeyBindingsForStaticMenuItems() {
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this);
        }

        public void initDragDrop() {
            GuiSwingView.setupTransferHandler(this, new LabelTransferHandler(this));
        }

        @Override
        public boolean isSwingCurrentValueSupported() {
            return false;
        }

        /**
         * a table renderer call the method after changing it's color and before updating the value.
         *  the foreground and the background will be saved
         * @param selected the label is selected or not
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
            this.currentBackground = getBackground();
            this.currentForeground = getForeground();
        }

        public boolean isSelected() {
            return selected;
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(
                        Arrays.asList(
                                infoLabel,
                                new ContextRefreshAction(context, this),
                                new LabelJsonCopyAction(this, context),
                                new LabelJsonSaveAction(this, context),
                                new LabelToStringCopyAction(this),
                                new LabelTextSaveAction(this),
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
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue, contextClock));
        }

        @Override
        public Object getSwingViewValue() {
            return value;
        }

        @Override
        public void setSwingViewValue(Object value) {
            viewClock.increment();
            this.value = value;
            setTextWithFormattingCurrentValue();
        }

        public String format(Object value) {
            return "" + value;
        }

        public void setTextWithFormatting(Object value) {
            if (value == null) {
                setForeground(Color.gray);
            } else {
                setForeground(currentForeground);
            }
            setText(format(value));
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            setSwingViewValue(value);
        }

        @Override
        public void setSwingViewValue(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                this.value = value;
                setTextWithFormattingCurrentValue();
            }
        }

        public void setTextWithFormattingCurrentValue() {
            GuiReprValue label = (GuiReprValue) context.getRepresentation();
            setTextWithFormatting(label.toUpdateValue(context, value));
            revalidate();
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value, GuiTaskClock clock) {
            setSwingViewValue(value, clock);
        }

        public String getValueAsString() {
            return getValueAsString(getSwingViewValue());
        }

        public String getValueAsString(Object v) {
            GuiReprValue repr = getSwingViewContext().getReprValue();
            return repr.toHumanReadableString(getSwingViewContext(), v);
        }

        public Object getValueFromString(String s) {
            GuiReprValue repr = getSwingViewContext().getReprValue();
            return repr.fromHumanReadableString(getSwingViewContext(), s);
        }

        public void setSwingViewValueWithUpdateFromString(String s) {
            setSwingViewValueWithUpdate(getValueAsString(s));
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
        }

        @Override
        public void setKeyStrokeString(String keyStrokeString) {
            infoLabel.setAdditionalInfo(keyStrokeString);
        }

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
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

        public float getStrokeSize() {
            return UIManagerUtil.getInstance().getScaledSizeFloat(3f);
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
            float ss = getStrokeSize();
            RoundRectangle2D rr = new RoundRectangle2D.Float(x + ss / 2f, y + ss / 2f, width - ss, height - ss, ss * 1.3f, ss * 1.3f);
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
            int s = (int) getStrokeSize();
            return new Insets(s, s, s, s);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    public static class LabelToStringCopyAction extends ToStringCopyAction {
        protected PropertyLabel label;
        public LabelToStringCopyAction(PropertyLabel label) {
            super(label, label.getSwingViewContext());
            this.label = label;
        }

        @Override
        public String toString(Object v) {
            return label.getValueAsString(v);
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

    public static class LabelJsonSaveAction extends GuiSwingJsonTransfer.JsonSaveAction {
        public LabelJsonSaveAction(ValuePane<?> component, GuiMappingContext context) {
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

    public static class LabelTextSaveAction extends PopupExtensionText.TextSaveAction {
        protected GuiSwingViewLabel.PropertyLabel label;

        public LabelTextSaveAction(GuiSwingViewLabel.PropertyLabel label) {
            super(null);
            putValue(NAME, "Save Text...");
            this.label = label;
        }

        @Override
        protected JComponent getComponent() {
            return label;
        }

        @Override
        public void save(Path path) {
            saveLines(path, Collections.singletonList(label.getValueAsString()));
        }
    }



}

