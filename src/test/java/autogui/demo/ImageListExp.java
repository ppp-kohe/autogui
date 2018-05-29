package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;
import autogui.swing.icons.GuiSwingIcons;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@GuiIncluded
public class ImageListExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new ImageListExp());
    }

    public List<IconItem> items;

    public ImageListExp() {
        GuiSwingIcons ics = GuiSwingIcons.getInstance();
        items = ics.getIconWords().stream()
                .map(n -> new IconItem(n, ics.getIcon(n)))
                .collect(Collectors.toList());
    }

    @GuiIncluded
    public List<IconItem> getItems() {
        return items;
    }

    @GuiIncluded
    public static class IconItem {
        protected String name;
        protected Image image;

        public IconItem(String name, Icon icon) {
            this.name = name;
            if (icon instanceof GuiSwingIcons.ResourceIcon) {
                image = ((GuiSwingIcons.ResourceIcon) icon).getImage();
            } else {
                System.err.println("? " + icon);
            }
        }

        @GuiIncluded
        public String getName() {
            return name;
        }

        @GuiIncluded
        public Image getImage() {
            return image;
        }


    }
}
