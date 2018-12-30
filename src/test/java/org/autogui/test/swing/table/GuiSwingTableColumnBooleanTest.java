package org.autogui.test.swing.table;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingViewBooleanCheckBox;
import org.autogui.swing.table.*;
import org.autogui.swing.util.PopupExtension;
import org.autogui.test.swing.GuiSwingTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class GuiSwingTableColumnBooleanTest extends GuiSwingTestCase {

    GuiTypeBuilder builder;
    GuiMappingContext context;
    GuiTypeObject typeObject;
    TestObj obj;

    GuiMappingContext propContext;
    GuiMappingContext elemContext;
    GuiMappingContext boolContext;

    GuiSwingTableColumnBoolean column;
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
        obj.values.add(true);
        obj.values.add(false);
        obj.values.add(true);

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        propContext = context.getChildByName("values")
                .getChildByName("List");

        elemContext = propContext.getChildren().get(0);
        boolContext = elemContext.getChildByName("Boolean");

        column = new GuiSwingTableColumnBoolean();
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
        public List<Boolean> values;
    }



    JScrollPane createTable() {
        objColumn = column.createColumn(boolContext, specIndex, specIndex);

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
        Assert.assertEquals(true, runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(false, runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(true, runGet(() -> table.getValueAt(2, 0)));
        Assert.assertEquals(3, (int) runGet(table::getRowCount));
    }


    @Test
    public void testSort() {
        runGet(this::createTable);
        run(() -> table.getRowSorter().setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING))));
        runWait();

        Assert.assertEquals(true, runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(true, runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(false, runGet(() -> table.getValueAt(2, 0)));
        Assert.assertEquals(3, (int) runGet(table::getRowCount));
    }

    @Test
    public void testEdit() {
        runGet(this::createTable);

        run(() -> table.editCellAt(1, 0));

        TableCellEditor editor = runGet(() -> objColumn.getTableColumn().getCellEditor());
        Assert.assertTrue(editor instanceof ObjectTableColumnValue.ObjectTableCellEditor);

        ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;
        Assert.assertEquals(false, runGet(objEditor::getCellEditorValue));

        GuiSwingTableColumnBoolean.ColumnCheckBox component = (GuiSwingTableColumnBoolean.ColumnCheckBox) runGet(table::getEditorComponent);
        run(() -> component.setSelected(true));
        runWait();

        run(objEditor::stopCellEditing);

        Assert.assertEquals("editor change",
                true, obj.values.get(1));

        Rectangle rect = runGet(() -> table.getCellRect(0, 0, true));
        Point clickPoint = new Point(
                (int) rect.getCenterX(),
                (int) (rect.getCenterY()));
        System.err.println(clickPoint);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                System.err.println("pressed " + e);
            }
        });

        //click the top cell: (0,0) means top-left of first cell.
        run(() -> table.dispatchEvent(new MouseEvent(
                table, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0,
                clickPoint.x, clickPoint.y, 1, false, MouseEvent.BUTTON1)));
        run(() -> table.dispatchEvent(new MouseEvent(
                table, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0,
                clickPoint.x, clickPoint.y, 1, false, MouseEvent.BUTTON1)));
        runWait();
        Assert.assertEquals("editor click change",
                false, obj.values.get(0));
    }

    @Test
    public void testBooleanSetAction() {
        runGet(this::createTable);
        ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                runGet(() -> objColumn.getTableColumn().getCellRenderer());

        GuiSwingTableColumnBoolean.ColumnCheckBox component = (GuiSwingTableColumnBoolean.ColumnCheckBox) runGet(renderer::getComponent);
        GuiSwingViewBooleanCheckBox.BooleanSetValueAction valueAct =
                findMenuItemAction(component.getSwingStaticMenuItems(), GuiSwingViewBooleanCheckBox.BooleanSetValueAction.class);
        Boolean v = valueAct.getValue((Object) null); //not getValue(String key)
        Action action = convertRowsAction(valueAct);

        System.err.println(v);
        run(() -> table.setRowSelectionInterval(1, 1));
        run(() -> table.addRowSelectionInterval(2, 2));
        run(() -> action.actionPerformed(null));


        Assert.assertEquals(true, runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(v, runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(v, runGet(() -> table.getValueAt(2, 0)));
    }

    @Test
    public void testBooleanPasteAction() {
        runGet(this::createTable);
        ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                runGet(() -> objColumn.getTableColumn().getCellRenderer());

        GuiSwingTableColumnBoolean.ColumnCheckBox component = (GuiSwingTableColumnBoolean.ColumnCheckBox) runGet(renderer::getComponent);
        GuiSwingViewBooleanCheckBox.BooleanPasteAction valueAct =
                findMenuItemAction(component.getSwingStaticMenuItems(), GuiSwingViewBooleanCheckBox.BooleanPasteAction.class);
        Action action = convertRowsAction(valueAct);

        setClipboardText("true\nfalse");

        run(() -> table.setRowSelectionInterval(1, 1));
        run(() -> table.addRowSelectionInterval(2, 2));
        run(() -> action.actionPerformed(null));

        Assert.assertEquals(true, runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(true, runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(false, runGet(() -> table.getValueAt(2, 0)));
    }

    public Action convertRowsAction(Object menu) {
        return (Action) new ObjectTableColumnValue.CollectionRowsActionBuilder(table, objColumn, PopupExtension.MENU_FILTER_IDENTITY).convert(menu);
    }
}

