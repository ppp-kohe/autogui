package autogui.swing;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;

public class GuiPreferencesExp {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(new GuiPreferencesExp()::run);
    }


    MutableTreeNode root;
    DefaultTreeModel model;
    JTree tree;

    public GuiPreferencesExp() {
    }

    public void run() {
        JFrame frame = new JFrame("Prefs");
        JPanel pane = new JPanel();
        {
            pane.setLayout(new BorderLayout());
            JToolBar bar = new JToolBar();
            {
                JTextField field = new JTextField(20);
                field.addActionListener(e -> {
                    load(field.getText());
                });
                bar.add(field);

                bar.add(new RemoveAction());
            }
            pane.add(bar, BorderLayout.NORTH);

            initTree();
            pane.add(new JScrollPane(tree), BorderLayout.CENTER);
        }
        frame.setContentPane(pane);
        frame.pack();
        frame.setVisible(true);
    }

    public void initTree() {
        root = new DefaultMutableTreeNode("root");
        model = new DefaultTreeModel(root);
        tree = new JTree(model);
        tree.setCellRenderer(new Renderer());
    }

    public JTree getTree() {
        return tree;
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
        model.setRoot(root);
        model.reload();
        expand(root);
        tree.invalidate();
        tree.repaint();
    }

    protected MutableTreeNode node(Preferences p) {
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
            tree.expandPath(new TreePath(model.getPathToRoot(n)));
        }
        for (int i = 0, c = n.getChildCount(); i < c; ++i) {
            expand(n.getChildAt(i));
        }
    }

    public void reload() {
        load((Preferences) ((DefaultMutableTreeNode) root).getUserObject());
    }

    class RemoveAction extends AbstractAction {
        public RemoveAction() {
            super("Remove");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
            Object value = node.getUserObject();
            try {
                if (value instanceof Preferences) {
                    Preferences p = ((Preferences) value);
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
