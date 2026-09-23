package org.autogui.test.swing.table;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingViewLabel;
import org.autogui.swing.table.*;
import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.util.PopupExtensionText;
import org.autogui.test.swing.GuiSwingTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    public GuiSwingTableColumnStringTest() {
    }

    @BeforeEach
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

        elemContext = propContext.getChildren().getFirst();
        strContext = elemContext.getChildByName("String");


        column = new GuiSwingTableColumnString();
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
        public List<String> values;

        public TestObj() {
        }
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
        assertEquals("hello", runGet(() -> table.getValueAt(0, 0)), "hello");
        assertEquals("world", runGet(() -> table.getValueAt(1, 0)), "world");
        assertEquals(3, (int) runGet(table::getRowCount));

        GuiReprValue.ObjectSpecifier colSpec = specIndex.getSpecifier();
        assertTrue(colSpec.isIndex());
        assertEquals(2, colSpec.getIndex(),
                "specifier returned by specIndex points last used index");
    }

    @Test
    public void testUpdate() {
        runGet(this::createTable);
        runWait();
        obj.values.set(1, "WORLD");
        runWait();
        run(model::refreshData);
        runWait();
        assertEquals("hello", runGet(() -> table.getValueAt(0, 0)), "hello");
        assertEquals("WORLD", runGet(() -> table.getValueAt(1, 0)),
                "reflects model update by refreshData");
    }

    @Test
    public void testSort() {
        runGet(this::createTable);
        run(() -> table.getRowSorter().setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING))));
        runWait();

        assertEquals("world", runGet(() -> table.getValueAt(0, 0)), "world");
        assertEquals("hello", runGet(() -> table.getValueAt(1, 0)), "hello");
        assertEquals("!!!!", runGet(() -> table.getValueAt(2, 0)), "!!!!");
        assertEquals(3, (int) runGet(table::getRowCount));
    }

    @Test
    public void testEdit() {
        runGet(this::createTable);
        run(() -> table.editCellAt(1, 0));

        TableCellEditor editor = runGet(() -> objColumn.getTableColumn().getCellEditor());
        assertTrue(editor instanceof ObjectTableColumnValue.ObjectTableCellEditor);

        ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;
        assertEquals("world", runGet(objEditor::getCellEditorValue), "world");

        GuiSwingTableColumnString.MultilineColumnTextPane component = runGetPane(GuiSwingTableColumnString.MultilineColumnTextPane.MultilineColumnScrollPane.class, table::getEditorComponent).getColumnTextPane();
        run(() -> component.setBackground(Color.black));
        run(() -> component.setForeground(Color.gray));

        assertEquals(Color.black, runGet(component::getBackground), "editorPane.setBackground also sets field background");
        assertEquals(Color.gray, runGet(component::getForeground), "editorPane.setForeground also sets field foreground");

        run(() -> component.setText("EDIT"));
        runWait();
        run(objEditor::stopCellEditing);

        assertEquals("hello", obj.values.get(0), "hello");
        assertEquals("EDIT", obj.values.get(1), "setText affects the target cell");
        assertEquals("!!!!", obj.values.get(2), "!!!!");
        assertEquals(3, obj.values.size());
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
                    findMenuItemAction(pane.getSwingStaticMenuItems(), PopupExtensionText.TextCopyAllAction.class));
            if (action == null) {
                action = convertRowsAction(
                        findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingViewLabel.LabelToStringCopyAction.class));
            }

            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            Action copyAction = action;
            run(() -> copyAction.actionPerformed(null));
            assertEquals("hello\n!!!!", getClipboardText(),
                    "only copies selected rows");
        });
    }

    @Test
    public void testActionPasteAll() {
        withClipLock(() -> {
            runGet(this::createTable);

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnString.MultilineColumnTextPane pane = (GuiSwingTableColumnString.MultilineColumnTextPane) renderer.getComponent();

            Action action = convertRowsAction(findMenuItemAction(pane.getSwingStaticMenuItems(), PopupExtensionText.TextPasteAllAction.class));
            if (action == null) {
                action = convertRowsAction(findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingTableColumnString.LabelTextPasteAllAction.class));
            }

            setClipboardText("HELLO\nWORLD");
            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            runWait();
            Action pasteAction = action;
            run(() -> pasteAction.actionPerformed(null));

            assertEquals("HELLO", runGet(() -> table.getValueAt(0, 0)),
                    "paste 1st line to 1st selected row");
            assertEquals("world", runGet(() -> table.getValueAt(1, 0)), "world");
            assertEquals("WORLD", runGet(() -> table.getValueAt(2, 0)),
                    "paste 2nd line to 2nd selected row");
        });
    }
}
