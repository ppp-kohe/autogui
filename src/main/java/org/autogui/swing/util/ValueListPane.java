package org.autogui.swing.util;

import org.autogui.swing.icons.GuiSwingIcons;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * a component for listing {@link JComponent}s.
 * <pre>
 *   -----------------------------
 *   |  ValueListPane : JComponent(BorderLayout)
 *   |  --------------------------
 *   |    toolBar JPanel
 *   |  --------------------------
 *   |   {@link ValueListContentPane} ({@link ValueListModel})
 *   |     0: {@link ValueListElementPane}
 *   |          header JLabel |  contentPane PaneType
 *   |     ...
 * </pre>
 * @param <ValueType> the type for the source-element type
 * @param <PaneType> the type for contentPane of the list
 */
public abstract class ValueListPane<ValueType, PaneType extends JComponent> extends JComponent {
    /**
     * Note: the default constructors does not call the method at initialization.
     *   the constructors of sub-classes need to {@link #syncElements()} for initializing elements
     * @return the direct reference to the source-list. it needs to be stable and modifiable
     */
    public abstract List<ValueType> takeSource();

    /**
     * @param i the index for the creating element
     * @return a new i-th element-value, or null it cannot add a new element
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
     * called before moving items
     * @param indices the moved indices
     * @param values the moved values
     * @param targetIndex the targetIndex of the source ranged (0...size) .
     *                   Note that the index is a part of the current source before moving; including the moving indices
     * @return true if the moving is valid
     */
    public boolean moveSourceValues(int[] indices, List<ValueType> values, int targetIndex) {
        return true;
    }

    /**
     * called after moving items
     * @param insertedTargetIndex the target index of the list after moving
     * @param movedValues removed and inserted items
     */
    public void sourceMoved(int insertedTargetIndex, List<ValueType> movedValues) { }

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

    /** the model of the list*/
    protected ValueListModel<ValueType, PaneType> model;
    /** the list */
    protected ValueListContentPane<ValueType, PaneType> list;
    /** the wrapper-factory for appending the list; typically JScrollPane::new */
    protected Function<ValueListContentPane<ValueType, PaneType>, JComponent> listWrapper;

    /** the tool-action for adding */
    protected ValueListAddAction addAction;
    /** the tool-action for removing */
    protected ValueListRemoveAction removeAction;
    /** the tool-action for item-up */
    protected ValueListUpAction upAction;
    /** the tool-action for item-down */
    protected ValueListDownAction downAction;

    protected PropertyChangeListener globalFocusChangeListener;

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
     * @param listWrapper the wrapper factory at adding the list to the pane; e.g. {@link #scrollWrapper()}
     */
    @SuppressWarnings("this-escape")
    protected ValueListPane(boolean doInit, Function<ValueListContentPane<ValueType, PaneType>, JComponent> listWrapper) {
        this.listWrapper = listWrapper;
        if (doInit) {
            init();
        }
    }

    /**
     * @param listWrapper the wrapper factory at adding the list to the pane; e.g. {@link #scrollWrapper()}
     */
    @SuppressWarnings("this-escape")
    protected ValueListPane(Function<ValueListContentPane<ValueType, PaneType>, JComponent> listWrapper) {
        this(true, listWrapper);
    }


    /** do initialization processes */
    public void init() {
        initLayout();
        initTool();
        initList();
        initFocusRepainter();
    }

    /** initialize the layout with {@link BorderLayout} */
    protected void initLayout() {
        setLayout(new BorderLayout());
        setOpaque(false);
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
        list = new ValueListContentPane<>(model);
        list.addSelectionListener(this::updateSelectedElements);
        new ValueListElementTransferHandler(this).install();
        add(listWrapper.apply(list), BorderLayout.CENTER);
    }

    /** add a listener to global {@link KeyboardFocusManager} for observing focus changes:
     *  the listener checks whether focus gained/lost panes are chilren of the list and then calls {@link #focusCchange(JComponent, JComponent)} */
    protected void initFocusRepainter() {
        globalFocusChangeListener = e -> {
            JComponent oldDescendant = null;
            if (e.getOldValue() instanceof JComponent oldPane && isParent(oldPane)) {
                oldDescendant = oldPane;
            }
            JComponent newDescendant = null;
            if (e.getNewValue() instanceof JComponent newPane && isParent(newPane)) {
                newDescendant = newPane;
            }
            if (oldDescendant != null || newDescendant != null) {
                focusCchange(oldDescendant, newDescendant);
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", globalFocusChangeListener);
    }

    public static <ValueType, PaneType extends JComponent> Function<ValueListContentPane<ValueType, PaneType>, JComponent> scrollWrapper() {
        return w -> {
            var p = new JScrollPane(w);
            p.getVerticalScrollBar().setUnitIncrement(UIManagerUtil.getInstance().getScaledSizeInt(16));
            return p;
        };
    }

    public void close() {
        if (globalFocusChangeListener != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", globalFocusChangeListener);
            globalFocusChangeListener = null;
        }
    }

    /**
     * called when focus changed for given 2 panes;
     * the default behavior is selecting the newDescendant, and repaint entire list.
     * @param oldDescendant a focus lost descendant component of the list, or null
     * @param newDescendant a focus gained descendant component of the list, or null
     */
    protected void focusCchange(JComponent oldDescendant, JComponent newDescendant) {
        var newElem = upperElement(newDescendant);
        if (newElem != null) {
            getList().setSelectedIndex(newElem.index());
        }
        getList().repaint();
    }

    /**
     * @param pane a child component
     * @return true if ancestor of the pane is this list
     */
    public boolean isParent(JComponent pane) {
        return matchOrParent(pane, p -> {
            if (p == this) {
                return true;
            } else if (p instanceof ValueListElementPane<?, ?> vp) {
                return vp.getOwner() == ValueListPane.this;
            } else {
                return false;
            }
        }) != null;
    }

    /**
     * @param descendant a descendant compoennt of the list
     * @return the element pane containing the descendant
     */
    @SuppressWarnings("unchecked")
    public ValueListElementPane<ValueType, PaneType> upperElement(JComponent descendant) {
        if (isParent(descendant)) {
            var elemPane = matchOrParent(descendant, p -> p instanceof ValueListPane.ValueListElementPane<?,?>);
            return (ValueListElementPane<ValueType, PaneType>) elemPane;
        } else {
            return null;
        }
    }

    /**
     * @param pane a component or null
     * @param cond a test for ancestors of the pane
     * @return the pane or an ancesotor of the pane that matches the cond, or null
     */
    public static JComponent matchOrParent(JComponent pane, Predicate<JComponent> cond) {
        if (pane == null) {
            return null;
        } else if (cond.test(pane)) {
            return pane;
        } else if (pane.getParent() != null && pane.getParent() instanceof JComponent parent) {
            return matchOrParent(parent, cond);
        } else {
            return null;
        }
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
            sourceMoved(targetIndexAfterRemove, removedValues);
            model.updateSourceWithMoving(targetIndex, movedIndicesArrayActual);
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
     * called after model's updateSource
     */
    public void afterUpdateElements() { }

    /**
     * checks model-updating
     */
    public void syncElements() {
        getModel().updateSource();
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
    public ValueListContentPane<ValueType, PaneType> getList() {
        return list;
    }

    /// selection

    public void selectionFlip(int n) {
        if (0 <= n && n <= getModel().getSize()) {
            var sel = getModel().getElementAt(n).isSelected();
            if (sel) {
                getList().removeSelectionInterval(n, n);
            } else {
                getList().addSelectionInterval(n, n);
            }
        }
        getList().repaint();
    }

    public void selectionRangeTo(int n) {
        var sels = getList().getSelectedIndices();
        if (sels.length == 0) {
            selectionSet(n);
        } else if (n < sels[0]) {
            getList().clearSelection();
            getList().addSelectionInterval(n, sels[0]);
        } else {
            getList().clearSelection();
            getList().addSelectionInterval(sels[0], n);
        }
        getList().repaint();
    }

    public void selectionSet(int n) {
        getList().setSelectedIndex(n);
        getList().repaint();
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
            updateSource(() -> {
                var src = owner.takeSource();
                int nextIndex = elementPanes.size();
                var elemPane = newElementPane();
                elementPanes.add(elemPane);
                elemPane.setContentPane(owner.newElementPane(nextIndex, elemPane));
                elemPane.updateIndex();
                owner.updateSourceValueToElementPane(nextIndex, nextIndex < src.size() ? src.get(nextIndex) : null, elemPane);
                fireIntervalAdded(this, nextIndex, nextIndex);
            });
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
            updateSource(() -> {
                var removingPanes = removeAll(elementPanes, removedIndices);
                removingPanes.forEach(this::removeElementPane);
                for (int i : removedIndices) {
                    fireIntervalRemoved(this, i, i);
                }
                updateSourceBody();
            });
        }

        /**
         * the callback when an element-pane is removed
         * @param pane remove the pane
         */
        protected void removeElementPane(ValueListElementPane<?,?> pane) {
        }

        /**
         * moving the selected-panes to the target-index and {@link #updateSource()}
         * @param targetIndex the target-index before moving
         * @param movedIndicesSorted indices of selected-panes
         */
        public void updateSourceWithMoving(int targetIndex, int... movedIndicesSorted) {
            updateSource(() -> {
                int movedBefore = (int) Arrays.stream(movedIndicesSorted)
                        .filter(i -> i < targetIndex)
                        .count();
                int targetIndexAfter = targetIndex - movedBefore;
                var movingPanes = removeAll(elementPanes, movedIndicesSorted);
                elementPanes.addAll(targetIndexAfter, movingPanes);
                updateSourceBody();
                movingPanes.forEach(p ->
                        fireContentsChanged(this, p.index(), p.index()));
            });
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
            updateSource(this::updateSourceBody);
        }

        protected void updateSource(Runnable task) {
            for (var l : getListDataListeners()) {
                if (l instanceof ValueListModelUpdater u) {
                    var prevTask = task;
                    task = () -> u.updateSourceFromModel(prevTask);
                }
            }
            task.run();
            owner.afterUpdateElements();
        }

        protected void updateSourceBody() {
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

    public interface ValueListModelUpdater extends ListDataListener {
        /**
         * @param task transaction of {@link ListDataListener}; might cause multiple calls of methods in the listener
         */
        void updateSourceFromModel(Runnable task);
    }

    public static class ValueListModelAdapter implements ValueListModelUpdater {
        public ValueListModelAdapter() {}
        @Override public void updateSourceFromModel(Runnable task) { task.run(); }
        @Override public void intervalAdded(ListDataEvent e) {}
        @Override public void intervalRemoved(ListDataEvent e) {}
        @Override public void contentsChanged(ListDataEvent e) {}
    }

    public static class ValueListContentPane<ValueType, PaneType extends JComponent> extends JPanel implements ValueListModelUpdater, DropTargetListener {
        protected ValueListModel<ValueType, PaneType> model;
        protected int updating;
        protected boolean updated;
        protected List<ListSelectionListener> selectionListeners = new ArrayList<>();

        @SuppressWarnings("this-escape")
        public ValueListContentPane(ValueListModel<ValueType, PaneType> model) {
            setLayout(new ResizableFlowLayout(false).setFitHeight(true));
            this.model = model;
            model.addListDataListener(this);
            addMouseListener(createSelectionHandler());
            setBorder(BorderFactory.createEmptyBorder(2, 0, 5, 0)); //for painting drop-target
        }

        public void addSelectionListener(ListSelectionListener s) {
            selectionListeners.add(s);
        }

        public MouseAdapter createSelectionHandler() {
            return new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    var p = e.getPoint();
                    Component component = findTargetComponent(p);
                    if (component instanceof ValueListPane.ValueListElementPane<?,?> elemPane) {
                        elemPane.selectionHandle(e);
                    }
                }
            };
        }

        public Component findTargetComponent(Point p) {
            int min = Integer.MAX_VALUE;
            Component component = null;
            int i = 0;
            for (var comp : getComponents()) {
                var bounds = comp.getBounds();
                if (bounds.contains(p)) {
                    component = comp;
                    break;
                }
                int compDis = Math.abs(p.y - (bounds.y + (bounds.height / 2)));
                if (compDis < min) {
                    component = comp;
                    min = compDis;
                }
                ++i;
            }
            return component;
        }

        @Override
        public void updateSourceFromModel(Runnable task) {
            try {
                updating++;
                updated = false;
                task.run();
            } finally {
                updating--;
                if (updating <= 0 && updated) {
                    sync();
                }
            }
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            if (updating > 0) {
                updated = true;
            } else {
                sync();
            }
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            if (updating > 0) {
                updated = true;
            } else {
                sync();
            }
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            if (updating > 0) {
                updated = true;
            } else {
                sync();
            }
        }

        public void sync() {
            var models = IntStream.range(0, model.getSize())
                    .mapToObj(model::getElementAt)
                    .map(Component.class::cast)
                    .toList();
            var views = Arrays.asList(getComponents());
            ResizableFlowLayout layout = null;
            if (getLayout() instanceof ResizableFlowLayout l) {
                layout = l;
            }
            removeAll();
            for (Component m : models) {
                add(m);
                if (m instanceof ValueListPane.ValueListElementPane<?, ?> elementPane) {
                    elementPane.updateIndex();
                }
                if (layout != null) {
                    layout.setResizable(m, false);
                }
            }
            revalidate();
        }

        public int[] getSelectedIndices() {
            return getSelectedValuesList().stream()
                    .mapToInt(ValueListElementPane::index)
                    .toArray();
        }

        public void clearSelection() {
            int[] old = getSelectedIndices();
            getSelectedValuesList()
                    .forEach(e -> e.setSelected(false));
            updateSelection(old);
        }

        public void removeSelectionInterval(int from, int toInclusive) {
            int[] old = getSelectedIndices();
            getSelectedValuesList().stream()
                    .filter(e -> from <= e.index() && e.index() <= toInclusive)
                    .forEach(e -> e.setSelected(false));
            updateSelection(old);
        }

        public void addSelectionInterval(int from, int toInclusive) {
            int[] old = getSelectedIndices();
            for (int i = from, s = model.getSize(); i < s && i <= toInclusive; ++i) {
                model.getElementAt(i).setSelected(true);
            }
            updateSelection(old);
        }

        public void setSelectedIndex(int i) {
            int[] old = getSelectedIndices();
            getSelectedValuesList()
                    .forEach(e -> e.setSelected(false));
            if (i < model.getSize()) {
                model.getElementAt(i).setSelected(true);
            }
            updateSelection(old);
        }

        protected void updateSelection(int[] old) {
            int[] curr = getSelectedIndices();
            int s = model.getSize() - 1;
            int min = Math.min(Arrays.stream(old).min().orElse(s), Arrays.stream(curr).max().orElse(s));
            int max = Math.max(Arrays.stream(old).max().orElse(0), Arrays.stream(curr).max().orElse(0));
            var e = new ListSelectionEvent(this, min, max, false);
            selectionListeners.forEach(l -> l.valueChanged(e));
            repaint();
        }

        public List<ValueListElementPane<ValueType,PaneType>> getSelectedValuesList() {
            return IntStream.range(0, model.getSize())
                    .mapToObj(model::getElementAt)
                    .filter(ValueListElementPane::isSelected)
                    .toList();
        }

        protected int dropTargetIndex = -1;

        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            var p = dtde.getLocation();
            updateDropPosition(p);
            repaint();
        }

        public int dropPositionIndex(Point p) {
            var comp = findTargetComponent(p);
            int i = -1;
            if (comp instanceof ValueListPane.ValueListElementPane<?,?> elementPane) {
                i = elementPane.index();
                var bounds = elementPane.getBounds();
                if (bounds.y + (bounds.height / 2) < p.y) {
                    ++i;
                }
            }
            return i;
        }

        protected void updateDropPosition(Point p) {
            dropTargetIndex = dropPositionIndex(p);
        }

        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            dragEnter(dtde);
        }

        @Override
        public void dropActionChanged(DropTargetDragEvent dtde) {}

        @Override
        public void dragExit(DropTargetEvent dte) {
            dropTargetIndex = -1;
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {
            updateDropPosition(dtde.getLocation());
            if (dropTargetIndex >= 0) {
                dtde.acceptDrop(dtde.getDropAction());
                dropTargetIndex = -1;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (dropTargetIndex >= 0) {
                var g2 = (Graphics2D) g;
                g2.setColor(UIManagerUtil.getInstance().getTextPaneSelectionForeground());
                g2.setStroke(new BasicStroke(2));
                int y;
                if (dropTargetIndex >= getComponentCount()) {
                    if (getComponentCount() == 0) {
                        y = 2;
                    } else {
                        var a = getComponent(getComponentCount() - 1);
                        var b = a.getBounds();
                        y = b.y + b.height + 1;
                    }
                } else {
                    var a = getComponent(dropTargetIndex);
                    y = a.getBounds().y - 1;
                }
                g.drawLine(0, y, getWidth(), y);
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
        @SuppressWarnings("this-escape")
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
            header.addMouseListener(createSelectionHandler());
            add(header, BorderLayout.WEST);
            DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this::handleDrag);
            DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(header, DnDConstants.ACTION_MOVE, this::handleDrag);
        }

        public void handleDrag(DragGestureEvent e) {
            if (getParent() instanceof JComponent list) {
                var t = list.getTransferHandler();
                if (t != null) {
                    t.exportAsDrag(list, e.getTriggerEvent(), TransferHandler.MOVE);
                }
            }
        }

        protected void initHandler() {
            addMouseListener(createSelectionHandler());
        }

        protected MouseAdapter createSelectionHandler() {
            return new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    selectionHandle(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    selectionHandle(e);
                }
            };
        }

        public void selectionHandle(MouseEvent e) {
            if (e.isShiftDown()) {
                owner.selectionRangeTo(index());
            } else if (e.isMetaDown()) {
                owner.selectionFlip(index());
            } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                owner.selectionSet(index());
            }
        }

        public ValueListPane<ValueType, PaneType> getOwner() {
            return owner;
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

    /** the action class for removing selected-panes */
    public static class ValueListRemoveAction extends AbstractAction {
        /** the target pane */
        protected ValueListPane<?, ?> owner;

        /**
         * @param owner the target pane
         */
        @SuppressWarnings("this-escape")
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
        @SuppressWarnings("this-escape")
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
        @SuppressWarnings("this-escape")
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
        @SuppressWarnings("this-escape")
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
        public void install(JComponent list) {
            list.setTransferHandler(this);
            DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(list, DnDConstants.ACTION_COPY, e ->
                list.getTransferHandler().exportAsDrag(list, e.getTriggerEvent(), TransferHandler.MOVE));
            if (list instanceof ValueListPane.ValueListContentPane<?, ?> contentPane) {
                try {
                    list.getDropTarget().addDropTargetListener(contentPane);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
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
            try {
                var data = support.getTransferable().getTransferData(getValueListElementFlavor());
                if (data instanceof ValueListElementTransferable panes
                        && Objects.equals(owner, panes.getOwner())) {
                    int i = -1;
                    if (loc instanceof JList.DropLocation listLoc) {
                        i = listLoc.getIndex();
                    } else if (owner.getList() instanceof ValueListPane.ValueListContentPane<?,?> listPane) {
                        i = owner.getList().dropPositionIndex(loc.getDropPoint());
                    }
                    if (i >= 0) {
                        owner.moveElements(i, panes.paneIndices());
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } catch (Exception ex) {
                System.err.println(ex);
                return false;
            }
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
