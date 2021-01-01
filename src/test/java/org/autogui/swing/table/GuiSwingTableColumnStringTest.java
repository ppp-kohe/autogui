package org.autogui.swing.table;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingViewLabel;
import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.GuiSwingTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class GuiSwingTableColumnStringTest extends GuiSwingTestCase {
//
//    public static void main(String[] args) {
//        GuiSwingTableColumnStringTest t = new GuiSwingTableColumnStringTest();
//        t.setUp();
//        t.testCreateAndGet();
//    }
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

    ObjectTableColumn objColumn;

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
        objColumn = column.createColumn(strContext, specIndex, specIndex);

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
        Assert.assertEquals("reflects model update by refreshData",
                "WORLD", runGet(() -> table.getValueAt(1, 0)));
    }

    @Test
    public void testSort() {
        runGet(this::createTable);
        run(() -> table.getRowSorter().setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING))));
        runWait();

        Assert.assertEquals("world", runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals("hello", runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals("!!!!", runGet(() -> table.getValueAt(2, 0)));
        Assert.assertEquals(3, (int) runGet(table::getRowCount));
    }

    @Test
    public void testEdit() {
        runGet(this::createTable);
        run(() -> table.editCellAt(1, 0));

        TableCellEditor editor = runGet(() -> objColumn.getTableColumn().getCellEditor());
        Assert.assertTrue(editor instanceof ObjectTableColumnValue.ObjectTableCellEditor);

        ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;
        Assert.assertEquals("world", runGet(objEditor::getCellEditorValue));

        GuiSwingTableColumnString.MultilineColumnTextPane component = ((GuiSwingTableColumnString.MultilineColumnTextPane.MultilineColumnScrollPane) runGet(table::getEditorComponent)).getColumnTextPane();
        run(() -> component.setBackground(Color.black));
        run(() -> component.setForeground(Color.gray));

        Assert.assertEquals("editorPane.setBackground also sets field background", Color.black, runGet(() -> component.getBackground()));
        Assert.assertEquals("editorPane.setForeground also sets field foreground", Color.gray, runGet(() -> component.getForeground()));

        run(() -> component.setText("EDIT"));
        runWait();
        run(objEditor::stopCellEditing);

        Assert.assertEquals("hello", obj.values.get(0));
        Assert.assertEquals("setText affects the target cell", "EDIT", obj.values.get(1));
        Assert.assertEquals("!!!!", obj.values.get(2));
        Assert.assertEquals(3, obj.values.size());
    }

    public Action convertRowsAction(Object menu) {
        return (Action) new ObjectTableColumnValue.CollectionRowsActionBuilder(table, objColumn, PopupExtension.MENU_FILTER_IDENTITY).convert(menu);
    }

    @Test
    public void testActionToStringCopy() {
        withClipLock(() -> {
            runGet(this::createTable);

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnString.MultilineColumnTextPane pane = (GuiSwingTableColumnString.MultilineColumnTextPane) renderer.getComponent();
            Action action = convertRowsAction(
                    findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingViewLabel.LabelToStringCopyAction.class));

            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            run(() -> action.actionPerformed(null));
            Assert.assertEquals("only copies selected rows",
                    "hello\n!!!!", getClipboardText());
        });
    }

    @Test
    public void testActionPasteAll() {
        withClipLock(() -> {
            runGet(this::createTable);

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnString.MultilineColumnTextPane pane = (GuiSwingTableColumnString.MultilineColumnTextPane) renderer.getComponent();

            Action action = convertRowsAction(findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingTableColumnString.LabelTextPasteAllAction.class));
            setClipboardText("HELLO\nWORLD");
            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            runWait();
            run(() -> action.actionPerformed(null));

            Assert.assertEquals("paste 1st line to 1st selected row",
                    "HELLO", runGet(() -> table.getValueAt(0, 0)));
            Assert.assertEquals("world", runGet(() -> table.getValueAt(1, 0)));
            Assert.assertEquals("paste 2nd line to 2nd selected row",
                    "WORLD", runGet(() -> table.getValueAt(2, 0)));
        });
    }
}
