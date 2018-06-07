package autogui.swing.util;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** a text-field with background searching file-items */
public class SearchTextFieldFilePath extends SearchTextField {

    public static String MENU_CATEGORY_FILE_DEFAULT = "Default";
    public static String MENU_CATEGORY_FILE_CHILD = "Child";
    public static String MENU_CATEGORY_FILE_SIBLING = "Sibling";
    public static String MENU_CATEGORY_FILE_PARENT = "Parent";
    public static String MENU_CATEGORY_FILE_CURRENT = "Current";
    public static String MENU_CATEGORY_FILE_CANDIDATE = "Candidate";

    public SearchTextFieldFilePath() {
        this(new SearchTextFieldModelFilePath());
    }

    public SearchTextFieldFilePath(SearchTextFieldModelFilePath model) {
        super(model);
    }

    @Override
    public void init() {
        super.init();
        initTransferHandler();
    }

    public void initTransferHandler() {
        setTransferHandlerWithSettingExportingDragSource(
                new FileTransferHandler(this));
    }

    @Override
    public List<Object> getMenuItemsSource() {
        if (menuItemsSource == null) {
            menuItemsSource = new ArrayList<>(Arrays.asList(
                    new PopupExtensionText.TextCutAction(field),
                    new PopupExtensionText.TextCopyAction(field),
                    new FileCopyAllAction(this),
                    new FilePasteAction(this),
                    new PopupExtensionText.TextSelectAllAction(field),
                    new DesktopOpenAction(this),
                    new DesktopRevealAction(this),
                    new OpenDialogAction(this)));
        }
        return menuItemsSource;
    }

    /** the method is a simulated GUI task originally caused by the user.
     *   thus, it will cause a new background search task
     *   @param file  the selected file
     * */
    public void setFile(Path file) {
        FileItem item = getFileItem(file);
        selectSearchedItemFromGui(item);
        editingRunner.schedule(item);
    }

    public FileItem getFileItem(Path file) {
        return ((SearchTextFieldModelFilePath) getModel())
                .getFileItem(file, null, true);
    }

    public Path getFile() {
        PopupCategorized.CategorizedMenuItem item = getModel().getSelection();
        if (item instanceof FileItem) {
            return ((FileItem) item).getPath();
        } else {
            return null;
        }
    }

    @Override
    public boolean isUpdateFieldModifiedEvent(Object e) {
        return super.isUpdateFieldModifiedEvent(e) || e instanceof PopupCategorized.CategorizedMenuItem;
    }

    @Override
    public void setTextFromSearchedItem(PopupCategorized.CategorizedMenuItem item) {
        if (item == null) {
            setTextWithoutUpdateField("");
        } else if (item instanceof FileItem) {
            Path path = ((FileItem) item).getPath();
            setTextWithoutUpdateField(toPathString(path));
        } else {
            setTextWithoutUpdateField(item.getName());
        }
    }

    public static String toPathString(Path p) {
        if (p == null) {
            return "";
        } else {
            return p.toString();
        }
    }

    public static String toFileNameString(Path p) {
        if (p == null) {
            return "";
        } else {
            Path f = p.getFileName();
            if (f == null) {
                return "";
            } else {
                return f.toString();
            }
        }
    }

    /** a transfer-handler for copying and pasting a file-list */
    public static class FileTransferHandler extends TransferHandler {
        protected SearchTextFieldFilePath component;

        public FileTransferHandler(SearchTextFieldFilePath component) {
            this.component = component;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return component.getField().isEnabled() && component.getField().isEditable() &&
                    support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                    support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return Arrays.stream(support.getDataFlavors())
                        .filter(DataFlavor.javaFileListFlavor::equals)
                        .map(f -> getTransferDataAsFiles(support, f))
                        .map(this::select)
                        .findFirst()
                        .orElse(false);
            } else if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String str = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    String line = str.split("\\n")[0];
                    return select(Collections.singletonList(Paths.get(line.trim()).toFile()));
                } catch (Exception ex) {
                    return false;
                }
            } else {
                return false;
            }
        }

        @SuppressWarnings("unchecked")
        public List<File> getTransferDataAsFiles(TransferSupport support, DataFlavor flavor) {
            try {
                return (List<File>) support.getTransferable().getTransferData(flavor);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public boolean select(List<File> files) {
            if (files != null && !files.isEmpty()) {
                component.setFile(files.get(0).toPath());
                return true;
            }  else {
                return false;
            }
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            Path path = component.getFile();
            if (path != null) {
                return new FileSelection(Collections.singletonList(path));
            } else {
                return null;
            }
        }
    }

    /** the action for completing a searched file-item */
    public static abstract class FileListEditAction extends AbstractAction {
        protected SearchTextFieldFilePath component;

        public FileListEditAction(String name, SearchTextFieldFilePath component) {
            this.component = component;
            putValue(NAME, name);
        }
        @Override
        public boolean isEnabled() {
            return super.isEnabled() && component.isSwingEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            run(fs -> component.setFile(fs.get(0)));
        }

        public abstract void run(Consumer<List<Path>> files);
    }

    /** the action for text paste */
    public static class FilePasteAction extends FileListEditAction implements PopupCategorized.CategorizedMenuItemAction {

        public FilePasteAction(SearchTextFieldFilePath component) {
            super("Paste Value", component);
        }

        @Override
        public boolean isEnabled() {
            return component.getField().isEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            run(fs -> {
                if (fs == null) {
                    component.getField().paste();
                } else {
                    component.setFile(fs.get(0));
                }
            });
        }

        /**
         * @param proc  it will receive null if the clipboard does not support listing files.
         *                if non-null, the list will have least one element.
         */
        @SuppressWarnings("unchecked")
        @Override
        public void run(Consumer<List<Path>> proc) {
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (board.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                try {
                    List<File> files = (List<File>) board.getData(DataFlavor.javaFileListFlavor);
                    if (files != null && files.size() > 0) {
                        proc.accept(files.stream()
                                .map(File::toPath)
                                .collect(Collectors.toList()));
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                proc.accept(null);
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_PASTE;
        }
    }

    /** an abstract definition of a file-list */
    public static abstract class FileListAction extends AbstractAction {
        protected SearchTextFieldFilePath component;

        public FileListAction(String name, SearchTextFieldFilePath component) {
            this.component = component;
            putValue(NAME, name);
        }
        @Override
        public boolean isEnabled() {
            return super.isEnabled() && component.getFile() != null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Path file = component.getFile();
            if (file != null) {
                run(Collections.singletonList(file));
            }
        }

        public abstract void run(List<Path> files);
    }

    /** copying the selected file */
    public static class FileCopyAllAction extends FileListAction implements PopupCategorized.CategorizedMenuItemAction {

        public FileCopyAllAction(SearchTextFieldFilePath component) {
            super("Copy Value", component);
        }

        @Override
        public void run(List<Path> list) {
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (list != null) {
                FileSelection selection = new FileSelection(list);
                board.setContents(selection, selection);
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_COPY;
        }
    }

    /** transferable object for a file list */
    public static class FileSelection implements Transferable, ClipboardOwner {
        protected List<Path> files;

        protected static DataFlavor[] flavors = {
                DataFlavor.javaFileListFlavor,
                DataFlavor.stringFlavor
        };

        public FileSelection(List<Path> files) {
            this.files = files;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return Arrays.stream(flavors)
                    .anyMatch(flavor::equals);
        }

        @Override
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {
            if (DataFlavor.stringFlavor.equals(flavor)) {
                return files.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .collect(Collectors.joining("\n"));
            } else if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                return files.stream()
                        .filter(Objects::nonNull)
                        .map(Path::toFile)
                        .collect(Collectors.toList());
            }
            throw new UnsupportedFlavorException(flavor);
        }

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) { }
    }

    /** the action for opening files */
    public static class DesktopOpenAction extends FileListAction implements PopupCategorized.CategorizedMenuItemAction {

        public DesktopOpenAction(SearchTextFieldFilePath component) {
            super("Open In Desktop", component);
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && Desktop.isDesktopSupported();
        }

        @Override
        public void run(List<Path> files) {
            try {
                Desktop desktop = Desktop.getDesktop();
                files.stream()
                        .map(Path::toFile)
                        .forEach(p -> {
                            try {
                                desktop.open(p);
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        });
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_JUMP;
        }
    }

    /** the action for showing files in Finder or Explorer */
    public static class DesktopRevealAction extends FileListAction implements PopupCategorized.CategorizedMenuItemAction {
        protected Consumer<Path> command;


        public DesktopRevealAction(SearchTextFieldFilePath component) {
            super("Reveal In Desktop", component);
            initCommand();
        }

        public void initCommand() {
            //open -R path
            //explorer.exe /select,path
            //nautilus path
            //in Java9: Desktop.browseFileDirectory(File)
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop desk = Desktop.getDesktop();
                    //TODO replace: command = path -> desk.browseFileDirectory(path.toFile());
                    Method method = Desktop.class.getMethod("browseFileDirectory", File.class);
                    command = path -> {
                        try {
                            method.invoke(desk, path.toFile());
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    };
                } catch (Exception ex) {
                    Function<Path,List<String>> commandGenerator;
                    String name = System.getProperty("os.name", "?").toLowerCase();
                    if (name.startsWith("mac")) {
                        commandGenerator = (path) -> Arrays.asList("open", "-R", path.toString());
                    } else if (name.startsWith("windows")) {
                        commandGenerator = (path) -> Arrays.asList("explorer", "/select," + path.toString());
                    } else {
                        commandGenerator = (path) -> Arrays.asList("nautilus", path.toString());
                    }
                    command = processCommand(commandGenerator);
                }
            }
        }

        public Consumer<Path> processCommand(Function<Path, List<String>> commandGenerator) {
            return file -> {
                try {
                    Process p = new ProcessBuilder(commandGenerator.apply(file.toAbsolutePath()))
                            .start();
                    p.waitFor(1, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            };
        }

        @Override
        public void run(List<Path> files) {
            for (Path file : files) {
                command.accept(file);
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_JUMP;
        }
    }

    /** an action for selecting a file item from an open dialog */
    public static class OpenDialogAction extends FileListEditAction implements PopupCategorized.CategorizedMenuItemAction {

        public OpenDialogAction(SearchTextFieldFilePath component) {
            super("Select...", component);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SettingsWindow.getFileDialogManager().setCurrentPath(getComponentFile());
            super.actionPerformed(e);
        }

        public Path getComponentFile() {
            return component.getFile();
        }

        public JComponent getDialogComponent() {
            return component;
        }

        @Override
        public void run(Consumer<List<Path>> setter) {
            Path path = SettingsWindow.getFileDialogManager().showOpenDialog(getDialogComponent(), null);
            if (path != null) {
                setter.accept(Collections.singletonList(path));
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_SET;
        }
    }

    /** the model definition for the completing file items */
    public static class SearchTextFieldModelFilePath implements SearchTextFieldModel {
        protected PopupCategorized.CategorizedMenuItem selection;
        protected FileSystemView iconSource;

        @Override
        public boolean isFixedCategorySize() {
            return true;
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getCandidates(String text, boolean editable, SearchTextFieldPublisher publisher) {
            List<PopupCategorized.CategorizedMenuItem> items = new ArrayList<>();
            try {
                Path path = setSelection(text);

                if (publisher.isSearchCancelled()) {
                    return items;
                }

                boolean exists = Files.exists(path);
                if (exists) {
                    items.add(new FileInfoItem(path));
                }

                if (publisher.isSearchCancelled()) {
                    return items;
                }

                if (editable) {
                    items.addAll(getCompletionItems(text, path));
                    if (publisher.isSearchCancelled()) {
                        return items;
                    }
                    items.addAll(getParentItems(path));
                    if (publisher.isSearchCancelled()) {
                        return items;
                    }
                    items.addAll(getDefaultItems());
                    if (publisher.isSearchCancelled()) {
                        return items;
                    }
                    items.addAll(getChildItems(path));
                }

                return items;
            } catch (InvalidPathException ex) {
                return items;
            }
        }

        public Path setSelection(String text) {
            Path path = Paths.get(text);
            selection = getFileItem(path, MENU_CATEGORY_FILE_CURRENT, false);
            return path;
        }

        public List<FileItem> getCompletionItems(String text, Path path) {
            try {
                Path dir;
                String head;
                if (Stream.of("/", "\\", ":").anyMatch(text::endsWith)) {
                    dir = path;
                    head = "";
                } else {
                    dir = path.getParent();
                    head = toFileNameString(path).toLowerCase();
                }
                if (dir != null && Files.isDirectory(dir)) {
                    return Files.list(dir)
                            .filter(p -> p.getFileName().toString().toLowerCase().startsWith(head))
                            .map(p -> getFileItem(p, MENU_CATEGORY_FILE_CANDIDATE, true))
                            .collect(Collectors.toList());
                } else {
                    return Collections.emptyList();
                }
            } catch (Exception ex) {
                return Collections.emptyList();
            }
        }



        public List<FileItem> getParentItems(Path path) {
            Path p = path.toAbsolutePath().getParent();
            List<FileItem> items = new ArrayList<>();
            while (p != null) {
                if (Files.exists(p)) {
                    items.add(getFileItem(p, MENU_CATEGORY_FILE_PARENT, false));
                }
                p = p.getParent();
            }
            return items;
        }

        public List<FileItem> getChildItems(Path path) {
            try {
                String category;
                if (!Files.isDirectory(path)) {
                    category = MENU_CATEGORY_FILE_SIBLING;
                    path = path.getParent();
                    if (!Files.isDirectory(path)) {
                        return Collections.emptyList();
                    }
                } else {
                    category = MENU_CATEGORY_FILE_CHILD;
                }
                return Files.list(path)
                        .sorted(this::compare)
                        .map(p -> getFileItem(p, category, true))
                        .collect(Collectors.toList());
            } catch (Exception ex) {
                return Collections.emptyList();
            }
        }

        public int compare(Path p1, Path p2) {
            String f1 = toFileNameString(p1);
            String f2 = toFileNameString(p2);
            boolean f1dot = f1.startsWith(".");
            boolean f2dot = f2.startsWith(".");
            if (f1dot && !f2dot) {
                return 1;
            } else if (!f1dot && f2dot) {
                return -1;
            } else {
                return f1.compareTo(f2);
            }
        }

        public List<FileItem> getDefaultItems() {
            List<FileItem> items = new ArrayList<>();

            //current
            items.add(getDefaultCurrent());

            //home
            String home = System.getProperty("user.home", "");
            if (!home.isEmpty()) {
                items.add(getFileItem(Paths.get(home), MENU_CATEGORY_FILE_DEFAULT, false));
            }

            //root
            for (Path p : FileSystems.getDefault().getRootDirectories()) {
                items.add(getFileItem(p, MENU_CATEGORY_FILE_DEFAULT, false));
            }
            return items;
        }

        public FileItem getDefaultCurrent() {
            Path p = Paths.get(".").toAbsolutePath();
            if (p.getFileName().toString().equals(".") && p.getParent() != null) {
                p = p.getParent();
            }
            return getFileItem(p, MENU_CATEGORY_FILE_DEFAULT, false);
        }



        public FileItem getFileItem(Path path, String category, boolean nameOnly) {
            if (iconSource == null) {
                iconSource = FileSystemView.getFileSystemView();
            }
            return new FileItem(path, p -> {
                if (path == null) {
                    return null;
                } else {
                    Icon icon = iconSource.getSystemIcon(p.toFile());
                    if (icon != null) {
                        return new IconWrapper(icon,
                            UIManagerUtil.getInstance().getScaledSizeInt(16));
                    } else {
                        return null;
                    }
                }
            }, category, "", nameOnly);
        }

        @Override
        public void select(PopupCategorized.CategorizedMenuItem item) {
            this.selection = item;
        }

        @Override
        public PopupCategorized.CategorizedMenuItem getSelection() {
            return selection;
        }
    }

    public static class IconWrapper implements Icon {
        protected Icon icon;
        protected float width;
        protected float height;
        protected float scale;

        public IconWrapper(Icon icon, int width) {
            this.icon = icon;
            this.width = width;
            if (icon != null) {
                scale = width / (float) icon.getIconWidth();
                this.height = icon.getIconHeight() * scale;
            } else {
                scale = 1.0f;
                height = width;
            }
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.scale(scale, scale);
            try {
                icon.paintIcon(c, g2, 0, 0);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return (int) width;
        }

        @Override
        public int getIconHeight() {
            return (int) height;
        }
    }

    /** a completed file item */
    public static class FileItem implements PopupCategorized.CategorizedMenuItem {
        protected Path path;
        protected Function<Path,Icon> iconGetter;
        protected String category;
        protected String subCategory = "";
        protected boolean nameOnly;

        public FileItem(Path path, Function<Path, Icon> iconGetter, String category, String subCategory, boolean nameOnly) {
            this.path = path;
            this.iconGetter = iconGetter;
            this.category = category;
            this.nameOnly = nameOnly;
        }

        @Override
        public String getName() {
            if (nameOnly) {
                return toFileNameString(path);
            } else {
                return toPathString(path);
            }
        }

        @Override
        public Icon getIcon() {
            return iconGetter == null ? null : iconGetter.apply(path);
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getSubCategory() {
            return subCategory;
        }

        public Path getPath() {
            return path;
        }

        public Function<Path, Icon> getIconGetter() {
            return iconGetter;
        }

        @Override
        public PopupCategorized.CategorizedMenuItem remap(String category, String subCategory) {
            return new FileItem(path, iconGetter, category, subCategory, nameOnly);
        }
    }

    /** a file info item */
    public static class FileInfoItem implements PopupCategorized.CategorizedMenuItemLabel {
        protected Path path;
        public FileInfoItem(Path path) {
            this.path = path;
        }

        @Override
        public String getName() {
            if (path != null && !path.toString().isEmpty() && Files.exists(path)) {
                try {
                    String str = Stream.of(
                            getNameTime(),
                            join(getNameSize(),
                                    getNamePermission()))
                        .collect(Collectors.joining("\n"));

                    if (str.contains("\n")) {
                        return "<html><pre>" +
                                Arrays.stream(str.split("\\n"))
                                    .collect(Collectors.joining("<br>"))
                                + "</pre></html>";
                    } else {
                        return str;
                    }
                } catch (Exception ex) {
                    return "error: " + ex;
                }
            }
            return null;
        }

        public String getNameTime() throws Exception {
            FileTime time = Files.getLastModifiedTime(path);
            List<String> times = new ArrayList<>();
            times.add("Modified: " + toFileTime(time));
            try {
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                times.add(0, " Created: " + toFileTime(attrs.creationTime()));
                times.add("Accessed: " + toFileTime(attrs.lastAccessTime()));
            } catch (Exception ex) {
                //nothing
            }
            return times.stream()
                    .collect(Collectors.joining("\n"));
        }

        public String toFileTime(FileTime time) {
            LocalDateTime l = LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault());
            return l.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault()));
        }

        public String getNameSize() throws Exception {
            if (Files.isDirectory(path)) {
                return Files.list(path).count()  + " items";
            } else {
                long size = Files.size(path);
                return formatFileSize(size);
            }
        }

        public String getNamePermission() throws Exception {
            String r = !Files.isReadable(path)? " No read" : "";
            String w = !Files.isWritable(path)? " No write" : "";
            return join(r, w);
        }

        private String join(String l, String r) {
            if (!l.isEmpty() && !r.isEmpty()) {
                return l + ", " + r;
            } else {
                return l + r;
            }
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public String getSubCategory() {
            return PopupCategorized.SUB_CATEGORY_LABEL_VALUE;
        }
    }

    public static String formatFileSize(long size) {
        long block = 1024;
        long bytes = size % block;

        size /= block;
        long kilo = size % block;

        size /= block;
        long mega = size % block;

        size /= block;
        long giga = size % block;

        size /= block;
        long tera = size % block;

        size /= block;
        long peta = size % block;

        size /= block;
        long exa = size;

        StringBuilder buf = new StringBuilder();
        boolean upperAppended;
        upperAppended = append(buf, false, exa, " E");
        upperAppended = append(buf, upperAppended, peta, " P");
        upperAppended = append(buf, upperAppended, tera, " T");
        upperAppended = append(buf, upperAppended, giga, " G");
        upperAppended = append(buf, upperAppended, mega, " M");
        upperAppended = append(buf, upperAppended, kilo, " K");
        if (bytes > 0) {
            if (upperAppended) {
                buf.append(" ");
            }
            buf.append(bytes).append(" ");
        }
        buf.append("B");
        return buf.toString();
    }

    private static boolean append(StringBuilder buf, boolean upperAppended, long n, String suffix) {
        if (upperAppended || n > 0) {
            if (upperAppended) {
                buf.append(" ");
            }
            buf.append(n).append(suffix);
            return true;
        } else {
            return false;
        }
    }
}
