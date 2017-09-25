package autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchTextFieldFilePath extends SearchTextField {

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
        FileTransferHandler handler = new FileTransferHandler(this);
        setTransferHandler(handler);
        getField().setTransferHandler(handler);
    }

    @Override
    public List<Action> getPopupEditActions() {
        return Arrays.asList(
                new PopupExtensionText.TextCutAction(field),
                new PopupExtensionText.TextCopyAction(field),
                new FileCopyAllAction(this),
                new FilePasteAction(this),
                new PopupExtensionText.TextSelectAllAction(field),
                new DesktopOpenAction(this),
                new DesktopRevealAction(this),
                new OpenDialogAction(this));
    }

    public void setFile(Path file) {
        selectSearchedItemFromGui(getFileItem(file));
    }

    public FileItem getFileItem(Path file) {
        return ((SearchTextFieldModelFilePath) getModel())
                .getFileItem(file, null, true);
    }

    public Path getFile() {
        PopupCategorized.CategorizedPopupItem item = getModel().getSelection();
        if (item instanceof FileItem) {
            return ((FileItem) item).getPath();
        } else {
            return null;
        }
    }

    @Override
    public boolean isUpdateFieldModifiedEvent(Object e) {
        return super.isUpdateFieldModifiedEvent(e) || e instanceof PopupCategorized.CategorizedPopupItem;
    }

    @Override
    public void setTextFromSearchedItem(PopupCategorized.CategorizedPopupItem item) {
        if (item == null) {
            setTextWithoutUpdateField("");
        } else if (item instanceof FileItem) {
            Path path = ((FileItem) item).getPath();
            setTextWithoutUpdateField(toPathString(path));
        } else {
            setTextWithoutUpdateField(item.getName());
        }
        editingRunner.schedule(item);
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

    public static class FileTransferHandler extends TransferHandler {
        protected SearchTextFieldFilePath component;

        public FileTransferHandler(SearchTextFieldFilePath component) {
            this.component = component;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            return Arrays.stream(support.getDataFlavors())
                    .filter(DataFlavor.javaFileListFlavor::equals)
                    .map(f -> getTransferDataAsFiles(support, f))
                    .map(this::select)
                    .findFirst()
                    .orElse(false);
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
    }


    public static class FilePasteAction extends AbstractAction {
        protected SearchTextFieldFilePath component;

        public FilePasteAction(SearchTextFieldFilePath component) {
            putValue(NAME, "Paste Value");
            this.component = component;
        }

        @Override
        public boolean isEnabled() {
            return component.isEditable();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void actionPerformed(ActionEvent e) {
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (board.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                try {
                    List<File> files = (List<File>) board.getData(DataFlavor.javaFileListFlavor);
                    if (files != null && files.size() > 0) {
                        component.setFile(files.get(0).toPath());
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                component.getField().paste();
            }
        }
    }

    public static class FileCopyAllAction extends AbstractAction {
        protected SearchTextFieldFilePath component;

        public FileCopyAllAction(SearchTextFieldFilePath component) {
            putValue(NAME, "Copy Value");
            this.component = component;
        }

        @Override
        public boolean isEnabled() {
            return component.getFile() != null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            Path file = component.getFile();
            if (file != null) {
                FileSelection selection = new FileSelection(file);
                board.setContents(selection, selection);
            }
        }
    }

    public static class FileSelection implements Transferable, ClipboardOwner {
        protected Path file;

        protected static DataFlavor[] flavors = {
                DataFlavor.javaFileListFlavor,
                DataFlavor.stringFlavor
        };

        public FileSelection(Path file) {
            this.file = file;
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
                return file.toString();
            } else if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                return Collections.singletonList(file.toFile());
            }
            throw new UnsupportedFlavorException(flavor);
        }

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) { }
    }


    public static class DesktopOpenAction extends AbstractAction {
        protected SearchTextFieldFilePath component;

        public DesktopOpenAction(SearchTextFieldFilePath component) {
            putValue(NAME, "Open In Desktop");
            this.component = component;
        }

        @Override
        public boolean isEnabled() {
            return component.getFile() != null && Desktop.isDesktopSupported();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Desktop.getDesktop().open(component.getFile().toFile());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static class DesktopRevealAction extends AbstractAction {
        protected SearchTextFieldFilePath component;
        protected Function<Path,List<String>> commandGenerator;

        public DesktopRevealAction(SearchTextFieldFilePath component) {
            putValue(NAME, "Reveal In Desktop");
            this.component = component;
            initCommand();
        }

        public void initCommand() {
            //open -R path
            //explorer.exe /select,path
            //nautilus path
            if (Desktop.isDesktopSupported()) {
                String name = System.getProperty("os.name", "?").toLowerCase();
                if (name.startsWith("mac")) {
                    commandGenerator = (path) -> Arrays.asList("open", "-R", path.toString());
                } else if (name.startsWith("windows")) {
                    commandGenerator = (path) -> Arrays.asList("explorer", "/select," + path.toString());
                } else {
                    commandGenerator = (path) -> Arrays.asList("nautilus", path.toString());
                }
            }
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && component.getFile() != null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Process p = new ProcessBuilder(commandGenerator.apply(component.getFile().toAbsolutePath()))
                        .start();
                p.waitFor(1, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static class OpenDialogAction extends AbstractAction {
        protected SearchTextFieldFilePath component;
        protected JFileChooser fileChooser;

        public OpenDialogAction(SearchTextFieldFilePath component) {
            putValue(NAME, "Select...");
            this.component = component;
        }

        @Override
        public boolean isEnabled() {
            return component.isEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            }
            Path path = component.getFile();
            if (path != null && Files.exists(path)) {
                if (!Files.isDirectory(path)) {
                    path = path.getParent();
                }
                fileChooser.setCurrentDirectory(path.toFile());
            }
            int r = fileChooser.showOpenDialog(component);
            if (r == JFileChooser.APPROVE_OPTION) {
                File f = fileChooser.getSelectedFile();
                component.setFile(f.toPath());
            }
        }
    }

    public static class SearchTextFieldModelFilePath implements SearchTextFieldModel {
        protected PopupCategorized.CategorizedPopupItem selection;

        @Override
        public boolean isFixedCategorySize() {
            return true;
        }

        @Override
        public List<PopupCategorized.CategorizedPopupItem> getCandidates(String text, boolean editable, SearchTextFieldPublisher publisher) {
            List<PopupCategorized.CategorizedPopupItem> items = new ArrayList<>();
            try {
                Path path = Paths.get(text);
                selection = getFileItem(path, "Current", false);

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
                    items.addAll(getChildItems(path));
                    if (publisher.isSearchCancelled()) {
                        return items;
                    }
                    items.addAll(getDefaultItems());
                }

                return items;
            } catch (InvalidPathException ex) {
                return items;
            }
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
                            .map(p -> getFileItem(p, "Candidate", true))
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
                    items.add(getFileItem(p, "Parent", false));
                }
                p = p.getParent();
            }
            return items;
        }

        public List<FileItem> getChildItems(Path path) {
            try {
                String category;
                if (!Files.isDirectory(path)) {
                    category = "Sibling";
                    path = path.getParent();
                    if (!Files.isDirectory(path)) {
                        return Collections.emptyList();
                    }
                } else {
                    category = "Child";
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
                items.add(getFileItem(Paths.get(home), "Default", false));
            }

            //root
            for (Path p : FileSystems.getDefault().getRootDirectories()) {
                items.add(getFileItem(p, "Default", false));
            }
            return items;
        }

        public FileItem getDefaultCurrent() {
            Path p = Paths.get(".").toAbsolutePath();
            if (p.getFileName().toString().equals(".") && p.getParent() != null) {
                p = p.getParent();
            }
            return getFileItem(p, "Default", false);
        }



        public FileItem getFileItem(Path path, String category, boolean nameOnly) {
            return new FileItem(path, null, category, nameOnly);
        }

        @Override
        public void select(PopupCategorized.CategorizedPopupItem item) {
            this.selection = item;
        }

        @Override
        public PopupCategorized.CategorizedPopupItem getSelection() {
            return selection;
        }
    }


    public static class FileItem implements PopupCategorized.CategorizedPopupItem {
        protected Path path;
        protected Function<Path,Icon> iconGetter;
        protected String category;
        protected boolean nameOnly;

        public FileItem(Path path, Function<Path, Icon> iconGetter, String category, boolean nameOnly) {
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

        public Path getPath() {
            return path;
        }

        public Function<Path, Icon> getIconGetter() {
            return iconGetter;
        }
    }

    public static class FileInfoItem implements PopupCategorized.CategorizedPopupItemLabel {
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
            return l.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
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
        append(buf, upperAppended, kilo, " K");
        if (bytes > 0) {
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
