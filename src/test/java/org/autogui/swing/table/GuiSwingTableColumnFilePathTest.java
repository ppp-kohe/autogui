package org.autogui.swing.table;

import org.autogui.GuiIncluded;
import org.autogui.base.JsonWriter;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingJsonTransfer;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.GuiSwingTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class GuiSwingTableColumnFilePathTest extends GuiSwingTestCase {

    GuiTypeBuilder builder;
    GuiMappingContext context;
    GuiTypeObject typeObject;
    TestObj obj;

    GuiMappingContext elemFileContext;
    GuiMappingContext elemPathContext;
    GuiMappingContext fileContext;
    GuiMappingContext pathContext;

    GuiSwingTableColumnFilePath column;
    Supplier<GuiReprValue.ObjectSpecifier> spec;
    GuiSwingTableColumn.SpecifierManagerIndex specIndex;

    JFrame frame;
    JTable table;
    ObjectTableModel model;
    ObjectTableColumn objColumn;

    public GuiSwingTableColumnFilePathTest() {}

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();
        obj.paths= new ArrayList<>();
        obj.paths.add(Paths.get("src", "main"));
        obj.paths.add(Paths.get(System.getProperty("user.home", "/")));
        obj.paths.add(Paths.get("src", "test"));

        obj.files = new ArrayList<>();
        obj.files.add(obj.paths.get(0).toFile());
        obj.files.add(obj.paths.get(1).toFile());
        obj.files.add(obj.paths.get(2).toFile());

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        GuiMappingContext propContext = context.getChildByName("paths")
                .getChildByName("List");

        elemPathContext = propContext.getChildren().getFirst();
        pathContext = elemPathContext.getChildByName("Path");

        GuiMappingContext propFileContext = context.getChildByName("files")
                .getChildByName("List");

        elemFileContext = propFileContext.getChildren().getFirst();
        fileContext = elemFileContext.getChildByName("File");

        column = new GuiSwingTableColumnFilePath();
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
        public List<Path> paths;

        @GuiIncluded
        public List<File> files;
        public TestObj() {}
    }

    JScrollPane createTable(GuiMappingContext ctx, Supplier<Object> src) {
        objColumn = column.createColumn(ctx, specIndex, specIndex);

        model = new ObjectTableModel();
        model.setSource(src);
        model.getColumns().addColumnStatic(objColumn);

        JScrollPane scroll = model.initTableWithScroll();
        table = (JTable) scroll.getViewport().getView();
        frame = createFrame(scroll);
        frame.setSize(new Dimension(300, 300));
        return scroll;
    }

    @Test
    public void testCreateAndGetPath() {
        run(() -> createTable(pathContext, () -> obj.paths));

        Assert.assertEquals(obj.paths.get(0), runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(obj.paths.get(1), runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(obj.paths.get(2), runGet(() -> table.getValueAt(2, 0)));
        Assert.assertEquals(3, (int) runGet(() -> table.getRowCount()));
    }

    @Test
    public void testCreateAndGetFile() {
        run(() -> createTable(fileContext, () -> obj.files));

        Assert.assertEquals(obj.files.get(0), runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(obj.files.get(1), runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(obj.files.get(2), runGet(() -> table.getValueAt(2, 0)));
        Assert.assertEquals(3, (int) runGet(() -> table.getRowCount()));
    }

    @Test
    public void testSortPath() {
        run(() -> createTable(pathContext, () -> obj.paths));
        run(() -> table.getRowSorter().setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING))));
        runWait();

        List<Path> sorted = new ArrayList<>(obj.paths);
        sorted.sort(Path::compareTo);
        System.err.println(sorted);

        Assert.assertEquals(sorted.get(2), runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(sorted.get(1), runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(sorted.get(0), runGet(() -> table.getValueAt(2, 0)));
        Assert.assertEquals(3, (int) runGet(table::getRowCount));
    }

    @Test
    public void testSortFile() {
        run(() -> createTable(fileContext, () -> obj.files));
        run(() -> table.getRowSorter().setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING))));
        runWait();

        List<File> sorted = new ArrayList<>(obj.files);
        sorted.sort(File::compareTo);
        System.err.println(sorted);

        Assert.assertEquals(sorted.get(2), runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(sorted.get(1), runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(sorted.get(0), runGet(() -> table.getValueAt(2, 0)));
        Assert.assertEquals(3, (int) runGet(table::getRowCount));
    }


    @Test
    public void testEditPath() {
        run(() -> createTable(pathContext, () -> obj.paths));
        run(() -> table.editCellAt(1, 0));

        TableCellEditor editor = runGet(() -> objColumn.getTableColumn().getCellEditor());
        Assert.assertTrue(editor instanceof ObjectTableColumnValue.ObjectTableCellEditor);

        ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;
        Assert.assertEquals(obj.paths.get(1), runGet(objEditor::getCellEditorValue));

        Path p = Paths.get("pom.xml");

        GuiSwingTableColumnFilePath.ColumnEditFilePathPane component = (GuiSwingTableColumnFilePath.ColumnEditFilePathPane) runGet(table::getEditorComponent);
        run(() -> component.setBackground(Color.black));
        run(() -> component.setForeground(Color.gray));

        Assert.assertEquals("editorPane.setBackground also sets field background", Color.black, runGet(() -> component.getField().getBackground()));
        Assert.assertEquals("editorPane.setForeground also sets field foreground", Color.gray, runGet(() -> component.getField().getForeground()));


        run(() -> component.setFile(p));
        runWait();

        run(objEditor::stopCellEditing);

        Assert.assertEquals("editor change",
                p, obj.paths.get(1));

    }

    @Test
    public void testEditFile() {
        run(() -> createTable(fileContext, () -> obj.files));
        run(() -> table.editCellAt(1, 0));

        TableCellEditor editor = runGet(() -> objColumn.getTableColumn().getCellEditor());
        Assert.assertTrue(editor instanceof ObjectTableColumnValue.ObjectTableCellEditor);

        ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;
        Assert.assertEquals("editor value is obtained as Path",
                obj.files.get(1).toPath(), runGet(objEditor::getCellEditorValue));

        Path p = Paths.get("pom.xml");

        GuiSwingTableColumnFilePath.ColumnEditFilePathPane component = (GuiSwingTableColumnFilePath.ColumnEditFilePathPane) runGet(table::getEditorComponent);
        run(() -> component.setSwingViewValue(p.toFile()));
        runWait();

        run(objEditor::stopCellEditing);

        Assert.assertEquals("editor change",
                p.toFile(), obj.files.get(1));
    }


    @Test
    public void testEditFileBySetFile() {
        run(() -> createTable(fileContext, () -> obj.files));
        run(() -> table.editCellAt(1, 0));

        TableCellEditor editor = runGet(() -> objColumn.getTableColumn().getCellEditor());
        Assert.assertTrue(editor instanceof ObjectTableColumnValue.ObjectTableCellEditor);

        ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;
        Assert.assertEquals("editor value is obtained as Path",
                obj.files.get(1).toPath(), runGet(objEditor::getCellEditorValue));

        Path p = Paths.get("pom.xml");

        GuiSwingTableColumnFilePath.ColumnEditFilePathPane component = (GuiSwingTableColumnFilePath.ColumnEditFilePathPane) runGet(table::getEditorComponent);
        run(() -> component.setFile(p));
        runWait();

        run(objEditor::stopCellEditing);

        Assert.assertEquals("editor change",
                p.toFile(), obj.files.get(1));
    }

    @Test
    public void testEditFileCopyAction() {
        withClipLock(() -> {
            run(() -> createTable(fileContext, () -> obj.files));

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnFilePath.ColumnFilePathPane pane = (GuiSwingTableColumnFilePath.ColumnFilePathPane) renderer.getComponent();
            Action action = convertRowsAction(
                    findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingTableColumnFilePath.ColumnFileCopyAction.class));

            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            run(() -> action.actionPerformed(null));

            String data = obj.files.get(0).getPath() + "\n" + obj.files.get(2).getPath();

            Assert.assertEquals("copied selected paths",
                    data, getClipboardText());
        });
    }

    @Test
    public void testEditFileCopyActionPath() {
        withClipLock(() -> {
            run(() -> createTable(pathContext, () -> obj.paths));

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnFilePath.ColumnFilePathPane pane = (GuiSwingTableColumnFilePath.ColumnFilePathPane) renderer.getComponent();
            Action action = convertRowsAction(
                    findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingTableColumnFilePath.ColumnFileCopyAction.class));

            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            run(() -> action.actionPerformed(null));

            String data = obj.paths.get(0) + "\n" + obj.paths.get(2);

            Assert.assertEquals("copied selected paths",
                    data, getClipboardText());
        });
    }

    @Test
    public void testEditFilePasteAction() {
        withClipLock(() -> {
            run(() -> createTable(fileContext, () -> obj.files));

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnFilePath.ColumnFilePathPane pane = (GuiSwingTableColumnFilePath.ColumnFilePathPane) renderer.getComponent();
            Action action = convertRowsAction(
                    findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingTableColumnString.LabelTextPasteAllAction.class));

            File file1 = new File("folder" + File.separator + "a.txt");
            File file2 = new File("folder" + File.separator + "b.txt");
            File ex = obj.files.get(1);
            String data = file1.getPath() + "\n" + file2.getPath();
            setClipboardText(data);

            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            run(() -> action.actionPerformed(null));
            runWait();

            Assert.assertEquals(file1, obj.files.get(0));
            Assert.assertEquals(ex, obj.files.get(1));
            Assert.assertEquals(file2, obj.files.get(2));
        });
    }

    @Test
    public void testEditFilePasteJsonAction() {
        withClipLock(() -> {
            run(() -> createTable(fileContext, () -> obj.files));

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnFilePath.ColumnFilePathPane pane = (GuiSwingTableColumnFilePath.ColumnFilePathPane) renderer.getComponent();

            //JsonPaste action is not included in column actions. instead, use cell actions.
            List<PopupCategorized.CategorizedMenuItem> items = runGet(() -> model.getBuildersForRowsOrCells(table, Collections.singletonList(objColumn), false));
            GuiSwingJsonTransfer.JsonPasteCellsAction item = findMenuItemAction(items, GuiSwingJsonTransfer.JsonPasteCellsAction.class);

            Action action = new ObjectTableModel.TableTargetCellExecutionAction(item, new TableTargetCellForJTable(table));

            File file1 = new File("folder" + File.separator + "a.txt");
            File file2 = new File("folder" + File.separator + "b.txt");
            File ex = obj.files.get(1);
            String data = JsonWriter.create().write(Arrays.asList(file1.getPath(), file2.getPath())).toSource();
            System.err.println("json: " + data);
            setClipboardText(data);

            run(() -> table.addColumnSelectionInterval(0, 0)); //it needs to explicitly select a column
            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            System.out.println(runGet(() -> table.getSelectedColumn()));
            runWait();
            run(() -> action.actionPerformed(null));
            runWait();

            Assert.assertEquals(file1, obj.files.get(0));
            Assert.assertEquals(ex, obj.files.get(1));
            Assert.assertEquals(file2, obj.files.get(2));
        });
    }

    @Test
    public void testEditFilePasteJsonActionWithPropList() {
        withClipLock(() -> {
            run(() -> createTable(fileContext, () -> obj.files));

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnFilePath.ColumnFilePathPane pane = (GuiSwingTableColumnFilePath.ColumnFilePathPane) renderer.getComponent();

            //JsonPaste action is not included in column actions. instead, use cell actions.
            List<PopupCategorized.CategorizedMenuItem> items = runGet(() -> model.getBuildersForRowsOrCells(table, Collections.singletonList(objColumn), false));
            GuiSwingJsonTransfer.JsonPasteCellsAction item = findMenuItemAction(items, GuiSwingJsonTransfer.JsonPasteCellsAction.class);

            Action action = new ObjectTableModel.TableTargetCellExecutionAction(item, new TableTargetCellForJTable(table));

            File file1 = new File("folder" + File.separator + "a.txt");
            File file2 = new File("folder" + File.separator + "b.txt");
            File ex = obj.files.get(1);

            Map<String, Object> json1 = new HashMap<>();
            json1.put("File", file1.getPath());

            Map<String, Object> json2 = new HashMap<>();
            json2.put("File", file2.getPath());

            String data = JsonWriter.create().write(Arrays.asList(json1, json2)).toSource();
            System.err.println("json: " + data);
            setClipboardText(data);

            run(() -> table.addColumnSelectionInterval(0, 0)); //it needs to explicitly select a column
            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            System.out.println(runGet(() -> table.getSelectedColumn()));
            runWait();
            run(() -> action.actionPerformed(null));
            runWait();

            Assert.assertEquals(file1, obj.files.get(0));
            Assert.assertEquals(ex, obj.files.get(1));
            Assert.assertEquals(file2, obj.files.get(2));
        });
    }

    @Test
    public void testEditFilePasteJsonActionWithListList() {
        withClipLock(() -> {
            run(() -> createTable(fileContext, () -> obj.files));

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnFilePath.ColumnFilePathPane pane = (GuiSwingTableColumnFilePath.ColumnFilePathPane) renderer.getComponent();

            //JsonPaste action is not included in column actions. instead, use cell actions.
            List<PopupCategorized.CategorizedMenuItem> items = runGet(() -> model.getBuildersForRowsOrCells(table, Collections.singletonList(objColumn), false));
            GuiSwingJsonTransfer.JsonPasteCellsAction item = findMenuItemAction(items, GuiSwingJsonTransfer.JsonPasteCellsAction.class);

            Action action = new ObjectTableModel.TableTargetCellExecutionAction(item, new TableTargetCellForJTable(table));

            File file1 = new File("folder" + File.separator + "a.txt");
            File file2 = new File("folder" + File.separator + "b.txt");
            File ex = obj.files.get(1);


            String data = JsonWriter.create().write(Arrays.asList(
                    Collections.singletonList(file1.getPath()),
                    Collections.singletonList(file2.getPath()))).toSource();
            System.err.println("json: " + data);
            setClipboardText(data);

            run(() -> table.addColumnSelectionInterval(0, 0)); //it needs to explicitly select a column
            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            System.out.println(runGet(() -> table.getSelectedColumn()));
            runWait();
            run(() -> action.actionPerformed(null));
            runWait();

            Assert.assertEquals(file1, obj.files.get(0));
            Assert.assertEquals(ex, obj.files.get(1));
            Assert.assertEquals(file2, obj.files.get(2));
        });
    }

    @Test
    public void testEditFilePasteJsonActionWithSingleValue() {
        withClipLock(() -> {
            run(() -> createTable(fileContext, () -> obj.files));

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnFilePath.ColumnFilePathPane pane = (GuiSwingTableColumnFilePath.ColumnFilePathPane) renderer.getComponent();

            //JsonPaste action is not included in column actions. instead, use cell actions.
            List<PopupCategorized.CategorizedMenuItem> items = runGet(() -> model.getBuildersForRowsOrCells(table, Collections.singletonList(objColumn), false));
            GuiSwingJsonTransfer.JsonPasteCellsAction item = findMenuItemAction(items, GuiSwingJsonTransfer.JsonPasteCellsAction.class);

            Action action = new ObjectTableModel.TableTargetCellExecutionAction(item, new TableTargetCellForJTable(table));

            File file1 = new File("folder" + File.separator + "a.txt");
            File ex = obj.files.get(1);

            String data = JsonWriter.create().write(file1).toSource();
            System.err.println("json: " + data);
            setClipboardText(data);

            run(() -> table.addColumnSelectionInterval(0, 0)); //it needs to explicitly select a column
            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            System.out.println(runGet(() -> table.getSelectedColumn()));
            runWait();
            run(() -> action.actionPerformed(null));
            runWait();

            Assert.assertEquals(file1, obj.files.get(0));
            Assert.assertEquals(ex, obj.files.get(1));
            Assert.assertEquals(file1, obj.files.get(2));
        });
    }


    @Test
    public void testEditFileSetHistoryAction() {
        GuiPreferences prefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        context.setPreferences(prefs);
        run(() -> createTable(fileContext, () -> obj.files));

        Path p = Paths.get("pom.xml");
        //edit cell for storing a history value
        {
            run(() -> table.editCellAt(1, 0));

            TableCellEditor editor = runGet(() -> objColumn.getTableColumn().getCellEditor());
            ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;


            GuiSwingTableColumnFilePath.ColumnEditFilePathPane component = (GuiSwingTableColumnFilePath.ColumnEditFilePathPane) runGet(table::getEditorComponent);
            run(() -> component.setFile(p));
            runWait();

            run(objEditor::stopCellEditing);
        }

        ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                runGet(() -> objColumn.getTableColumn().getCellRenderer());
        GuiSwingTableColumnFilePath.ColumnFilePathPane pane = (GuiSwingTableColumnFilePath.ColumnFilePathPane) renderer.getComponent();
        GuiSwingTableColumnFilePath.ColumnHistoryMenuFilePathForTableColumn action = (GuiSwingTableColumnFilePath.ColumnHistoryMenuFilePathForTableColumn)
                new ObjectTableColumnValue.CollectionRowsActionBuilder(table, objColumn, PopupExtension.MENU_FILTER_IDENTITY).convert(
                pane.getSwingStaticMenuItems().stream()
                    .filter(GuiSwingTableColumnFilePath.ColumnHistoryMenuFilePath.class::isInstance)
                    .findFirst().orElseThrow(RuntimeException::new));

        run(action::loadItems);

        run(() -> table.addRowSelectionInterval(0, 0));
        run(() -> table.addRowSelectionInterval(2, 2));
        run(() -> action.getItem(0)
                .getAction().actionPerformed(null));
        runWait();

        Assert.assertEquals(p.toFile(), obj.files.get(0));
        Assert.assertEquals(p.toFile(), obj.files.get(1));
        Assert.assertEquals(p.toFile(), obj.files.get(2));
    }


    @Test
    public void testEditFilePasteStringAction() {
        withClipLock(() -> {
            run(() -> createTable(fileContext, () -> obj.files));

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnFilePath.ColumnFilePathPane pane = (GuiSwingTableColumnFilePath.ColumnFilePathPane) renderer.getComponent();

            //JsonPaste action is not included in column actions. instead, use cell actions.
            List<PopupCategorized.CategorizedMenuItem> items = runGet(() -> model.getBuildersForRowsOrCells(table, Collections.singletonList(objColumn), false));
            ToStringCopyCell.ToStringPasteForCellsAction item = findMenuItemAction(items, ToStringCopyCell.ToStringPasteForCellsAction.class);

            Action action = new ObjectTableModel.TableTargetCellExecutionAction(item, new TableTargetCellForJTable(table));

            File file1 = new File("folder" + File.separator + "a.txt");
            File ex = obj.files.get(1);

            String data = file1.toString();
            setClipboardText(data);

            run(() -> table.addColumnSelectionInterval(0, 0)); //it needs to explicitly select a column
            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            System.out.println(runGet(() -> table.getSelectedColumn()));
            runWait();
            run(() -> action.actionPerformed(null));
            runWait();

            Assert.assertEquals(file1, obj.files.get(0));
            Assert.assertEquals(ex, obj.files.get(1));
            Assert.assertEquals(file1, obj.files.get(2));
        });
    }

    @Test
    public void testEditFileCopyStringAction() {
        withClipLock(() -> {
            run(() -> createTable(fileContext, () -> obj.files));

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnFilePath.ColumnFilePathPane pane = (GuiSwingTableColumnFilePath.ColumnFilePathPane) renderer.getComponent();

            //JsonPaste action is not included in column actions. instead, use cell actions.
            List<PopupCategorized.CategorizedMenuItem> items = runGet(() -> model.getBuildersForRowsOrCells(table, Collections.singletonList(objColumn), false));
            ToStringCopyCell.ToStringCopyForCellsAction item = findMenuItemAction(items, ToStringCopyCell.ToStringCopyForCellsAction.class);

            Action action = new ObjectTableModel.TableTargetCellExecutionAction(item, new TableTargetCellForJTable(table));

            run(() -> table.addColumnSelectionInterval(0, 0)); //it needs to explicitly select a column
            run(() -> table.addRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            System.out.println(runGet(() -> table.getSelectedColumn()));
            runWait();
            run(() -> action.actionPerformed(null));
            runWait();

            String text = getClipboardText();
            Assert.assertEquals(obj.files.get(0).getPath() + "\n" +
                    obj.files.get(2).getPath(), text);
        });
    }


    @Test
    public void testEditFileJsonCopyAction() {
        withClipLock(() -> {
            run(() -> createTable(fileContext, () -> obj.files));

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnFilePath.ColumnFilePathPane pane = (GuiSwingTableColumnFilePath.ColumnFilePathPane) renderer.getComponent();

            //JsonPaste action is not included in column actions. instead, use cell actions.
            List<PopupCategorized.CategorizedMenuItem> items = runGet(() -> model.getBuildersForRowsOrCells(table, Collections.singletonList(objColumn), false));
            GuiSwingJsonTransfer.JsonCopyCellsAction item = findMenuItemAction(items, GuiSwingJsonTransfer.JsonCopyCellsAction.class);

            Action action = new ObjectTableModel.TableTargetCellExecutionAction(item, new TableTargetCellForJTable(table));

            run(() -> table.addColumnSelectionInterval(0, 0)); //it needs to explicitly select a column
            run(() -> table.setRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            System.out.println(runGet(() -> table.getSelectedColumn()));
            runWait();
            run(() -> action.actionPerformed(null));
            String text = getClipboardText();

            Map<String, Object> json1 = new HashMap<>();
            json1.put("File", obj.files.get(0).getPath());
            Map<String, Object> json2 = new HashMap<>();
            json2.put("File", obj.files.get(2).getPath());

            String json = JsonWriter.create().write(Arrays.asList(json1, json2)).toSource();

            Assert.assertEquals(json, text);
        });
    }


    public Action convertRowsAction(Object menu) {
        return (Action) new ObjectTableColumnValue.CollectionRowsActionBuilder(table, objColumn, PopupExtension.MENU_FILTER_IDENTITY).convert(menu);
    }
}
