package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueBooleanCheckBox;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class GuiSwingViewBooleanCheckBox implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        //wraps the check-box by a panel with flow layout: the size of check-box becomes its preferred size.
        //  mouse clicks only pointing within bounds of the check-box will be processed
        JPanel pane = new GuiSwingView.ValueWrappingPane(new FlowLayout(FlowLayout.LEADING));
        pane.setOpaque(false);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.add(new PropertyCheckBox(context));
        return pane;
    }

    public static class PropertyCheckBox extends JCheckBox
            implements ActionListener, GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Boolean> {
        protected GuiMappingContext context;
        protected PopupExtension popup;

        public PropertyCheckBox(GuiMappingContext context) {
            addActionListener(this);
            this.context = context;
            setName(context.getName());

            //editable
            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();
            setEnabled(repr.isEditable(context));

            //context update
            context.addSourceUpdateListener(this);

            //set name label
            if (context.isTypeElementProperty()) {
                setText(context.getDisplayName());
            }

            //initial value
            update(context, context.getSource());

            //popup
            JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
            ContextRefreshAction refreshAction = new ContextRefreshAction(context);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.accept(info);
                menu.accept(refreshAction);
                GuiSwingJsonTransfer.getActions(this, context)
                        .forEach(menu::accept);
                menu.accept(new ToStringCopyAction(this, context));
                menu.accept(new HistoryMenu<>(this, getContext()));
            });
            setInheritsPopupMenu(true);

            //drag drop
            BooleanTransferHandler h = new BooleanTransferHandler(this);
            setTransferHandler(h);
            //TODO drag does not work properly
            DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, e -> {
                getTransferHandler().exportAsDrag(this, e.getTriggerEvent(), TransferHandler.COPY);
            });
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();
            repr.updateFromGui(context, isSelected());
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
            repr.updateFromGui(context, value);
            setSwingViewValue(value);
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            addActionListener(eventHandler::accept);
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
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
                    Boolean value = ((GuiReprValueBooleanCheckBox) pane.getContext().getRepresentation()).getBooleanValue(data);
                    if (value != null) {
                        pane.setSwingViewValue(value);
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
    }
}
