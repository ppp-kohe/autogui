package org.autogui.swing.table;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingViewImagePane;
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
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GuiSwingTableColumnImageTest extends GuiSwingTestCase {

    GuiTypeBuilder builder;
    GuiMappingContext context;
    GuiTypeObject typeObject;
    TestObj obj;

    GuiMappingContext propContext;
    GuiMappingContext elemContext;
    GuiMappingContext imageContext;

    GuiSwingTableColumnImage column;
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
        obj.values.add(createImage(10));
        obj.values.add(createImage(100));
        obj.values.add(createImage(150));

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        propContext = context.getChildByName("values")
                .getChildByName("List");

        elemContext = propContext.getChildren().get(0);
        imageContext = elemContext.getChildByName("BufferedImage");

        column = new GuiSwingTableColumnImage();
        spec = GuiReprValue.getNoneSupplier();
        specIndex = new GuiSwingTableColumn.SpecifierManagerIndex(spec, 0);
    }

    BufferedImage createImage(int c) {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR);
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                img.setRGB(i, j, new Color(c, c, c).getRGB());
            }
        }
        return img;
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
        public List<BufferedImage> values;
    }


    JScrollPane createTable() {
        objColumn = column.createColumn(imageContext, specIndex, specIndex);

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
        run(this::createTable);

        Assert.assertEquals(obj.values.get(0), runGet(() -> table.getValueAt(0, 0)));
        Assert.assertEquals(obj.values.get(1), runGet(() -> table.getValueAt(1, 0)));
        Assert.assertEquals(obj.values.get(2), runGet(() -> table.getValueAt(2, 0)));
        Assert.assertEquals(3, (int) runGet(table::getRowCount));
    }

    @Test
    public void testEdit() {
        run(this::createTable);
        run(() -> table.editCellAt(1, 0));

        BufferedImage ex1 = obj.values.get(0);
        BufferedImage newImg = createImage(200);

        TableCellEditor editor = runGet(() -> objColumn.getTableColumn().getCellEditor());
        Assert.assertTrue(editor instanceof ObjectTableColumnValue.ObjectTableCellEditor);

        ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;
        Assert.assertEquals(obj.values.get(1), runGet(objEditor::getCellEditorValue));

        GuiSwingTableColumnImage.ColumnEditImagePane component = (GuiSwingTableColumnImage.ColumnEditImagePane) runGet(table::getEditorComponent);
        run(() -> component.setImage(newImg));
        runWait();

        run(() -> {
            GuiSwingViewImagePane.ImageScaleMouseWheel wh = component.getImageScaleMouseWheel();
            component.setImageScale(wh); //copy a zoom value from existing scale-fit to wh
            wh.setCurrentZoom(2.0f); //update the zoom value
        });

        run(objEditor::stopCellEditing);

        Assert.assertEquals(ex1, obj.values.get(0));
        Assert.assertEquals(newImg, obj.values.get(1));

        GuiSwingTableColumnImage.ColumnEditImagePane renderPane = (GuiSwingTableColumnImage.ColumnEditImagePane)
                runGet(() -> ((ObjectTableColumnValue.ObjectTableCellRenderer) objColumn.getTableColumn().getCellRenderer()).getComponent());

        GuiSwingViewImagePane.ImageScale scale = runGet(renderPane::getImageScale);
        Assert.assertTrue("set same scale for viewer from editor", scale instanceof GuiSwingViewImagePane.ImageScaleMouseWheel);
        Assert.assertEquals(2.0f, ((GuiSwingViewImagePane.ImageScaleMouseWheel) scale).getCurrentZoom(), 0.001);
    }

    public Action convertRowsAction(Object menu) {
        return (Action) new ObjectTableColumnValue.CollectionRowsActionBuilder(table, objColumn, PopupExtension.MENU_FILTER_IDENTITY).convert(menu);
    }

    @Test
    public void testScaleSwitchFitAction() {
        run(this::createTable);

        ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                runGet(() -> objColumn.getTableColumn().getCellRenderer());
        GuiSwingTableColumnImage.ColumnEditImagePane component = (GuiSwingTableColumnImage.ColumnEditImagePane) runGet(renderer::getComponent);
        Action action = convertRowsAction(
                findMenuItemAction(component.getSwingStaticMenuItems(), GuiSwingViewImagePane.ImageScaleSwitchFitAction.class));
        run(() -> component.setImageScale(component.getImageScaleMouseWheel()));
        run(() -> action.actionPerformed(null));

        Assert.assertTrue(runGet(component::getImageScale) instanceof GuiSwingViewImagePane.ImageScaleFit);
    }

    @Test
    public void testScaleOriginalSizeAction() {
        run(this::createTable);

        ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                runGet(() -> objColumn.getTableColumn().getCellRenderer());
        GuiSwingTableColumnImage.ColumnEditImagePane component = (GuiSwingTableColumnImage.ColumnEditImagePane) runGet(renderer::getComponent);
        Action action = convertRowsAction(
                findMenuItemAction(component.getSwingStaticMenuItems(), GuiSwingViewImagePane.ImageScaleOriginalSizeAction.class));
        run(() -> component.setImageScale(component.getImageScaleFit()));
        run(() -> action.actionPerformed(null));

        Assert.assertTrue(runGet(component::getImageScale) instanceof GuiSwingViewImagePane.ImageScaleMouseWheel);
    }


    @Test
    public void testScaleSetSize() {
        run(this::createTable);

        ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                runGet(() -> objColumn.getTableColumn().getCellRenderer());
        GuiSwingTableColumnImage.ColumnEditImagePane component = (GuiSwingTableColumnImage.ColumnEditImagePane) runGet(renderer::getComponent);
        PopupCategorized.CategorizedMenuItemComponentDefault action =
                (PopupCategorized.CategorizedMenuItemComponentDefault) new ObjectTableColumnValue.CollectionRowsActionBuilder(table, objColumn, PopupExtension.MENU_FILTER_IDENTITY).convert((
                findMenuItem(component.getSwingStaticMenuItems(),
                        PopupCategorized.CategorizedMenuItemComponentDefault.class, null, null, null, null)));
        run(() -> component.setImageScale(component.getImageScaleFit()));
        run(() -> ((JMenu) action.getMenuItem()).getItem(0).getAction().actionPerformed(null)); //0.5x

        GuiSwingViewImagePane.ImageScale scale = runGet(component::getImageScale);
        Assert.assertTrue(scale instanceof GuiSwingViewImagePane.ImageScaleMouseWheel);
        Assert.assertEquals(0.5f, ((GuiSwingViewImagePane.ImageScaleMouseWheel) scale).getCurrentZoom(), 0.001f);
    }

    @Test
    public void testCopyAction() throws Exception {
        withClipLock(() -> {
            run(this::createTable);

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnImage.ColumnEditImagePane component = (GuiSwingTableColumnImage.ColumnEditImagePane) runGet(renderer::getComponent);
            Action action = convertRowsAction(
                    findMenuItemAction(component.getSwingStaticMenuItems(), GuiSwingViewImagePane.ImageCopyAction.class));

            run(() -> table.addRowSelectionInterval(2, 2));
            run(() -> action.actionPerformed(null));

            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            try {
                BufferedImage img = (BufferedImage) board.getData(DataFlavor.imageFlavor);
                Assert.assertEquals(obj.values.get(2), img);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }


    @Test
    public void testPasteAction() throws Exception {
        withClipLock(() -> {
            run(this::createTable);

            ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                    runGet(() -> objColumn.getTableColumn().getCellRenderer());
            GuiSwingTableColumnImage.ColumnEditImagePane component = (GuiSwingTableColumnImage.ColumnEditImagePane) runGet(renderer::getComponent);
            Action action = convertRowsAction(
                    findMenuItemAction(component.getSwingStaticMenuItems(), GuiSwingViewImagePane.ImagePasteAction.class));

            BufferedImage ex1 = obj.values.get(1);

            BufferedImage img = createImage(200);
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            GuiSwingViewImagePane.ImageSelection sel = new GuiSwingViewImagePane.ImageSelection(img);
            board.setContents(sel, sel);

            run(() -> table.setRowSelectionInterval(0, 0));
            run(() -> table.addRowSelectionInterval(2, 2));
            run(() -> action.actionPerformed(null));

            Assert.assertEquals(img, obj.values.get(0));
            Assert.assertEquals(ex1, obj.values.get(1));
            Assert.assertEquals(img, obj.values.get(2));
        });
    }

    @Test
    public void testSetHistoryAction() {
        GuiPreferences prefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        context.setPreferences(prefs);
        run(this::createTable);

        BufferedImage img = createImage(200);
        {
            run(() -> table.editCellAt(1, 0));

            TableCellEditor editor = runGet(() -> objColumn.getTableColumn().getCellEditor());
            ObjectTableColumnValue.ObjectTableCellEditor objEditor = (ObjectTableColumnValue.ObjectTableCellEditor) editor;


            GuiSwingTableColumnImage.ColumnEditImagePane component = (GuiSwingTableColumnImage.ColumnEditImagePane) runGet(table::getEditorComponent);
            run(() -> component.setImage(img));
            runWait();

            run(objEditor::stopCellEditing);
        }

        BufferedImage ex = obj.values.get(1);

        ObjectTableColumnValue.ObjectTableCellRenderer renderer = (ObjectTableColumnValue.ObjectTableCellRenderer)
                runGet(() -> objColumn.getTableColumn().getCellRenderer());
        GuiSwingTableColumnImage.ColumnEditImagePane component = (GuiSwingTableColumnImage.ColumnEditImagePane) runGet(renderer::getComponent);

        GuiSwingViewImagePane.HistoryMenuItemForTableColumn action = (GuiSwingViewImagePane.HistoryMenuItemForTableColumn)
                new ObjectTableColumnValue.CollectionRowsActionBuilder(table, objColumn, PopupExtension.MENU_FILTER_IDENTITY).convert(
                        component.getSwingStaticMenuItems().stream()
                                .filter(GuiSwingViewImagePane.HistoryMenuImage.class::isInstance)
                                .findFirst().orElseThrow(RuntimeException::new));

        run(action::loadItems);

        run(() -> table.addRowSelectionInterval(0, 0));
        run(() -> table.addRowSelectionInterval(2, 2));
        run(() -> action.getItem(0)
                .getAction().actionPerformed(null));

        runWait();

        Assert.assertEquals(img, obj.values.get(0));
        Assert.assertEquals(ex, obj.values.get(1));
        Assert.assertEquals(img, obj.values.get(2));
    }


}
