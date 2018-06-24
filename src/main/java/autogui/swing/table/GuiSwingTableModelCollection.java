package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiMappingContext.GuiSourceValue;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.base.mapping.GuiReprValue;
import autogui.base.mapping.GuiReprValue.ObjectSpecifier;
import autogui.swing.GuiSwingJsonTransfer;
import autogui.swing.GuiSwingPreferences;
import autogui.swing.GuiSwingPreferences.PreferencesUpdateEvent;
import autogui.swing.GuiSwingPreferences.PreferencesUpdateSupport;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingView.SettingsWindowClient;
import autogui.swing.GuiSwingViewCollectionTable;
import autogui.swing.GuiSwingViewCollectionTable.CollectionTable;
import autogui.swing.table.GuiSwingTableColumn.ObjectTableColumnWithContext;
import autogui.swing.table.GuiSwingTableColumn.SpecifierManagerIndex;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.PopupExtensionText;
import autogui.swing.util.SettingsWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * a table-model with {@link GuiMappingContext}
 */
public class GuiSwingTableModelCollection extends ObjectTableModel {

    protected GuiMappingContext context;
    protected GuiMappingContext elementContext;
    protected Supplier<ObjectSpecifier> tableSpecifier;
    protected SpecifierManagerIndex rowSpecifierManager;

    public GuiSwingTableModelCollection(GuiMappingContext context, Supplier<ObjectSpecifier> tableSpecifier,
                                      Supplier<Object> source) {
        this.context = context;
        this.tableSpecifier = tableSpecifier;
        this.rowSpecifierManager = new SpecifierManagerIndex(tableSpecifier);
        setElementContextFromContext();
        setSource(source);
    }

    @Override
    public void initColumns() {
        columns = new GuiSwingTableModelColumns(this);
    }

    public GuiSwingTableModelColumns getColumnsWithContext() {
        return (GuiSwingTableModelColumns) columns;
    }

    protected void setElementContextFromContext() {
        elementContext = context.getReprCollectionTable().getElementContext(context);
    }

    @Override
    public Object getRowAtIndex(int row) {
        Object collection = getCollectionFromSource();
        try {
            ObjectSpecifier specifier = rowSpecifierManager.getSpecifierWithSettingIndex(row);
            return elementContext.getReprValue()
                    .getValueWithoutNoUpdate(elementContext, GuiSourceValue.of(collection), specifier);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int getRowCountUpdated() {
        Object collection = getCollectionFromSource();
        try {
            return elementContext.getReprValue()
                    .getValueCollectionSize(elementContext, GuiSourceValue.of(collection), tableSpecifier.get());
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Object getCollectionFromSource() {
        Object collection = super.getCollectionFromSource();
        if (collection == null) {
            try {
                collection = context.getReprValue()
                        .getUpdatedValueWithoutNoUpdate(context, tableSpecifier.get());
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }
        return collection;
    }

    public SpecifierManagerIndex getRowSpecifierManager() {
        return rowSpecifierManager;
    }

    @Override
    public void columnAdded(ObjectTableColumn column) {
        super.columnAdded(column);
        JTable table = getTable();
        if (table instanceof CollectionTable) {
            ((CollectionTable) table).getPopup().setupCompositeKeyMapByAddingColumn(column);
        }
    }


    public static class GuiSwingTableModelColumns extends ObjectTableModelColumns
            implements PreferencesUpdateSupport, SettingsWindowClient {

        protected SettingsWindow settingsWindow;
        protected Consumer<PreferencesUpdateEvent> prefsUpdater;
        protected GuiPreferences currentPreferences;

        public GuiSwingTableModelColumns(ObjectTableModelColumnsListener updater) {
            super(updater);
        }

        @Override
        protected void columnAdded(ObjectTableColumn column, DynamicColumnContainer d) {
            super.columnAdded(column, d);

            if (settingsWindow != null && column instanceof SettingsWindowClient) {
                ((SettingsWindowClient) column).setSettingsWindow(settingsWindow);
            }
            if (prefsUpdater != null && column instanceof PreferencesUpdateSupport) {
                ((PreferencesUpdateSupport) column).setPreferencesUpdater(prefsUpdater);
            }
            if (currentPreferences != null && column instanceof ObjectTableColumnWithContext) {
                ((ObjectTableColumnWithContext) column).loadSwingPreferences(currentPreferences);
            }
        }

        @Override
        public void setSettingsWindow(SettingsWindow settingsWindow) {
            this.settingsWindow = settingsWindow;
            setColumns(SettingsWindowClient.class, c -> c.setSettingsWindow(settingsWindow));
        }

        private <T> void setColumns(Class<T> type, Consumer<T> setter) {
            columns.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .forEach(setter);
        }

        @Override
        public SettingsWindow getSettingsWindow() {
            return settingsWindow;
        }

        @Override
        public void setPreferencesUpdater(Consumer<PreferencesUpdateEvent> updater) {
            prefsUpdater = updater;
            setColumns(PreferencesUpdateSupport.class, c -> c.setPreferencesUpdater(updater));
        }


        public void loadSwingPreferences(GuiPreferences prefs) {
            currentPreferences = prefs;
            setColumns(ObjectTableColumnWithContext.class, c -> c.loadSwingPreferences(prefs));
        }

        public void saveSwingPreferences(GuiPreferences prefs) {
            setColumns(ObjectTableColumnWithContext.class, c -> c.saveSwingPreferences(prefs));
        }

        @Override
        public void addColumnRowIndex() {
            addColumnStatic(new ObjectTableColumnRowIndexWithActions());
        }
    }

    public static class ObjectTableColumnRowIndexWithActions
            extends ObjectTableColumn.ObjectTableColumnRowIndex implements ObjectTableColumn.PopupMenuBuilderSource {
        public ObjectTableColumnRowIndexWithActions() {
            withComparator(new GuiSwingTableColumnNumber.NumberComparator());
        }

        @Override
        public List<TableMenuComposite> getCompositesForRows() {
            int index = getTableColumn().getModelIndex();
            return Arrays.asList(
                    new ToStringCopyCell.TableMenuCompositeToStringCopy(index),
                    new ToStringCopyCell.TableMenuCompositeToStringPaste(index),
                    new GuiSwingJsonTransfer.TableMenuCompositeJsonCopy(index),
                    new GuiSwingJsonTransfer.TableMenuCompositeJsonPaste(index));
        }

        @Override
        public List<TableMenuComposite> getCompositesForCells() {
            int index = getTableColumn().getModelIndex();
            return Arrays.asList(
                    new ToStringCopyCell.TableMenuCompositeToStringCopy(index),
                    new ToStringCopyCell.TableMenuCompositeToStringPaste(index),
                    new GuiSwingJsonTransfer.TableMenuCompositeJsonCopy(index),
                    new GuiSwingJsonTransfer.TableMenuCompositeJsonPaste(index));
        }

        @Override
        public PopupMenuBuilderSource getMenuBuilderSource() {
            return this;
        }

        @Override
        public Consumer<Object> getMenuTargetPane() {
            return null;
        }

        @Override
        public PopupExtension.PopupMenuBuilder getMenuBuilder(JTable table) {
            return new ObjectTableColumnValue.ObjectTableColumnActionBuilder(table, this,
                    new PopupCategorized(() -> Collections.singletonList(new NumberCopyAction()), null,
                            new ObjectTableModel.MenuBuilderWithEmptySeparator()));
        }
    }

    /**
     * an action for copying an index number to the clip-board
     */
    public static class NumberCopyAction extends PopupExtensionText.TextCopyAllAction
            implements TableTargetColumnAction {

        public NumberCopyAction() {
            super(null);
            putValue(NAME, "Copy Indices");
            putValue(ACCELERATOR_KEY, null);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            actionPerformedOnTable(e, target.getSelectedCells().stream()
                    .map(GuiReprCollectionTable.CellValue::getValue)
                    .collect(Collectors.toList()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (src instanceof JLabel) {
                String text = ((JLabel) src).getText();
                copy(text);
            }
        }
    }
}
