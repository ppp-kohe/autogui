package org.autogui.swing;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiRepresentation;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * a fallback swing view for {@link GuiReprValue}
 *
 * <h2>swing-value</h2>
 * {@link PropertyLabel#getSwingViewValue()}
 * latest set value as (read-only) Object.
 *   {@link PropertyLabel#setSwingViewValueWithUpdate(Object)} just causes GUI update without updating the context's value.
 *
 *  <h2>history-value</h2>
 *  unsupported.
 *
 *  <h2>string-transfer</h2>
 *  {@link LabelTransferHandler}.
 *   exporting-only {@link GuiRepresentation#toHumanReadableString(GuiMappingContext, Object)}
 */
public class GuiSwingViewLabel implements GuiSwingView {
    public GuiSwingViewLabel() {}
    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        PropertyLabel label = new PropertyLabel(context, new SpecifierManagerDefault(parentSpecifier));
        if (context.isTypeElementProperty()) {
            return label.wrapSwingNamed();
        } else {
            return label;
        }
    }

    @SuppressWarnings("this-escape")
    public static class PropertyLabel extends JLabel
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object> {
        @Serial private static final long serialVersionUID = 1L;
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
            SwingDeferredRunner.invokeLater(() -> setSwingViewValue(newValue, contextClock));
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
            GuiSwingView.updateViewClockSync(viewClock, context);
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

    public static class FocusBorder extends org.autogui.swing.util.FocusBorder {
        public FocusBorder(JComponent target) {
            super(target);
        }
    }

    public static class LabelToStringCopyAction extends ToStringCopyAction {
        @Serial private static final long serialVersionUID = 1L;
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
        @Serial private static final long serialVersionUID = 1L;
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
        @Serial private static final long serialVersionUID = 1L;
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
        @Serial private static final long serialVersionUID = 1L;
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
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingViewLabel.PropertyLabel label;

        @SuppressWarnings("this-escape")
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

