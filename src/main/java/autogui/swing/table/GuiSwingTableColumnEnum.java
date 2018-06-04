package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.base.mapping.GuiReprValueEnumComboBox;
import autogui.swing.*;
import autogui.swing.util.PopupCategorized;

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
 *    The renderer is realized by {@link autogui.swing.GuiSwingViewLabel.PropertyLabel}.
 *    The editor is realized by {@link autogui.swing.GuiSwingViewEnumComboBox.PropertyEnumComboBox}.
 */
public class GuiSwingTableColumnEnum implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          GuiSwingView.SpecifierManager parentSpecifier) {
        GuiSwingView.SpecifierManager valueSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);
        GuiSwingViewLabel.PropertyLabel label = new ColumnEnumPane(context, valueSpecifier);

        GuiSwingViewEnumComboBox.PropertyEnumComboBox comboBox = new GuiSwingViewEnumComboBox.PropertyEnumComboBox(context, valueSpecifier);
        comboBox.setBorder(BorderFactory.createEmptyBorder());
        comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        ObjectTableColumnValue.ObjectTableCellEditor editor = new ObjectTableColumnValue.ObjectTableCellEditor(
                comboBox, false, rowSpecifier);
        editor.setClickCount(2);

        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                new ObjectTableColumnValue.ObjectTableCellRenderer(label, rowSpecifier),
                editor)
                .withComparator(Comparator.naturalOrder())
                .withValueType(Enum.class);
    }

    public static class ColumnEnumPane extends GuiSwingViewLabel.PropertyLabel {
        public ColumnEnumPane(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager) {
            super(context, specifierManager);
            setOpaque(true);
        }

        @Override
        public Object getValueFromString(String str) {
            return ((GuiReprValueEnumComboBox) getSwingViewContext().getRepresentation()).getEnumValue(getSwingViewContext(), str);
        }

        @Override
        public String getValueAsString(Object v) {
            if (v == null) {
                return "null";
            } else {
                return ((GuiReprValueEnumComboBox) context.getRepresentation()).getDisplayName(context, (Enum<?>) v);
            }
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                    menuItems = PopupCategorized.getMenuItems(Arrays.asList(
                            infoLabel,
                            new GuiSwingView.ContextRefreshAction(getSwingViewContext()),
                            new GuiSwingView.HistoryMenu<>(this, getSwingViewContext()),
                            new GuiSwingViewLabel.LabelToStringCopyAction(this),
                            new GuiSwingTableColumnString.LabelTextPasteAllAction(this),
                            new GuiSwingTableColumnString.LabelTextLoadAction(this),
                            new GuiSwingTableColumnString.LabelTextSaveAction(this),
                            new ColumnEnumSetMenu(this)
                    ), GuiSwingJsonTransfer.getActions(this, getSwingViewContext()));
            }
            return menuItems;
        }
    }

    public static class ColumnEnumSetMenu extends GuiSwingViewEnumComboBox.EnumSetMenu implements TableTargetMenu {
        public ColumnEnumSetMenu(GuiSwingView.ValuePane<Object> pane) {
            super(pane);
        }

        @Override
        public JMenu convert(GuiReprCollectionTable.TableTargetColumn target) {
            return new ColumnEnumSetMenuForTableColumn(pane, target);
        }
    }

    public static class ColumnEnumSetMenuForTableColumn extends GuiSwingViewEnumComboBox.EnumSetMenu {
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
