package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueFilePathField;
import autogui.swing.util.NamedPane;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.SearchTextFieldFilePath;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuiSwingViewFilePathField implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyFilePathPane field = new PropertyFilePathPane(context);
        if (context.isTypeElementProperty()) {
            return new NamedPane(context.getDisplayName(), field);
        } else {
            return field;
        }
    }

    public static class PropertyFilePathPane extends SearchTextFieldFilePath
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane {
        protected GuiMappingContext context;

        public PropertyFilePathPane(GuiMappingContext context) {
            this.context = context;
            initLazy();

            getField().setEditable(((GuiReprValueFilePathField) context.getRepresentation())
                    .isEditable(context));

            context.addSourceUpdateListener(this);
            update(context, context.getSource());
            setInheritsPopupMenu(true);
        }

        @Override
        public void init() {
            //nothing
        }

        public void initLazy() {
            super.init();
        }

        @Override
        public List<? extends JComponent> getPopupEditMenuItems() {
            return Stream.concat(Stream.of(GuiSwingContextInfo.get().getInfoLabel(context)),
                    super.getPopupEditMenuItems().stream())
                    .collect(Collectors.toList());
        }

        @Override
        public void selectSearchedItemFromGui(PopupCategorized.CategorizedPopupItem item) {
            super.selectSearchedItemFromGui(item);
            //updated
            GuiReprValueFilePathField path = (GuiReprValueFilePathField) context.getRepresentation();
            path.updateFromGui(context, getFile());
        }

        @Override
        public void selectSearchedItemFromModel(PopupCategorized.CategorizedPopupItem item) {
            super.selectSearchedItemFromModel(item);
            //updated
            GuiReprValueFilePathField path = (GuiReprValueFilePathField) context.getRepresentation();
            path.updateFromGui(context, getFile());
        }

        public void selectSearchedItemWithoutUpdateContext(PopupCategorized.CategorizedPopupItem item) {
            super.selectSearchedItemFromGui(item);
            //no update callback
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return getFile();
        }

        @Override
        public void setSwingViewValue(Object value) {
            Path path = ((GuiReprValueFilePathField) context.getRepresentation())
                    .toUpdateValue(context, value);
            FileItem item = getFileItem(path);
            selectSearchedItemWithoutUpdateContext(item);
        }
    }
}
