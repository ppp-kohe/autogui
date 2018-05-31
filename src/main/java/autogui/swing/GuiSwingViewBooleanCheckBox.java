package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.mapping.GuiReprValueBooleanCheckBox;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        //wraps the check-box by a panel with flow layout: the size of the check-box becomes its preferred size.
        //  mouse clicks only pointing within bounds of the check-box will be processed
        JPanel pane = new GuiSwingView.ValueWrappingPane(new FlowLayout(FlowLayout.LEADING));
        pane.setOpaque(false);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.add(new PropertyCheckBox(context, new SpecifierManagerDefault(parentSpecifier)));
        return pane;
    }

    public static class PropertyCheckBox extends JCheckBox
            implements ActionListener, GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Boolean> {
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected PopupExtension popup;
        protected MenuBuilder.MenuLabel infoLabel;

        protected List<PopupCategorized.CategorizedMenuItem> menuItems;

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
            setEnabled(repr.isEditable(context));
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initValue() {
            update(context, context.getSource().getValue());
        }

        public void initListener() {
            addActionListener(this);
        }

        public void initPopup() {
            popup = new PopupExtension(this, new PopupCategorized(this::getSwingStaticMenuItems));
            setInheritsPopupMenu(true);
        }

        public void initDragDrop() {
            GuiSwingView.setupTransferHandler(this, new BooleanTransferHandler(this));
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(
                        Arrays.asList(
                                infoLabel,
                                new ContextRefreshAction(context),
                                new ToStringCopyAction(this, context),
                                new HistoryMenu<>(this, getSwingViewContext()),
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
            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();
            repr.updateFromGui(context, isSelected(), getSpecifier());
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue((Boolean) newValue));
        }

        @Override
        public Boolean getSwingViewValue() {
            return isSelected();
        }

        @Override
        public void setSwingViewValue(Boolean value) {
            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();
            //setSelected seems not to cause ActionEvent
            setSelected(repr.toUpdateValue(context, value));
        }

        @Override
        public void setSwingViewValueWithUpdate(Boolean value) {
            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();
            if (repr.isEditable(context)) {
                repr.updateFromGui(context, value, getSpecifier());
            }
            setSwingViewValue(value);
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            addActionListener(eventHandler::accept);
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
    }

    public static class BooleanTransferHandler extends TransferHandler {
        protected PropertyCheckBox pane;

        public BooleanTransferHandler(PropertyCheckBox pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return pane.isEnabled() &&
                    support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }


        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    Boolean value = ((GuiReprValueBooleanCheckBox) pane.getSwingViewContext().getRepresentation()).getBooleanValue(data);
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
                    pane.getSwingViewContext().getRepresentation()
                            .toHumanReadableString(pane.getSwingViewContext(), pane.getSwingViewValue()));
        }
    }
}
