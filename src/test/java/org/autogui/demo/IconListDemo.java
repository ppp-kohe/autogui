package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.swing.AutoGuiShell;
import org.autogui.swing.icons.GuiSwingIcons;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@GuiIncluded
public class IconListDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new IconListDemo());
    }

    public List<IconItem> items;

    public IconListDemo() {
        GuiSwingIcons ics = GuiSwingIcons.getInstance();
        items = ics.getIconWords().stream()
                .map(n -> new IconItem(n, ics.getIcon(n), getSynonyms(n)))
                .collect(Collectors.toList());
    }

    public List<String> getSynonyms(String n) {
        List<String> ss = new ArrayList<>();
        GuiSwingIcons.getInstance().getSynonyms()
                .forEach((k,v) -> {
                    if (v.equals(n)) {
                        ss.add(k);
                    }
                });
        return ss;
    }

    @GuiIncluded
    public List<IconItem> getItems() {
        return items;
    }

    @GuiIncluded
    public static class IconItem {
        protected String name;
        protected Image image;
        protected List<String> synonyms;

        public IconItem(String name, Icon icon, List<String> synonyms) {
            this.name = name;
            if (icon instanceof GuiSwingIcons.ResourceIcon) {
                image = ((GuiSwingIcons.ResourceIcon) icon).getImage();
            } else {
                System.err.println("? " + icon);
            }
            this.synonyms = synonyms;
        }

        @GuiIncluded
        public String getName() {
            return name;
        }

        @GuiIncluded
        public Image getImage() {
            return image;
        }

        @GuiIncluded
        public List<String> getSynonyms() {
            return synonyms;
        }

        @GuiIncluded
        public int getSynonymSize() {
            return synonyms.size();
        }

        @Override
        public String toString() {
            return "IconItem:" + name + "," + synonyms;
        }
    }
}
