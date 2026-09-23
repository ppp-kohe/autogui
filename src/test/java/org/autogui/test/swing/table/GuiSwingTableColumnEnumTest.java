package org.autogui.test.swing.table;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.table.*;
import org.autogui.swing.util.PopupExtension;
import org.autogui.test.swing.GuiSwingTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuiSwingTableColumnEnumTest extends GuiSwingTestCase {

    GuiTypeBuilder builder;
    GuiMappingContext context;
    GuiTypeObject typeObject;
    TestObj obj;

    GuiMappingContext propContext;
    GuiMappingContext elemContext;
    GuiMappingContext enumContext;

    GuiSwingTableColumnEnum column;
    Supplier<GuiReprValue.ObjectSpecifier> spec;
    GuiSwingTableColumn.SpecifierManagerIndex specIndex;

    JFrame frame;
    JTable table;
    ObjectTableModel model;
    ObjectTableColumn objColumn;

    public GuiSwingTableColumnEnumTest() {
    }

    @BeforeEach
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();
        obj.values = new ArrayList<>();
        obj.values.add(TestEnum.Hello);
        obj.values.add(TestEnum.World);
        obj.values.add(TestEnum.Again);

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        propContext = context.getChildByName("values")
                .getChildByName("List");

        elemContext = propContext.getChildren().getFirst();
        enumContext = elemContext.getChildByName("TestEnum");

        column = new GuiSwingTableColumnEnum();
        spec = GuiReprValue.getNoneSupplier();
        specIndex = new GuiSwingTableColumn.SpecifierManagerIndex(spec, 0);
    }

    @AfterEach
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded
        public List<TestEnum> values;

        public TestObj() {
        }
    }

    public enum TestEnum {
        Hello,
        World,
        Again
    }

    JScrollPane createTable() {
        objColumn = column.createColumn(enumContext, specIndex, specIndex);

        model = new ObjectTableModel();
        model.setSource(() -> obj.values);
        model.getColumns().addColumnStatic(objColumn);

        JScrollPane scroll = model.initTableWithScroll();
        table = (JTable) scroll.getViewport().getView();
        frame = createFrame(scroll);
        return scroll;
    }

    @Test
    public void testCreateAndGet() {
        runGet(this::createTable);
        assertEquals(TestEnum.Hello, runGet(() -> table.getValueAt(0, 0)));
        assertEquals(TestEnum.World, runGet(() -> table.getValueAt(1, 0)));
        assertEquals(TestEnum.Again, runGet(() -> table.getValueAt(2, 0)));
        assertEquals(3, (int) runGet(table::getRowCount));
    }

    @Test
    public void testSort() {
        runGet(this::createTable);
        run(() -> table.getRowSorter().setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING))));
        runWait();

        assertEquals(TestEnum.Again, runGet(() -> table.getValueAt(0, 0)));
        assertEquals(TestEnum.World, runGet(() -> table.getValueAt(1, 0)));
        assertEquals(TestEnum.Hello, runGet(() -> table.getValueAt(2, 0)));
        assertEquals(3, (int) runGet(table::getRowCount));
    }

    @Test
    public void testEdit() {

        runGet(this::createTable);

        run(() -> table.editCellAt(1, 0));

        TableCellEditor editor = runGet(() -> objColumn.getTableColumn().getCellEditor());
        assertTrue(editor instanceof ObjectTableColumnValue.ObjectTableCellEditor);

        ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;
        assertEquals(TestEnum.World, runGet(objEditor::getCellEditorValue));

        GuiSwingTableColumnEnum.ColumnEditEnumComboBox component = runGetPane(GuiSwingTableColumnEnum.ColumnEditEnumComboBox.class, table::getEditorComponent);
        run(() -> component.setSelectedItem(TestEnum.Hello));
        runWait();

        run(objEditor::stopCellEditing);

        assertEquals(TestEnum.Hello, obj.values.get(1),
                "editor change");

    }

    @Test
    public void testAction() {
        runGet(this::createTable);
        ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                runGet(() -> objColumn.getTableColumn().getCellRenderer());
        GuiSwingTableColumnEnum.ColumnEnumPane pane = (GuiSwingTableColumnEnum.ColumnEnumPane) renderer.getComponent();
        GuiSwingTableColumnEnum.ColumnEnumSetMenuForTableColumn menu = (GuiSwingTableColumnEnum.ColumnEnumSetMenuForTableColumn)
                convertRowsAction(pane.getSwingStaticMenuItems().stream()
                        .filter(GuiSwingTableColumnEnum.ColumnEnumSetMenu.class::isInstance)
                        .map(GuiSwingTableColumnEnum.ColumnEnumSetMenu.class::cast)
                        .findFirst().orElseThrow(() -> new RuntimeException("not found")));
        run(() -> table.setRowSelectionInterval(0, 0));
        run(() -> table.addRowSelectionInterval(2, 2));
        run(() -> menu.getItem(2).getAction()
                .actionPerformed(null));
        runWait();

        System.err.println(obj.values);
        assertEquals(TestEnum.Again, obj.values.get(0),
                "set sub-menu action applied to selected rows");
        assertEquals(TestEnum.World, obj.values.get(1),
                "set sub-menu action applied to selected rows");
        assertEquals(TestEnum.Again, obj.values.get(2),
                "set sub-menu action applied to selected rows");
    }


    @Test
    public void testPasteAction() {
        withClipLock(() -> {
            runGet(this::createTable);
            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnEnum.ColumnEnumPane pane = (GuiSwingTableColumnEnum.ColumnEnumPane) renderer.getComponent();
            Action menu = (Action) convertRowsAction(
                    findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingTableColumnString.LabelTextPasteAllAction.class));

            setClipboardText(TestEnum.World.name() + "\n" + TestEnum.Hello.name());

            run(() -> table.setRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            run(() -> menu.actionPerformed(null));

            System.err.println(obj.values);
            assertEquals(TestEnum.World, obj.values.get(0),
                    "set paste action applied to selected rows");
            assertEquals(TestEnum.World, obj.values.get(1),
                    "set paste action applied to selected rows");
            assertEquals(TestEnum.Hello, obj.values.get(2),
                    "set paste action applied to selected rows");
        });
    }


    public Object convertRowsAction(Object menu) {
        return new ObjectTableColumnValue.CollectionRowsActionBuilder(table, objColumn, PopupExtension.MENU_FILTER_IDENTITY).convert(menu);
    }
}
