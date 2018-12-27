package org.autogui.test.swing.table;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.table.*;
import org.autogui.test.swing.GuiSwingTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GuiSwingTableColumnStringTest extends GuiSwingTestCase {

    public static void main(String[] args) {
        GuiSwingTableColumnStringTest t = new GuiSwingTableColumnStringTest();
        t.setUp();
        t.testCreateAndGet();
    }
    GuiSwingTableColumnString column;

    GuiTypeBuilder builder;
    GuiMappingContext context;
    GuiTypeObject typeObject;
    TestObj obj;

    GuiMappingContext propContext;
    GuiMappingContext elemContext;
    GuiMappingContext strContext;

    JFrame frame;
    JTable table;
    ObjectTableModel model;

    Supplier<GuiReprValue.ObjectSpecifier> spec;
    GuiSwingTableColumn.SpecifierManagerIndex specIndex;

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();
        obj.values = new ArrayList<>();
        obj.values.add("hello");
        obj.values.add("world");
        obj.values.add("!!!!");

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        propContext = context.getChildByName("values")
                .getChildByName("List");

        elemContext = propContext.getChildren().get(0);
        strContext = elemContext.getChildByName("String");


        column = new GuiSwingTableColumnString();
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
        public List<String> values;
    }

    JScrollPane createTable() {
        ObjectTableColumn col = column.createColumn(strContext, specIndex, specIndex);

        model = new ObjectTableModel();
        model.setSource(() -> obj.values);
        model.getColumns().addColumnStatic(col);

        JScrollPane scroll = model.initTableWithScroll();
        table = (JTable) scroll.getViewport().getView();
        frame = createFrame(scroll);
        return scroll;
    }

    @Test
    public void testCreateAndGet() {
        runGet(this::createTable);
        Assert.assertEquals("hello", runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals("world", runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(3, (int) runGet(table::getRowCount));

        GuiReprValue.ObjectSpecifier colSpec = specIndex.getSpecifier();
        Assert.assertTrue(colSpec.isIndex());
        Assert.assertEquals("specifier returned by specIndex points last used index",
                2, colSpec.getIndex());
    }

    @Test
    public void testUpdate() {
        runGet(this::createTable);
        runWait();
        obj.values.set(1, "WORLD");
        runWait();
        run(model::refreshData);
        runWait();
        Assert.assertEquals("hello", runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals("WORLD", runGet(() -> table.getValueAt(1, 0)));
    }

    @Test
    public void testEdit() {
        runGet(this::createTable);
        run(() -> table.editCellAt(1, 0));

        TableCellEditor editor = runGet(() -> model.getColumnAt(0).getTableColumn().getCellEditor());
        Assert.assertTrue(editor instanceof ObjectTableColumnValue.ObjectTableCellEditor);

        ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;
        Assert.assertEquals("world", runGet(objEditor::getCellEditorValue));

        Component component = runGet(table::getEditorComponent);
        run(() -> ((GuiSwingTableColumnString.ColumnEditTextPane) component).getField().setText("EDIT"));
        runWait();
        run(objEditor::stopCellEditing);

        Assert.assertEquals("hello", obj.values.get(0));
        Assert.assertEquals("EDIT", obj.values.get(1));
        Assert.assertEquals("!!!!", obj.values.get(2));
        Assert.assertEquals(3, obj.values.size());
    }

    @Test
    public void testAction() {
        runGet(this::createTable);

        GuiSwingTableColumnString.ColumnTextPane pane = (GuiSwingTableColumnString.ColumnTextPane) runGet(() -> model.getColumns().getColumnAt(0).getTableColumn().getCellRenderer());
        run(() -> pane.getSwingStaticMenuItems().get(0));
    }
}
