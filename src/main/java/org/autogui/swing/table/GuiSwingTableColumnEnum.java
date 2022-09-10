package org.autogui.swing.table;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprCollectionTable;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.*;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.GuiSwingView.SpecifierManagerDefault;
import org.autogui.swing.GuiSwingViewEnumComboBox.PropertyEnumComboBox;
import org.autogui.swing.GuiSwingViewEnumComboBox.PropertyLabelEnum;
import org.autogui.swing.table.ObjectTableColumnValue.ObjectTableCellEditor;
import org.autogui.swing.table.ObjectTableColumnValue.ObjectTableCellRenderer;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.TextCellRenderer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * a column factory for {@link Enum}.
 *
 * <p>
 *    The renderer is realized by {@link GuiSwingViewLabel.PropertyLabel}.
 *    The editor is realized by {@link PropertyEnumComboBox}.
 */
public class GuiSwingTableColumnEnum implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          SpecifierManager parentSpecifier) {
        SpecifierManager valueSpecifier = new SpecifierManagerDefault(parentSpecifier::getSpecifier);
        GuiSwingViewLabel.PropertyLabel label = new ColumnEnumPane(context, valueSpecifier);

        PropertyEnumComboBox comboBox = new ColumnEditEnumComboBox(context, valueSpecifier);
        ObjectTableCellEditor editor = new ObjectTableCellEditor(
                GuiSwingTableColumn.wrapEditor(comboBox), false, rowSpecifier);
        editor.setClickCount(1);

        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                new ObjectTableCellRenderer(label, rowSpecifier),
                editor)
                .withBorderType(ObjectTableColumnValue.CellBorderType.ComboBox)
                .withComparator(Comparator.naturalOrder())
                .withValueType(Enum.class);
    }

    public static class ColumnEnumPane extends PropertyLabelEnum {
        private static final long serialVersionUID = 1L;

        public ColumnEnumPane(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
            TextCellRenderer.setCellDefaultProperties(this);
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                    menuItems = PopupCategorized.getMenuItems(Arrays.asList(
                            infoLabel,
                            new GuiSwingView.ContextRefreshAction(getSwingViewContext(), this),
                            new GuiSwingHistoryMenu<>(this, getSwingViewContext()),
                            new GuiSwingViewLabel.LabelToStringCopyAction(this),
                            new GuiSwingTableColumnString.LabelTextPasteAllAction(this),
                            new GuiSwingTableColumnString.LabelTextLoadAction(this),
                            new GuiSwingTableColumnString.ColumnLabelTextSaveAction(this),
                            new ColumnEnumSetMenu(this)
                    ), GuiSwingJsonTransfer.getActions(this, getSwingViewContext()));
            }
            return menuItems;
        }
    }

    public static class ColumnEditEnumComboBox extends PropertyEnumComboBox {
        private static final long serialVersionUID = 1L;

        public ColumnEditEnumComboBox(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
            setCurrentValueSupported(false);
            TextCellRenderer.setCellDefaultProperties(this);
            setBorder(BorderFactory.createEmptyBorder());
            putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
            setOpaque(false);
        }

        @Override
        public void updateFromGui(Object value, GuiTaskClock viewClock) {
            //nothing
        }
    }

    public static class ColumnEnumSetMenu extends GuiSwingViewEnumComboBox.EnumSetMenu implements TableTargetMenu {
        private static final long serialVersionUID = 1L;

        public ColumnEnumSetMenu(GuiSwingView.ValuePane<Object> pane) {
            super(pane);
        }

        @Override
        public JMenu convert(GuiReprCollectionTable.TableTargetColumn target) {
            return new ColumnEnumSetMenuForTableColumn(pane, target);
        }
    }

    public static class ColumnEnumSetMenuForTableColumn extends GuiSwingViewEnumComboBox.EnumSetMenu {
        private static final long serialVersionUID = 1L;

        protected GuiReprCollectionTable.TableTargetColumn target;

        public ColumnEnumSetMenuForTableColumn(GuiSwingView.ValuePane<Object> pane, GuiReprCollectionTable.TableTargetColumn target) {
            super(pane);
            this.target = target;
            setItemsWithTarget();
        }

        @Override
        public void setItems() {
        }

        public void setItemsWithTarget() {
            super.setItems();
        }

        @Override
        public Action createItem(Object e) {
            return new ColumnEnumSetAction(pane, e, target);
        }
    }

    public static class ColumnEnumSetAction extends GuiSwingViewEnumComboBox.EnumSetAction {
        private static final long serialVersionUID = 1L;

        protected GuiReprCollectionTable.TableTargetColumn target;

        public ColumnEnumSetAction(GuiSwingView.ValuePane<Object> pane, Object value, GuiReprCollectionTable.TableTargetColumn target) {
            super(pane, value);
            this.target = target;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            target.setSelectedCellValuesLoop(Collections.singletonList(value));
        }
    }
}
