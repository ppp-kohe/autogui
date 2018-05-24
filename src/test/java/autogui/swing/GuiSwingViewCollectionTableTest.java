package autogui.swing;

import autogui.GuiIncluded;
import autogui.GuiListSelectionCallback;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.type.GuiTypeBuilder;
import autogui.base.type.GuiTypeObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.util.*;

public class GuiSwingViewCollectionTableTest extends  GuiSwingTestCase {
    public static void main(String[] args) {
        GuiSwingViewCollectionTableTest test = new GuiSwingViewCollectionTableTest();
        test.setUp();
        test.testViewCollectionUpdateObjMatrix();
    }

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;
    TestObj obj;

    GuiMappingContext context;
    GuiMappingContext propContext;

    //////////

    GuiTypeObject typeValues;
    TestValues values;
    GuiMappingContext valuesContext;
    GuiMappingContext valuesPropContext;

    //////////

    GuiTypeObject typeMatrix;
    TestMatrix matrix;
    GuiMappingContext matrixContext;
    GuiMappingContext matrixPropContext;

    //////////

    GuiTypeObject typeObjMatrix;
    TestObjMatrix objMatrix;
    GuiMappingContext objMatrixContext;
    GuiMappingContext objMatrixPropContext;

    //////////

    GuiSwingViewCollectionTable table;

    JFrame frame;

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        table = new GuiSwingViewCollectionTable(GuiSwingMapperSet.getDefaultMapperSet());

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);
        propContext = context.getChildByName("value").getChildByName("List");

        ////////

        typeValues = (GuiTypeObject) builder.get(TestValues.class);

        values = new TestValues();
        valuesContext = new GuiMappingContext(typeValues, values);
        GuiSwingMapperSet.getReprDefaultSet().match(valuesContext);
        valuesPropContext = valuesContext.getChildByName("value").getChildByName("List");

        ////////

        typeMatrix = (GuiTypeObject) builder.get(TestMatrix.class);

        matrix = new TestMatrix();
        matrixContext = new GuiMappingContext(typeMatrix, matrix);
        GuiSwingMapperSet.getReprDefaultSet().match(matrixContext);
        matrixPropContext = matrixContext.getChildByName("value").getChildByName("List");

        ////////

        typeObjMatrix = (GuiTypeObject) builder.get(TestObjMatrix.class);

        objMatrix = new TestObjMatrix();
        objMatrixContext = new GuiMappingContext(typeObjMatrix, objMatrix);
        GuiSwingMapperSet.getReprDefaultSet().match(objMatrixContext);
        objMatrixPropContext = objMatrixContext.getChildByName("value").getChildByName("List");


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

    ////////////////////////

    @GuiIncluded
    public static class TestValues {
        @GuiIncluded
        public List<String> value = new ArrayList<>();
    }

    ////////////////////////

    @GuiIncluded
    public static class TestMatrix {
        @GuiIncluded
        public List<List<String>> value = new ArrayList<>();
    }


    ////////////////////////

    @GuiIncluded
    public static class TestObjMatrix {
        @GuiIncluded
        public List<List<TestRow>> value = new ArrayList<>();
    }

    ////////////////////////


    public GuiSwingViewCollectionTable.CollectionTable create() {
        JComponent comp = table.createView(propContext, GuiReprValue.getNoneSupplier());
        GuiSwingViewCollectionTable.CollectionTable view =
                GuiSwingView.findChildByType(comp,
                        GuiSwingViewCollectionTable.CollectionTable.class);
        frame = createFrame(comp);
        frame.setSize(300, 500);
        return view;
    }

    public GuiSwingViewCollectionTable.CollectionTable createValues() {
        JComponent comp = table.createView(valuesPropContext, GuiReprValue.getNoneSupplier());
        GuiSwingViewCollectionTable.CollectionTable view =
                GuiSwingView.findChildByType(comp,
                        GuiSwingViewCollectionTable.CollectionTable.class);
        frame = createFrame(comp);
        frame.setSize(300, 500);
        return view;
    }

    public GuiSwingViewCollectionTable.CollectionTable createMatrix() {
        JComponent comp = table.createView(matrixPropContext, GuiReprValue.getNoneSupplier());
        GuiSwingViewCollectionTable.CollectionTable view =
                GuiSwingView.findChildByType(comp,
                        GuiSwingViewCollectionTable.CollectionTable.class);
        frame = createFrame(comp);
        frame.setSize(300, 500);
        return view;
    }

    public GuiSwingViewCollectionTable.CollectionTable createObjMatrix() {
        JComponent comp = table.createView(objMatrixPropContext, GuiReprValue.getNoneSupplier());
        GuiSwingViewCollectionTable.CollectionTable view =
                GuiSwingView.findChildByType(comp,
                        GuiSwingViewCollectionTable.CollectionTable.class);
        frame = createFrame(comp);
        frame.setSize(300, 500);
        return view;
    }
    ////////////////////////


    @Test
    public void testViewCollectionUpdate() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::create);
        for (int i = 0; i < 100; ++i) {
            obj.value.add(new TestRow("hello" + i, "world" + i));
        }
        context.updateSourceFromRoot();
        runWait();
        Assert.assertEquals("column size from row-index and 2 props",
                3, runGet(colTable::getColumnCount).intValue());

        Assert.assertEquals("row size",
                100, runGet(colTable::getRowCount).intValue());

        Assert.assertEquals("row-index 30",
                30, runGet(() -> colTable.getValueAt(30, 0)));

        Assert.assertEquals("prop world 30",
                "world30", runGet(() -> colTable.getValueAt(30, 2)));
    }

    @Test
    public void testViewCollectionSetViewValue() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::create);
        List<TestRow> rs = new ArrayList<>(Arrays.asList(
                new TestRow("a", "b"),
                new TestRow("c", "d"),
                new TestRow("e", "f")
        ));
        run(() -> colTable.setSwingViewValueWithUpdate(rs));

        runWait();
        Assert.assertEquals("row size",
                3, runGet(colTable::getRowCount).intValue());

        Assert.assertEquals("row-index 2",
                2, runGet(() -> colTable.getValueAt(2, 0)));

        Assert.assertEquals("prop world 2",
                "f", runGet(() -> colTable.getValueAt(2, 2)));

        Assert.assertEquals("list updated",
                rs,
                obj.value);
    }

    @Test
    public void testViewCollectionSetViewValueSetColumn() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::create);
        List<TestRow> rs = new ArrayList<>(Arrays.asList(
                new TestRow("a", "b"),
                new TestRow("c", "d"),
                new TestRow("e", "f")
        ));
        run(() -> colTable.setSwingViewValueWithUpdate(rs));

        runWait();
        run(() -> colTable.setValueAt("hello", 1, 2));

        runWait();
        Assert.assertEquals("no update",
                "c",
                rs.get(1).name);
        Assert.assertEquals("no update",
                "f",
                rs.get(2).value);
        Assert.assertEquals("update column value",
                "hello",
                rs.get(1).value);
    }

    @Test
    public void testViewCollectionSelectAction() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::create);
        List<TestRow> rs = new ArrayList<>(Arrays.asList(
                new TestRow("a", "b"),
                new TestRow("c", "d"),
                new TestRow("e", "f"),
                new TestRow("g", "h")
        ));
        run(() -> colTable.setSwingViewValueWithUpdate(rs));

        JButton btn = (JButton) runGet(() -> colTable.getActionToolBar().getComponentAtIndex(0));
        JButton btn2 = (JButton) runGet(() -> colTable.getActionToolBar().getComponentAtIndex(1));

        Assert.assertEquals("first button is action of row object",
                "Action",
                btn.getText());

        Assert.assertEquals("second button is action of enclosing object, and text starts with * because of list-selection action",
                "*Select",
                btn2.getText());

        Assert.assertFalse("no selection and disabled",
                runGet(btn::isEnabled));

        Assert.assertFalse("no selection and disabled",
                runGet(btn2::isEnabled));

        run(() -> colTable.setRowSelectionInterval(1, 2));

        Assert.assertTrue("selected and enabled",
                runGet(btn::isEnabled));

        Assert.assertTrue("selected and enabled",
                runGet(btn2::isEnabled));

        run(btn::doClick);

        Assert.assertEquals("first button action",
                0,
                obj.value.get(0).actionCount);
        Assert.assertEquals("first button action",
                1,
                obj.value.get(1).actionCount);
        Assert.assertEquals("first button action",
                1,
                obj.value.get(2).actionCount);
        Assert.assertEquals("first button action",
                0,
                obj.value.get(3).actionCount);

        Assert.assertEquals("selection cause second action",
                new HashSet<>(Arrays.asList(obj.value.get(1), obj.value.get(2))),
                obj.selectedItems);

    }

    @Test
    public void testViewCollectionSelectionSourceForRowIndexes() {

        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::create);
        List<TestRow> rs = new ArrayList<>(Arrays.asList(
                new TestRow("a", "b"),
                new TestRow("c", "d"),
                new TestRow("e", "f"),
                new TestRow("g", "h")
        ));
        run(() -> colTable.setSwingViewValueWithUpdate(rs));
        run(() -> colTable.setRowSelectionInterval(1, 2));

        Assert.assertFalse("has selection",
                runGet(() -> colTable.getSelectionSourceForRowIndexes().isSelectionEmpty()));

        Assert.assertEquals("selection",
                Arrays.asList(1, 2),
                runGet(() -> colTable.getSelectionSourceForRowIndexes().getSelectedItems()));
    }


    @Test
    public void testViewCollectionSelectionSourceForRowAndColumnIndexes() {

        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::create);
        List<TestRow> rs = new ArrayList<>(Arrays.asList(
                new TestRow("a", "b"),
                new TestRow("c", "d"),
                new TestRow("e", "f"),
                new TestRow("g", "h")
        ));
        run(() -> colTable.setSwingViewValueWithUpdate(rs));
        run(() -> colTable.setRowSelectionInterval(1, 2));

        Assert.assertFalse("has selection",
                runGet(() -> colTable.getSelectionSourceForRowAndColumnIndexes().isSelectionEmpty()));

        List<?> items = runGet(() -> colTable.getSelectionSourceForRowAndColumnIndexes().getSelectedItems());
        Assert.assertArrayEquals("1st row, idx col", new int[] {1, 0}, (int[]) items.get(0));
        Assert.assertArrayEquals("1st row, 2nd col", new int[] {1, 1}, (int[]) items.get(1));
        Assert.assertArrayEquals("1st row, 3rd col", new int[] {1, 2}, (int[]) items.get(2));
        Assert.assertArrayEquals("2nd row, idx col", new int[] {2, 0}, (int[]) items.get(3));
        Assert.assertArrayEquals("2nd row, 2nd col", new int[] {2, 1}, (int[]) items.get(4));
        Assert.assertArrayEquals("2nd row, 3rd col", new int[] {2, 2}, (int[]) items.get(5));
    }

    //////////////////////////


    @Test
    public void testViewCollectionUpdateValues() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::createValues);
        for (int i = 0; i < 100; ++i) {
            values.value.add("hello" + i);
        }
        valuesContext.updateSourceFromRoot();
        runWait();
        Assert.assertEquals("column size from row-index and value",
                2, runGet(colTable::getColumnCount).intValue());

        Assert.assertEquals("row size",
                100, runGet(colTable::getRowCount).intValue());

        Assert.assertEquals("row-index 30",
                30, runGet(() -> colTable.getValueAt(30, 0)));

        Assert.assertEquals("prop world 30",
                "hello30", runGet(() -> colTable.getValueAt(30, 1)));
    }

    @Test
    public void testViewCollectionSetViewValueSetColumnValues() {
        GuiSwingViewCollectionTable.CollectionTable colTable = runGet(this::createValues);
        List<String> rs = new ArrayList<>(Arrays.asList(
                "a", "b", "c", "d"
        ));
        run(() -> colTable.setSwingViewValueWithUpdate(rs));

        runWait();
        run(() -> colTable.setValueAt("hello", 1, 1));

        runWait();
        Assert.assertEquals("no update",
                "c",
                rs.get(2));
        Assert.assertEquals("update column value",
                "hello",
                rs.get(1));
    }



    //////////////////////////

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
        Assert.assertEquals("column size from row-index and dynamic values",
                100, runGet(colTable::getColumnCount).intValue());

        Assert.assertEquals("row size",
                100, runGet(colTable::getRowCount).intValue());

        Assert.assertEquals("row-index 30",
                30, runGet(() -> colTable.getValueAt(30, 0)));

        Assert.assertEquals("prop world 30",
                "x30-14", runGet(() -> colTable.getValueAt(30, 15)));
    }


    //////////////////////////

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
        Assert.assertEquals("column size from row-index and dynamic values",
                199, runGet(colTable::getColumnCount).intValue());

        Assert.assertEquals("row size",
                100, runGet(colTable::getRowCount).intValue());

        Assert.assertEquals("row-index 30",
                30, runGet(() -> colTable.getValueAt(30, 0)));

        Assert.assertEquals("prop world 30",
                "x30-7", runGet(() -> colTable.getValueAt(30, 15)));
    }

}
