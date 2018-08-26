package org.autogui.swing;

import org.autogui.swing.table.TableTargetColumnAction;
import org.autogui.swing.util.MenuBuilder;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.util.PopupExtensionText;
import org.autogui.base.mapping.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * a swing view for
 * {@link GuiReprValueBooleanCheckBox}
 *
 * <h3>swing-value</h3>
 * {@link PropertyCheckBox#getSwingViewValue()}: check-box selection as {@link Boolean}
 *
 * <h3>history-value</h3>
 * supported.
 *
 * <h3>string-transfer</h3>
 * {@link BooleanTransferHandler}.
 *  "true" or non "0" for true,
 *  and "false" or "0" for false.
 *   @see GuiReprValueBooleanCheckBox#getBooleanValue(String)
 */
public class GuiSwingViewBooleanCheckBox implements GuiSwingView {
    @Override
    @SuppressWarnings("rawtypes")
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        //wraps the check-box by a panel with flow layout: the size of the check-box becomes its preferred size.
        //  mouse clicks only pointing within bounds of the check-box will be processed
        JPanel pane = new GuiSwingViewWrapper.ValueWrappingPane(new FlowLayout(FlowLayout.LEADING));
        pane.setOpaque(false);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.add(new PropertyCheckBox(context, new SpecifierManagerDefault(parentSpecifier)));
        return pane;
    }

    public static class PropertyCheckBox extends JCheckBox
            implements ActionListener, GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Boolean> {
        private static final long serialVersionUID = 1L;
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected PopupExtension popup;
        protected MenuBuilder.MenuLabel infoLabel;

        protected GuiTaskClock viewClock = new GuiTaskClock(true);

        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected boolean editable;
        protected boolean lastValue;
        protected boolean currentValueSupported = true;

        protected int editing = 0;

        public PropertyCheckBox(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            init();
        }

        public void init() {
            initName();
            initEditable();
            initContextUpdate();
            initValue();
            initListener();
            initPopup();
            initDragDrop();
        }

        public void initName() {
            setName(context.getName());
            if (context.isTypeElementProperty()) {
                setText(context.getDisplayName());
            }
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
            GuiSwingView.setDescriptionToolTipText(context, this);
            setOpaque(false);
        }

        public void initEditable() {
            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();
            editable = repr.isEditable(context);
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initValue() {
            update(context, context.getSource().getValue(), context.getContextClock().copy());
        }

        public void initListener() {
            addActionListener(this);
        }

        public void initPopup() {
            popup = new PopupExtension(this, new PopupCategorized(this::getSwingStaticMenuItems));
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this);
            setInheritsPopupMenu(true);
        }

        public void initDragDrop() {
            GuiSwingView.setupTransferHandler(this, new BooleanTransferHandler(this));
        }

        @Override
        public boolean isSwingCurrentValueSupported() {
            return currentValueSupported && getSwingViewContext().isHistoryValueSupported();
        }

        public void setCurrentValueSupported(boolean currentValueSupported) {
            this.currentValueSupported = currentValueSupported;
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(
                        Arrays.asList(
                                infoLabel,
                                new ContextRefreshAction(context, this),
                                new ToStringCopyAction(this, context),
                                new BooleanPasteAction(this),
                                new GuiSwingHistoryMenu<>(this, getSwingViewContext()),
                                new BooleanFlipAction(this),
                                new BooleanSetValueAction(this, true),
                                new BooleanSetValueAction(this, false),
                                new BooleanTextLoadAction(this),
                                new BooleanTextSaveAction(this),
                        GuiSwingJsonTransfer.getActions(this, context)));
            }
            return menuItems;
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (editing <= 0) {
                ++editing;
                try {
                    if (editable) {
                        updateFromGui(isSelected(), viewClock.increment());
                    } else {
                        setSelected(lastValue);
                    }
                } finally {
                    --editing;
                }
            }
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            SwingUtilities.invokeLater(() -> setSwingViewValue((Boolean) newValue, contextClock));
        }

        @Override
        public Boolean getSwingViewValue() {
            return isSelected();
        }

        @Override
        public void setSwingViewValue(Boolean value) {
            viewClock.increment();
            setSwingViewValueWithoutClock(value);
        }

        private void setSwingViewValueWithoutClock(Boolean value) {
            lastValue = (value == null ? false : value);
            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();
            //setSelected seems not to cause ActionEvent
            setSelected(repr.toUpdateValue(context, value));
        }

        @Override
        public void setSwingViewValueWithUpdate(Boolean value) {
            viewClock.increment();
            setSwingViewValueWithUpdateWithoutClock(value);
        }

        private void setSwingViewValueWithUpdateWithoutClock(Boolean value) {
            updateFromGui(value, viewClock);
            setSwingViewValueWithoutClock(value);
        }

        public void updateFromGui(Object value, GuiTaskClock viewClock) {
            GuiSwingView.updateFromGui(this, value, viewClock);
        }

        @Override
        public void setSwingViewValue(Boolean value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                setSwingViewValueWithoutClock(value);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Boolean value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                setSwingViewValueWithUpdateWithoutClock(value);
            }
        }

        @Override
        public void addSwingEditFinishHandler(Runnable eventHandler) {
            addActionListener(e -> eventHandler.run());
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
        }

        public PopupExtension getPopup() {
            return popup;
        }

        @Override
        public void setKeyStrokeString(String keyStrokeString) {
            infoLabel.setAdditionalInfo(keyStrokeString);
        }

        public Boolean getValueFromString(String str) {
            return (Boolean) getSwingViewContext().getRepresentation()
                    .fromHumanReadableString(getSwingViewContext(), str);
        }

        public String getValueAsString(Object v) {
            return getSwingViewContext().getRepresentation()
                    .toHumanReadableString(getSwingViewContext(), v);
        }

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
        }
    }

    public abstract static class BooleanSetAction extends AbstractAction implements
            PopupCategorized.CategorizedMenuItemAction, TableTargetColumnAction {
        private static final long serialVersionUID = 1L;

        protected PropertyCheckBox pane;

        public BooleanSetAction(PropertyCheckBox pane) {
            this.pane = pane;
        }

        @Override
        public boolean isEnabled() {
            return pane.isSwingEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            pane.setSwingViewValueWithUpdate(getValue(pane.getSwingViewValue()));
        }

        public abstract Boolean getValue(Object exValue);

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            target.setSelectedCellValuesLoop(
                    target.getSelectedCellValues().stream()
                        .map(this::getValue)
                        .collect(Collectors.toList()));
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_SET;
        }
    }

    public static class BooleanFlipAction extends BooleanSetAction {
        private static final long serialVersionUID = 1L;

        public BooleanFlipAction(PropertyCheckBox pane) {
            super(pane);
            putValue(NAME, "Flip");
        }

        @Override
        public Boolean getValue(Object exValue) {
            return !(Boolean) exValue;
        }
    }

    public static class BooleanSetValueAction extends BooleanSetAction {
        private static final long serialVersionUID = 1L;

        protected boolean value;
        public BooleanSetValueAction(PropertyCheckBox pane, boolean v) {
            super(pane);
            this.value = v;
            putValue(NAME, "Set " + (v ? "True" : "False"));
        }

        @Override
        public Boolean getValue(Object exValue) {
            return value;
        }
    }

    public static class BooleanPasteAction extends PopupExtensionText.TextPasteAllAction
            implements TableTargetColumnAction {
        private static final long serialVersionUID = 1L;

        protected PropertyCheckBox checkBox;

        public BooleanPasteAction(PropertyCheckBox checkBox) {
            super(null);

            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_V,
                            PopupExtension.getMenuShortcutKeyMask()));
            this.checkBox = checkBox;
        }

        @Override
        public boolean isEnabled() {
            return checkBox.isSwingEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            paste(v -> checkBox.setSwingViewValueWithUpdate(getBoolean(v)));
        }

        public Boolean getBoolean(String str) {
            return checkBox.getValueFromString(str);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            pasteLines(lines ->
                    target.setSelectedCellValuesLoop(
                            lines.stream()
                                    .map(this::getBoolean)
                                    .collect(Collectors.toList())));
        }
    }

    public static class BooleanTextLoadAction extends PopupExtensionText.TextLoadAction
            implements TableTargetColumnAction {
        private static final long serialVersionUID = 1L;

        protected PropertyCheckBox checkBox;

        public BooleanTextLoadAction(PropertyCheckBox checkBox) {
            super(null);
            putValue(NAME, "Load Text...");
            this.checkBox = checkBox;
        }

        @Override
        public boolean isEnabled() {
            return checkBox != null && checkBox.isSwingEditable();
        }

        @Override
        protected JComponent getComponent() {
            return checkBox;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String str = load();
            if (str == null) {
                return;
            }
            Matcher m = Pattern.compile("\\n").matcher(str);
            if (m.find()) {
                str = str.substring(0, m.start());
            }
            checkBox.setSwingViewValueWithUpdate(checkBox.getValueFromString(str));
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            String str = load();
            if (str != null) {
                target.setSelectedCellValuesLoop(
                        Arrays.stream(str.split("\\n"))
                                .map(checkBox::getValueFromString)
                                .collect(Collectors.toList()));
            }
        }
    }

    public static class BooleanTextSaveAction extends PopupExtensionText.TextSaveAction
            implements TableTargetColumnAction {
        private static final long serialVersionUID = 1L;

        protected PropertyCheckBox checkBox;

        public BooleanTextSaveAction(PropertyCheckBox checkBox) {
            super(null);
            putValue(NAME, "Save Text...");
            this.checkBox = checkBox;
        }

        @Override
        protected JComponent getComponent() {
            return checkBox;
        }

        @Override
        public void save(Path path) {
            saveLines(path, Collections.singletonList(
                    checkBox.getValueAsString(checkBox.getSwingViewValue())));
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            Path path = getPath();
            if (path != null) {
                saveLines(path, target.getSelectedCellValues().stream()
                        .map(checkBox::getValueAsString)
                        .collect(Collectors.toList()));
            }
        }
    }

    public static class BooleanTransferHandler extends TransferHandler {
        private static final long serialVersionUID = 1L;

        protected PropertyCheckBox pane;

        public BooleanTransferHandler(PropertyCheckBox pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return pane.isSwingEditable() && pane.isEnabled() &&
                    support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }


        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor) && canImport(support)) {
                try {
                    String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    Boolean value = pane.getValueFromString(data);
                    if (value != null) {
                        pane.setSwingViewValueWithUpdate(value);
                        return true;
                    }
                    return false;
                } catch (Exception ex) {
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection(
                    pane.getValueAsString(pane.getSwingViewValue()));
        }
    }
}
