package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@GuiIncluded
public class FileListExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new FileListExp());
    }

    protected Path dir;
    protected FileList list = new FileList();

    protected Preview preview = new Preview();

    @GuiIncluded
    public Path getDir() {
        return dir;
    }

    @GuiIncluded
    public void setDir(Path dir) {
        this.dir = dir;
        list.update(dir);
    }

    @GuiIncluded
    public FileList getList() {
        return list;
    }

    @GuiIncluded
    public Preview getPreview() {
        return preview;
    }

    @GuiIncluded
    public class FileList {
        protected List<FileItem> items = new ArrayList<>();

        public void update(Path dir) {
            try {
                items = Files.list(dir)
                        .map(FileItem::new)
                        .collect(Collectors.toList());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @GuiIncluded
        public List<FileItem> getItems() {
            return items;
        }

        @GuiIncluded
        public void preview(List<FileItem> item) {
            preview.setImage(item.get(0).getThumb());
        }
    }

    @GuiIncluded
    public static class Preview {
        Image image;

        public void setImage(Image image) {
            this.image = image;
        }

        @GuiIncluded
        public Image getImage() {
            return image;
        }
    }

    static Image empty = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);

    @GuiIncluded
    public static class FileItem {
        protected Path path;
        protected Image thumb;

        public FileItem(Path path) {
            this.path = path;
        }

        @GuiIncluded
        public Path getPath() {
            return path;
        }

        @GuiIncluded
        public Image getThumb() {
            if (thumb == null) {
                String fn = path.getFileName().toString();
                try {
                    if (fn.endsWith(".png") || fn.endsWith(".jpeg") || fn.endsWith(".jpg")) {
                        thumb = ImageIO.read(path.toFile());
                    } else {
                        thumb = empty;
                    }
                } catch (Exception ex) {
                    thumb = empty;
                    ex.printStackTrace();
                }
            }
            return thumb;
        }

        @GuiIncluded
        public long getSize() {
            if (Files.exists(path)) {
                try {
                    return Files.size(path);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return -1;
                }
            } else {
                return 0;
            }
        }
    }
}
