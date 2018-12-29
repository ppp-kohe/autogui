package org.autogui.test.swing.table;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingViewLabel;
import org.autogui.swing.table.*;
import org.autogui.swing.util.PopupExtension;
import org.autogui.test.swing.GuiSwingTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

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

    @Before
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

        elemContext = propContext.getChildren().get(0);
        enumContext = elemContext.getChildByName("TestEnum");

        column = new GuiSwingTableColumnEnum();
        spec = GuiReprValue.getNoneSupplier();
        specIndex = new GuiSwingTableColumn.SpecifierManagerIndex(spec, 0);
    }

    @After
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
        Assert.assertEquals(TestEnum.Hello, runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(TestEnum.World, runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(TestEnum.Again, runGet(() -> table.getValueAt(2, 0)));
        Assert.assertEquals(3, (int) runGet(table::getRowCount));
    }

    @Test
    public void testSort() {
        runGet(this::createTable);
        run(() -> table.getRowSorter().setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING))));
        runWait();

        Assert.assertEquals(TestEnum.Again, runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(TestEnum.World, runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(TestEnum.Hello, runGet(() -> table.getValueAt(2, 0)));
        Assert.assertEquals(3, (int) runGet(table::getRowCount));
    }

    @Test
    public void testEdit() {

        runGet(this::createTable);

        run(() -> table.editCellAt(1, 0));

        TableCellEditor editor = runGet(() -> objColumn.getTableColumn().getCellEditor());
        Assert.assertTrue(editor instanceof ObjectTableColumnValue.ObjectTableCellEditor);

        ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;
        Assert.assertEquals(TestEnum.World, runGet(objEditor::getCellEditorValue));

        GuiSwingTableColumnEnum.ColumnEditEnumComboBox component = (GuiSwingTableColumnEnum.ColumnEditEnumComboBox) runGet(table::getEditorComponent);
        run(() -> component.setSelectedItem(TestEnum.Hello));
        runWait();

        run(objEditor::stopCellEditing);

        Assert.assertEquals("editor change",
                TestEnum.Hello, obj.values.get(1));

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
        Assert.assertEquals("set sub-menu action applied to selected rows",
                TestEnum.Again, obj.values.get(0));
        Assert.assertEquals("set sub-menu action applied to selected rows",
                TestEnum.World, obj.values.get(1));
        Assert.assertEquals("set sub-menu action applied to selected rows",
                TestEnum.Again, obj.values.get(2));
    }

    public Object convertRowsAction(Object menu) {
        return new ObjectTableColumnValue.CollectionRowsActionBuilder(table, objColumn, PopupExtension.MENU_FILTER_IDENTITY).convert(menu);
    }
}
