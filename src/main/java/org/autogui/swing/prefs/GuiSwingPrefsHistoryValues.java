package org.autogui.swing.prefs;

import org.autogui.GuiIncluded;
import org.autogui.base.JsonReader;
import org.autogui.base.JsonWriter;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeElement;
import org.autogui.base.type.GuiTypeMemberProperty;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.LambdaProperty;
import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.swing.mapping.GuiReprEmbeddedComponent;
import org.autogui.swing.mapping.GuiReprValueImagePane;
import org.autogui.swing.util.ResizableFlowLayout;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class GuiSwingPrefsHistoryValues {
    protected GuiTypeBuilder typeBuilder = new GuiTypeBuilder();

    public GuiSwingPrefsHistoryValues() {}

    public GuiTypeBuilder typeBuilder() {
        return typeBuilder;
    }

    public HistoryPaneResult createHistory(GuiPreferences.HistoryValueEntry entry, GuiPreferences prefs) {
        return createHistoryObjectPrefs(entry.getValue(), prefs, true).withEntry(entry);
    }

    public HistoryPaneResult createHistoryObjectPrefs(Object value, GuiPreferences prefs, boolean setCurrentValue) {
        var repr = prefs.getContext().getRepresentation();
        Consumer<Object> currentValueSetter = setCurrentValue ?
                prefs::setCurrentValue : null;
        return switch (repr) {
            case GuiReprValueImagePane ignored -> createHistoryImagePrefs(value)
                    .withGuiToSourceUpdater(currentValueSetter);
            case GuiReprEmbeddedComponent e ->
                createJsonEntrySourceForCurrentValue(prefs);
            case GuiReprValue v -> createObjectSimpleType(value, v.getValueType(prefs.getContext()))
                    .withGuiToSourceUpdater(currentValueSetter);
            default ->  createValue(null);
        };
    }

    public HistoryPaneResult createObjectSimpleType(Object value, Class<?> type) {
        var objType = new GuiTypeObject(ValueHolder.class);
        var prop = new GuiTypeMemberProperty("value", "setValue", "getValue", "value", typeBuilder.get(type));
        //find methods and field; relying on just their names
        objType.addProperties(prop);
        prop.getGetter();
        prop.getSetter();
        prop.getField();
        return createObjectComponent(value, objType);
    }

    public HistoryPaneResult createHistoryImagePrefs(Object value) {
        Path path = null;
        Image image = null;
        if (value instanceof Image) {
            image = (Image) value;
        } else if (value instanceof GuiReprValueImagePane.ImageHistoryEntry e) {
            path = e.getPath();
            image = e.getImage();
        }
        var editor = new ImageHistoryEntryEditor(path, image);
        var editorPane = new ImageHistoryPane(editor);
        return new HistoryPaneResult(editorPane, editorPane::setHistoryEntrySourceToGui)
                .withSettingGuiToSourceUpdater(editor::setGuiToSourceUpdater);
    }

    /**
     *  wraps the value by {@link ValueHolder} and creates a GUI component for the value-holder with the type.
     *   The returned result can update the value by updating {@link GuiPreferences.HistoryValueEntry};
     *    {@link HistoryPaneResult#withEntry(GuiPreferences.HistoryValueEntry)} can update the value-holder and the GUI component.
     *    {@link HistoryPaneResult#withGuiToSourceUpdater(Consumer)} can supply a callback of the value update by the GUI component.
     * @param value a value
     * @param type the type of the value
     * @return GUI component for the value
     */
    public HistoryPaneResult createObjectComponent(Object value, GuiTypeElement type) {
        var holder = new ValueHolder(value);
        var context = new GuiMappingContext(type, holder);
        holder.setContext(context);
        return createObjectComponent(context)
                .withSourceToGuiUpdater(holder::setEntryFromSourceToGui)
                .withSettingGuiToSourceUpdater(holder::setGuiToSourceUpdater);
    }

    public HistoryPaneResult createObjectComponent(GuiMappingContext context) {
        GuiSwingMapperSet.getReprDefaultSet().matchAndSetNotifiersAsInit(context);
        var view = GuiSwingMapperSet.getDefaultMapperSet().view(context);
        context.updateSourceFromRoot();
        if (view instanceof GuiSwingView swingView) {
            return new HistoryPaneResult(swingView.createView(context, GuiReprValue.getNoneSupplier()));
        } else {
            return createTreeJson(context.getSource().getValue());
        }
    }

    public HistoryPaneResult createValue(Object v) {
        if (v != null && !(v instanceof List<?>) && !(v instanceof Map<?, ?>)) {
            return createObjectComponent(new GuiMappingContext(new GuiTypeBuilder().get(v.getClass()), v));
        } else {
            return createTreeJson(v);
        }
    }

    public static HistoryPaneResult createTreeJson(Object v) {
        var tree = new TreeWithHistoryValueEntry(v);
        tree.setCellRenderer(new GuiSwingPrefsTrees.PrefsTreeCellRenderer());
        return new HistoryPaneResult(tree, tree::setEntry);
    }

    public HistoryPaneResult createJsonEntrySourceForCurrentValue(GuiPreferences prefs) {
        return createJsonEntrySource(prefs::getCurrentValueAsJsonSupported, prefs::setCurrentValueAsJsonSupported);
    }

    /**
     *
     * @param prefs the sprefs of the source used as {@link org.autogui.swing.prefs.GuiSwingPrefsSupports.PreferencesByJsonEntry#loadFromAndToJson(GuiPreferences)}
     *              and {@link org.autogui.swing.prefs.GuiSwingPrefsSupports.PreferencesByJsonEntry#setJsonAndSaveTo(Object, GuiPreferences)}
     * @param prefsFactory a constructor for creating temporarry object
     * @return a pane containing JSON source editor
     * @see #createJsonEntrySource(Supplier, Consumer)
     */
    public HistoryPaneResult createJsonEntrySource(GuiPreferences prefs, Supplier<? extends GuiSwingPrefsSupports.PreferencesByJsonEntry> prefsFactory) {
        return createJsonEntrySource(
                () -> prefsFactory.get().loadFromAndToJson(prefs),
                o -> prefsFactory.get().setJsonAndSaveTo(o, prefs));
    }

    /**
     * creates a small pane of JSON editor. The retruned pane contains a tool-bar with the "Reset" button, and the editor-pane with a scroll-pane.
     *  {@link HistoryPaneResult#updateLastEntrySource()} will reload and set the text from the given JSON getter;
     *    Note it does not have HistoryValueEntry, but it overides the method with updating the text-pane.
     * @param source the source getter of a JSON object (do not get called often)
     * @param updater the source setter of a JSON object (immediately called each edits)
     * @return a pane containg a text-pane for editing JSON source.
     */
    public static HistoryPaneResult createJsonEntrySource(Supplier<Object> source, Consumer<Object> updater) {
        var text = new JsonSourceEditPane(source, updater);
        JScrollPane scroll = new JScrollPane(text);
        var u = UIManagerUtil.getInstance();
        scroll.setPreferredSize(new Dimension(u.getScaledSizeInt(500), u.getScaledSizeInt(150)));

        JPanel pane = new JPanel(new BorderLayout());
        {
            JPanel tool = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel label = new JLabel("JSON for embedded component: ");
            label.setForeground(u.getLabelDisabledForeground());
            tool.add(label);
            tool.add(new GuiSwingIcons.ActionButton(new JsonSourceResetAction(text)));
            pane.add(tool, BorderLayout.NORTH);

            pane.add(scroll, BorderLayout.CENTER);
        }

        return new HistoryPaneResult(pane) {
            @Override
            public void updateLastEntrySource() {
                super.updateLastEntrySource();
                text.setTextFromSource(true);
            }
        };
    }

    public static class JsonSourceEditPane extends JTextPane implements DocumentListener {
        protected Supplier<Object> source;
        protected Consumer<Object> updater;
        protected boolean editing;
        protected int readingSource;
        @SuppressWarnings("this-escape")
        public JsonSourceEditPane(Supplier<Object> source, Consumer<Object> updater) {
            this.source = source;
            this.updater = updater;
            getDocument().addDocumentListener(this);
            setFont(UIManagerUtil.getInstance().getConsoleFont());
            setTextFromSource(true);
        }

        public void setEditing(boolean b) {
            boolean changed = this.editing != b;
            editing = b;
            if (changed) {
                var u = UIManagerUtil.getInstance();
                setForeground(b ? u.getLabelDisabledForeground() : u.getTextPaneForeground());
            }
        }

        public void setTextFromSource(boolean forceWhileEditing) {
            if (!editing && forceWhileEditing && source != null) {
                try {
                    ++readingSource;
                    var obj = source.get();
                    var src = JsonWriter.create().withNewLines(true)
                            .write(obj)
                            .toSource();
                    var doc = getDocument();
                    if (doc.getLength() > 0) {
                        doc.remove(0, doc.getLength());
                    }
                    doc.insertString(0, src, null);
                } catch (Exception ex) {
                    //
                } finally {
                    --readingSource;
                }
            }
        }

        protected void updated() {
            if (readingSource > 0) {
                return;
            }
            var doc = getDocument();
            if (doc.getLength() > 0) {
                try {
                    var text = doc.getText(0, doc.getLength());
                    var obj = JsonReader.create(text).parseValue();
                    updater.accept(obj);
                    setEditing(false);
                } catch (Exception ex) {
                    setEditing(true);
                }
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updated();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updated();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updated();
        }
    }

    public static class JsonSourceResetAction extends AbstractAction {
        protected JsonSourceEditPane pane;
        @SuppressWarnings("this-escape")
        public JsonSourceResetAction(JsonSourceEditPane pane) {
            this.pane = pane;
            putValue(NAME, "Reset from Preferences");
            putValue(SMALL_ICON, GuiSwingIcons.getInstance().getUpdateIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            pane.setTextFromSource(true);
        }
    }

    @GuiIncluded  public static class ValueHolder {
        protected GuiMappingContext context;
        protected Object value;
        protected int sourceUpdating;
        protected Consumer<Object> guiToSourceUpdater;

        public ValueHolder(Object value) { this.value = value; }

        @GuiIncluded public Object getValue() { return value; }
        @GuiIncluded public void setValue(Object value) {
            boolean changed = !Objects.equals(this.value, value);
            this.value = value;
            if (changed && sourceUpdating == 0 && guiToSourceUpdater != null) {
                guiToSourceUpdater.accept(value);
            }
        }

        public void setGuiToSourceUpdater(Consumer<Object> guiToSourceUpdater) {
            this.guiToSourceUpdater = guiToSourceUpdater;
        }

        public void setContext(GuiMappingContext context) {
            this.context = context;
        }

        public void setEntryFromSourceToGui(GuiPreferences.HistoryValueEntry entry) {
            try {
                ++sourceUpdating;
                if (entry != null) {
                    setValue(entry.getValue());
                }
                if (context != null) {
                    context.updateSourceFromRoot();
                }
            } finally {
                --sourceUpdating;
            }
        }
    }

    /** result component */
    public static class HistoryPaneResult extends JPanel { //opaque=true
        protected JComponent component;
        protected GuiPreferences.HistoryValueEntry lastEntry;
        protected boolean lastEntryUpdated;
        protected List<Consumer<GuiPreferences.HistoryValueEntry>> sourceToGuiUpdaters;
        protected List<Consumer<Object>> guiToSourceUpdaters;
        protected JLabel timeView;

        protected Object lastEntryValue;
        protected Instant lastEntryTime;
        protected int lastEntryIndex;

        public HistoryPaneResult(JComponent component) {
            this(component, null);
        }

        @SuppressWarnings("this-escape")
        public HistoryPaneResult(JComponent component, Consumer<GuiPreferences.HistoryValueEntry> sourceToGuiUpdater) {
            super();
            setLayout(new BorderLayout());
            add(component, BorderLayout.CENTER);
            sourceToGuiUpdaters = new ArrayList<>(3);
            guiToSourceUpdaters = new ArrayList<>(3);
            if (sourceToGuiUpdater != null) {
                sourceToGuiUpdaters.add(sourceToGuiUpdater);
            }
            this.component = component;
            timeView = new JLabel();
            add(timeView, BorderLayout.EAST);
        }

        public JComponent component() {
            return component;
        }

        public HistoryPaneResult withEntry(GuiPreferences.HistoryValueEntry e) {
            updateEntrySource(e);
            return this;
        }

        public boolean lastEntryUpdated() {
            var up = lastEntryUpdated;
            this.lastEntryUpdated = false;
            return up;
        }

        public void updateLastEntrySource() {
            updateEntrySource(lastEntry);
        }

        public boolean setLastEntrySource(GuiPreferences.HistoryValueEntry e) {
            var value = e == null ? null : e.getValue();
            var time = e == null ? null : e.getTime();
            var index = e == null ? -1 : e.getIndex();
            boolean diff = !Objects.equals(this.lastEntry, e) ||
                    !Objects.equals(lastEntryValue, value) || !Objects.equals(lastEntryTime, time) || (lastEntryIndex != index);;
            if (diff) {
                this.lastEntryValue = value;
                this.lastEntryTime = time;
                this.lastEntryIndex = index;
                lastEntryUpdated = true;
            }
            this.lastEntry = e;
            if (e != null) {
                var t = e.getTime();
                if (t != null) {
                    timeView.setText(t.toString());
                } else {
                    timeView.setText("");
                }
            }
            return diff;
        }

        public void updateEntrySource(GuiPreferences.HistoryValueEntry e) {
            if (setLastEntrySource(e)) {
                sourceToGuiUpdaters.forEach(entry -> entry.accept(e));
            }
        }

        public HistoryPaneResult withSourceToGuiUpdater(Consumer<GuiPreferences.HistoryValueEntry> sourceToGuiUpdater) {
            if (sourceToGuiUpdater != null) {
                sourceToGuiUpdaters.add(sourceToGuiUpdater);
            }
            return this;
        }

        public HistoryPaneResult withSettingGuiToSourceUpdater(Consumer<Consumer<Object>> guiToSourceUpdaterSetter) {
            guiToSourceUpdaterSetter.accept(this::guiToSourceUpdate);
            return this;
        }

        public void guiToSourceUpdate(Object valueFromGui) {
            guiToSourceUpdaters.forEach(u -> u.accept(valueFromGui));
        }

        public HistoryPaneResult withGuiToSourceUpdater(Consumer<Object> guiToSourceUpdater) {
            if (guiToSourceUpdater != null) {
                this.guiToSourceUpdaters.add(guiToSourceUpdater);
            }
            return this;
        }
    }


    @GuiIncluded
    public static class ImageHistoryEntryEditor extends GuiReprValueImagePane.ImageHistoryEntry {
        protected Consumer<Object> guiToSourceUpdater;
        protected int sourceUpdating;

        public ImageHistoryEntryEditor(Path path, Image image) {
            super(path, image);
        }
        @GuiIncluded(index = 20) public void setImage(Image image) {
            boolean changed = !Objects.equals(this.image, image);
            this.image = image;
            if (changed) {
                updateGuiToSource();
            }
        }

        @GuiIncluded(index = 10) public void setPath(Path path) {
            boolean changed = !Objects.equals(this.path, path);
            this.path = path;
            var oldImg = this.image;
            this.image = null;
            var newImg = getImage();
            if (newImg == null) {
                this.image = oldImg;
            }
            changed = changed || !Objects.equals(this.image, oldImg);
            if (changed) {
                updateGuiToSource();
            }
        }

        public void updateGuiToSource() {
            if (sourceUpdating == 0 && guiToSourceUpdater != null) {
                guiToSourceUpdater.accept(new GuiReprValueImagePane.ImageHistoryEntry(path, image));
            }
        }

        @GuiIncluded @Override public Path getPath() {
            return super.getPath();
        }

        @GuiIncluded @Override public Image getImage() {
            return super.getImage();
        }

        public void edit(Consumer<ImageHistoryEntryEditor> runner) {
            try {
                ++sourceUpdating;
                runner.accept(this);
            } finally {
                --sourceUpdating;
                updateGuiToSource();
            }
        }

        public void setEntrySourceToGui(GuiPreferences.HistoryValueEntry entry) {
            try {
                ++sourceUpdating;
                var val = entry != null ? entry.getValue() : null;
                if (val instanceof Image i) {
                    setImage(i);
                } else if (val instanceof Path p) {
                    setPath(p);
                } else if (val instanceof GuiReprValueImagePane.ImageHistoryEntry e) {
                    if (e.getPath() != null) {
                        setPath(e.getPath());
                    } else {
                        setImage(e.getImage());
                    }
                }
            } finally {
                --sourceUpdating;
            }
        }

        public void setGuiToSourceUpdater(Consumer<Object> guiToSourceUpdater) {
            this.guiToSourceUpdater = guiToSourceUpdater;
        }
    }

    public static class TreeWithHistoryValueEntry extends JTree {
        public TreeWithHistoryValueEntry(Object v) {
            super(new DefaultTreeModel(createTreeNodeJson("", v)));
        }

        public void setEntry(GuiPreferences.HistoryValueEntry e) {
            Object v = e == null ? null : e.getValue();
            TreeModel model = getModel();
            if (model.getRoot() instanceof DefaultMutableTreeNode node &&
                model instanceof DefaultTreeModel defModel) {
                if (!Objects.equals(v, node.getUserObject())) {
                    defModel.setRoot(createTreeNodeJson("", v));
                }
            }
        }
    }

    public static DefaultMutableTreeNode createTreeNodeJson(String key, Object v) {
        if (v instanceof Map<?, ?> map) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(key);
            map.entrySet().stream()
                    .map(e -> createTreeNodeJson(Objects.toString(e.getKey()), e.getValue()))
                    .forEach(node::add);
            return node;
        } else if (v instanceof List<?> list) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(key);
            IntStream.range(0, list.size())
                    .mapToObj(i -> createTreeNodeJson(Integer.toString(i), list.get(i)))
                    .forEach(node::add);
            return node;
        } else {
            return new DefaultMutableTreeNode(new GuiSwingPrefsTrees.PrefsValueStoreEntry(key, v));
        }
    }


    public static class ImageHistoryPane extends JComponent {
        protected ImageHistoryEntryEditor entry;
        protected LambdaProperty.LambdaFilePathPane pathPane;
        protected LambdaProperty.LambdaImagePane imagePane;

        @SuppressWarnings("this-escape")
        public ImageHistoryPane(ImageHistoryEntryEditor entry) {
            this.entry = entry;
            init();
        }

        public ImageHistoryPane() {
            this(new ImageHistoryEntryEditor(null, null));
        }

        public void init() {
            UIManagerUtil u = UIManagerUtil.getInstance();
            setLayout(new ResizableFlowLayout(true, u.getScaledSizeInt(10)).setFitHeight(false));
            setBorder(BorderFactory.createEmptyBorder(u.getScaledSizeInt(5), u.getScaledSizeInt(5), u.getScaledSizeInt(5), u.getScaledSizeInt(5)));

            imagePane = new LambdaProperty.LambdaImagePane(this::getImage, this::setImage);
            var imageWrapPane = imagePane.wrapSwingScrollPane(false, false);
            imageWrapPane.setPreferredSize(new Dimension(u.getScaledSizeInt(100), u.getScaledSizeInt(80)));

            pathPane = new LambdaProperty.LambdaFilePathPane(this::getPath, this::setPath);

            ResizableFlowLayout.add(this, imageWrapPane, false);
            ResizableFlowLayout.add(this, pathPane.wrapSwingPane(), true);
        }

        private int updateLock;

        public void setHistoryEntrySourceToGui(GuiPreferences.HistoryValueEntry e) {
            if (updateLock > 0) {
                return;
            }
            try {
                updateLock++;
                entry.setEntrySourceToGui(e);
                pathPane.updateSwingViewSource();
                imagePane.updateSwingViewSource();
            } finally {
                updateLock--;
            }
        }

        public void setEntry(ImageHistoryEntryEditor entry) {
            if (updateLock > 0) {
                return;
            }
            try {
                updateLock++;
                this.entry = entry;
                pathPane.updateSwingViewSource();
                imagePane.updateSwingViewSource();
            } finally {
                updateLock--;
            }
        }

        public void setImage(Image image) {
            if (updateLock > 0) {
                return;
            }
            try {
                updateLock++;
                entry.edit(e -> {
                    entry.setImage(image);
                    if (imagePane.getSwingViewContext().getRepresentation() instanceof GuiReprValueImagePane imgRepr) {
                        var path = imgRepr.getImagePath(image);
                        entry.setPath(path);
                    } else {
                        entry.setPath(null);
                    }
                });
                pathPane.updateSwingViewSource();
                imagePane.updateSwingViewSource();
            } finally {
                updateLock--;
            }
        }

        public Image getImage() {
            return entry.getImage();
        }

        public void setPath(Path path) {
            if (updateLock > 0) {
                return;
            }
            try {
                updateLock++;
                boolean changed = !Objects.equals(entry.getPath(), path);
                if (changed) {
                    entry.edit(e -> entry.setPath(path));
                    imagePane.updateSwingViewSource();
                }
            } finally {
                updateLock--;
            }
        }

        public Path getPath() {
            return entry.getPath();
        }
    }

}
