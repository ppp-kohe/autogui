package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.GuiInits;
import org.autogui.base.annotation.GuiInitAction;
import org.autogui.swing.AutoGuiShell;

import java.io.File;
import java.util.*;

@GuiIncluded
public class FileRenameDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new FileRenameDemo());
    }

    File dir;

    @GuiIncluded public File getDir() { return dir; }
    @GuiIncluded public void setDir(File dir) {
        boolean update = dir != null && !Objects.equals(this.dir, dir);
        this.dir = dir;
        if (update && dir.isDirectory()) {
            List<RenameEntry> es = new ArrayList<>();
            int i = 0;
            List<File> files = new ArrayList<>(Arrays.asList(dir.listFiles()));
            files.sort(Comparator.naturalOrder());
            for (File file : files) {
                es.add(new RenameEntry(file, String.format("%03d-%s", i, file.getName())));
                ++i;
            }
            entries = es;
        }
    }

    List<RenameEntry> entries = new ArrayList<>();

    @GuiIncluded public List<RenameEntry> getEntries() { return entries; }

    @GuiIncluded public static class RenameEntry {
        File file;
        String newName;
        public RenameEntry(File file, String newName) {
            this.file = file;
            this.newName = newName;
        }
        @GuiIncluded public File getFile() { return file; }
        @GuiIncluded public String getNewName() { return newName; }
        @GuiIncluded public void setNewName(String newName) { this.newName = newName; }
    }

    @GuiInits(action = @GuiInitAction(confirm = true))
    @GuiIncluded public void rename() {
        for (RenameEntry e : entries) {
            File newFile = new File(e.getFile().getParentFile(), e.getNewName());
            if (e.getFile().exists() && !newFile.exists()) {
                e.getFile().renameTo(newFile);
            }
        }
    }
}
