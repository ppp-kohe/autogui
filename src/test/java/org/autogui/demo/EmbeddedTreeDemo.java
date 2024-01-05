package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.swing.AutoGuiShell;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.io.Serial;
import java.util.prefs.Preferences;

@GuiIncluded
public class EmbeddedTreeDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new EmbeddedTreeDemo());
    }

    JScrollPane pane;
    JTree tree;
    DefaultTreeModel treeModel;
    MutableTreeNode root;

    @GuiIncluded(description = "prefs package path separated by '/'. e.g. '/org/autogui/demo'")
    public String name = getClass().getPackage().getName().replace('.', '/');

    @GuiIncluded
    public void load() {
        if (!name.isEmpty()) {
            load(name);
        }
    }

    @GuiIncluded
    public JComponent getTree() {
        if (pane == null) {
            root = new DefaultMutableTreeNode("root");
            treeModel = new DefaultTreeModel(root);
            tree = new JTree(treeModel);
            tree.setCellRenderer(new Renderer());
            pane = new JScrollPane(tree);
        }
        return pane;
    }

    @GuiIncluded
    public void remove() {
        if (tree != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
            if (node == null) {
                System.err.println("no selection");
                return;
            }
            Object value = node.getUserObject();
            try {
                if (value instanceof Preferences p) {
                    p.removeNode();
                    p.flush();
                } else if (value instanceof KeyValue) {
                    ((KeyValue) value).remove();
                }
                System.err.println("removed: " + value);
                reload();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void load(String name) {
        System.err.println("load " + name);
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        Preferences p = Preferences.userRoot().node(name);
        load(p);
    }

    public void load(Preferences p) {
        root = node(p);
        treeModel.setRoot(root);
        treeModel.reload();
        expand(root);
        tree.invalidate();
        tree.repaint();
    }

    public MutableTreeNode node(Preferences p) {
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(p);
        try {
            for (String e : p.keys()) {
                n.add(new DefaultMutableTreeNode(new KeyValue(p, e, p.get(e, ""))));
            }
            for (String e : p.childrenNames()) {
                n.add(node(p.node(e)));
            }
        } catch (Exception ex) {
            n.add(new DefaultMutableTreeNode("Error: " + ex));
        }
        return n;
    }

    public void expand(TreeNode n) {
        if (!n.isLeaf()) {
            tree.expandPath(new TreePath(treeModel.getPathToRoot(n)));
        }
        for (int i = 0, c = n.getChildCount(); i < c; ++i) {
            expand(n.getChildAt(i));
        }
    }

    public void reload() {
        load((Preferences) ((DefaultMutableTreeNode) root).getUserObject());
    }




    static class KeyValue {
        public Preferences p;
        public String key;
        public String value;

        public KeyValue(Preferences p, String key, String value) {
            this.p = p;
            this.key = key;
            this.value = value;
        }

        public void remove() {
            try {
                p.remove(key);
                p.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return key + " = " + value;
        }
    }


    static class Renderer extends DefaultTreeCellRenderer {
        @Serial private static final long serialVersionUID = 1L;
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof DefaultMutableTreeNode) {
                value = ((DefaultMutableTreeNode) value).getUserObject();
            }
            if (value instanceof Preferences) {
                value = ((Preferences) value).name();
            } else if (value instanceof KeyValue) {
                value = value.toString();
            }
            return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        }
    }
}
