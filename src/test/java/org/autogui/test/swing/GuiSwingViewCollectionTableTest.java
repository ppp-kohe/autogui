package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.GuiListSelectionCallback;
import org.autogui.GuiListSelectionUpdater;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.*;
import org.autogui.swing.table.GuiSwingTableColumnCollection;
import org.autogui.swing.util.ToolBarHiddenMenu;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class GuiSwingViewCollectionTableTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        GuiSwingViewCollectionTableTest test = new GuiSwingViewCollectionTableTest();
        test.setUp();
        test.testViewCollectionSelectAction();
    }

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;
    TestObj obj;

    GuiMappingContext context;
    GuiMappingContext propContext;

    //------------------

    GuiTypeObject typeValues;
    TestValues values;
    GuiMappingContext valuesContext;
    GuiMappingContext valuesPropContext;

    //------------------

    GuiTypeObject typeMatrix;
    TestMatrix matrix;
    GuiMappingContext matrixContext;
    GuiMappingContext matrixPropContext;

    //------------------

    GuiTypeObject typeObjMatrix;
    TestObjMatrix objMatrix;
    GuiMappingContext objMatrixContext;
    GuiMappingContext objMatrixPropContext;

    //------------------

    GuiTypeObject typeObjAction;
    TestObjAction objAction;
    GuiMappingContext objActionContext;
    GuiMappingContext objActionListContext;
    GuiMappingContext objActionMatrixContext;

    //------------------

    GuiSwingViewCollectionTable table;

    JFrame frame;

    public GuiSwingViewCollectionTableTest() {
    }

    @BeforeEach
    public void setUp() {
        timeFactor = 0.3;
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        table = new GuiSwingViewCollectionTable(GuiSwingMapperSet.getDefaultMapperSet());

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);
        propContext = context.getChildByName("value").getChildByName("List");

        //------------------

        typeValues = (GuiTypeObject) builder.get(TestValues.class);

        values = new TestValues();
        valuesContext = new GuiMappingContext(typeValues, values);
        GuiSwingMapperSet.getReprDefaultSet().match(valuesContext);
        valuesPropContext = valuesContext.getChildByName("value").getChildByName("List");

        //------------------

        typeMatrix = (GuiTypeObject) builder.get(TestMatrix.class);

        matrix = new TestMatrix();
        matrixContext = new GuiMappingContext(typeMatrix, matrix);
        GuiSwingMapperSet.getReprDefaultSet().match(matrixContext);
        matrixPropContext = matrixContext.getChildByName("value").getChildByName("List");

        //------------------

        typeObjMatrix = (GuiTypeObject) builder.get(TestObjMatrix.class);

        objMatrix = new TestObjMatrix();
        objMatrixContext = new GuiMappingContext(typeObjMatrix, objMatrix);
        GuiSwingMapperSet.getReprDefaultSet().match(objMatrixContext);
        objMatrixPropContext = objMatrixContext.getChildByName("value").getChildByName("List");

        //------------------

        typeObjAction = (GuiTypeObject) builder.get(TestObjAction.class);
        objAction = new TestObjAction();
        objActionContext = new GuiMappingContext(typeObjAction, objAction);
        GuiSwingMapperSet.getReprDefaultSet().match(objActionContext);
        objActionListContext = objActionContext.getChildByName("list").getChildByName("List");
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
        public List<TestRow> value = new ArrayList<>();

        public Set<TestRow> selectedItems = new HashSet<>();

        @GuiListSelectionCallback
        @GuiIncluded
        public void select(List<TestRow> ss) {
            System.err.println(ss);
            selectedItems.addAll(ss);
        }

        public TestObj() {
        }
    }

    @GuiIncluded
    public static class TestRow {
        @GuiIncluded
        public String name;
        @GuiIncluded
        public String value;

        public int actionCount;

        public TestRow(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @GuiIncluded
        public void action() {
            actionCount++;
        }

        @Override
        public String toString() {
            return "TestRow(" + name + "," + value + ")";
        }
    }

    //------------------

    @GuiIncluded
    public static class TestValues {
        @GuiIncluded
        public List<String> value = new ArrayList<>();

        public TestValues() {
        }
    }

    //------------------

    @GuiIncluded
    public static class TestMatrix {
        @GuiIncluded
        public List<List<String>> value = new ArrayList<>();

        public TestMatrix() {
        }
    }


    //------------------

    @GuiIncluded
    public static class TestObjMatrix {
        @GuiIncluded
        public List<List<TestRow>> value = new ArrayList<>();

        public TestObjMatrix() {
        }
    }

    //------------------

    @GuiIncluded
    public static class TestObjAction {
        @GuiIncluded
        public List<TestRowAction> list = new ArrayList<>();

        public TestEditor lastCompleted;
        public boolean lastOk;

        @GuiIncluded(editor = true, index = 10)
        public TestEditor selectInObj(List<TestRowAction> selected) {
            return new TestEditor("obj", selected.stream()
                    .map(t -> t.data)
                    .toList());
        }

        @GuiIncluded
        public void completeSelectInObj(TestEditor editor, boolean ok) {
            this.lastCompleted = editor;
            this.lastOk = ok;
        }

        @GuiIncluded(index = 20)
        @GuiListSelectionUpdater
        public List<TestRowAction> selectShiftRow(List<TestRowAction> selected) {
            return shift(list, selected);
        }
    }

    public static <R> List<R> shift(List<R> all, List<R> is) {
        return is.stream()
                .map(i -> {
                    var idx = all.indexOf(i);
                    return all.isEmpty() ?  null :
                            idx + 1 < all.size() ? all.get(idx + 1) : all.get(0);
                }).toList();
    }

    @GuiIncluded
    public static class TestRowAction {
        public int editCount;
        public int completeCount;
        public TestEditor lastCompleted;
        public boolean lastOk;

        @GuiIncluded
        public String data;

        @GuiIncluded
        public List<TestColumnAction> subList = new ArrayList<>();

        public TestRowAction(String data) {
            this.data = data;
        }

        @GuiIncluded(editor = true, index = 10)
        public TestEditor selectInRow(List<TestColumnAction> selected) {
            editCount++;
            return new TestEditor("row:" + data, selected.stream()
                    .map(t -> t.data)
                    .toList());
        }

        @GuiIncluded
        public void completeSelectInRow(TestEditor editor, boolean ok) {
            completeCount++;
            this.lastCompleted = editor;
            this.lastOk = ok;
        }

        @GuiIncluded(index = 20)
        @GuiListSelectionUpdater
        public List<TestColumnAction> selectShiftCol(List<TestColumnAction> selected) {
            return shift(subList, selected);
        }
    }

    @GuiIncluded
    public static class TestColumnAction {
        @GuiIncluded
        public String data;

        public TestEditor lastCompleted;
        public boolean lastOk;

        public TestColumnAction(String data) {
            this.data = data;
        }

        @GuiIncluded(editor = true, index = 10)
        public TestEditor editInColumn() {
            return new TestEditor("column:" + data, List.of());
        }

        @GuiIncluded
        public void completeEditInColumn(TestEditor editor, boolean ok) {
            this.lastCompleted = editor;
            this.lastOk = ok;
        }

    }

    @GuiIncluded
    public static class TestEditor {
        public String sender;
        @GuiIncluded
        public List<String> actions;

        public TestEditor(String sender, List<String> actions) {
            this.sender = sender;
            this.actions = actions;
        }
    }

    //------------------


    public GuiSwingViewCollectionTable.CollectionTable create() {
        JComponent comp = table.createView(propContext, GuiReprValue.getNoneSupplier());
        GuiSwingViewCollectionTable.CollectionTable view = GuiSwingView.findDescendantByType(comp, GuiSwingViewCollectionTable.CollectionTable.class);
        frame = createFrame(comp);
        frame.setSize(300, 500);
        return view;
    }

    public GuiSwingViewCollectionTable.CollectionTable createValues() {
        JComponent comp = table.createView(valuesPropContext, GuiReprValue.getNoneSupplier());
        GuiSwingViewCollectionTable.CollectionTable view = GuiSwingView.findDescendantByType(comp, GuiSwingViewCollectionTable.CollectionTable.class);
        frame = createFrame(comp);
        frame.setSize(300, 500);
        return view;
    }

    public GuiSwingViewCollectionTable.CollectionTable createMatrix() {
        JComponent comp = table.createView(matrixPropContext, GuiReprValue.getNoneSupplier());
        GuiSwingViewCollectionTable.CollectionTable view = GuiSwingView.findDescendantByType(comp, GuiSwingViewCollectionTable.CollectionTable.class);
        frame = createFrame(comp);
        frame.setSize(300, 500);
        return view;
    }

    public GuiSwingViewCollectionTable.CollectionTable createObjMatrix() {
        JComponent comp = table.createView(objMatrixPropContext, GuiReprValue.getNoneSupplier());
        GuiSwingViewCollectionTable.CollectionTable view = GuiSwingView.findDescendantByType(comp, GuiSwingViewCollectionTable.CollectionTable.class);
        frame = createFrame(comp);
        frame.setSize(300, 500);
        return view;
    }

    public GuiSwingViewCollectionTable.CollectionTable createObjAction() {
        JComponent comp = table.createView(objActionListContext, GuiReprValue.getNoneSupplier());
        GuiSwingViewCollectionTable.CollectionTable view = GuiSwingView.findDescendantByType(comp, GuiSwingViewCollectionTable.CollectionTable.class);
        frame = createFrame(comp);
        frame.setSize(300, 500);
        return view;
    }

    private JButton findToolButton(JComponent comp, int index) {
        return (JButton) ToolBarHiddenMenu.listToolBarItemsWithHiddenMenu(comp).get(index);
    }

    //------------------


    @Test
    public void testViewCollectionUpdate() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::create);
        for (int i = 0; i < 100; ++i) {
            obj.value.add(new TestRow("hello" + i, "world" + i));
        }
        context.updateSourceFromRoot();
        runWait();
        assertEquals(3, runGet(colTable::getColumnCount).intValue(), "column size from row-index and 2 props");

        assertEquals(100, runGet(colTable::getRowCount).intValue(), "row size");

        assertEquals(30, runGet(() -> colTable.getValueAt(30, 0)), "row-index 30");

        assertEquals("world30", runGet(() -> colTable.getValueAt(30, 2)), "prop world 30");
    }

    @Test
    public void testViewCollectionSetViewValue() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::create);
        List<TestRow> rs = new ArrayList<>(Arrays.asList(new TestRow("a", "b"), new TestRow("c", "d"), new TestRow("e", "f")));
        run(() -> colTable.setSwingViewValueWithUpdate(rs));

        runWait();
        assertEquals(3, runGet(colTable::getRowCount).intValue(), "row size");

        assertEquals(2, runGet(() -> colTable.getValueAt(2, 0)), "row-index 2");

        assertEquals("f", runGet(() -> colTable.getValueAt(2, 2)), "prop world 2");

        assertEquals(rs, obj.value, "list updated");
    }

    @Test
    public void testViewCollectionSetViewValueSetColumn() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::create);
        List<TestRow> rs = new ArrayList<>(Arrays.asList(new TestRow("a", "b"), new TestRow("c", "d"), new TestRow("e", "f")));
        run(() -> colTable.setSwingViewValueWithUpdate(rs));

        runWait();
        run(() -> colTable.setValueAt("hello", 1, 2));

        runWait();
        assertEquals("c", rs.get(1).name, "no update");
        assertEquals("f", rs.get(2).value, "no update");
        assertEquals("hello", rs.get(1).value, "update column value");
    }

    @Test
    public void testViewCollectionSelectAction() {
        timeFactor = 0.5;
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::create);
        List<TestRow> rs = new ArrayList<>(Arrays.asList(new TestRow("a", "b"), new TestRow("c", "d"), new TestRow("e", "f"), new TestRow("g", "h")));
        run(() -> colTable.setSwingViewValueWithUpdate(rs));

        JButton btn = runGet(() -> findToolButton(colTable.getActionToolBar(), 0));
        JButton btn2 = runGet(() -> findToolButton(colTable.getActionToolBar(), 1));

        assertEquals("Action", btn.getText(), "first button is action of row object");

        assertEquals("*Select", btn2.getText(), "second button is action of enclosing object, and text starts with * because of list-selection action");

        assertFalse(runGet(btn::isEnabled), "no selection and disabled");

        assertFalse(runGet(btn2::isEnabled), "no selection and disabled");

        run(() -> colTable.setRowSelectionInterval(1, 2));
        run(() -> colTable.setColumnSelectionInterval(1, 2));

        assertTrue(runGet(btn::isEnabled), "selected and enabled");

        assertTrue(runGet(btn2::isEnabled), "selected and enabled");

        run(btn::doClick);

        assertEquals(0, obj.value.get(0).actionCount, "first button action");
        assertEquals(1, obj.value.get(1).actionCount, "first button action");
        assertEquals(1, obj.value.get(2).actionCount, "first button action");
        assertEquals(0, obj.value.get(3).actionCount, "first button action");

        run(btn2::doClick);

        assertEquals(new HashSet<>(Arrays.asList(obj.value.get(1), obj.value.get(2))), runGet(() -> obj.selectedItems), "selection cause second action");

    }

    @Test
    public void testViewCollectionSelectionSourceForRowIndexes() {

        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::create);
        List<TestRow> rs = new ArrayList<>(Arrays.asList(new TestRow("a", "b"), new TestRow("c", "d"), new TestRow("e", "f"), new TestRow("g", "h")));
        run(() -> colTable.setSwingViewValueWithUpdate(rs));
        run(() -> colTable.setRowSelectionInterval(1, 2));

        assertFalse(runGet(() -> colTable.getSelectionSourceForRowIndices().isSelectionEmpty()), "has selection");

        assertEquals(Arrays.asList(1, 2), runGet(() -> colTable.getSelectionSourceForRowIndices().getSelectedItems()), "selection");
    }


    @Test
    public void testViewCollectionSelectionSourceForRowAndColumnIndexes() {

        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::create);
        List<TestRow> rs = new ArrayList<>(Arrays.asList(new TestRow("a", "b"), new TestRow("c", "d"), new TestRow("e", "f"), new TestRow("g", "h")));
        run(() -> colTable.setSwingViewValueWithUpdate(rs));
        run(() -> colTable.setRowSelectionInterval(1, 2));
        run(() -> colTable.setColumnSelectionInterval(0, 2));

        assertFalse(runGet(() -> colTable.getSelectionSourceForRowAndColumnIndices().isSelectionEmpty()), "has selection");

        List<?> items = runGet(() -> colTable.getSelectionSourceForRowAndColumnIndices().getSelectedItems());
        assertArrayEquals(new int[]{1, 1}, (int[]) items.get(0), "1st row, 2nd col");
        assertArrayEquals(new int[]{1, 2}, (int[]) items.get(1), "1st row, 3rd col");
        assertArrayEquals(new int[]{2, 1}, (int[]) items.get(2), "2nd row, 2nd col");
        assertArrayEquals(new int[]{2, 2}, (int[]) items.get(3), "2nd row, 3rd col");
    }

    //------------------


    @Test
    public void testViewCollectionUpdateValues() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::createValues);
        for (int i = 0; i < 100; ++i) {
            values.value.add("hello" + i);
        }
        valuesContext.updateSourceFromRoot();
        runWait();
        assertEquals(2, runGet(colTable::getColumnCount).intValue(), "column size from row-index and value");

        assertEquals(100, runGet(colTable::getRowCount).intValue(), "row size");

        assertEquals(30, runGet(() -> colTable.getValueAt(30, 0)), "row-index 30");

        assertEquals("hello30", runGet(() -> colTable.getValueAt(30, 1)), "prop world 30");
    }

    @Test
    public void testViewCollectionSetViewValueSetColumnValues() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::createValues);
        List<String> rs = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        run(() -> colTable.setSwingViewValueWithUpdate(rs));

        runWait();
        run(() -> colTable.setValueAt("hello", 1, 1));
        run(() -> colTable.setValueAt("world", 3, 1));

        runWait();
        assertEquals("c", rs.get(2), "no update");
        assertEquals("hello", rs.get(1), "update column value");
        assertEquals("world", rs.get(3), "update column value");
    }


    //------------------

    @Test
    public void testViewCollectionUpdateMatrix() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::createMatrix);
        for (int i = 0; i < 100; ++i) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < i; ++j) {
                row.add("x" + i + "-" + j);
            }
            matrix.value.add(row);
        }
        matrixContext.updateSourceFromRoot();
        runWait();
        assertEquals(100, runGet(colTable::getColumnCount).intValue(), "column size from row-index and dynamic values");

        assertEquals(100, runGet(colTable::getRowCount).intValue(), "row size");

        assertEquals(30, runGet(() -> colTable.getValueAt(30, 0)), "row-index 30");

        assertEquals("x30-14", runGet(() -> colTable.getValueAt(30, 15)), "prop world 30");
    }


    //------------------

    @Test
    public void testViewCollectionUpdateObjMatrix() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::createObjMatrix);
        for (int i = 0; i < 100; ++i) {
            List<TestRow> row = new ArrayList<>();
            for (int j = 0; j < i; ++j) {
                row.add(new TestRow("x" + i + "-" + j, "v" + i + "-" + j));
            }
            objMatrix.value.add(row);
        }
        objMatrixContext.updateSourceFromRoot();
        runWait();
        assertEquals(199, runGet(colTable::getColumnCount).intValue(), "column size from row-index and dynamic values");

        assertEquals(100, runGet(colTable::getRowCount).intValue(), "row size");

        assertEquals(30, runGet(() -> colTable.getValueAt(30, 0)), "row-index 30");

        assertEquals("x30-7", runGet(() -> colTable.getValueAt(30, 15)), "prop world 30");
    }

    //------------------

    @Test
    public void testViewCollectionAction() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::createObjAction);

        List<TestRowAction> rowActions = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            var r = new TestRowAction("r" + i);
            for (int j = 0; j < 3; ++j) {
                r.subList.add(new TestColumnAction("r" + i + "-c" + j));
            }
            rowActions.add(r);
        }
        objAction.list.addAll(rowActions);

        objActionContext.updateSourceFromRoot();
        runWait();

        run(() -> colTable.setRowSelectionInterval(1, 2));
        run(() -> colTable.setColumnSelectionInterval(1, 2));
        runWait();

        JButton btn1 = runGet(() -> findToolButton(colTable.getActionToolBar(), 0));
        JButton btn2 = runGet(() -> findToolButton(colTable.getActionToolBar(), 1));
        JButton btn3 = runGet(() -> findToolButton(colTable.getActionToolBar(), 2));
        JButton btn4 = runGet(() -> findToolButton(colTable.getActionToolBar(), 3));
        JButton btn5 = runGet(() -> findToolButton(colTable.getActionToolBar(), 4));

        assertEquals("Select In Obj", btn1.getText(), "1st button from root-obj");
        assertEquals("Select Shift Row", btn2.getText(), "2nd button from root-obj");
        assertEquals("Select In Row", btn3.getText(), "3rd button from row");
        assertEquals("Select Shift Col", btn4.getText(), "4th button from row");
        assertEquals("Edit In Column", btn5.getText(), "5th button from column");


        run(btn2::doClick); //selectShiftRow

        run(btn1::doClick); //selectInObj
        runWait(500);

        var execActionBtn1 = (GuiSwingActionDefault.ExecutionAction) btn1.getAction();
        var edit1 = GuiSwingFeature.findFirstChild(execActionBtn1, GuiSwingActionDefault.ActionResultHandlerEditStarter.class, Function.identity());
        var editFrame1 = edit1.getFrame();
        assertTrue(editFrame1.isVisible(), "displayed edit-frame");

        var okBtn = GuiSwingFeature.traverseFindFirstIf(editFrame1, JButton.class, b -> b.getText().equals("OK"));
        run(okBtn::doClick);

        runWait(500);

        assertFalse(editFrame1.isVisible(), "closed edit-frame by OK");

        assertTrue(objAction.lastOk, "completeSelectInObj by edit OK");
        assertEquals("obj", objAction.lastCompleted.sender, "completeSelectInObj by edit OK: sender");
        assertEquals(List.of("r0", "r2"), objAction.lastCompleted.actions, "completeSelectInObj by edit OK: rows are shifted selections");

        //--------

        run(btn3::doClick); //selectInRow
        runWait(500);

        var execActionBtn3 = (GuiSwingActionDefault.ExecutionAction) btn3.getAction();
        var edit3 = GuiSwingFeature.findFirstChild(execActionBtn3, GuiSwingActionDefault.ActionResultHandlerEditStarter.class, Function.identity());
        var editFrame3 = edit3.getFrame();
        assertTrue(editFrame3.isVisible(), "displayed edit-frame");

        var okBtn3 = GuiSwingFeature.traverseFindFirstIf(editFrame3, JButton.class, b -> b.getText().equals("OK"));
        run(okBtn3::doClick);

        runWait(500);
        assertFalse(editFrame1.isVisible(), "closed edit-frame by OK");

        var row0 = objAction.list.get(0);
        var row1 = objAction.list.get(1);
        var row2 = objAction.list.get(2);

        assertTrue(row0.lastOk, "only top selected item edited");
        assertFalse(row1.lastOk, "only top selected item edited");
        assertFalse(row2.lastOk, "only top selected item edited");
        assertEquals(1, row0.editCount, "only top selected item edited");
        assertEquals(1, row0.completeCount, "only top selected item edited");
        assertEquals(0, row1.editCount, "no selection");
        assertEquals(0, row1.completeCount, "no selection");
        assertEquals(1, row2.editCount, "only top selected item edited but selected items are invoked");
        assertEquals(0, row2.completeCount, "only top selected item edited");

        runWait();

        //GuiSwingViewCollectionTable.createView
        //  -> ObjectTableModelColumns.getDynamicColumnsActions
        //     -> dynamicColumns DynamicColumnContainer.getDynamicColumnActions
        //        -> factory DynamicColumnFactoryRoot: DynamicColumnFactoryCollectionRoot.getRootActions
        //           -> rootActions toAction(a, ...)
        //              -> toActionForList
        //                  -> new TableSelectionListDynamicAction(selectInObj, ...)
        //        -> factory.getActions
        //           -> DynamicColumnFactoryList.getActions -> getActions(...[elementFactory]...)
        //              -> no [actionContexts]
        //              -> elementFactory DynamicColumnFactoryComposite.getActions -> getActions(...[factories]...)
        //                 -> factory.getActions -> getActions(...[DynamicColumnFactoryList]...)
        //                    -> DynamicColumnFactoryList.getActions -> getActions(...[DynamicColumnFactoryComposite]...)
        //                       -> getActions(...[ObjectTableColumnSizeConcrete(1,Data)]...)
        //                          -> actionContexts: toActions(...[editInColumn]...)
        //                             -> toActionForTarget
        //                                -> new TableSelectionAction(editInColumn...)

        //                -> toActionForList(actionContexts[0]...[factories]...)
        //                    -> children collectListActions(...)
        //                       -> composite.context : property(collection(object(TestColumnAction))) collectListActions(actionContext:actionList(selectInRow), prop:composite.factories[0])
        //                          -> composite.collectListActions : listAction.isSelection...
        //                                     elementType: object(TestRowAction) == tableContext.elementType:listContext(object(TestColumnAction))

        //table : CollectionTable(collection(object(TestRowAction)))
        //  dataModel : GuiSwingTableModelCollection(object(TestRowAction))
        //    columns : GuiSwingTableModelColumns
        //      columns, staticColumns : [ObjectTableColumnRowIndexWithActions, ObjectTableColumnValue(property(data))]
        //      dynamicColumns : [DynamicColumnContainer(collection(object(TestRowAction))]
        //        [0]
        //          factory : DynamicColumnFactoryCollectionRoot(collection(object(TestRowAction)))
        //            rootActions : [actionList(selectInObj)]
        //            elementFactory : DynamicColumnFactoryComposite(object(TestRowAction))
        //               actionContexts : [actionList(selectInRow)]
        //               factories : [DynamicColumnFactoryComposite(property(collection(TestColumnAction)))]
        //                 [0]
        //                    factories : [DynamicColumnFactoryList(collection(object(TestColumnAction)))]
        //                      [0]
        //                         elementFactory : DynamicColumnFactoryComposite(collection(object(TestColumnAction)))
        //                           factories : [ObjectTableColumnSizeConcrete(1, Data)]
        //                              column : GuiSwingTableColumnString
        //                              factoryBase : DynamicColumnFactoryBase(property(data))
        //                           actionContexts : [action(editInColumn)]

    }
}
