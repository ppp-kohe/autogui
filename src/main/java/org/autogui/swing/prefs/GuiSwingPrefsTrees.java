package org.autogui.swing.prefs;

import org.autogui.base.mapping.GuiPreferences;
import org.autogui.swing.util.TextCellRenderer;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.io.Serial;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GuiSwingPrefsTrees {
    private static final GuiSwingPrefsTrees instance = new GuiSwingPrefsTrees();

    public static GuiSwingPrefsTrees getInstance() {
        return instance;
    }

    public JTree createTree() {
        var contentTree = new JTree(new DefaultMutableTreeNode(""));
        contentTree.setCellRenderer(new GuiSwingPrefsTrees.PrefsTreeCellRenderer());
        return contentTree;
    }

    public void setTreeModel(JTree contentTree, DefaultMutableTreeNode rootNode) {
        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        contentTree.setModel(model);
        expandTreeAll(contentTree, model, (TreeNode) model.getRoot());
    }

    public JTree createTree(GuiPreferences... prefs) {
        var tree = createTree();
        setTreeModel(tree, createTreeNode(List.of(prefs)));
        return tree;
    }

    public JTree createTree(List<GuiPreferences> prefs) {
        var tree = createTree();
        setTreeModel(tree, createTreeNode(prefs));
        return tree;
    }

    /**
     *
     * @param list list of prefs
     * @return tree-model of list
     * @since 1.3
     */
    public DefaultMutableTreeNode createTreeNode(List<GuiPreferences> list) {
        if (list.size() == 1) {
            return createTreeNode(list.getFirst());
        } else {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(list.size() + " preferences");
            list.forEach(p -> node.add(createTreeNode(p)));
            return node;
        }
    }



    /**
     * @param prefs a prefs
     * @return the node for prefs
     * @since 1.3
     */
    public DefaultMutableTreeNode createTreeNode(GuiPreferences prefs) {
        return createTreeNode(prefs.getName(), prefs.copyOnMemoryAsRoot().getValueStore());
    }

    public DefaultMutableTreeNode createTreeNode(String key, GuiPreferences.GuiValueStore store) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new GuiSwingPrefsTrees.PrefsValueStoreEntry(key, store));
        Map<String,Object> json = store instanceof GuiPreferences.GuiValueStoreOnMemory ?
                ((GuiPreferences.GuiValueStoreOnMemory) store).toJson() : null;
        store.getKeys().stream()
                .filter(store::hasEntryKey)
                .map(n -> new DefaultMutableTreeNode(new GuiSwingPrefsTrees.PrefsValueStoreEntry(n,
                        json == null ? store.getString(n, "") : json.get(n))))
                .forEach(node::add);

        store.getKeys().stream()
                .filter(store::hasNodeKey)
                .map(n -> createTreeNode(n, store.getChild(n)))
                .forEach(node::add);
        return node;
    }

    /**
     * @param contentTree the target tree
     * @param treeModel a model
     * @param n a node in the model
     * @since 1.3
     */
    public void expandTreeAll(JTree contentTree, DefaultTreeModel treeModel, TreeNode n) {
        if (!n.isLeaf()) {
            contentTree.expandPath(new TreePath(treeModel.getPathToRoot(n)));
        }
        for (int i = 0, c = n.getChildCount(); i < c; ++i) {
            expandTreeAll(contentTree, treeModel, n.getChildAt(i));
        }
    }


    /**
     * tree-model value for prefs-tree
     * @since 1.3
     */
    public static class PrefsValueStoreEntry {
        protected String key;
        protected Object value;

        public PrefsValueStoreEntry(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return "ValueStoreEntry{" +
                    "key='" + key + '\'' +
                    ", value=" + value +
                    '}';
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public boolean isNode() {
            return value instanceof GuiPreferences.GuiValueStore;
        }
    }

    /**
     * cell-renderer for prefs-tree
     * @since 1.3
     */
    public static class PrefsTreeCellRenderer extends DefaultTreeCellRenderer {
        @Serial
        private static final long serialVersionUID = 1L;
        protected String name;
        protected String entryValue;
        protected transient TextCellRenderer.LineInfo nameInfo;
        protected transient TextCellRenderer.LineInfo valueInfo;
        protected Map<Color, Map<Color, Color>> colorMap = new HashMap<>();
        protected boolean selected;
        protected boolean node;

        public PrefsTreeCellRenderer() {}

        public void setValue(Object value) {
            node = false;
            if (value instanceof DefaultMutableTreeNode treeNode) {
                setValueTreeNode(treeNode);
            } else {
                setValueUserObject(value);
            }
        }

        public void setValueTreeNode(DefaultMutableTreeNode treeNode) {
            node = (treeNode.getChildCount() > 0);
            setValueUserObject(treeNode.getUserObject());
        }

        public void setValuePrefsValueStoreEntry(PrefsValueStoreEntry e) {
            setTreeName(e.getKey());
            node = e.isNode();
            if (!e.isNode()) {
                entryValue = Objects.toString(e.getValue());
                if (entryValue.isEmpty()) {
                    entryValue = " ";
                }
            } else {
                entryValue = null;
            }
        }

        protected void setValueUserObject(Object value) {
            if (value instanceof PrefsValueStoreEntry e) {
                setValuePrefsValueStoreEntry(e);
            } else {
                setTreeName(Objects.toString(value));
                entryValue = null;
            }
        }

        protected void setTreeName(String name) {
            if (name != null && name.isEmpty()) {
                this.name = " ";
            } else {
                this.name = name;
            }
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            setValue(value);
            super.getTreeCellRendererComponent(tree, value, sel, expanded, !node, row, hasFocus);
            setNameInfo(name);
            setEntryValueInfo(entryValue);
            selected = sel;

            Graphics2D g = (Graphics2D) getGraphics();
            if (g != null) {
                paintOrLayout(g, false);
            }
            return this;
        }

        protected void setNameInfo(String name) {
            AttributedString aStr = new AttributedString(name);
            aStr.addAttribute(TextAttribute.FOREGROUND, new Color(80, 80, 80));
            nameInfo = new TextCellRenderer.LineInfo(aStr, 0, name.length());
        }

        protected void setEntryValueInfo(String entryValue) {
            if (entryValue != null) {
                var aStr = new AttributedString(entryValue);
                aStr.addAttribute(TextAttribute.FOREGROUND, new Color(50, 50, 140));
                valueInfo = new TextCellRenderer.LineInfo(aStr, 0, entryValue.length());
            } else {
                valueInfo = null;
            }
        }

        protected void paintOrLayout(Graphics2D g, boolean paint) {
            Icon icon = getIcon();
            int x = 0;
            int h = 0;
            if (icon != null) {
                if (paint) {
                    icon.paintIcon(this, g, 0, 0);
                }
                x += icon.getIconWidth();
                h = Math.max(h, icon.getIconHeight());
            }

            UIManagerUtil ui = UIManagerUtil.getInstance();
            Color foreground = getForeground();//or ui.getTextPaneSelectionForeground();
            Color background = ui.getLabelBackground();//or ui.getTextPaneSelectionBackground();
            if (paint) {
                g.setColor(getForeground());
            }
            Font font = getFont();
            g.setFont(font);
            FontRenderContext frc = g.getFontRenderContext();
            TextLayout layout;

            x += ui.getScaledSizeInt(3);
            if (paint) {
                layout = nameInfo.getLayout(frc, 0, 0, foreground, background,
                        foreground, background, colorMap);
                layout.draw(g, x, layout.getAscent());
            } else {
                layout = nameInfo.getLayout(frc);
            }
            x += (int) layout.getAdvance();
            h = Math.max(h, (int) (layout.getAscent() + layout.getDescent()));

            if (valueInfo != null) {
                x += ui.getScaledSizeInt(10);

                if (paint) {
                    layout = valueInfo.getLayout(frc, 0, 0, foreground, background,
                            foreground, background, colorMap);
                    layout.draw(g, x, layout.getAscent());
                } else {
                    layout = valueInfo.getLayout(frc);
                }
                x += (int) layout.getAdvance();
                h = Math.max(h, (int) (layout.getAscent() + layout.getDescent()));
            }
            if (!paint) {
                setPreferredSize(new Dimension(x, h));
            }
        }


        @Override
        protected void paintComponent(Graphics g) {
            if (isOpaque()) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            paintOrLayout((Graphics2D) g, true);
        }
    }
}
