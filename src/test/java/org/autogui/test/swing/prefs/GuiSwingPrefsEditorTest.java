package org.autogui.test.swing.prefs;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValueNumberSpinner;
import org.autogui.base.mapping.GuiRepresentation;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.swing.*;
import org.autogui.swing.mapping.GuiReprValueImagePane;
import org.autogui.swing.prefs.GuiSwingPrefsEditor;
import org.autogui.swing.prefs.GuiSwingPrefsSupports;
import org.autogui.swing.table.GuiSwingTableModelCollection;
import org.autogui.swing.util.TextPaneBuilder;
import org.autogui.swing.util.UIManagerUtil;
import org.autogui.test.swing.GuiSwingTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static org.junit.jupiter.api.Assertions.*;

public class GuiSwingPrefsEditorTest extends GuiSwingTestCase {
    GuiTypeBuilder typeBuilder;
    GuiSwingPrefsEditor editor;
    JFrame frame;

    GuiMappingContext context;
    GuiPreferences.GuiValueStore store;
    GuiPreferences prefs;

    List<Path> tempFiles = new ArrayList<>();

    public GuiSwingPrefsEditorTest() {
    }

    @BeforeEach
    public void setUp() {
        timeFactor = 1;
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

    @AfterEach
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
        assertEquals(10, runGet(() -> numX.getEditorField().getValue()), "get X");
        assertEquals(20, runGet(() -> numY.getEditorField().getValue()), "get Y");
        assertEquals(30, runGet(() -> numW.getEditorField().getValue()), "get W");
        assertEquals(40, runGet(() -> numH.getEditorField().getValue()), "get H");
        run(() -> numX.getEditorField().setValue(123));
        run(() -> numY.getEditorField().setValue(456));
        run(() -> numW.getEditorField().setValue(789));
        run(() -> numH.getEditorField().setValue(1011));
        runWait();

        GuiSwingPrefsSupports.PreferencesForWindow prefsActual = new GuiSwingPrefsSupports.PreferencesForWindow(key);
        run(() -> prefsActual.loadFrom(prefs));
        assertEquals(123, prefsActual.getX(), "set x");
        assertEquals(456, prefsActual.getY(), "set y");
        assertEquals(789, prefsActual.getWidth(), "set width");
        assertEquals(1011, prefsActual.getHeight(), "set height");
    }

    @Test
    public void testTabPrefs() {
        GuiSwingViewTabbedPane.PreferencesForTab prefsProvide = new GuiSwingViewTabbedPane.PreferencesForTab();
        prefsProvide.setSelectedIndex(10);
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createTabPrefs(prefs));
        frame = createFrame(pane);

        var numSelIdx = runGetNumber(pane, GuiSwingPrefsEditor.TAB_PREFS_PROP_SELECTED);
        assertEquals(10, runGet(() -> numSelIdx.getEditorField().getValue()), "get selectedIndex");
        run(() -> numSelIdx.getEditorField().setValue(123));
        runWait();

        GuiSwingViewTabbedPane.PreferencesForTab prefsActual = new GuiSwingViewTabbedPane.PreferencesForTab();
        run(() -> prefsActual.loadFrom(prefs));
        assertEquals(123, prefsActual.getSelectedIndex(), "set selectedIndex");
    }

    @Test
    public void testSplitPrefs() {
        GuiSwingViewObjectPane.PreferencesForSplit prefsProvide = new GuiSwingViewObjectPane.PreferencesForSplit();
        prefsProvide.getSplits().add(new GuiSwingViewObjectPane.PreferencesForSplitEntry(10, true));
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createSplitPrefs(prefs));
        UIManagerUtil u = UIManagerUtil.getInstance();
        pane.setPreferredSize(new Dimension(u.getScaledSizeInt(600), u.getScaledSizeInt(800)));
        frame = createFrame(pane);

        var list = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingPrefsEditor.ValueListPaneSplitEntry.class));
        assertEquals((Integer) 1, runGet(() -> list.getModel().getSize()), "init size");
        var contentPane = runGet(() -> list.getModel().getElementAt(0).contentPane());
        runWait();
        assertEquals((Integer) 10, runGet(() -> contentPane.getEntry().dividerLocation), "init dividerLocation");
        assertTrue(runGet(() -> contentPane.getEntry().horizontal), "init horizontal");

        run(() -> GuiSwingView.findDescendantByType(contentPane, GuiSwingViewNumberSpinner.PropertyNumberSpinner.class).getEditorField().setValue(123));

        run(list::addNewElement);
        run(list::addNewElement);
        assertEquals((Integer) 3, runGet(() -> list.getModel().getSize()), "after add size");
        var contentPaneAdded = runGet(() -> list.getModel().getElementAt(2).contentPane());

        run(() -> GuiSwingView.findDescendantByType(contentPaneAdded, GuiSwingViewNumberSpinner.PropertyNumberSpinner.class).getEditorField().setValue(456));
        run(() -> GuiSwingView.findDescendantByType(contentPaneAdded, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class).setSwingViewValueWithUpdate(true));
        runWait();

        run(() -> list.getList().clearSelection());
        run(() -> list.getList().setSelectedIndex(1));
        runWait();
        run(list::removeSelectedElements);
        runWait();

        GuiSwingViewObjectPane.PreferencesForSplit prefsActual = new GuiSwingViewObjectPane.PreferencesForSplit();
        run(() -> prefsActual.loadFrom(prefs));
        runWait();
        assertEquals(2, prefsActual.getSplits().size(), "size");
        assertEquals(123, prefsActual.getSplits().get(0).dividerLocation, "dividerLocation");
        assertTrue(prefsActual.getSplits().get(0).horizontal, "horizontal");
        assertEquals(456, prefsActual.getSplits().get(1).dividerLocation, "dividerLocation2");
        assertTrue(prefsActual.getSplits().get(1).horizontal, "horizontal2");
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

        var backPathPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewFilePathField.PropertyFilePathPane.class,
                t -> t.getName().equals(GuiSwingPrefsEditor.FILE_DIALOG_PREFS_PROP_BACK_PATH)));
        var currDirPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewFilePathField.PropertyFilePathPane.class,
                t -> t.getName().equals(GuiSwingPrefsEditor.FILE_DIALOG_PREFS_PROP_CURRENT_DIRECTORY)));
        var fileListPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingPrefsEditor.ValueListPaneFileDialogList.class));
        runWait();
        assertEquals(path1, runGet(backPathPane::getFile), "init backPath");
        assertEquals(path2, runGet(currDirPane::getFile), "init currentDirectory");

        assertEquals((Integer) 1, runGet(() -> fileListPane.getModel().getSize()), "init fileList size");
        var elemPane0 = runGet(() -> fileListPane.getModel().getElementAt(0));
        assertEquals(path1, runGet(() -> elemPane0.contentPane().getFile()), "init fileList 0");

        run(fileListPane::addNewElement);
        run(() -> fileListPane.getModel().getElementAt(1).contentPane().setFile(path3));
        run(() -> backPathPane.setFile(path3));
        run(() -> currDirPane.setFile(path4));
        runWait();

        GuiSwingPrefsSupports.PreferencesForFileDialog prefsActual = new GuiSwingPrefsSupports.PreferencesForFileDialog();
        prefsActual.loadFrom(prefs);
        assertEquals(path3, prefsActual.getBackPath(), "set backPath");
        assertEquals(path4, prefsActual.getCurrentDirectory(), "set currentDirectory");
        assertEquals(2, prefsActual.getFileListDirect().size(), "set fileList size");
        assertEquals(path1.toString(), prefsActual.getFileListDirect().get(0), "set fileList 0");
        assertEquals(path3.toString(), prefsActual.getFileListDirect().get(1), "set fileList 1");
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

        var docPane = runGet(() -> GuiSwingView.findDescendantByType(pane, TextPaneBuilder.DocumentSettingPaneBase.class));
        assertEquals(new Color(10, 20, 30, 40), runGet(() -> docPane.getBackgroundColor().getColor()), "init background");
        assertEquals(new Color(50, 60, 70, 80), runGet(() -> docPane.getForegroundColor().getColor()), "init foreground");
        assertTrue(runGet(() -> docPane.getStyleBold().isSelected()), "init bold");
        assertTrue(runGet(() -> docPane.getStyleItalic().isSelected()), "init italic");
        assertEquals(2.5f, ((Number) runGet(() -> docPane.getSpaceLine().getValue())).floatValue(), 0.01f, "init lineSpacing");
        assertEquals(3.6f, ((Number) runGet(() -> docPane.getSpaceAbove().getValue())).floatValue(), 0.01f, "init spaceAbove");
        assertTrue(runGet(() -> docPane.getStyleWrapLine().isSelected()), "init wapText");
        assertTrue(runGet(() -> docPane.getBackgroundCustom().isSelected()), "init backgroundCustom");
        assertTrue(runGet(() -> docPane.getForegroundCustom().isSelected()), "init foregroundCustom");
        assertEquals(nonExistingFont, runGet(() -> docPane.getFontFamily().getModel().getSelectedItem()), "init fontFamily");
        assertEquals(24, ((Number) runGet(() -> docPane.getFontSize().getValue())).intValue(), "init fontSize");

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
        assertEquals(Color.green, prefObjActual.getBackgroundColor(), "set background");
        assertEquals(Color.blue, prefObjActual.getForegroundColor(), "set foreground");
        assertFalse(prefObjActual.isBold(), "set bold");
        assertFalse(prefObjActual.isItalic(), "set italic");
        assertEquals(0.6f, prefObjActual.getLineSpacing(), 0.01f, "set lineSpacing");
        assertEquals(0.4f, prefObjActual.getSpaceAbove(), 0.01f, "set spaceAbove");
        assertFalse(prefObjActual.isWrapText(), "set wapText");
        assertFalse(prefObjActual.isBackgroundCustom(), "set backgroundCustom");
        assertFalse(prefObjActual.isForegroundCustom(), "set foregroundCustom");
        assertEquals(nonExistingFont2, prefObjActual.getFontFamily(), "set fontFamily");
        assertEquals(11, prefObjActual.getFontSize(), "set fontSize");
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

        var settings = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewNumberSpinner.NumberSettingPane.class));
        assertEquals("##_###.####", runGet(() -> settings.getModel().getFormatPattern()), "init pattern");
        assertEquals(1000.0, ((Number) runGet(() -> settings.getModel().getMaximum())).doubleValue(), 0.01, "init max");
        assertEquals(-3000.0, ((Number) runGet(() -> settings.getModel().getMinimum())).doubleValue(), 0.01, "init min");
        assertEquals(0.5, runGet(() -> settings.getModel().getStepSize()).doubleValue(), 0.01, "init stepSize");

        run(() -> settings.getFormatField().setText("##.##"));
        run(() -> settings.getMaxSpinner().setValue(12345.6));
        run(() -> settings.getMinSpinner().setValue(789.1));
        run(() -> settings.getStepSpinner().setValue(1.23));
        runWait();

        GuiSwingViewNumberSpinner.TypedSpinnerNumberModel prefsActual = new GuiSwingViewNumberSpinner.TypedSpinnerNumberModel(GuiReprValueNumberSpinner.DOUBLE);
        prefsActual.loadFrom(prefs);

        assertEquals("##.##", prefsActual.getFormatPattern(), "set format");
        assertEquals(12345.6, ((Number) prefsActual.getMaximum()).doubleValue(), 0.01, "set max");
        assertEquals(789.1, ((Number) prefsActual.getMinimum()).doubleValue(), 0.01, "set min");
        assertEquals(1.23, prefsActual.getStepSize().doubleValue(), 0.01, "set stepSize");
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
        var colOrder = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class, t -> t.getName().equals(GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER)));
        var colWidth = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class, t -> t.getName().equals(GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_WIDTH)));
        var rowHeight = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> t.getName().equals(GuiSwingPrefsEditor.TABLE_PREFS_ROW_HEIGHT)));
        var rowCustom = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class, t -> t.getName().equals(GuiSwingPrefsEditor.TABLE_PREFS_ROW_CUSTOM)));
        var fitToContent = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class, t -> t.getName().equals(GuiSwingPrefsEditor.TABLE_PREFS_ROW_FIT_TO_CONTENT)));
        var rowSort = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingPrefsEditor.ValueListTableRowSort.class));

        assertEquals("10 20", runGet(colOrder::getSwingViewValue), "init colOrder");
        assertEquals("100 200", runGet(colWidth::getSwingViewValue), "init colWidth");
        assertEquals(30, runGet(rowHeight::getSwingViewValue), "init rowHeight");
        assertEquals(true, runGet(rowCustom::getSwingViewValue), "init rowCustom");
        assertEquals(true, runGet(fitToContent::getSwingViewValue), "init fitTtoContent");
        assertEquals(2, (long) runGet(() -> rowSort.getModel().getSize()), "init rowSort size");
        assertEquals(2, runGet(() -> rowSort.getModel().getElementAt(0).value()).getColumn(), "init rowSort 0 column");
        assertEquals(SortOrder.ASCENDING.name(), runGet(() -> rowSort.getModel().getElementAt(0).value()).getOrder(), "init rowSort 0 order");
        assertEquals(1, runGet(() -> rowSort.getModel().getElementAt(1).value()).getColumn(), "init rowSort 1 column");
        assertEquals(SortOrder.DESCENDING.name(), runGet(() -> rowSort.getModel().getElementAt(1).value()).getOrder(), "init rowSort 1 order");

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
        assertEquals(List.of(30, 40, 50), prefsActual.getColumnOrder(), "set colOrder");
        assertEquals(List.of(300, 400, 500), prefsActual.getColumnWidth(), "set colWidth");
        assertEquals(40, prefsActual.getRowHeight(), "set rowHeight");
        assertFalse(prefsActual.isRowCustom(), "set rowCustom");
        assertFalse(prefsActual.isRowFitToContent(), "set fitToContent");
        assertEquals(3, prefsActual.getRowSort().size(), "set rowSort size");
        assertEquals(3, prefsActual.getRowSort().getFirst().getColumn(), "set rowSort 0 column");
        assertEquals(SortOrder.DESCENDING.name(), prefsActual.getRowSort().getFirst().getOrder(), "set rowSort 0 order");
        assertEquals(10, prefsActual.getRowSort().get(2).getColumn(), "set rowSort 2 column");
        assertEquals(SortOrder.ASCENDING.name(), prefsActual.getRowSort().get(2).getOrder(), "set rowSort 2 order");
    }

    @Test
    public void testTableColumnWidthStatic() {
        GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic prefsProvide = new GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic();
        prefsProvide.put(0, new GuiSwingTableModelCollection.PreferencesForTableColumnWidth(123));
        prefsProvide.put(1, new GuiSwingTableModelCollection.PreferencesForTableColumnWidth(456));
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createTableColumnWidthStatic(prefs));
        frame = createFrame(pane);

        var mapPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingPrefsEditor.ValueListMapPaneForTableColumnWidthStatic.class));
        assertEquals(2, ((Number) runGet(() -> mapPane.getModel().getSize())), "init size");

        var item1 = runGet(() -> GuiSwingView.findDescendantByType(mapPane.getModel().getElementAt(0).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_WIDTH)));
        var item2 = runGet(() -> GuiSwingView.findDescendantByType(mapPane.getModel().getElementAt(1).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_WIDTH)));
        assertEquals(123, runGet(item1::getValue), "init 0 key");
        assertEquals(456, runGet(item2::getValue), "init 1 key");

        run(() -> item2.setSwingViewValueWithUpdate(100));
        run(mapPane::addNewElement);
        var item3 = runGet(() -> GuiSwingView.findDescendantByType(mapPane.getModel().getElementAt(2).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_WIDTH)));
        run(() -> item3.setSwingViewValueWithUpdate(200));
        runWait();

        var item3key = runGet(() -> GuiSwingView.findDescendantByType(mapPane.getModel().getElementAt(2).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_INDEX)));
        run(() -> item3key.setSwingViewValueWithUpdate(3));
        runWait();

        GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic prefsActual = new GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic();
        prefsActual.loadFrom(prefs);
        assertEquals(3, prefsActual.getModelIndexToWidthDirect().size(), "size ");
        assertEquals(123, prefsActual.getModelIndexToWidthDirect().get(0).getWidth(), "set 0 ");
        assertEquals(100, prefsActual.getModelIndexToWidthDirect().get(1).getWidth(), "set 1 ");
        assertEquals(200, prefsActual.getModelIndexToWidthDirect().get(3).getWidth(), "set 2 ");

        run(() -> item3key.setSwingViewValueWithUpdate(1));
        runWait();
        GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic prefsActual2 = new GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic();
        prefsActual2.loadFrom(prefs);
        assertEquals(2, prefsActual2.getModelIndexToWidthDirect().size(), "merged key size ");
        assertEquals(123, prefsActual2.getModelIndexToWidthDirect().get(0).getWidth(), "merged value 0 ");
        assertEquals(200, prefsActual2.getModelIndexToWidthDirect().get(1).getWidth(), "merged value 1 ");
    }

    @Test
    public void testTableColumnOrderStatic() {
        GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic prefsProvide = new GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic();
        prefsProvide.put(0, new GuiSwingTableModelCollection.PreferencesForTableColumnOrder(10, 123));
        prefsProvide.put(1, new GuiSwingTableModelCollection.PreferencesForTableColumnOrder(20, 456));
        prefsProvide.saveTo(prefs);

        var pane = runGet(() -> editor.createTableColumnOrderStatic(prefs));
        frame = createFrame(pane);

        var mapPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingPrefsEditor.ValueListMapPaneForTableColumnOrder.class));
        assertEquals(2, ((Number) runGet(() -> mapPane.getModel().getSize())), "init size");

        var itemModel1 = runGet(() -> GuiSwingView.findDescendantByType(mapPane.getModel().getElementAt(0).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_MODEL_INDEX)));
        var itemModel2 = runGet(() -> GuiSwingView.findDescendantByType(mapPane.getModel().getElementAt(1).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_MODEL_INDEX)));
        var itemView1 = runGet(() -> GuiSwingView.findDescendantByType(mapPane.getModel().getElementAt(0).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_VIEW_INDEX)));
        var itemView2 = runGet(() -> GuiSwingView.findDescendantByType(mapPane.getModel().getElementAt(1).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_VIEW_INDEX)));
        assertEquals(10, runGet(itemModel1::getValue), "init 0 key modelIndex");
        assertEquals(20, runGet(itemModel2::getValue), "init 1 key modelIndex");
        assertEquals(123, runGet(itemView1::getValue), "init 0 key viewIndex");
        assertEquals(456, runGet(itemView2::getValue), "init 1 key viewIndex");
        run(() -> itemModel1.setSwingViewValueWithUpdate(100));
        run(() -> itemView2.setSwingViewValueWithUpdate(200));
        run(mapPane::addNewElement);

        var itemModel3 = runGet(() -> GuiSwingView.findDescendantByType(mapPane.getModel().getElementAt(2).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_MODEL_INDEX)));
        run(() -> itemModel3.setSwingViewValueWithUpdate(300));
        var itemView3 = runGet(() -> GuiSwingView.findDescendantByType(mapPane.getModel().getElementAt(2).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_ORDER_VIEW_INDEX)));
        run(() -> itemView3.setSwingViewValueWithUpdate(400));
        runWait();

        var item3key = runGet(() -> GuiSwingView.findDescendantByType(mapPane.getModel().getElementAt(2).contentPane(), GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> Objects.equals(t.getName(), GuiSwingPrefsEditor.TABLE_PREFS_COLUMN_INDEX)));
        run(() -> item3key.setSwingViewValueWithUpdate(3));
        runWait();

        GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic prefsActual = new GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic();
        prefsActual.loadFrom(prefs);
        assertEquals(3, prefsActual.getModelIndexToOrderDirect().size(), "size ");
        assertEquals(100, prefsActual.getModelIndexToOrderDirect().get(0).getModelIndex(), "set 0 modelIndex");
        assertEquals(20, prefsActual.getModelIndexToOrderDirect().get(1).getModelIndex(), "set 1 modelIndex");
        assertEquals(300, prefsActual.getModelIndexToOrderDirect().get(3).getModelIndex(), "set 2 modelIndex");
        assertEquals(123, prefsActual.getModelIndexToOrderDirect().get(0).getViewIndex(), "set 0 viewIndex");
        assertEquals(200, prefsActual.getModelIndexToOrderDirect().get(1).getViewIndex(), "set 1 viewIndex");
        assertEquals(400, prefsActual.getModelIndexToOrderDirect().get(3).getViewIndex(), "set 2 viewIndex");

        run(() -> item3key.setSwingViewValueWithUpdate(1));
        runWait();
        GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic prefsActual2 = new GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic();
        prefsActual2.loadFrom(prefs);
        assertEquals(2, prefsActual2.getModelIndexToOrderDirect().size(), "merged key size ");
        assertEquals(100, prefsActual2.getModelIndexToOrderDirect().get(0).getModelIndex(), "merged 0 modelIndex");
        assertEquals(300, prefsActual2.getModelIndexToOrderDirect().get(1).getModelIndex(), "merged 1 modelIndex");
        assertEquals(123, prefsActual2.getModelIndexToOrderDirect().get(0).getViewIndex(), "merged 0 viewIndex");
        assertEquals(400, prefsActual2.getModelIndexToOrderDirect().get(1).getViewIndex(), "merged 1 viewIndex");
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
        assertEquals(123, ((Number) runGet(wPane::getSwingViewValue)).intValue(), "init view");
        run(() -> wPane.setSwingViewValueWithUpdate(100));
        runWait();

        GuiSwingTableModelCollection.PreferencesForTableColumnWidth prefsActual = new GuiSwingTableModelCollection.PreferencesForTableColumnWidth();
        prefsActual.loadFrom(prefs);
        assertEquals(100, prefsActual.getWidth(), "set value");
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
        assertEquals(123, ((Number) runGet(mPane::getSwingViewValue)).intValue(), "init model");
        assertEquals(456, ((Number) runGet(vPane::getSwingViewValue)).intValue(), "init view");
        run(() -> mPane.setSwingViewValueWithUpdate(100));
        run(() -> vPane.setSwingViewValueWithUpdate(200));
        runWait();

        GuiSwingTableModelCollection.PreferencesForTableColumnOrder prefsActual = new GuiSwingTableModelCollection.PreferencesForTableColumnOrder();
        prefsActual.loadFrom(prefs);
        assertEquals(100, prefsActual.getModelIndex(), "set value model");
        assertEquals(200, prefsActual.getViewIndex(), "set value view");

    }

    @Test
    public void testHistoryString() {
        prefs.setCurrentValue("HelloWorld");
        var pane = runGet(() -> editor.createLastHistoryValueBySwingViewHistoryValue(prefs, prefs.getCurrentValue()));
        frame = createFrame(pane);

        var text = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class));
        runWait();
        assertEquals("HelloWorld", runGet(text::getSwingViewValue), "init text");

        run(() -> text.setSwingViewValueWithUpdate("UPDATED"));
        runWait();

        assertEquals("UPDATED", prefs.getCurrentValue(), "update prefs current value");
    }

    @Test
    public void testHistoryBoolean() {
        context = new GuiMappingContext(typeBuilder.get(Boolean.class));
        context.setRepresentation(GuiRepresentation.createValueBooleanCheckBox());
        prefs = new GuiPreferences(store, context);

        prefs.setCurrentValue(true);
        var pane = runGet(() -> editor.createLastHistoryValueBySwingViewHistoryValue(prefs, prefs.getCurrentValue()));
        frame = createFrame(pane);

        var value = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));
        runWait();
        assertEquals(Boolean.TRUE, runGet(value::getSwingViewValue), "init boolean");

        run(() -> value.setSwingViewValueWithUpdate(false));
        runWait();

        assertEquals(Boolean.FALSE, prefs.getCurrentValue(), "update prefs current value");
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

        var value = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewNumberSpinner.PropertyNumberSpinner.class));
        runWait();
        assertEquals(10.234f, ((Number) runGet(value::getSwingViewValue)).floatValue(), 0.001f, "init float");

        run(() -> value.setSwingViewValueWithUpdate(456.789f));
        runWait();

        assertEquals(456.789f, ((Number) prefs.getCurrentValue()).floatValue(), 0.001f, "update prefs current value");
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

        var value = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewImagePane.PropertyImagePane.class));
        runWait();

        assertEquals(toStr(img), toStr((BufferedImage) runGet(value::getImage)), "init image");

        run(() -> {
            value.setImagePath(img2, imgFile);
            value.setImage(img2);
        });
        runWait(2_000);

        var outImg = (GuiReprValueImagePane.ImageHistoryEntry) prefs.getCurrentValue();

        assertEquals(imgFile, outImg.getPath(), "update prefs current value path");
        assertEquals(toStr(img2), toStr((BufferedImage) outImg.getImage()), "update prefs current value img");
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

        var item0 = runGet(() -> GuiSwingView.findDescendantByType(pane.getModel().getElementAt(0).contentPane(), GuiSwingViewStringField.PropertyStringPane.class));
        var item1 = runGet(() -> GuiSwingView.findDescendantByType(pane.getModel().getElementAt(1).contentPane(), GuiSwingViewStringField.PropertyStringPane.class));

        assertEquals("hello", runGet(item0::getSwingViewValue), "init 0");
        assertEquals("world", runGet(item1::getSwingViewValue), "init 1");

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
                int r = (0xFF & (rgb >> 16));
                int g = (0xFF & (rgb >> 8));
                int b = (0xFF & rgb);
                buf.append(String.format("[%d,%d,%d]", r, g, b));
            }
            buf.append("\n");
        }
        return buf.toString();
    }

    public GuiSwingViewNumberSpinner.PropertyNumberSpinner runGetNumber(JComponent pane, String name) {
        return this.runGet(() ->
                GuiSwingView.findDescendantByType(pane, GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, t -> t.getName().equals(name)));
    }
}
