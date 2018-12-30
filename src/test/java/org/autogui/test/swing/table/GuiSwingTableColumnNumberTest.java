package org.autogui.test.swing.table;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingViewNumberSpinner;
import org.autogui.swing.table.*;
import org.autogui.swing.util.PopupExtension;
import org.autogui.test.swing.GuiSwingTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class GuiSwingTableColumnNumberTest extends GuiSwingTestCase {

    GuiTypeBuilder builder;
    GuiMappingContext context;
    GuiTypeObject typeObject;
    TestObj obj;

    GuiMappingContext propContext;
    GuiMappingContext elemContext;
    GuiMappingContext numContext;

    GuiSwingTableColumnNumber column;
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
        obj.values.add(1230);
        obj.values.add(4560);
        obj.values.add(7890);

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        propContext = context.getChildByName("values")
                .getChildByName("List");

        elemContext = propContext.getChildren().get(0);
        numContext = elemContext.getChildByName("Integer");

        column = new GuiSwingTableColumnNumber();
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
        public List<Integer> values;
    }


    JScrollPane createTable() {
        objColumn = column.createColumn(numContext, specIndex, specIndex);

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
        Assert.assertEquals(1230, runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(4560, runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(3, (int) runGet(table::getRowCount));
    }

    @Test
    public void testSort() {
        runGet(this::createTable);
        run(() -> table.getRowSorter().setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING))));
        runWait();

        Assert.assertEquals(7890, runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(4560, runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(1230, runGet(() -> table.getValueAt(2, 0)));
        Assert.assertEquals(3, (int) runGet(table::getRowCount));
    }

    @Test
    public void testEdit() {
        runGet(this::createTable);
        run(() -> table.editCellAt(1, 0));

        TableCellEditor editor = runGet(() -> objColumn.getTableColumn().getCellEditor());
        Assert.assertTrue(editor instanceof ObjectTableColumnValue.ObjectTableCellEditor);

        ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;
        Assert.assertEquals(4560, runGet(objEditor::getCellEditorValue));

        GuiSwingTableColumnNumber.ColumnEditNumberSpinner component = (GuiSwingTableColumnNumber.ColumnEditNumberSpinner) runGet(table::getEditorComponent);
        run(() -> component.getModelTyped().setFormatPattern("####"));
        run(() -> component.setValue(12345));
        runWait();
        run(objEditor::stopCellEditing);

        Assert.assertEquals(1230, (int) obj.values.get(0));
        Assert.assertEquals("editor change",
                12345, (int) obj.values.get(1));

        ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                runGet(() -> objColumn.getTableColumn().getCellRenderer());
        GuiSwingTableColumnNumber.ColumnNumberPane numberPane = (GuiSwingTableColumnNumber.ColumnNumberPane) runGet(renderer::getComponent);
        Assert.assertEquals("editor's setting change reflects renderer's setting",
                "####", numberPane.getModelTyped().getFormatPattern());
    }

    @Test
    public void testEditIncrementAction() {
        runGet(this::createTable);

        ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                runGet(() -> objColumn.getTableColumn().getCellRenderer());

        GuiSwingTableColumnNumber.ColumnNumberPane component = (GuiSwingTableColumnNumber.ColumnNumberPane)
                runGet(renderer::getComponent);
        Action action = convertRowsAction(findMenuItem(component.getSwingStaticMenuItems(), GuiSwingViewNumberSpinner.NumberIncrementAction.class,
                null, null, null, GuiSwingViewNumberSpinner.NumberIncrementAction::isInc));

        run(() -> table.setRowSelectionInterval(0, 0));
        run(() -> table.addRowSelectionInterval(2, 2));
        run(() -> action.actionPerformed(null));

        Assert.assertEquals(1231, runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(4560, runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(7891, runGet(() -> table.getValueAt(2, 0)));
    }


    public Action convertRowsAction(Object menu) {
        return (Action) new ObjectTableColumnValue.CollectionRowsActionBuilder(table, objColumn, PopupExtension.MENU_FILTER_IDENTITY).convert(menu);
    }
}
