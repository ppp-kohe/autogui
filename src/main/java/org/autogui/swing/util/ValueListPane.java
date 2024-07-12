package org.autogui.swing.util;

import org.autogui.swing.icons.GuiSwingIcons;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * a {@link JList} wrapper of listing {@link JComponent}s.
 * <pre>
 *   -----------------------------
 *   |  ValueListPane : JComponent(BorderLayout)
 *   |  --------------------------
 *   |    toolBar JPanel
 *   |  --------------------------
 *   |   JList ({@link ValueListModel})
 *   |     0: {@link ValueListElementPane}
 *   |          header JLabel |  contentPane PaneType
 *   |     ...
 * </pre>
 * @param <ValueType> the type for the source-element type
 * @param <PaneType> the type for contentPane of the list
 */
public abstract class ValueListPane<ValueType, PaneType extends JComponent> extends JComponent {
    /**
     * @return the direct reference to the source-list. it needs to be stable and modifiable
     */
    public abstract List<ValueType> takeSource();

    /**
     * @param i the index for the creating element
     * @return a new i-th element-value
     */
    public abstract ValueType newSourceValue(int i);


    /**
     * re-assign the i-th element-value to the target pane; the method is always called even if the i is not changed.
     * @param i the index of the element
     * @param value the value of the element
     * @param pane the i-th element-pane
     * @return true if the value of the pane is updated by the given (i,value).
     */
    public abstract boolean updateSourceValueToElementPane(int i, ValueType value, ValueListElementPane<ValueType, PaneType> pane);

    /**
     * Note: for a combo-box, the returning element-pane should be opaque=true.
     *   1) The property will affect to the frequency of repainting.
     *   2) The repainting will lead to the ancestor change by the cell-rendering mechanism.
     *   3) The change will fire the default event-handler of the combo-box that hides pop-up.
     * @param i the initial index for the pane
     * @param elementPane the wrapper element-pane for the creating pane
     * @return a new-element pane which will be initialized by {@link #updateSourceValueToElementPane(int, Object, ValueListElementPane)} later
     */
    public abstract PaneType newElementPane(int i, ValueListElementPane<ValueType, PaneType> elementPane);

    /**
     * do the given action and update the entire display
     * @param v the action for editing the source-list
     * @return the returned value of the action
     * @param <V> the returning type of the action
     */
    public <V> V editSource(Function<List<ValueType>, V> v) {
        try {
            return v.apply(takeSource());
        } finally {
            model.updateSource();
        }
    }

    /**
     * checks removing of the i-th element; the default is just returns true
     * @param i the index for the removing element
     * @param value the removed value
     * @return true if the value can be removed
     */
    public boolean removeSourceValue(int i, ValueType value) {
        return true;
    }

    /**
     *
     * @param indices
     * @param values
     * @param targetIndex the targetIndex of the source ranged (0...size) .
     *                   Note that the index is a part of the current source before moving; including the moving indices
     * @return true if the moving is valid
     */
    public boolean moveSourceValues(int[] indices, List<ValueType> values, int targetIndex) {
        return true;
    }

    /**
     * called after the source-element is added: the default-impl is nothing
     * @param newIndex the new-element index
     * @param v the added value
     */
    public void sourceAdded(int newIndex, ValueType v) { }

    /**
     * called after removing elements from the source: the default-impl is nothing
     * @param removedIndices removed indices
     * @param removed removed values
     */
    public void sourceRemoved(int[] removedIndices, List<ValueType> removed) { }

    /**
     * called after updating the source-element: the default-impl is nothing
     * @param index the updated index
     * @param v the updated value
     */
    public void sourceUpdated(int index, ValueType v) { }

    /** the model of the list*/
    protected ValueListModel<ValueType, PaneType> model;
    /** the list */
    protected JList<ValueListElementPane<ValueType, PaneType>> list;
    /** the wrapper-factory for appending the list; typically JScrollPane::new */
    protected Function<JList<ValueListElementPane<ValueType, PaneType>>, JComponent> listWrapper;

    /** the tool-action for adding */
    protected ValueListAddAction addAction;
    /** the tool-action for removing */
    protected ValueListRemoveAction removeAction;
    /** the tool-action for item-up */
    protected ValueListUpAction upAction;
    /** the tool-action for item-down */
    protected ValueListDownAction downAction;

    /** a constructor for doInit and no-scroll-wrapper */
    protected ValueListPane() {
        this(true);
    }

    /**
     * a constructor with no-scroll-wrapper
     * @param doInit if true, {@link #init()}
     */
    protected ValueListPane(boolean doInit) {
        this(doInit, list -> list); //no-scroll-wrapper
    }

    /**
     * @param doInit if true, {@link #init()} ; a subclass can control initialization timing
     * @param listWrapper the wrapper factory at adding the list to the pane; typically {@code JScrollPane::new}
     */
    protected ValueListPane(boolean doInit, Function<JList<ValueListElementPane<ValueType, PaneType>>, JComponent> listWrapper) {
        this.listWrapper = listWrapper;
        if (doInit) {
            init();
        }
    }

    /** do initialization processes */
    public void init() {
        initLayout();
        initTool();
        initList();
    }

    /** initialize the layout with {@link BorderLayout} */
    protected void initLayout() {
        setLayout(new BorderLayout());
    }

    /** creating the toolbar and actions, and adding it to this by {@link #initToolActions(JComponent)} */
    protected void initTool() {
        JPanel tool = new JPanel(new FlowLayout(FlowLayout.LEFT));
        initToolActions(tool);
        add(tool, BorderLayout.NORTH);
    }

    /**
     * creating actions and adding button to the toolbar
     * @param tool the toolbar
     */
    protected void initToolActions(JComponent tool) {
        addAction = new ValueListAddAction(this);
        addAction.setEnabled(true);
        tool.add(new GuiSwingIcons.ActionButton(addAction));

        removeAction = new ValueListRemoveAction(this);
        removeAction.setEnabled(true);
        tool.add(new GuiSwingIcons.ActionButton(removeAction));

        upAction = new ValueListUpAction(this);
        upAction.setEnabled(true);
        tool.add(new GuiSwingIcons.ActionButton(upAction));

        downAction = new ValueListDownAction(this);
        downAction.setEnabled(true);
        tool.add(new GuiSwingIcons.ActionButton(downAction));
    }

    /** initialize the model and the list, and adding it to this with calling {@link #listWrapper} */
    protected void initList() {
        model = new ValueListModel<>(this);
        list = new JList<>(model);
        list.setOpaque(false);

        new ValueListElementTransferHandler(this).install();

        list.setCellRenderer(new ValueListRenderer());
        list.addListSelectionListener(this::updateSelectedElements);
        new ValueListEventDispatcher(list, (l, i) -> (JComponent) l.getModel().getElementAt(i));
        add(listWrapper.apply(list), BorderLayout.CENTER);
    }

    /**
     * adding a new element to the source and update the pane.
     * {@link #takeSource()} and {@link #newSourceValue(int)} ; if the new-value is null, no addition.
     * After that, (if added) {@link #sourceAdded(int, Object)} and {@link ValueListModel#updateSource()}
     */
    public void addNewElement() {
        var src = takeSource();
        int newIdx = src.size();
        var newVal = newSourceValue(newIdx);
        if (newVal != null) {
            src.add(newVal);
            sourceAdded(newIdx, newVal);
            model.updateSource();
        }
    }

    /**
     * remove selected elements values and components.
     * @see #removeElements(int...) 
     */
    public void removeSelectedElements() {
        removeElements(list.getSelectedIndices());
    }

    /**
     * {@link #removeSourceValue(int, Object)} for selected panes and
     *  remove the items from {@link #takeSource()};
     *  Note:  removing will be done by {@link #removeAll(List, int...)}
     * After that, {@link #sourceRemoved(int[], List)} and {@link ValueListModel#updateSourceWithRemoving(int...)}
     * @param targetIndices the indices of removing elements in the source
     */
    public void removeElements(int... targetIndices) {
        var src = takeSource();
        var removedIndices = Arrays.stream(targetIndices)
                .filter(i -> i < src.size() && removeSourceValue(i, src.get(i)))
                .toArray();
        var removedValues = removeAll(src, removedIndices);
        sourceRemoved(removedIndices, removedValues);
        model.updateSourceWithRemoving(removedIndices);
        list.clearSelection();
    }

    /**
     * generic impl. of removing elements at target-indices from the list by using {@link Iterator#remove()}
     * @param src the list
     * @param removedIndices the removing indices in the list
     * @return removed objects
     * @param <E> the element type of the list
     */
    public static <E> List<E> removeAll(List<E> src, int... removedIndices) {
        int nextRemove = 0;
        int index = 0;
        Arrays.sort(removedIndices);
        while (nextRemove < removedIndices.length && removedIndices[nextRemove] < 0) { //ignore minus indices
            ++nextRemove;
        }
        List<E> removedValues = new ArrayList<>(removedIndices.length);
        for (var iter = src.iterator();
             iter.hasNext() && nextRemove < removedIndices.length; ++index) {
            var v = iter.next();
            if (index == removedIndices[nextRemove]) {
                ++nextRemove;
                iter.remove();
                removedValues.add(v);
            }
        }
        return removedValues;
    }

    /**
     * move selected elements to the targetIndex
     * @param targetIndex ranged (0, size)
     * @see #moveElements(int, int[])
     */
    public void moveSelectedElements(int targetIndex) {
        moveElements(targetIndex, list.getSelectedIndices());
    }

    /**
     * move selected elements to the targetIndex
     * @param targetIndex  the target index (before moving of the source)
     * @param movedIndicesArray moved indices
     */
    public void moveElements(int targetIndex, int... movedIndicesArray) {
        var src = takeSource();
        List<Integer> movedIndices = new ArrayList<>(movedIndicesArray.length);
        int movedBefores = 0;
        List<ValueType> movedValues = new ArrayList<>(movedIndicesArray.length);
        for (int i : movedIndicesArray) {
            if (i < src.size()) {
                if (i < targetIndex) {
                    ++movedBefores;
                }
                movedIndices.add(i);
                movedValues.add(src.get(i));
            }
        }
        if (movedIndices.isEmpty()) {
            return; //nothing
        }
        if (targetIndex < 0) {
            targetIndex = 0;
        } else if (targetIndex > src.size()) {
            targetIndex = src.size();
        }
        var movedIndicesArrayActual = movedIndices.stream()
                .mapToInt(Integer::intValue)
                .sorted()
                .toArray();
        if (moveSourceValues(movedIndicesArrayActual, movedValues, targetIndex)) {
            int targetIndexAfterRemove = targetIndex - movedBefores;
            var removedValues = removeAll(src, movedIndicesArrayActual);
            src.addAll(targetIndexAfterRemove, removedValues);
            model.updateSourceWithMoving(targetIndex, movedIndicesArrayActual);
            list.clearSelection();
            list.addSelectionInterval(targetIndexAfterRemove, targetIndexAfterRemove + movedValues.size() - 1);
        }
    }

    /**
     * move selected elements to the top -1 (or to the end)
     * @see #moveElements(int, int[])
     */
    public void moveUpSelectedElements() {
        var index = Arrays.stream(list.getSelectedIndices())
                .min();
        if (index.isPresent()) {
            int n = index.orElse(-1) - 1;
            if (n < 0) {
                n = takeSource().size();
            }
            moveSelectedElements(n);
        }
    }

    /**
     * move selected elements to the bottom + 1 (or to the top)
     * @see #moveElements(int, int[])
     */
    public void moveDownSelectedElements() {
        var index = Arrays.stream(list.getSelectedIndices())
                .max();
        if (index.isPresent()) {
            int n = index.orElse(-1) + 2; //+1+after
            if (n > takeSource().size()) {
                n = 0;
            }
            moveSelectedElements(n);
        }
    }

    /**
     * the handler of {@link javax.swing.event.ListSelectionListener};
     * update toolbar actions
     * @param e the event
     */
    public void updateSelectedElements(ListSelectionEvent e) {
        var selected = list.getSelectedIndices();
        var hasSelected = (selected.length > 0);
        removeAction.setEnabled(hasSelected);
        upAction.setEnabled(hasSelected);
        downAction.setEnabled(hasSelected);
    }

    /**
     * @return the model of the list
     */
    public ValueListModel<ValueType, PaneType> getModel() {
        return model;
    }

    /**
     * @return the list
     */
    public JList<ValueListElementPane<ValueType, PaneType>> getList() {
        return list;
    }

    /**
     * the list-model for element-panes
     * @param <ValueType> the value type of the source
     * @param <PaneType> the element-pane type
     */
    public static class ValueListModel<ValueType, PaneType extends JComponent> extends AbstractListModel<ValueListElementPane<ValueType, PaneType>> {
        /** the owner pane*/
        protected ValueListPane<ValueType, PaneType> owner;
        /** created element-panes*/
        protected List<ValueListElementPane<ValueType, PaneType>> elementPanes = new ArrayList<>();

        /**
         * @param owner the pane of the list, non-null
         */
        public ValueListModel(ValueListPane<ValueType, PaneType> owner) {
            this.owner = owner;
        }

        @Override
        public int getSize() {
            return owner.takeSource().size();
        }

        /**
         * @param pane the pane in the list
         * @return index of the pane
         */
        public int indexOf(ValueListElementPane<?, ?> pane) {
            return elementPanes.indexOf(pane);
        }

        @Override
        public ValueListElementPane<ValueType, PaneType> getElementAt(int index) {
            while (index >= elementPanes.size()) {
                addNewElementPane();
            }
            return elementPanes.get(index);
        }

        /**
         * called from {@link #getElementAt(int)} if the index is over the element-panes.
         * <ol>
         *  <li>it creates a pane by {@link #newElementPane()} and add it to the list of panes (fix the index of the pane)</li>
         *  <li>{@link ValueListElementPane#setContentPane(JComponent)} with {@link ValueListPane#newElementPane(int, ValueListElementPane)}</li>
         *  <li>{@link ValueListElementPane#updateIndex()}</li>
         *  <li>{@link ValueListPane#updateSourceValueToElementPane(int, Object, ValueListElementPane)}</li>
         *  <li>fire a model event for adding</li>
         * </ol>
         */
        protected void addNewElementPane() {
            var src = owner.takeSource();
            int nextIndex = elementPanes.size();
            var elemPane = newElementPane();
            elementPanes.add(elemPane);
            elemPane.setContentPane(owner.newElementPane(nextIndex, elemPane));
            elemPane.updateIndex();
            owner.updateSourceValueToElementPane(nextIndex, nextIndex < src.size() ? src.get(nextIndex) : null, elemPane);
            fireIntervalAdded(this, nextIndex, nextIndex);
        }

        /**
         * @return create an element-pane (without the content-pane)
         */
        protected ValueListElementPane<ValueType, PaneType> newElementPane() {
            return new ValueListElementPane<>(owner);
        }

        /**
         * remove element panes, fire a model-removing event and {@link #updateSource()}
         * @param removedIndices moved indices of panes
         */
        public void updateSourceWithRemoving(int... removedIndices) {
            var removingPanes = removeAll(elementPanes, removedIndices);
            removingPanes.forEach(this::removeElementPane);
            for (int i : removedIndices) {
                fireIntervalRemoved(this, i, i);
            }
            updateSource();
        }

        /**
         * the callback when an element-pane is removed
         * @param pane remove the pane
         */
        protected void removeElementPane(ValueListElementPane<?,?> pane) {
            if (pane.getParent() != null) {
                owner.getList().remove(pane);
            }
        }

        /**
         * moving the selected-panes to the target-index and {@link #updateSource()}
         * @param targetIndex the target-index before moving
         * @param movedIndicesSorted indices of selected-panes
         */
        public void updateSourceWithMoving(int targetIndex, int... movedIndicesSorted) {
            int movedBefore = (int) Arrays.stream(movedIndicesSorted)
                    .filter(i -> i < targetIndex)
                    .count();
            int targetIndexAfter = targetIndex - movedBefore;
            var movingPanes = removeAll(elementPanes, movedIndicesSorted);
            elementPanes.addAll(targetIndexAfter, movingPanes);
            updateSource();
        }

        /**
         * checks differences between element-panes and the source-list.
         * <li>
         *     <li>if element-panes are over the size of the source-list, remove panes</li>
         *     <li>{@link ValueListElementPane#updateIndex()} and {@link ValueListPane#updateSourceValueToElementPane(int, Object, ValueListElementPane)};
         *        if updated, fire a change-event </li>
         *     <li>if the source-list  is over the panes, create new panes</li>
         *     <li>fire events for removing and adding panes</li>
         * </li>
         */
        public void updateSource() {
            var src = owner.takeSource();
            int srcSize = src.size();
            int paneSize = elementPanes.size();
            while (srcSize < elementPanes.size()) {
                removeElementPane(elementPanes.removeLast());
            }
            for (int i = 0; i < srcSize; ++i) {
                if (i < paneSize) {
                    var e = getElementAt(i);
                    e.updateIndex();
                    if (owner.updateSourceValueToElementPane(i, src.get(i), e)) {
                        fireContentsChanged(this, i, i);
                    }
                } else {
                    addNewElementPane();
                }
            }
            if (srcSize > paneSize) {
                fireIntervalAdded(this, paneSize, srcSize - 1);
            }
            if (srcSize < paneSize) {
                fireIntervalRemoved(this, srcSize, paneSize - 1);
            }
        }
    }

    /**
     * the element-pane wrapping a custom content-pane
     * @param <ValueType> the element-value type
     * @param <PaneType> the content-pane type
     */
    public static class ValueListElementPane<ValueType, PaneType extends JComponent> extends JComponent {
        /** the owner pane*/
        protected ValueListPane<ValueType, PaneType> owner;
        /** the content-pane */
        protected PaneType contentPane;
        /** the header index */
        protected JLabel header;
        /** true if selected; affects to border-painting */
        protected boolean selected;
        /** the current index */
        protected int lastHeaderIndex = -1;

        /**
         * the constructor
         * @param owner the ancestor pane
         */
        public ValueListElementPane(ValueListPane<ValueType, PaneType> owner) {
            this.owner = owner;
            init();
        }

        /** initialize the pane */
        protected void init() {
            initLayout();
            initHeader();
        }

        /** initialization for border and layout */
        protected void initLayout() {
            setOpaque(false); //opaque: important for combo-box
            var selBorder = new FocusBorder(this) {
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                    super.paintBorder(c, g, x, y, width, height);
                    if (isSelected()) {
                        paintStrokes(g, x, y, width, height);
                    }
                }
            };
            var u = UIManagerUtil.getInstance();
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(u.getScaledSizeInt(5), u.getScaledSizeInt(10), u.getScaledSizeInt(5), u.getScaledSizeInt(10)),
                    BorderFactory.createCompoundBorder(
                            selBorder,
                            BorderFactory.createEmptyBorder(u.getScaledSizeInt(5), u.getScaledSizeInt(10), u.getScaledSizeInt(5), u.getScaledSizeInt(10))
                    )));
            setLayout(new BorderLayout());
        }

        /** initialization for the header-label */
        protected void initHeader() {
            header = new JLabel("");
            UIManagerUtil u = UIManagerUtil.getInstance();
            header.setPreferredSize(new Dimension(u.getScaledSizeInt(72), u.getScaledSizeInt(24)));
            header.setBorder(BorderFactory.createEmptyBorder(0, u.getScaledSizeInt(15), 0, u.getScaledSizeInt(15)));
            add(header, BorderLayout.WEST);
        }

        /**
         * add the content-pane to the pane; if it already has a content-pane, remove it first.
         * @param pane the content-pane
         */
        public void setContentPane(PaneType pane) {
            if (contentPane != null) {
                remove(contentPane);
            }
            contentPane = pane;
            add(contentPane, BorderLayout.CENTER);
        }

        /**
         * @return the header-label
         */
        public JLabel header() {
            return header;
        }

        /**
         * @return the current content-pane
         */
        public PaneType contentPane() {
            return contentPane;
        }

        /**
         * @return obtains the index in the owner list, or -1
         */
        public int index() {
            return owner == null ? -1 : owner.getModel().indexOf(this);
        }

        /**
         * @return obtains the value of the source at the index of the element-pane
         */
        public ValueType value() {
            int i = index();
            return 0 <= i && i < owner.takeSource().size() ?
                    owner.takeSource().get(i) : null;
        }

        /** update the header-label by the current index */
        public void updateIndex() {
            int nextIndex = index();
            if (nextIndex != lastHeaderIndex) {
                lastHeaderIndex = nextIndex;
                header.setText(Integer.toString(nextIndex));
            }
        }

        /**
         * @return the current selected-index
         */
        public boolean isSelected() {
            return selected;
        }

        /**
         * called at painting by the cell-renderer
         * @param selected the new selected-index
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }

    /**
     * the cell-renderer for element-panes; returns the value as a component with setting the selected property.
     * Note the cell-rendering is used for current updating rows with the default JList-rendering mechanism.
     * In order to support frequent updating by mouse events, other rows are directly repainted by adding those panes to the list.
     */
    public static class ValueListRenderer extends DefaultListCellRenderer {
        public ValueListRenderer() {}

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof ValueListPane.ValueListElementPane<?, ?> comp) {
                comp.setSelected(isSelected);
                return comp;
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

    /**
     * the event-handler for repainting element panes.
     */
    public static class ValueListEventDispatcher implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, InputMethodListener {
        /** the parent list pane*/
        protected JList<?> pane;
        /** a mapper between an index to a element-pane */
        protected BiFunction<JList<?>, Integer, JComponent> componentMapper;

        /**
         * @param pane the parent list; if a non-null list is given, this is added as listeners to the list
         * @param componentMapper the mapper for {@link #componentMapper}
         */
        public ValueListEventDispatcher(JList<?> pane, BiFunction<JList<?>, Integer, JComponent>  componentMapper) {
            this.pane = pane;
            this.componentMapper = componentMapper;
            if (pane != null) {
                pane.addMouseListener(this);
                pane.addMouseMotionListener(this);
                pane.addMouseWheelListener(this);
                pane.addKeyListener(this);
                pane.addInputMethodListener(this);
            }
        }

        /**
         * processing the event by calling {@link #processEvent(AWTEvent, int...)} with pointing rows
         * @param e the processed event
         */
        public void processMouseEvent(MouseEvent e) {
            if (eventGuard > 0) {
                processEventToAncestor(e);
                return;
            }
            var pos = e.getPoint();
            processEvent(e, row(pos));
        }

        /**
         * handling a recursive event by the parent-pane; this is for mouse-wheel scrolling in a scroll-pane
         * @param e the recursive event processed by the parent
         */
        protected void processEventToAncestor(AWTEvent e) {
            var parent = pane.getParent();
            if (parent != null) {
                parent.dispatchEvent(e);
            }
        }

        /** the recursive guard for {@link #processEvent(AWTEvent, int...)}*/
        protected int eventGuard = 0;

        /**
         * <ol>
         *     <li>for each row of pointing-panes it obtains the element-pane by {@link #componentMapper}
         *          and bounds by {@link JList#getCellBounds(int, int)}</li> 
         *     <li>set the bounds to the element-pane</li>
         *     <li>Also, if the parent of the element-pane is null, the pane is added to the JList.</li>
         *     <li>{@link #dispatchEvent(Component, JComponent, AWTEvent)}</li>
         *  </ol>
         * @param e  the processing event
         * @param pointingRows indices of the model the event points to
         */
        public void processEvent(AWTEvent e, int... pointingRows) {
            if (eventGuard > 0) {
                processEventToAncestor(e);
                return;
            }
            eventGuard++;
            try {
                for (int n : pointingRows) {
                    if (0 <= n && n < pane.getModel().getSize()) {
                        var cellPane = componentMapper.apply(pane, n);
                        var cellBounds = pane.getCellBounds(n, n);
                        cellPane.setBounds(cellBounds);
                        if (cellPane.getParent() == null) {
                            pane.add(cellPane);
                        }
                        dispatchEvent(pane, cellPane, e);
                    }
                }
            } finally {
                eventGuard--;
            }
        }

        /**
         *
         * @param positionListLocal a JList-local coordination
         * @return a row-index
         */
        public int row(Point positionListLocal) {
            var list = pane;
            int h = list.getHeight();
            int size = pane.getModel().getSize();
            if (size <= 0) {
                return -1;
            }
            int rowHeight = h / size;
            int n = Math.min(positionListLocal.y / rowHeight, size - 1);
            while (0 <= n && n < size) {
                var cellBounds = list.getCellBounds(n, n);
                if (cellBounds != null) {
                    if (cellBounds.contains(positionListLocal)) {
                        return n;
                    } else if (positionListLocal.y < cellBounds.y) {
                        n--;
                    } else if (positionListLocal.y > cellBounds.getMaxY()) {
                        n++;
                    } else {
                        return -1;
                    }
                } else {
                    return -1;
                }
            }
            return -1;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            processMouseEvent(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            processMouseEvent(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            processMouseEvent(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            processMouseEvent(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            processMouseEvent(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            processMouseEvent(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            processMouseEvent(e);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            processMouseEvent(e);
        }

        @Override
        public void keyTyped(KeyEvent e) {
            processEvent(e, pane.getSelectedIndices());
        }

        @Override
        public void keyPressed(KeyEvent e) {
            processEvent(e, pane.getSelectedIndices());
        }

        @Override
        public void keyReleased(KeyEvent e) {
            processEvent(e, pane.getSelectedIndices());
        }

        @Override
        public void inputMethodTextChanged(InputMethodEvent event) {
            processEvent(event, pane.getSelectedIndices());
        }

        @Override
        public void caretPositionChanged(InputMethodEvent event) {
            processEvent(event, pane.getSelectedIndices());
        }

        /**
         * @param sender the list {@link #pane}
         * @param component an element-pane
         * @param e the processing event
         */
        public void dispatchEvent(Component sender, JComponent component, AWTEvent e) {
            var componentEvent = eventTarget(sender, component, e);
            if (componentEvent == null) {
                return;
            }
            component.dispatchEvent(componentEvent);
            for (Component sub : component.getComponents()) {
                if (sub instanceof JComponent subContainer) {
                    dispatchEvent(component, subContainer, componentEvent);
                }
            }
        }

        /**
         * @param e the tested event
         * @return  {@link InputEvent#isConsumed()}
         */
        public boolean isConsumed(AWTEvent e) {
            return e instanceof InputEvent ie && ie.isConsumed();
        }

        /**
         * converting the event to a subcomponent-local event
         * @param sender the list {@link #pane}
         * @param sub an element-pane
         * @param e the processing event
         * @return non-consumed event
         */
        public AWTEvent eventTarget(Component sender, Component sub, AWTEvent e) {
            if (isConsumed(e)) {
                return null;
            }
            if (e instanceof MouseEvent me) {
                if (sub.getBounds().contains(me.getPoint())) {
                    return SwingUtilities.convertMouseEvent(sender, me, sub);
                } else {
                    return null;
                }
            }
            return e;
        }
    }

    /** the action class for removing selected-panes */
    public static class ValueListRemoveAction extends AbstractAction {
        /** the target pane */
        protected ValueListPane<?, ?> owner;

        /**
         * @param owner the target pane
         */
        public ValueListRemoveAction(ValueListPane<?, ?> owner) {
            this.owner = owner;
            putValue(NAME, "Remove");
            putValue(Action.SMALL_ICON, GuiSwingIcons.getInstance().getRemoveIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            owner.removeSelectedElements();
        }
    }

    /** the action class for adding a new element */
    public static class ValueListAddAction extends AbstractAction {
        /** the target pane */
        protected ValueListPane<?, ?> owner;

        /**
         * @param owner the target pane
         */
        public ValueListAddAction(ValueListPane<?, ?> owner) {
            this.owner = owner;
            putValue(NAME, "Add");
            putValue(Action.SMALL_ICON, GuiSwingIcons.getInstance().getAddIcon());
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            owner.addNewElement();
        }
    }

    /** the action class for reordering selected-panes; moving-up */
    public static class ValueListUpAction extends AbstractAction {
        /** the target pane */
        protected ValueListPane<?, ?> owner;

        /**
         * @param owner the target pane
         */
        public ValueListUpAction(ValueListPane<?, ?> owner) {
            this.owner = owner;
            putValue(NAME, "Up");
            putValue(Action.SMALL_ICON, GuiSwingIcons.getInstance().getUpIcon());
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            owner.moveUpSelectedElements();
        }
    }

    /** the action class for reordering selected-panes; moving-down */
    public static class ValueListDownAction extends AbstractAction {
        /** the target pane */
        protected ValueListPane<?, ?> owner;

        /**
         * @param owner the target pane
         */
        public ValueListDownAction(ValueListPane<?, ?> owner) {
            this.owner = owner;
            putValue(NAME, "Down");
            putValue(Action.SMALL_ICON, GuiSwingIcons.getInstance().getDownIcon());
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            owner.moveDownSelectedElements();
        }
    }

    /**
     * the transfer handler for reordering element-panes; using {@link ValueListElementTransferable}
     */
    public static class ValueListElementTransferHandler extends TransferHandler {
        /** the target pane */
        protected ValueListPane<?, ?> owner;

        /**
         * @param owner the target pane
         */
        public ValueListElementTransferHandler(ValueListPane<?, ?> owner) {
            super(null);
            this.owner = owner;
        }

        /** install to the owner */
        public void install() {
            install(owner.getList());
        }

        /**
         * setting up the list pane with this
         * @param list the target list
         */
        public void install(JList<?> list) {
            list.setDragEnabled(true);
            list.setDropMode(DropMode.INSERT);
            list.setTransferHandler(this);
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new ValueListElementTransferable(owner, new ArrayList<>(owner.getList().getSelectedValuesList()));
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(getValueListElementFlavor());
        }

        @Override
        public boolean importData(TransferSupport support) {
            var loc = support.getDropLocation();
            if (loc instanceof JList.DropLocation listLoc) {
                try {
                    var data = support.getTransferable().getTransferData(getValueListElementFlavor());
                    if (data instanceof ValueListElementTransferable panes
                        && Objects.equals(owner, panes.getOwner())) {
                        int i = listLoc.getIndex();
                        owner.moveElements(i, panes.paneIndices());
                        return true;
                    } else {
                        return false;
                    }
                } catch (Exception ex) {
                    System.err.println(ex);
                    return false;
                }
            }
            return false;
        }
    }

    private static DataFlavor valueListElementFlavor;

    /**
     * @return a data-flavor of {@link DataFlavor#javaJVMLocalObjectMimeType} for {@link ValueListElementTransferable}
     */
    public static DataFlavor getValueListElementFlavor() {
        if (valueListElementFlavor == null) {
            try {
                valueListElementFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + ValueListElementTransferable.class.getName(),
                        "value-list-element", ValueListElementTransferable.class.getClassLoader());
            } catch (Exception ex) {
                valueListElementFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, "value-list-element");
                ex.printStackTrace();
            }
        }
        return valueListElementFlavor;
    }

    /**
     * a transferable class for reordering
     */
    public static class ValueListElementTransferable implements Transferable {
        /** the target pane */
        protected ValueListPane<?,?> owner;
        /** the selected panes */
        protected List<ValueListElementPane<?,?>> panes;
        /** {@link #getValueListElementFlavor()}*/
        protected static DataFlavor[] flavors = {getValueListElementFlavor()};

        /**
         * @param owner the target pane
         * @param panes the selected element-panes
         */
        public ValueListElementTransferable(ValueListPane<?,?> owner, List<ValueListElementPane<?, ?>> panes) {
            this.owner = owner;
            this.panes = panes;
        }

        /**
         * @return the target pane
         */
        public ValueListPane<?, ?> getOwner() {
            return owner;
        }

        /**
         * @return the target element-panes
         */
        public List<ValueListElementPane<?, ?>> getPanes() {
            return panes;
        }

        /**
         * @return the indices of target element-panes (only currently contained in the owner-pane)
         */
        public int[] paneIndices() {
            return panes.stream()
                    .mapToInt(ValueListElementPane::index)
                    .filter(i -> i != -1)
                    .toArray();
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return Arrays.stream(flavors)
                    .anyMatch(flavor::match);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (flavor.getMimeType().equals(getValueListElementFlavor().getMimeType())) {
                return this;
            }
            throw new UnsupportedFlavorException(flavor);
        }

    }
}
