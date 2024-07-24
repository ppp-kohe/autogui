package org.autogui.swing.prefs;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValueNumberSpinner;
import org.autogui.base.mapping.GuiRepresentation;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.swing.*;
import org.autogui.swing.mapping.GuiReprValueImagePane;
import org.autogui.swing.table.GuiSwingTableModelCollection;
import org.autogui.swing.util.UIManagerUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GuiSwingPrefsEditorTest extends GuiSwingTestCase {
    GuiTypeBuilder typeBuilder;
    GuiSwingPrefsEditor editor;
    JFrame frame;

    GuiMappingContext context;
    GuiPreferences.GuiValueStore store;
    GuiPreferences prefs;

    List<Path> tempFiles = new ArrayList<>();

    public GuiSwingPrefsEditorTest() {}

    @Before
    public void setUp() {
        typeBuilder = new GuiTypeBuilder();
        editor = new GuiSwingPrefsEditor();

        store = new GuiPreferences.GuiValueStoreOnMemory();
        context = new GuiMappingContext(typeBuilder.get(String.class));
        context.setRepresentation(GuiRepresentation.createValueStringField());
        prefs = new GuiPreferences(store, context);
        context.setPreferences(prefs);
    }

    @Override
    public JFrame createFrame(JComponent pane) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(pane);
        var f = super.createFrame(p);
        f.setSize(new Dimension(800, 600));
        return f;
    }

    @After
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        tempFiles.forEach(f -> {
                try {
                    Files.deleteIfExists(f);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
        });
    }

    @Test
    public void testWindowPrefs() {
        String key = "hello";

        GuiSwingPrefsSupports.PreferencesForWindow prefsProvide = new GuiSwingPrefsSupports.PreferencesForWindow(key);
        prefsProvide.setX(10);
        prefsProvide.setY(20);
        prefsProvide.setWidth(30);
        prefsProvide.setHeight(40);
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createWindowPrefs(key, prefs));

        frame = createFrame(pane);

        var numX = runGetNumber(pane, GuiSwingPrefsEditor.WINDOW_PREFS_PROP_X);
        var numY = runGetNumber(pane, GuiSwingPrefsEditor.WINDOW_PREFS_PROP_Y);
        var numW = runGetNumber(pane, GuiSwingPrefsEditor.WINDOW_PREFS_PROP_WIDTH);
        var numH = runGetNumber(pane, GuiSwingPrefsEditor.WINDOW_PREFS_PROP_HEIGHT);
        Assert.assertEquals("get X", 10, runGet(() -> numX.getEditorField().getValue()));
        Assert.assertEquals("get Y", 20, runGet(() -> numY.getEditorField().getValue()));
        Assert.assertEquals("get W", 30, runGet(() -> numW.getEditorField().getValue()));
        Assert.assertEquals("get H", 40, runGet(() -> numH.getEditorField().getValue()));
        run(() -> numX.getEditorField().setValue(123));
        run(() -> numY.getEditorField().setValue(456));
        run(() -> numW.getEditorField().setValue(789));
        run(() -> numH.getEditorField().setValue(1011));
        runWait();

        GuiSwingPrefsSupports.PreferencesForWindow prefsActual = new GuiSwingPrefsSupports.PreferencesForWindow(key);
        run(() -> prefsActual.loadFrom(prefs));
        Assert.assertEquals("set x", 123, prefsActual.getX());
        Assert.assertEquals("set y", 456, prefsActual.getY());
        Assert.assertEquals("set width", 789, prefsActual.getWidth());
        Assert.assertEquals("set height", 1011, prefsActual.getHeight());
    }

    @Test
    public void testTabPrefs() {
        GuiSwingViewTabbedPane.PreferencesForTab prefsProvide = new GuiSwingViewTabbedPane.PreferencesForTab();
        prefsProvide.setSelectedIndex(10);
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createTabPrefs(prefs));
        frame = createFrame(pane);

        var numSelIdx = runGetNumber(pane, GuiSwingPrefsEditor.TAB_PREFS_PROP_SELECTED);
        Assert.assertEquals("get selectedIndex", 10, runGet(() -> numSelIdx.getEditorField().getValue()));
        run(() -> numSelIdx.getEditorField().setValue(123));
        runWait();

        GuiSwingViewTabbedPane.PreferencesForTab prefsActual = new GuiSwingViewTabbedPane.PreferencesForTab();
        run(() -> prefsActual.loadFrom(prefs));
        Assert.assertEquals("set selectedIndex", 123, prefsActual.getSelectedIndex());
    }

    @Test
    public void testSplitPrefs() {
        GuiSwingViewObjectPane.PreferencesForSplit prefsProvide = new GuiSwingViewObjectPane.PreferencesForSplit();
        prefsProvide.getSplits().add(new GuiSwingViewObjectPane.PreferencesForSplitEntry(10, true));
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createSplitPrefs(prefs));
        UIManagerUtil  u = UIManagerUtil.getInstance();
        pane.setPreferredSize(new Dimension(u.getScaledSizeInt(600), u.getScaledSizeInt(800)));
        frame = createFrame(pane);

        var list = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingPrefsEditor.ValueListPaneSplitEntry.class, t -> true));
        Assert.assertEquals("init size",  (Integer) 1, runGet(() -> list.getModel().getSize()));
        var contentPane = runGet(() -> list.getModel().getElementAt(0).contentPane());
        runWait();
        Assert.assertEquals("init dividerLocation", (Integer) 10, runGet(() -> contentPane.getEntry().dividerLocation));
        Assert.assertTrue("init horizontal", runGet(() -> contentPane.getEntry().horizontal));

        run(() -> GuiSwingView.findComponent(contentPane, GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> true).getEditorField().setValue(123));

        run(list::addNewElement);
        run(list::addNewElement);
        Assert.assertEquals("after add size",  (Integer) 3, runGet(() -> list.getModel().getSize()));
        var contentPaneAdded = runGet(() -> list.getModel().getElementAt(2).contentPane());

        run(() -> GuiSwingView.findComponent(contentPaneAdded, GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> true).getEditorField().setValue(456));
        run(() -> GuiSwingView.findComponent(contentPaneAdded, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class, t -> true).setSwingViewValueWithUpdate(true));
        runWait();

        run(() -> list.getList().clearSelection());
        run(() -> list.getList().setSelectedIndex(1));
        runWait();
        run(list::removeSelectedElements);
        runWait();

        GuiSwingViewObjectPane.PreferencesForSplit prefsActual = new GuiSwingViewObjectPane.PreferencesForSplit();
        run(() -> prefsActual.loadFrom(prefs));
        runWait();
        Assert.assertEquals("size", 2, prefsActual.getSplits().size());
        Assert.assertEquals("dividerLocation", 123, prefsActual.getSplits().get(0).dividerLocation);
        Assert.assertTrue("horizontal", prefsActual.getSplits().get(0).horizontal);
        Assert.assertEquals("dividerLocation2", 456, prefsActual.getSplits().get(1).dividerLocation);
        Assert.assertTrue("horizontal2", prefsActual.getSplits().get(1).horizontal);
    }

    @Test
    public void testFileDialogPrefs() {
        var path1 = Paths.get("a");
        var path2 = Paths.get("b");
        var path3 = Paths.get("c");
        var path4 = Paths.get("d");
        GuiSwingPrefsSupports.PreferencesForFileDialog prefsProvide = new GuiSwingPrefsSupports.PreferencesForFileDialog();
        prefsProvide.setBackPath(path1);
        prefsProvide.setCurrentDirectory(path2);
        prefsProvide.getFileListDirect().add(path1.toString());
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createFileDialogPrefs(prefs));

        var backPathPane = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewFilePathField.PropertyFilePathPane.class,
                t -> t.getName().equals(GuiSwingPrefsEditor.FILE_DIALOG_PREFS_PROP_BACK_PATH)));
        var currDirPane = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewFilePathField.PropertyFilePathPane.class,
                t -> t.getName().equals(GuiSwingPrefsEditor.FILE_DIALOG_PREFS_PROP_CURRENT_DIRECTORY)));
        var fileListPane = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingPrefsEditor.ValueListPaneFileDialogList.class, t -> true));
        runWait();
        Assert.assertEquals("init backPath", path1, runGet(backPathPane::getFile));
        Assert.assertEquals("init currentDirectory", path2, runGet(currDirPane::getFile));

        Assert.assertEquals("init fileList size", (Integer) 1, runGet(() -> fileListPane.getModel().getSize()));
        var elemPane0 = runGet(() -> fileListPane.getModel().getElementAt(0));
        Assert.assertEquals("init fileList 0", path1, runGet(() -> elemPane0.contentPane().getFile()));

        run(fileListPane::addNewElement);
        run(() -> fileListPane.getModel().getElementAt(1).contentPane().setFile(path3));
        run(() -> backPathPane.setFile(path3));
        run(() -> currDirPane.setFile(path4));
        runWait();

        GuiSwingPrefsSupports.PreferencesForFileDialog prefsActual = new GuiSwingPrefsSupports.PreferencesForFileDialog();
        prefsActual.loadFrom(prefs);
        Assert.assertEquals("set backPath",  path3, prefsActual.getBackPath());
        Assert.assertEquals("set currentDirectory",  path4, prefsActual.getCurrentDirectory());
        Assert.assertEquals("set fileList size",  2, prefsActual.getFileListDirect().size());
        Assert.assertEquals("set fileList 0", path1.toString(), prefsActual.getFileListDirect().get(0));
        Assert.assertEquals("set fileList 1", path3.toString(), prefsActual.getFileListDirect().get(1));
    }

    @Test
    public void testDocSettingsPrefs() {
        var nonExistingFont = "UnknownFont";
        var nonExistingFont2 = "UnknownFont2";
        GuiSwingViewDocumentEditor.PreferencesForDocumentSetting prefsProvide = new GuiSwingViewDocumentEditor.PreferencesForDocumentSetting();
        prefsProvide.setBackgroundColor(new Color(10, 20, 30, 40));
        prefsProvide.setForegroundColor(new Color(50, 60, 70, 80));
        prefsProvide.setBold(true);
        prefsProvide.setItalic(true);
        prefsProvide.setLineSpacing(2.5f);
        prefsProvide.setSpaceAbove(3.6f);
        prefsProvide.setWrapText(true);
        prefsProvide.setBackgroundCustom(true);
        prefsProvide.setForegroundCustom(true);
        prefsProvide.setFontFamily(nonExistingFont);
        prefsProvide.setFontSize(24);
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createDocumentSettingPrefs(prefs));
        frame = createFrame(pane);
        runWait(600);

        var docPane = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewDocumentEditor.DocumentSettingPane.class, p -> true));
        Assert.assertEquals("init background", new Color(10, 20, 30, 40), runGet(() -> docPane.getBackgroundColor().getColor()));
        Assert.assertEquals("init foreground", new Color(50, 60, 70, 80), runGet(() -> docPane.getForegroundColor().getColor()));
        Assert.assertTrue("init bold", runGet(() -> docPane.getStyleBold().isSelected()));
        Assert.assertTrue("init italic", runGet(() -> docPane.getStyleItalic().isSelected()));
        Assert.assertEquals("init lineSpacing", 2.5f, ((Number) runGet(() -> docPane.getSpaceLine().getValue())).floatValue(), 0.01f);
        Assert.assertEquals("init spaceAbove", 3.6f, ((Number) runGet(() -> docPane.getSpaceAbove().getValue())).floatValue(), 0.01f);
        Assert.assertTrue("init wapText", runGet(() -> docPane.getStyleWrapLine().isSelected()));
        Assert.assertTrue("init backgroundCustom", runGet(() -> docPane.getBackgroundCustom().isSelected()));
        Assert.assertTrue("init foregroundCustom", runGet(() -> docPane.getForegroundCustom().isSelected()));
        Assert.assertEquals("init fontFamily", nonExistingFont, runGet(() -> docPane.getFontFamily().getModel().getSelectedItem()));
        Assert.assertEquals("init fontSize", 24, ((Number) runGet(() -> docPane.getFontSize().getValue())).intValue());

        run(() -> docPane.getBackgroundColor().setColor(Color.green));
        run(() -> docPane.getForegroundColor().setColor(Color.blue));
        run(() -> docPane.getStyleBold().setSelected(false));
        run(() -> docPane.getStyleItalic().setSelected(false));
        run(() -> docPane.getSpaceLine().setValue(0.6f));
        run(() -> docPane.getSpaceAbove().setValue(0.4f));
        run(() -> docPane.getStyleWrapLine().setSelected(false));
        run(() -> docPane.getBackgroundCustom().setSelected(false));
        run(() -> docPane.getForegroundCustom().setSelected(false));
        run(() -> docPane.getFontFamily().setSelectedItem(nonExistingFont2));
        run(() -> docPane.getFontSize().setValue(11));
        runWait();

        GuiSwingViewDocumentEditor.PreferencesForDocumentSetting prefObjActual = new GuiSwingViewDocumentEditor.PreferencesForDocumentSetting();
        prefObjActual.loadFrom(prefs);
        Assert.assertEquals("set background", Color.green, prefObjActual.getBackgroundColor());
        Assert.assertEquals("set foreground", Color.blue, prefObjActual.getForegroundColor());
        Assert.assertFalse("set bold", prefObjActual.isBold());
        Assert.assertFalse("set italic", prefObjActual.isItalic());
        Assert.assertEquals("set lineSpacing", 0.6f,  prefObjActual.getLineSpacing(),  0.01f);
        Assert.assertEquals("set spaceAbove", 0.4f,  prefObjActual.getSpaceAbove(), 0.01f);
        Assert.assertFalse("set wapText", prefObjActual.isWrapText());
        Assert.assertFalse("set backgroundCustom", prefObjActual.isBackgroundCustom());
        Assert.assertFalse("set foregroundCustom", prefObjActual.isForegroundCustom());
        Assert.assertEquals("set fontFamily", nonExistingFont2, prefObjActual.getFontFamily());
        Assert.assertEquals("set fontSize", 11, prefObjActual.getFontSize());
    }

    @Test
    public void testNumberPrefs() {
        GuiSwingViewNumberSpinner.TypedSpinnerNumberModel prefsProvide = new GuiSwingViewNumberSpinner.TypedSpinnerNumberModel(GuiReprValueNumberSpinner.DOUBLE);
        prefsProvide.setFormatPattern("##_###.####");
        prefsProvide.setMaximum(1000);
        prefsProvide.setMinimum(-3000);
        prefsProvide.setStepSize(0.5);
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createNumberPrefs(GuiReprValueNumberSpinner.DOUBLE, prefs));
        frame = createFrame(pane);

        var settings = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewNumberSpinner.NumberSettingPane.class, t -> true));
        Assert.assertEquals("init pattern", "##_###.####", runGet(() -> settings.getModel().getFormatPattern()));
        Assert.assertEquals("init max", 1000.0, ((Number) runGet(() -> settings.getModel().getMaximum())).doubleValue(), 0.01);
        Assert.assertEquals("init min", -3000.0, ((Number) runGet(() -> settings.getModel().getMinimum())).doubleValue(), 0.01);
        Assert.assertEquals("init stepSize", 0.5, runGet(() -> settings.getModel().getStepSize()).doubleValue(), 0.01);

        run(() -> settings.getFormatField().setText("##.##"));
        run(() -> settings.getMaxSpinner().setValue(12345.6));
        run(() -> settings.getMinSpinner().setValue(789.1));
        run(() -> settings.getStepSpinner().setValue(1.23));
        runWait();

        GuiSwingViewNumberSpinner.TypedSpinnerNumberModel prefsActual = new GuiSwingViewNumberSpinner.TypedSpinnerNumberModel(GuiReprValueNumberSpinner.DOUBLE);
        prefsActual.loadFrom(prefs);

        Assert.assertEquals("set format", "##.##", prefsActual.getFormatPattern());
        Assert.assertEquals("set max", 12345.6, ((Number) prefsActual.getMaximum()).doubleValue(), 0.01);
        Assert.assertEquals("set min", 789.1, ((Number) prefsActual.getMinimum()).doubleValue(), 0.01);
        Assert.assertEquals("set stepSize", 1.23, prefsActual.getStepSize().doubleValue(), 0.01);
    }

    @Test
    public void testTablePrefs() {
        GuiSwingViewCollectionTable.PreferencesForTable prefsProvide = new GuiSwingViewCollectionTable.PreferencesForTable();
        prefsProvide.getColumnOrder().add(10);
        prefsProvide.getColumnOrder().add(20);
        prefsProvide.getColumnWidth().add(100);
        prefsProvide.getColumnWidth().add(200);
        prefsProvide.setRowCustom(true);
        prefsProvide.setRowHeight(30);
        prefsProvide.setRowFitToContent(true);
        prefsProvide.getRowSort().add(new GuiSwingViewCollectionTable.PreferencesForTableRowSort(2, SortOrder.ASCENDING));
        prefsProvide.getRowSort().add(new GuiSwingViewCollectionTable.PreferencesForTableRowSort(1, SortOrder.DESCENDING));
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createTablePrefs(prefs));
        frame = createFrame(pane);
        var colOrder = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewStringField.PropertyStringPane.class, t -> t.getName().equals(GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER)));
        var colWidth = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewStringField.PropertyStringPane.class, t -> t.getName().equals(GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_WIDTH)));
        var rowHeight = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> t.getName().equals(GuiSwingPrefsEditor.TABLE_PREFS_ROW_HEIGHT)));
        var rowCustom = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class, t -> t.getName().equals(GuiSwingPrefsEditor.TABLE_PREFS_ROW_CUSTOM)));
        var fitToContent = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class, t -> t.getName().equals(GuiSwingPrefsEditor.TABLE_PREFS_ROW_FIT_TO_CONTENT)));
        var rowSort = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingPrefsEditor.ValueListTableRowSort.class, t -> true));

        Assert.assertEquals("init colOrder", "10 20", runGet(colOrder::getSwingViewValue));
        Assert.assertEquals("init colWidth", "100 200", runGet(colWidth::getSwingViewValue));
        Assert.assertEquals("init rowHeight", 30, runGet(rowHeight::getSwingViewValue));
        Assert.assertEquals("init rowCustom", true, runGet(rowCustom::getSwingViewValue));
        Assert.assertEquals("init fitTtoContent", true, runGet(fitToContent::getSwingViewValue));
        Assert.assertEquals("init rowSort size", 2, (long ) runGet(() -> rowSort.getModel().getSize()));
        Assert.assertEquals("init rowSort 0 column", 2, runGet(() -> rowSort.getModel().getElementAt(0).value()).getColumn());
        Assert.assertEquals("init rowSort 0 order", SortOrder.ASCENDING.name(), runGet(() -> rowSort.getModel().getElementAt(0).value()).getOrder());
        Assert.assertEquals("init rowSort 1 column", 1, runGet(() -> rowSort.getModel().getElementAt(1).value()).getColumn());
        Assert.assertEquals("init rowSort 1 order", SortOrder.DESCENDING.name(), runGet(() -> rowSort.getModel().getElementAt(1).value()).getOrder());

        run(() -> colOrder.setSwingViewValueWithUpdate("30 40 50"));
        run(() -> colWidth.setSwingViewValueWithUpdate("300 400 500"));
        run(() -> rowHeight.setSwingViewValueWithUpdate(40));
        run(() -> rowCustom.setSwingViewValueWithUpdate(false));
        run(() -> fitToContent.setSwingViewValueWithUpdate(false));
        run(rowSort::addNewElement);
        run(() -> rowSort.getModel().getElementAt(0).contentPane().getColumnNumber().setSwingViewValueWithUpdate(3));
        run(() -> rowSort.getModel().getElementAt(0).contentPane().getOrderEnum().setSwingViewValueWithUpdate(SortOrder.DESCENDING));
        run(() -> rowSort.getModel().getElementAt(2).contentPane().getColumnNumber().setSwingViewValueWithUpdate(10));
        run(() -> rowSort.getModel().getElementAt(2).contentPane().getOrderEnum().setSwingViewValueWithUpdate(SortOrder.ASCENDING));
        runWait();

        GuiSwingViewCollectionTable.PreferencesForTable prefsActual = new GuiSwingViewCollectionTable.PreferencesForTable();
        prefsActual.loadFrom(prefs);
        Assert.assertEquals("set colOrder", List.of(30, 40, 50), prefsActual.getColumnOrder());
        Assert.assertEquals("set colWidth", List.of(300, 400, 500), prefsActual.getColumnWidth());
        Assert.assertEquals("set rowHeight", 40, prefsActual.getRowHeight());
        Assert.assertFalse("set rowCustom", prefsActual.isRowCustom());
        Assert.assertFalse("set fitToContent", prefsActual.isRowFitToContent());
        Assert.assertEquals("set rowSort size", 3, prefsActual.getRowSort().size());
        Assert.assertEquals("set rowSort 0 column", 3, prefsActual.getRowSort().getFirst().getColumn());
        Assert.assertEquals("set rowSort 0 order", SortOrder.DESCENDING.name(), prefsActual.getRowSort().getFirst().getOrder());
        Assert.assertEquals("set rowSort 2 column", 10, prefsActual.getRowSort().get(2).getColumn());
        Assert.assertEquals("set rowSort 2 order", SortOrder.ASCENDING.name(), prefsActual.getRowSort().get(2).getOrder());
    }

    @Test
    public void testTableColumnWidthStatic() {
        GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic prefsProvide = new GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic();
        prefsProvide.put(0, new GuiSwingTableModelCollection.PreferencesForTableColumnWidth(123));
        prefsProvide.put(1, new GuiSwingTableModelCollection.PreferencesForTableColumnWidth(456));
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createTableColumnWidthStatic(prefs));
        frame = createFrame(pane);

        var mapPane = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingPrefsEditor.ValueListMapPaneForTableColumnWidthStatic.class, t -> true));
        Assert.assertEquals("init size", 2, ((Number) runGet(() -> mapPane.getModel().getSize())));

        var item1 = runGet(() -> GuiSwingView.findComponent(mapPane.getModel().getElementAt(0).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_WIDTH)));
        var item2 = runGet(() -> GuiSwingView.findComponent(mapPane.getModel().getElementAt(1).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_WIDTH)));
        Assert.assertEquals("init 0 key", 123, runGet(item1::getValue));
        Assert.assertEquals("init 1 key", 456, runGet(item2::getValue));

        run(() -> item2.setSwingViewValueWithUpdate(100));
        run(mapPane::addNewElement);
        var item3 = runGet(() -> GuiSwingView.findComponent(mapPane.getModel().getElementAt(2).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_WIDTH)));
        run(() -> item3.setSwingViewValueWithUpdate(200));
        runWait();

        var item3key = runGet(() -> GuiSwingView.findComponent(mapPane.getModel().getElementAt(2).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_INDEX)));
        run(() -> item3key.setSwingViewValueWithUpdate(3));
        runWait();

        GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic prefsActual = new GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic();
        prefsActual.loadFrom(prefs);
        Assert.assertEquals("size ", 3, prefsActual.getModelIndexToWidthDirect().size());
        Assert.assertEquals("set 0 ", 123, prefsActual.getModelIndexToWidthDirect().get(0).getWidth());
        Assert.assertEquals("set 1 ", 100, prefsActual.getModelIndexToWidthDirect().get(1).getWidth());
        Assert.assertEquals("set 2 ", 200, prefsActual.getModelIndexToWidthDirect().get(3).getWidth());

        run(() -> item3key.setSwingViewValueWithUpdate(1));
        runWait();
        GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic prefsActual2 = new GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic();
        prefsActual2.loadFrom(prefs);
        Assert.assertEquals("merged key size ", 2, prefsActual2.getModelIndexToWidthDirect().size());
        Assert.assertEquals("merged value 0 ", 123, prefsActual2.getModelIndexToWidthDirect().get(0).getWidth());
        Assert.assertEquals("merged value 1 ", 200, prefsActual2.getModelIndexToWidthDirect().get(1).getWidth());
    }

    @Test
    public void testTableColumnOrderStatic() {
        GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic prefsProvide = new GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic();
        prefsProvide.put(0, new GuiSwingTableModelCollection.PreferencesForTableColumnOrder(10, 123));
        prefsProvide.put(1, new GuiSwingTableModelCollection.PreferencesForTableColumnOrder(20, 456));
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createTableColumnOrderStatic(prefs));
        frame = createFrame(pane);

        var mapPane = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingPrefsEditor.ValueListMapPaneForTableColumnOrder.class, t -> true));
        Assert.assertEquals("init size", 2, ((Number) runGet(() -> mapPane.getModel().getSize())));

        var itemModel1 = runGet(() -> GuiSwingView.findComponent(mapPane.getModel().getElementAt(0).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_MODEL_INDEX)));
        var itemModel2 = runGet(() -> GuiSwingView.findComponent(mapPane.getModel().getElementAt(1).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_MODEL_INDEX)));
        var itemView1 = runGet(() -> GuiSwingView.findComponent(mapPane.getModel().getElementAt(0).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_VIEW_INDEX)));
        var itemView2 = runGet(() -> GuiSwingView.findComponent(mapPane.getModel().getElementAt(1).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_VIEW_INDEX)));
        Assert.assertEquals("init 0 key modelIndex", 10, runGet(itemModel1::getValue));
        Assert.assertEquals("init 1 key modelIndex", 20, runGet(itemModel2::getValue));
        Assert.assertEquals("init 0 key viewIndex", 123, runGet(itemView1::getValue));
        Assert.assertEquals("init 1 key viewIndex", 456, runGet(itemView2::getValue));
        run(() -> itemModel1.setSwingViewValueWithUpdate(100));
        run(() -> itemView2.setSwingViewValueWithUpdate(200));
        run(mapPane::addNewElement);

        var itemModel3 = runGet(() -> GuiSwingView.findComponent(mapPane.getModel().getElementAt(2).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_MODEL_INDEX)));
        run(() -> itemModel3.setSwingViewValueWithUpdate(300));
        var itemView3 = runGet(() -> GuiSwingView.findComponent(mapPane.getModel().getElementAt(2).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_VIEW_INDEX)));
        run(() -> itemView3.setSwingViewValueWithUpdate(400));
        runWait();

        var item3key = runGet(() -> GuiSwingView.findComponent(mapPane.getModel().getElementAt(2).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_INDEX)));
        run(() -> item3key.setSwingViewValueWithUpdate(3));
        runWait();

        GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic prefsActual = new GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic();
        prefsActual.loadFrom(prefs);
        Assert.assertEquals("size ", 3, prefsActual.getModelIndexToOrderDirect().size());
        Assert.assertEquals("set 0 modelIndex", 100, prefsActual.getModelIndexToOrderDirect().get(0).getModelIndex());
        Assert.assertEquals("set 1 modelIndex", 20, prefsActual.getModelIndexToOrderDirect().get(1).getModelIndex());
        Assert.assertEquals("set 2 modelIndex", 300, prefsActual.getModelIndexToOrderDirect().get(3).getModelIndex());
        Assert.assertEquals("set 0 viewIndex", 123, prefsActual.getModelIndexToOrderDirect().get(0).getViewIndex());
        Assert.assertEquals("set 1 viewIndex", 200, prefsActual.getModelIndexToOrderDirect().get(1).getViewIndex());
        Assert.assertEquals("set 2 viewIndex", 400, prefsActual.getModelIndexToOrderDirect().get(3).getViewIndex());

        run(() -> item3key.setSwingViewValueWithUpdate(1));
        runWait();
        GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic prefsActual2 = new GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic();
        prefsActual2.loadFrom(prefs);
        Assert.assertEquals("merged key size ", 2, prefsActual2.getModelIndexToOrderDirect().size());
        Assert.assertEquals("merged 0 modelIndex", 100, prefsActual2.getModelIndexToOrderDirect().get(0).getModelIndex());
        Assert.assertEquals("merged 1 modelIndex", 300, prefsActual2.getModelIndexToOrderDirect().get(1).getModelIndex());
        Assert.assertEquals("merged 0 viewIndex", 123, prefsActual2.getModelIndexToOrderDirect().get(0).getViewIndex());
        Assert.assertEquals("merged 1 viewIndex", 400, prefsActual2.getModelIndexToOrderDirect().get(1).getViewIndex());
    }

    @Test
    public void testTableColumnWidth() {
        GuiSwingTableModelCollection.PreferencesForTableColumnWidth prefsProvide = new GuiSwingTableModelCollection.PreferencesForTableColumnWidth();
        prefsProvide.setWidth(123);
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createTableColumnWidth(prefs));
        frame = createFrame(pane);
        runWait();

        var wPane = runGetNumber(pane, GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_WIDTH);
        Assert.assertEquals("init view", 123, ((Number) runGet(wPane::getSwingViewValue)).intValue());
        run(() -> wPane.setSwingViewValueWithUpdate(100));
        runWait();

        GuiSwingTableModelCollection.PreferencesForTableColumnWidth prefsActual = new GuiSwingTableModelCollection.PreferencesForTableColumnWidth();
        prefsActual.loadFrom(prefs);
        Assert.assertEquals("set value", 100, prefsActual.getWidth());
    }

    @Test
    public void testTableColumnOrder() {
        GuiSwingTableModelCollection.PreferencesForTableColumnOrder prefsProvide = new GuiSwingTableModelCollection.PreferencesForTableColumnOrder();
        prefsProvide.setModelIndex(123);
        prefsProvide.setViewIndex(456);
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createTableColumnOrder(prefs));
        frame = createFrame(pane);
        runWait();

        var mPane = runGetNumber(pane, GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_MODEL_INDEX);
        var vPane = runGetNumber(pane, GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_VIEW_INDEX);
        Assert.assertEquals("init model", 123, ((Number) runGet(mPane::getSwingViewValue)).intValue());
        Assert.assertEquals("init view", 456, ((Number) runGet(vPane::getSwingViewValue)).intValue());
        run(() -> mPane.setSwingViewValueWithUpdate(100));
        run(() -> vPane.setSwingViewValueWithUpdate(200));
        runWait();

        GuiSwingTableModelCollection.PreferencesForTableColumnOrder prefsActual = new GuiSwingTableModelCollection.PreferencesForTableColumnOrder();
        prefsActual.loadFrom(prefs);
        Assert.assertEquals("set value model", 100, prefsActual.getModelIndex());
        Assert.assertEquals("set value view", 200, prefsActual.getViewIndex());

    }

    @Test
    public void testHistoryString() {
        prefs.setCurrentValue("HelloWorld");
        var pane = runGet(() -> editor.createLastHistoryValueBySwingViewHistoryValue(prefs, prefs.getCurrentValue()));
        frame = createFrame(pane);

        var text = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewStringField.PropertyStringPane.class, t -> true));
        runWait();
        Assert.assertEquals("init text", "HelloWorld", runGet(text::getSwingViewValue));

        run(() -> text.setSwingViewValueWithUpdate("UPDATED"));
        runWait();

        Assert.assertEquals("update prefs current value", "UPDATED", prefs.getCurrentValue());
    }

    @Test
    public void testHistoryBoolean() {
        context = new GuiMappingContext(typeBuilder.get(Boolean.class));
        context.setRepresentation(GuiRepresentation.createValueBooleanCheckBox());
        prefs = new GuiPreferences(store, context);

        prefs.setCurrentValue(true);
        var pane = runGet(() -> editor.createLastHistoryValueBySwingViewHistoryValue(prefs, prefs.getCurrentValue()));
        frame = createFrame(pane);

        var value = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class, t -> true));
        runWait();
        Assert.assertEquals("init boolean", Boolean.TRUE, runGet(value::getSwingViewValue));

        run(() -> value.setSwingViewValueWithUpdate(false));
        runWait();

        Assert.assertEquals("update prefs current value", Boolean.FALSE, prefs.getCurrentValue());
    }

    @Test
    public void testHistoryNumber() {
        context = new GuiMappingContext(typeBuilder.get(Float.class));
        context.setRepresentation(GuiRepresentation.createValueNumberSpinner()
                .createNumberSpinner(GuiReprValueNumberSpinner.FLOAT));
        prefs = new GuiPreferences(store, context);

        prefs.setCurrentValue(10.234f);
        var pane = runGet(() -> editor.createLastHistoryValueBySwingViewHistoryValue(prefs, prefs.getCurrentValue()));
        frame = createFrame(pane);

        var value = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> true));
        runWait();
        Assert.assertEquals("init float", 10.234f, ((Number)runGet(value::getSwingViewValue)).floatValue(), 0.001f);

        run(() -> value.setSwingViewValueWithUpdate(456.789f));
        runWait();

        Assert.assertEquals("update prefs current value", 456.789f, ((Number)prefs.getCurrentValue()).floatValue(), 0.001f);
    }

    @Test
    public void testHistoryImage() throws Exception {
        context = new GuiMappingContext(typeBuilder.get(Image.class));
        context.setRepresentation(GuiReprValueImagePane.getInstance());
        prefs = new GuiPreferences(store, context);

        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR);
        {
            var g = img.createGraphics();
            g.setColor(Color.white);
            g.fillRect(0, 0, 16, 16);
            g.setColor(Color.blue);
            g.fillRect(8, 8, 2, 2);
            g.dispose();
        }
        BufferedImage img2 = new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR);
        {
            var g = img2.createGraphics();
            g.setColor(Color.red);
            g.fillRect(0, 0, 16, 16);
            g.setColor(Color.green);
            g.fillRect(8, 8, 2, 2);
            g.dispose();
        }

        var imgFile = Files.createTempFile(getClass().getSimpleName(), ".png");
        ImageIO.write(img2, "png", imgFile.toFile());
        tempFiles.add(imgFile);

        prefs.setCurrentValue(img);
        var pane = runGet(() -> editor.createLastHistoryValueBySwingViewHistoryValue(prefs, prefs.getCurrentValue()));
        frame = createFrame(pane);

        var value = runGet(() -> GuiSwingView.findComponent(pane, GuiSwingViewImagePane.PropertyImagePane.class, t -> true));
        runWait();

        Assert.assertEquals("init image", toStr(img), toStr((BufferedImage) runGet(value::getImage)));

        run(() -> {
                value.setImagePath(img2, imgFile);
                value.setImage(img2);
            });
        runWait(2_000);

        var outImg = (GuiReprValueImagePane.ImageHistoryEntry) prefs.getCurrentValue();

        Assert.assertEquals("update prefs current value path", imgFile, outImg.getPath());
        Assert.assertEquals("update prefs current value img", toStr(img2), toStr((BufferedImage) outImg.getImage()));
    }

    @Test
    public void testHistoryValues() {
        var t1 = Instant.ofEpochSecond(100);
        var t2 = Instant.ofEpochSecond(200);
        prefs.addHistoryValue("hello", t1);
        prefs.addHistoryValue("world", t2);

        var pane = runGet(() -> editor.createHistoryValuesPane(prefs));
        frame = createFrame(pane);
        runWait();

        var item0 = runGet(() -> GuiSwingView.findComponent(pane.getModel().getElementAt(0).contentPane(), GuiSwingViewStringField.PropertyStringPane.class, t -> true));
        var item1 = runGet(() -> GuiSwingView.findComponent(pane.getModel().getElementAt(1).contentPane(), GuiSwingViewStringField.PropertyStringPane.class, t -> true));

        Assert.assertEquals("init 0", "hello", runGet(item0::getSwingViewValue));
        Assert.assertEquals("init 1", "world", runGet(item1::getSwingViewValue));

        //runWait(50000);
    }

    public String toStr(BufferedImage img) {
        StringBuilder buf = new StringBuilder();
        for (int j = 0; j < img.getHeight(); ++j) {
            for (int i = 0; i < img.getWidth(); ++i) {
                int rgb = img.getRGB(i, j);
                if (i > 0) {
                    buf.append(",");
                }
                int r = (0xFF & (rgb >> 16)) ;
                int g = (0xFF & (rgb >> 8)) ;
                int b = (0xFF & rgb);
                buf.append(String.format("[%d,%d,%d]", r,g,b));
            }
            buf.append("\n");
        }
        return buf.toString();
    }

    public GuiSwingViewNumberSpinner.PropertyNumberSpinner runGetNumber(JComponent pane, String name) {
        return this.runGet(() ->
                GuiSwingView.findComponent(pane, GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> t.getName().equals(name)));
    }
}
