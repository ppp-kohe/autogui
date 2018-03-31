package autogui.swing.util;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** a shared window manager for the setting panel */
public class SettingsWindow {
    protected static SettingsWindow instance;

    public SettingsFrame window;
    protected SettingSupport settingSupport;

    public static SettingsWindow get() {
        if (instance == null) {
            instance = new SettingsWindow();
        }
        return instance;
    }

    public SettingsWindow() {
        window = new SettingsFrame();
        window.setType(Window.Type.UTILITY);
        window.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (settingSupport != null) {
                    settingSupport.resized(window);
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (settingSupport != null) {
                    settingSupport.moved(window);
                }
            }

            @Override
            public void componentShown(ComponentEvent e) { }

            @Override
            public void componentHidden(ComponentEvent e) { }
        });
    }

    public class SettingsFrame extends JFrame {
        protected boolean shown;

        public void setShown(boolean shown) {
            this.shown = shown;
        }

        public boolean isShown() {
            return shown;
        }
    }

    public JFrame getWindow() {
        return window;
    }

    public void show() {
        window.setShown(true);
        window.setVisible(true);
    }

    public void show(JComponent sender) {
        if (sender != null) {
            window.setLocationRelativeTo(sender);
        }
        if (settingSupport != null) {
            settingSupport.setup(window);
        }
        show();
    }

    public void show(String title, JComponent sender, JComponent contentPane) {
        show(title, sender, contentPane, null);
    }

    public void show(String title, JComponent sender, JComponent contentPane, SettingSupport settingSupport) {
        window.setTitle(title);
        window.setContentPane(contentPane);
        window.pack();
        this.settingSupport = settingSupport;
        show(sender);
    }

    public interface SettingSupport {
        void resized(JFrame window);
        void moved(JFrame window);
        void setup(JFrame window);
    }

    ///////// header text:

    /** a group of labels */
    public static class LabelGroup {
        protected List<JComponent> names = new ArrayList<>();
        protected int align;
        protected JComponent pane;

        /** just for names: only {@link #addName(JComponent)} and {@link #fitWidth()} are valid */
        public LabelGroup() {
        }

        public LabelGroup(JComponent pane) {
            this(pane, FlowLayout.RIGHT);
        }

        public LabelGroup(JComponent pane, int align) {
            this.align = align;
            this.pane = pane;
            if (pane != null) {
                pane.setLayout(new ResizableFlowLayout(false).setFitHeight(true));
            }
        }

        public LabelGroup addRow(String label, JComponent content) {
            JPanel pane = new JPanel(new FlowLayout(align));
            pane.add(new JLabel(label));
            return addRow(pane, content);
        }

        public LabelGroup addRowFixed(String label, JComponent content) {
            JPanel namePane = new JPanel(new FlowLayout(align));
            namePane.add(new JLabel(label));

            ResizableFlowLayout.add(pane,
                    ResizableFlowLayout.create(true)
                            .add(addName(namePane))
                            .add(content).getContainer(), false);
            return this;
        }

        public LabelGroup addRow(JComponent label, JComponent content) {
            ResizableFlowLayout.add(pane,
                    ResizableFlowLayout.create(true)
                            .add(addName(label))
                            .add(content, true).getContainer(), false);
            return this;
        }

        public JComponent addName(JComponent component) {
            names.add(component);
            return component;
        }

        public void fitWidth() {
            Dimension dim = new Dimension();
            for (JComponent item : names){
                Dimension p = item.getPreferredSize();
                dim.width = Math.max(p.width, dim.width);
            }

            for (JComponent item : names){
                Dimension p = item.getPreferredSize();
                p.width = dim.width;
                item.setPreferredSize(p);
            }
        }
    }

    ////////////// color chooser

    protected static ColorWindow colorWindow;

    public static ColorWindow getColorWindow() {
        if (colorWindow == null) {
            colorWindow = new ColorWindow();
        }
        return colorWindow;
    }

    /** a window holder for the color chooser */
    public static class ColorWindow {
        protected JFrame window;

        protected Consumer<Color> callback;
        protected JColorChooser colorChooser;
        protected boolean updateDisabled;

        protected AtomicInteger refCount = new AtomicInteger();

        public ColorWindow() {
            window = new JFrame();
            window.setType(Window.Type.UTILITY);
            colorChooser = new JColorChooser();
            colorChooser.getSelectionModel().addChangeListener(e ->
                    update(colorChooser.getColor()));
            window.setContentPane(colorChooser);
            window.pack();
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    callback = null;
                }
            });
        }

        public void show(Component sender, Color color, Consumer<Color> callback) {
            this.callback = callback;
            if (sender != null) {
                window.setLocationRelativeTo(sender);
            }
            if (color != null) {
                updateDisabled = true;
                colorChooser.setColor(color);
                updateDisabled = false;
            }
            window.setVisible(true);
        }

        public JFrame getWindow() {
            return window;
        }

        public void update(Color c) {
            if (!updateDisabled && callback != null) {
                callback.accept(c);
            }
        }

        public void retain() {
            refCount.incrementAndGet();
        }

        public boolean release() {
            if (refCount.decrementAndGet() <= 0) {
                window.dispose();
                if (colorWindow == this) {
                    colorWindow = null;
                }
                return true;
            } else {
                return false;
            }
        }
    }

    /** a color well can be changed by a shared color-panel */
    public static class ColorButton extends JButton implements ActionListener {
        protected Color color;
        protected Consumer<Color> callback;
        protected ColorWindow window;

        public ColorButton(Color color, Consumer<Color> callback) {
            this.color = color;
            this.callback = callback;
            setPreferredSize(new Dimension(32, 18));
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (window == null) {
                window = getColorWindow();
                window.retain();
            }
            window.show(this, color, this::setColor);
        }

        public void dispose() {
            if (window != null) {
                window.release();
            }
        }

        public void setColor(Color color) {
            setColorWithoutUpdate(color);
            callback.accept(color);
        }

        public void setColorWithoutUpdate(Color color) {
            this.color = color;
            repaint();
        }

        public Color getColor() {
            return color;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(color);
            Dimension size = new Dimension(Math.min(32, getWidth() - 8), Math.min(14, getHeight() - 8));
            Rectangle2D.Float rect = new Rectangle2D.Float(getWidth() / 2 - size.width / 2, getHeight() / 2 - size.height / 2,
                    size.width, size.height);

            g2.fill(rect);
            g2.setColor(Color.gray);
            g2.draw(rect);
        }
    }

    protected static FileDialogManager fileDialogManager;

    public static FileDialogManager getFileDialogManager() {
        if (fileDialogManager == null) {
            fileDialogManager = new FileDialogManager();
        }
        return fileDialogManager;
    }

    public static class FileDialogManager {
        protected JFileChooser fileChooser;
        protected JList<Path> historyList;
        protected FileListModel historyListModel;
        protected JPanel accessory;
        protected FileBackAction backAction;
        protected FileListClearAction clearAction;
        protected FileListRemoveAction removeAction;

        protected boolean setDirByUser = true;
        protected int settingDirBySystem = 0;

        protected JComponent extraAccessory;

        public FileDialogManager setCurrentPath(Path p) {
            initFileChooser();
            if (p != null) {
                if (!Files.isDirectory(p)) {
                    p = p.getParent();
                }
                if (p != null) {
                    fileChooser.setCurrentDirectory(p.toFile());
                }
            }
            return this;
        }

        public Path showOpenDialog(Component sender, JComponent accessory) {
            initFileChooser();
            setAccessory(accessory);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int r = fileChooser.showOpenDialog(sender);
            if (r == JFileChooser.APPROVE_OPTION) {
                Path p = fileChooser.getSelectedFile().toPath();
                historyListModel.addPath(p);
                return p;
            } else {
                return null;
            }
        }

        public Path showSaveDialog(Component sender, JComponent accessory, String defaultName) {
            initFileChooser();
            setAccessory(accessory);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (defaultName != null) {
                fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), defaultName));
            }
            int r = fileChooser.showSaveDialog(sender);
            if (r == JFileChooser.APPROVE_OPTION) {
                Path p = fileChooser.getSelectedFile().toPath();
                historyListModel.addPath(p);
                return p;
            } else {
                return null;
            }
        }

        public Path showConfirmDialogIfOverwriting(JComponent sender, Path p) {
            if (p != null && Files.isRegularFile(p)) {
                int op = JOptionPane.showConfirmDialog(sender, p.toString() + " exists. Overwrites?",
                        "File Saving", JOptionPane.OK_CANCEL_OPTION);
                if (op == JOptionPane.OK_OPTION) {
                    return p;
                } else {
                    return null;
                }
            }
            return p;
        }

        public JFileChooser getFileChooser() {
            return fileChooser;
        }

        public void initFileChooser() {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
                initHistory();
            }
        }

        public void initHistory() {
            historyListModel = new FileListModel();
            historyList = new JList<>(historyListModel);
            historyList.setCellRenderer(new FileItemRenderer());
            historyList.addListSelectionListener(this::selectList);

            fileChooser.addPropertyChangeListener(JFileChooser.DIRECTORY_CHANGED_PROPERTY,
                        this::currentDirectoryChanged);

            accessory = new JPanel(new BorderLayout());
            {
                backAction = new FileBackAction(fileChooser);
                JButton back = new JButton(backAction);
                back.setHorizontalAlignment(SwingConstants.LEADING);
                back.setMargin(new Insets(5, 5, 5, 5));
                accessory.add(back, BorderLayout.NORTH);

                JScrollPane scrollPane = new JScrollPane(historyList);
                scrollPane.setBorder(BorderFactory.createEtchedBorder());
                accessory.add(scrollPane, BorderLayout.CENTER);

                extraAccessory = new JPanel(new BorderLayout());
                extraAccessory.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                accessory.add(extraAccessory, BorderLayout.SOUTH);
            }
            List<Path> initPath = new ArrayList<>();
            initPath.add(Paths.get(System.getProperty("user.home", ".")));
            Arrays.stream(FileSystemView.getFileSystemView().getRoots())
                    .map(File::toPath)
                    .forEach(initPath::add);
            historyListModel.setInitPaths(initPath);
            fileChooser.setAccessory(accessory);

            initPopup();
        }

        public void initPopup() {
            JPopupMenu popupMenu = new JPopupMenu();

            removeAction = new FileListRemoveAction(historyList);
            popupMenu.add(removeAction);
            popupMenu.addSeparator();
            popupMenu.add(new FileListAddAction(historyListModel, fileChooser));
            popupMenu.add(new FileListClearAction(historyList));

            historyList.setComponentPopupMenu(popupMenu);
        }

        public void setAccessory(JComponent accessory) {
            extraAccessory.removeAll();
            if (accessory != null) {
                extraAccessory.add(accessory);
            }
            extraAccessory.revalidate();
        }

        public void selectList(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }

            removeAction.isEnabled(); //update

            int r = historyList.getSelectedIndex();
            Path p = (r < 0 ? null : historyListModel.getElementAt(r));
            if (p == null) {
                return;
            }
            if (setDirByUser) {
                backAction.setPath(fileChooser.getCurrentDirectory().toPath());
            }
            setDirByUser = false;
            settingDirBySystem++;
            try {
                Path dir = p;
                if (Files.isRegularFile(p)) {
                    dir = p.getParent();
                }
                fileChooser.setCurrentDirectory(dir.toFile());
                if (Files.isRegularFile(p)) {
                    fileChooser.setSelectedFile(p.toFile());
                }
            } finally {
                settingDirBySystem--;
            }
        }

        public void currentDirectoryChanged(PropertyChangeEvent e) {
            if (settingDirBySystem <= 0) {
                setDirByUser = true;
                historyList.getSelectionModel().clearSelection();
            }
        }
    }

    public static class FileBackAction extends AbstractAction {
        protected JFileChooser fileChooser;
        protected Path path;
        protected FileSystemView iconSource;

        public FileBackAction(JFileChooser fileChooser) {
            this.fileChooser = fileChooser;
            iconSource = FileSystemView.getFileSystemView();
            setPath(fileChooser.getCurrentDirectory().toPath());
        }

        public void setPath(Path path) {
            this.path = path;
            String name = "";
            if (path != null) {
                Path fileName = path.getFileName();
                if (fileName == null) {
                    name = path.toString();
                } else {
                    name = fileName.toString();
                }
            }
            putValue(NAME, name);
            putValue(LARGE_ICON_KEY, path == null ? null : iconSource.getSystemIcon(path.toFile()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (path != null) {
                fileChooser.setCurrentDirectory(path.toFile());
            }
        }
    }

    public static class FileItemRenderer extends DefaultListCellRenderer {
        protected FileSystemView iconSource;
        protected Icon dummy;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, getText(value), index, isSelected, cellHasFocus);
            setIconFromPath(value);
            setLayout();
            return this;
        }

        public String getText(Object value) {
            if (value == null) {
                return "";
            } else if (value instanceof Path) {
                Path path = (Path) value;
                Path f = path.getFileName();
                if (f != null) {
                    return f.toString();
                } else {
                    return path.toString();
                }
            } else {
                return "" + value;
            }
        }

        public void setIconFromPath(Object value) {
            Icon icon = null;
            if (value instanceof Path) {
                if (iconSource == null) {
                    iconSource = FileSystemView.getFileSystemView();
                }
                icon = iconSource.getSystemIcon(((Path) value).toFile());
            }
            if (icon == null) {
                if (dummy == null) {
                    BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);
                    Graphics2D g = img.createGraphics();
                    {
                        g.setColor(new Color(255, 255, 255, 0));
                        g.fillRect(0, 0, img.getWidth(), img.getHeight());
                        g.dispose();
                    }
                    dummy = new ImageIcon(img);
                }
                icon = dummy;
            }
            setIcon(icon);
        }

        public void setLayout() {
            Dimension s = getPreferredSize();
            if (s.width < 150) {
                s.width = 150;
            }
            if (s.height < 30) {
                s.height = 30;
            }
            setPreferredSize(s);
            setMinimumSize(new Dimension(150, 30));
            setBorder(BorderFactory.createEmptyBorder(7, 5, 7, 5));
        }
    }

    public static class FileListModel extends AbstractListModel<Path> {
        protected List<Path> paths;
        protected List<Path> initPaths;

        public FileListModel() {
            this.paths = new ArrayList<>();
            this.initPaths = Collections.emptyList();
        }

        public void setPaths(List<Path> paths) {
            paths = paths.stream()
                    .distinct()
                    .collect(Collectors.toList());
            int oldSize = this.paths.size();
            this.paths.clear();
            this.paths.addAll(paths);
            int newSize = paths.size();
            int diff = newSize - oldSize;
            if (newSize > 0) {
                fireContentsChanged(this, 0, newSize - 1);
            }
            if (diff < 0) {
                fireIntervalRemoved(this, oldSize + diff, oldSize - 1);
            } else if (diff > 0) {
                fireIntervalAdded(this, oldSize, newSize - 1);
            }
        }

        public List<Path> getInitPaths() {
            return initPaths;
        }

        public void setInitPaths(List<Path> paths) {
            setPaths(paths);
            initPaths = new ArrayList<>(this.paths);
        }

        public void addPath(Path path) {
            int i = paths.size();
            if (!paths.contains(path)) {
                paths.add(path);
                fireIntervalAdded(this, i, i);
            }
        }

        @Override
        public int getSize() {
            return paths.size();
        }

        @Override
        public Path getElementAt(int index) {
            return paths.get(index);
        }

        public void remove(int i) {
            if (canBeRemoved(i)) {
                paths.remove(i);
                fireIntervalRemoved(this, i, i);
            }
        }

        public boolean canBeRemoved(int i) {
            return i >= 0 && i < paths.size() && !initPaths.contains(paths.get(i));
        }
    }


    public static class FileListClearAction extends AbstractAction {
        protected FileListModel listModel;
        protected JList<Path> list;

        public FileListClearAction(JList<Path> list) {
            this.list = list;
            this.listModel =(FileListModel) list.getModel();
            putValue(NAME, "Clear");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            listModel.setPaths(listModel.getInitPaths());
        }
    }

    public static class FileListRemoveAction extends AbstractAction {
        protected FileListModel listModel;
        protected JList<Path> list;

        public FileListRemoveAction(JList<Path> list) {
            this.list = list;
            this.listModel =(FileListModel) list.getModel();
            putValue(NAME, "Remove");
        }

        @Override
        public boolean isEnabled() {
            boolean b = listModel.canBeRemoved(list.getSelectedIndex());
            setEnabled(b);
            return b;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            listModel.remove(list.getSelectedIndex());
        }
    }

    public static class FileListAddAction extends AbstractAction {
        protected FileListModel listModel;
        protected JFileChooser chooser;

        public FileListAddAction(FileListModel listModel, JFileChooser chooser) {
            this.listModel = listModel;
            this.chooser = chooser;
            putValue(NAME, "Add Current");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File f = chooser.getSelectedFile();
            if (f == null) {
                f = chooser.getCurrentDirectory();
            }
            if (f != null) {
                listModel.addPath(f.toPath());
            }
        }
    }
}
