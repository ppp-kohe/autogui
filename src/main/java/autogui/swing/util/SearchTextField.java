package autogui.swing.util;

import autogui.swing.icons.GuiSwingIcons;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * a text-field implementation with supporting background searching.
 * <pre>
 *     [ icon: JButton | textField | popupButton ]
 *       [ popupMenu:
 *             model.getCandidates(textField.text,...) ]
 * </pre>
 *
 * The popup menus are composition of
 *    {@link #getMenuItemsSource()}
 *       -&gt; {@link PopupExtensionText#getEditActions(JTextComponent)},
 *      and
 *       {@link #getSearchedItems()} whose items are set by
 *         {@link #setCurrentSearchedItems(List, boolean)} or
 *          {@link #setCurrentSearchedItems(List, PopupCategorized.CategorizedMenuItem)},
 *           called from {@link SearchTask}'s background task via {@link SearchTextFieldModel}.
 *
 *  <p>
 *     If a background task calls setCurrentSearchedItems and {@link #getSearchedItems()} is changed,
 *       then {@link SearchedItemsListener} is called.
 *      In {@link #initPopup()}, it registers a listener returned by
 *         {@link #getPopupUpdateListener(PopupExtensionText, PopupCategorized)},
 *         which dynamically calls {@link PopupExtension#setupMenu()}.
 *
 *  <p>
 *     If the user edits texts, then it needs to dynamically update the popup.
 *      To do this, {@link #initField()} sets up
 *        {@link EditingRunner} with {@link #updateField(List)}
 *         and registers it to the field as document listener, action listener and focus listener .
 */
public class SearchTextField extends JComponent {
    protected SearchTextFieldModel model;
    protected JButton icon;
    protected JTextField field;
    protected EditingRunner editingRunner;

    protected PopupExtensionText popup;
    protected JButton popupButton;
    protected List<Object> menuItemsSource;

    protected List<PopupCategorized.CategorizedMenuItem> currentSearchedItems;
    protected SearchTask currentTask;
    protected List<SearchedItemsListener> searchedItemsListeners;

    protected static ImageIcon emptyIcon;

    protected SearchBackgroundPainter backgroundPainter;

    protected KeyUndoManager undoManager;

    /** the interface for the searching model */
    public interface SearchTextFieldModel {
        /** the method is executed under the background thread of {@link SwingWorker}.
         *   The publisher is passed for checking cancellation and publishing intermediate result.
         *   <pre>
         *       List&lt;CategorizedMenuItem&gt; results = new ArrayList&lt;&gt;();
         *       for (CategorizedMenuItem item : ...) {
         *           if (publisher.isSearchCancelled()) {
         *               break;
         *           }
         *           results.add(item);
         *           publisher.publishSearch(results);
         *       }
         *       return results;
         *   </pre>
         *   Note: the method is also called by selection of a searched menu item.
         *    Then the select is already set as the same value of the text.
         *
         * @param text the input text
         * @param editable whether the field is editable or not
         * @param publisher the publisher for submitting the searched item
         * @return the total results of searched items
         */
        List<PopupCategorized.CategorizedMenuItem> getCandidates(String text, boolean editable, SearchTextFieldPublisher publisher);

        /** <strike>The method is executed under the event dispatching thread.</strike>
         *   The user selects the item from a menu and then this method will be called.
         * @param item the selected item
         */
        void select(PopupCategorized.CategorizedMenuItem item);

        /** After {@link #getCandidates(String, boolean, SearchTextFieldPublisher)},
         *    an exact matching item might be found, and then the method returns the item.
         *    Otherwise returns null.
         *    The method is executed under the event dispatching thread.
         * @return the exact matched item or null
         */
        PopupCategorized.CategorizedMenuItem getSelection();

        default boolean isFixedCategorySize() {
            return false;
        }

        default boolean isBackgroundTask() {
            return true;
        }
    }

    /** the intermediate items submission target */
    public interface SearchTextFieldPublisher {
        boolean isSearchCancelled();
        void publishSearch(List<PopupCategorized.CategorizedMenuItem> intermediateResult);
    }

    /** empty impl. of the publisher*/
    public static class SearchTextFieldPublisherEmpty implements SearchTextFieldPublisher {
        @Override
        public boolean isSearchCancelled() {
            return false;
        }

        @Override
        public void publishSearch(List<PopupCategorized.CategorizedMenuItem> intermediateResult) {
        }
    }

    /** a listener interface for receiving the searched items */
    public interface SearchedItemsListener {
        void updateCurrentSearchedItems(List<PopupCategorized.CategorizedMenuItem> items, boolean done);
    }

    /** the empty model for searching nothing */
    public static class SearchTextFieldModelEmpty implements SearchTextFieldModel {
        @Override
        public List<PopupCategorized.CategorizedMenuItem> getCandidates(String text, boolean editable, SearchTextFieldPublisher publisher) {
            return new ArrayList<>();
        }

        @Override
        public void select(PopupCategorized.CategorizedMenuItem item) { }

        @Override
        public PopupCategorized.CategorizedMenuItem getSelection() {
            return null;
        }

        @Override
        public boolean isBackgroundTask() {
            return false;
        }
    }

    public SearchTextField() {
        this(new SearchTextFieldModelEmpty());
    }

    public SearchTextField(SearchTextFieldModel model) {
        this.model = model;
        this.searchedItemsListeners = new ArrayList<>(3);
        init();
    }

    public void init() {
        initIcon();
        initField();
        initPopup();
        initLayout();
    }

    public void initIcon() {
        icon = new JButton(getEmptyIcon());
        int size = UIManagerUtil.getInstance().getScaledSizeInt(16);
        icon.setPreferredSize(new Dimension(size, size));
        icon.setBorderPainted(false);
        icon.setOpaque(false);
        icon.setFocusable(false);
        icon.setContentAreaFilled(false);
        icon.setBackground(getBackground());
    }

    public Icon getEmptyIcon() {
        if (emptyIcon == null) {
            int size = UIManagerUtil.getInstance().getScaledSizeInt(16);
            BufferedImage img = new BufferedImage(size,size, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics g = img.createGraphics();
            g.setColor(new Color(255, 255, 255, 0));
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            g.dispose();
            emptyIcon = new ImageIcon(img);
        }
        return emptyIcon;
    }

    public void initField() {
        editingRunner = new EditingRunner(getEditingRunnerDelay(), this::updateField);
        field = new JTextField() {
            @Override
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                int w = UIManagerUtil.getInstance().getScaledSizeInt(100);
                if (dim.width < w) {
                    dim.width = w;
                }
                return dim;
            }
        };
        field.setOpaque(false);
        setOpaque(false);

        undoManager = new KeyUndoManager();
        undoManager.putListenersAndActionsTo(field);

        field.getDocument().addDocumentListener(editingRunner);
        field.addActionListener(editingRunner);
        field.addFocusListener(editingRunner);
        field.addInputMethodListener(editingRunner);
    }

    /**
     * set the transfer handler to the pane, the field and the icon button.
     * Also, set up the dragging gesture with COPY operation for the field and the icon.
     * @param handler the target handler
     */
    public void setTransferHandlerWithSettingExportingDragSource(TransferHandler handler) {
        setTransferHandler(handler);
        getField().setTransferHandler(handler);
//        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(getField(), DnDConstants.ACTION_COPY, e -> {
//            getTransferHandler().exportAsDrag(getField(), e.getTriggerEvent(), TransferHandler.COPY);
//        });
        getField().setDragEnabled(true);

        getIcon().setTransferHandler(handler);
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(getIcon(), DnDConstants.ACTION_COPY, e -> {
            getTransferHandler().exportAsDrag(getIcon(), e.getTriggerEvent(), TransferHandler.COPY);
        });

        JComponent component = getIcon();
        setupCopyAndPaste(component);
    }

    public static void setupCopyAndPaste(JComponent component) {
        Action copy = TransferHandler.getCopyAction();
        component.getActionMap().put(copy.getValue(Action.NAME), copy);
        Action paste = TransferHandler.getPasteAction();
        component.getActionMap().put(paste.getValue(Action.NAME), paste);

        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), copy.getValue(Action.NAME));
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), paste.getValue(Action.NAME));
    }

    protected int getEditingRunnerDelay() {
        return 500;
    }


    public void initPopup() {
        PopupCategorized categorized = initPopupCategorized(getMenuItems(),
                this::selectSearchedItemFromGui);

        PopupExtensionText.putInputEditActions(field);
        PopupExtensionText.putUnregisteredEditActions(field);
        popup = new PopupExtensionText(field, PopupExtensionText.getDefaultKeyMatcher(), categorized);
        popupButton = new JButton(popup.getAction());
        popupButton.setContentAreaFilled(false);
        popupButton.setFocusable(false);
        addSearchItemsListener(getPopupUpdateListener(popup, categorized));
    }


    public PopupCategorized initPopupCategorized(Supplier<? extends Collection<PopupCategorized.CategorizedMenuItem>> itemSupplier,
                                                 Consumer<PopupCategorized.CategorizedMenuItem> itemConsumer) {
        return model.isFixedCategorySize() ?
                new PopupCategorized.PopupCategorizedFixed(itemSupplier, itemConsumer) :
                new PopupCategorized(itemSupplier, itemConsumer);

    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);
        getField().setToolTipText(text);
        getIcon().setToolTipText(text);
        getPopupButton().setToolTipText(text);
    }

    public Supplier<List<PopupCategorized.CategorizedMenuItem>> getMenuItems() {
        return () -> PopupCategorized.getMenuItems(
                getMenuItemsSource(),
                getSearchedItems());
    }

    /**
     * @return list of menu items including {@link Action} or {@link JComponent}
     */
    public List<Object> getMenuItemsSource() {
        if (menuItemsSource == null) {
            menuItemsSource = new ArrayList<>();
            menuItemsSource.addAll(PopupExtensionText.getEditActions(field));
        }
        return menuItemsSource;
    }

    public SearchedItemsListener getPopupUpdateListener(PopupExtensionText popup, PopupCategorized categorized) {
        return (items,done) -> {
            if (popup.getMenu().isVisible()) {
                popup.setupMenu();
                if (!done) {
                    popup.getMenu().add(categorized.getMenuBuilder().createLabel("Adding..."));
                }
                popup.getMenu().pack();
            }
        };
    }

    public void initLayout() {
        setBackgroundWithoutInit(Color.white);
        initBackgroundPainter();

        setLayout(new BorderLayout());
        add(icon, BorderLayout.WEST);
        add(field, BorderLayout.CENTER);
        add(popupButton, BorderLayout.EAST);
    }

    public void initBackgroundPainter() {
        backgroundPainter = new SearchBackgroundPainterBordered(this);
    }

    @Override
    public void add(Component comp, Object constraints) {
        super.add(comp, constraints);
        if (comp instanceof JComponent) {
            backgroundPainter.setChild((JComponent) comp);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        backgroundPainter.paintComponent(g);
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        backgroundPainter.init();
    }

    public void setBackgroundWithoutInit(Color bg) {
        super.setBackground(bg);
    }

    public SearchTextFieldModel getModel() {
        return model;
    }

    public PopupExtensionText getPopup() {
        return popup;
    }

    public JButton getPopupButton() {
        return popupButton;
    }

    public EditingRunner getEditingRunner() {
        return editingRunner;
    }

    public JButton getIcon() {
        return icon;
    }

    public JTextField getField() {
        return field;
    }

    public SearchTask getCurrentTask() {
        return currentTask;
    }

    public void addSearchItemsListener(SearchedItemsListener itemsListener) {
        searchedItemsListeners.add(itemsListener);
    }

    public void removeSearchItemsListener(SearchedItemsListener itemsListener) {
        searchedItemsListeners.remove(itemsListener);
    }

    public List<SearchedItemsListener> getSearchedItemsListeners() {
        return searchedItemsListeners;
    }

    public boolean isSwingEditable() {
        return isEnabled() && getField().isEnabled() && getField().isEditable();
    }

    ////////////////////

    /** After editing text or action performed, this method will be executed under the scheduler thread
     * @param events the accumulated events
     */
    public void updateField(List<Object> events) {
        try {
            boolean modified = isUpdateFieldModifiedEvents(events);
            SwingUtilities.invokeLater(() -> updateFieldInEvent(modified));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean isUpdateFieldModifiedEvents(List<Object> events) {
        return events.stream()
                .anyMatch(this::isUpdateFieldModifiedEvent);
    }

    public boolean isUpdateFieldModifiedEvent(Object e) {
        return !(e instanceof ActionEvent || e instanceof FocusEvent);
    }

    /** executed under event thread:
     *  start a new search task in background
     * @param modified  true if the field is actually edited
     */
    public void updateFieldInEvent(boolean modified) {
        if (modified || currentSearchedItems == null) {
            String text = field.getText();
            if (currentTask != null && !currentTask.isDone()) {
                currentTask.cancel(true);
            }
            if (model.isBackgroundTask()) {
                currentTask = createSearchTask(text);
                currentTask.execute();
            } else {
                //immediate
                setCurrentSearchedItems(
                        model.getCandidates(text, field.isEditable(), new SearchTextFieldPublisherEmpty()),
                        model.getSelection());
            }
        }
    }

    public SearchTask createSearchTask(String text) {
        return new SearchTask(this, text);
    }

    /** set the searched items from the background task: it might be an intermediate result
     * @param currentSearchedItems the intermediate searched items
     * @param done true if the search is done
     */
    public void setCurrentSearchedItems(List<PopupCategorized.CategorizedMenuItem> currentSearchedItems, boolean done) {
        this.currentSearchedItems = currentSearchedItems;
        searchedItemsListeners.forEach(l -> l.updateCurrentSearchedItems(currentSearchedItems, done));
    }

    /** called once when the search is done
     * @param currentSearchedItems the final result of the search
     * @param selection the selected item by the model
     */
    public void setCurrentSearchedItems(List<PopupCategorized.CategorizedMenuItem> currentSearchedItems,
                                        PopupCategorized.CategorizedMenuItem selection) {
        this.currentSearchedItems = currentSearchedItems;
        selectSearchedItemFromModel(selection);
        searchedItemsListeners.forEach(l -> l.updateCurrentSearchedItems(currentSearchedItems, true));
    }

    public List<PopupCategorized.CategorizedMenuItem> getSearchedItems() {
        List<PopupCategorized.CategorizedMenuItem> items = currentSearchedItems;
        if (items == null) {
            return Collections.emptyList();
        } else {
            return items;
        }
    }

    /** the user selects the item from the menu.
     * update selection in the model and also GUI display.
     * It will stop the current running task if exists, and starts a new task.
     * <strike>This is the task using an item supplied by the model.
     *  So, it does not cause a further update that leads to a background task </strike>
     *
     * @param item the selected item
     */
    public void selectSearchedItemFromGui(PopupCategorized.CategorizedMenuItem item) {
        model.select(item);
        setIconFromSearchedItem(item);
        setTextFromSearchedItem(item);
        if (SwingUtilities.isEventDispatchThread()) {
            updateFieldInEvent(true);
        } else {
            SwingUtilities.invokeLater(() -> updateFieldInEvent(true));
        }
    }

    /** called when the search is done, and update only the icon
     * @param item  the selected item by the model
     * */
    public void selectSearchedItemFromModel(PopupCategorized.CategorizedMenuItem item) {
        setIconFromSearchedItem(item);
    }

    public void setIconFromSearchedItem(PopupCategorized.CategorizedMenuItem item) {
        icon.setIcon(convertIcon(item == null ? null : item.getIcon()));
    }

    public Icon convertIcon(Icon icon) {
        if (icon == null) {
            return getEmptyIcon();
        }
        Icon ei = getEmptyIcon();
        int ew = ei.getIconWidth();
        int eh = ei.getIconHeight();

        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        if (w != ew || h != eh) {
            if (icon instanceof ImageIcon) {
                icon = new ImageIcon(((ImageIcon) icon).getImage().getScaledInstance(ew, eh, Image.SCALE_SMOOTH));
            } else if (icon instanceof GuiSwingIcons.ResourceIcon) {
                icon = new GuiSwingIcons.ResourceIcon(((GuiSwingIcons.ResourceIcon) icon).getImage(), ew, eh);
            } else {
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
                icon.paintIcon(this.icon, img.getGraphics(), 0, 0);
                icon = new ImageIcon(img.getScaledInstance(ew, eh, Image.SCALE_SMOOTH));
            }
        }
        return icon;
    }

    public void setTextFromSearchedItem(PopupCategorized.CategorizedMenuItem item) {
        setTextWithoutUpdateField(item.getName());
    }

    public void setTextWithoutUpdateField(String text) {
        editingRunner.setEnabled(false);
        try {
            field.setText(text);
        } finally {
            editingRunner.setEnabled(true);
        }
        SwingUtilities.invokeLater(() -> {
            String str = field.getText();
            field.setSelectionEnd(str.length());
            field.setSelectionStart(str.length());
        });
    }

    public void shutdown() {
        this.editingRunner.shutdown();
    }

    /** the background searching task */
    public static class SearchTask extends SwingWorker<List<PopupCategorized.CategorizedMenuItem>, List<PopupCategorized.CategorizedMenuItem>>
        implements SearchTextFieldPublisher {
        protected SearchTextField field;
        protected String text;

        public SearchTask(SearchTextField field, String text) {
            this.field = field;
            this.text = text;
        }

        public String getText() {
            return text;
        }

        //at first, the background thread call the method.
        //while in the method, this is passed as SearchTextFieldPublisher
        @Override
        protected List<PopupCategorized.CategorizedMenuItem> doInBackground() throws Exception {
            try{
                return field.getModel().getCandidates(text, field.isSwingEditable(), this);
            } catch (CancellationException ex) {
                return new ArrayList<>();
            }
        }

        //pushSearch cause the method in the event dispatching thread
        @Override
        protected void process(List<List<PopupCategorized.CategorizedMenuItem>> chunks) {
            int last = chunks.size() - 1;
            if (last >= 0) {
                field.setCurrentSearchedItems(chunks.get(last), false);
            }
        }

        @Override
        protected void done() {
            try {
                field.setCurrentSearchedItems(get(), field.getModel().getSelection());
            } catch (CancellationException ex) {
                //nothing
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public boolean isSearchCancelled() {
            return isCancelled();
        }

        @Override
        public void publishSearch(List<PopupCategorized.CategorizedMenuItem> intermediateResult) {
            publish(new ArrayList<>(intermediateResult));
        }
    }

    /** an interface for painting background */
    public static class SearchBackgroundPainter {
        public void setChild(JComponent child) { }
        public void init() { }
        public void paintComponent(Graphics g) { }
    }

    public static Color getFocusColor() {
        Color focusColor = UIManagerUtil.getInstance().getFocusColor();
        float[] hsb = new float[3];
        hsb = Color.RGBtoHSB(focusColor.getRed(), focusColor.getGreen(), focusColor.getBlue(), hsb);
        focusColor = Color.getHSBColor(hsb[0] * 0.97f, hsb[1] * 0.53f, hsb[2]);
        return focusColor;
    }

    /** the painter impl. */
    public static class SearchBackgroundPainterBordered extends SearchBackgroundPainter {
        protected JComponent component;
        protected Color[] gradientColors;
        protected Color focusColor;
        protected BasicStroke[] strokes;

        public SearchBackgroundPainterBordered(JComponent component) {
            this.component = component;
            setToComponent();
        }

        protected void setToComponent() {
            component.setLayout(new BorderLayout());
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int h = ui.getScaledSizeInt(7);
            int w = ui.getScaledSizeInt(5);
            component.setBorder(BorderFactory.createEmptyBorder(h, w, h, w));
        }

        public void init() {
            initGradientColors();
            initFocusColor();
            initStrokes();
        }

        public void initGradientColors() {
            int gradMax = 3;

            List<Color> cs = new ArrayList<Color>();
            for (int i = 0; i < gradMax; ++i) {
                float p = (((float) i + 1) / ((float) gradMax));
                cs.add(getGradientColor(p));
            }
            gradientColors = cs.toArray(new Color[cs.size()]);
        }

        public void initFocusColor() {
            focusColor = getFocusColor();
        }

        public void initStrokes() {
            strokes = new BasicStroke[3];
            for (int i = 0; i < strokes.length; ++i) {
                strokes[i] = new BasicStroke(strokes.length / 2.0f);
            }
        }

        public Color getGradientColor(float p) {
            Color max = component.getBackground();
            if (max == null) {
                max = Color.white;
            }
            int red = max.getRed();
            int green = max.getGreen();
            int blue = max.getBlue();
            int alpha = max.getAlpha();
            return new Color(getColorComponent(red, p), getColorComponent(green, p), getColorComponent(blue, p), alpha);
        }

        public int getColorComponent(int max, float p) {
            float base = ((float) max) * 0.9f;
            return (int) ((max - base) * p + base);
        }

        //////////

        public void setChild(JComponent comp) {
            setChildFocusListener(comp);
            setChildEmptyBorder(comp);
        }

        public void setChildFocusListener(Component comp) {
            comp.addFocusListener(new FocusRepaint());
        }

        public class FocusRepaint implements FocusListener {
            @Override
            public void focusGained(FocusEvent e) {
                component.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                component.repaint();
            }
        }

        public void setChildEmptyBorder(JComponent comp) {
            comp.setOpaque(false);
            comp.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        }

        /////////

        public void paintComponent(Graphics g) {
            if (gradientColors == null) {
                init();
            }
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int x = ui.getScaledSizeInt(2);
            int y = ui.getScaledSizeInt(2);
            int width = component.getWidth() - (x * 2);
            int height = component.getHeight() - (y * 2);
            float arc = ui.getScaledSizeInt(4);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D rr = new RoundRectangle2D.Float(x, y, width - (x/2), height - (y/2), arc, arc);

            paintGradientColors(g2, rr);

            if (hasFocusChild(component)) {
                paintFocusStrokes(g2, rr);
            }
        }

        public void paintGradientColors(Graphics2D g2, RoundRectangle2D rr) {
            int x = (int) rr.getX();
            int y = (int) rr.getY();
            int size = Math.max(1, UIManagerUtil.getInstance().getScaledSizeInt(1));
            int width = (int) rr.getWidth() + size;
            int height = (int) rr.getHeight() + size;
            float arcW = (float) rr.getArcWidth();
            float arcH = (float) rr.getArcHeight();
            //g2.setPaint(Color.white);
            //g2.fill(rr);
            Color[] cs = gradientColors;
            for (int i = 0, l = cs.length; i < l; ++i) {
                g2.setPaint(cs[i]);
                RoundRectangle2D rp = new RoundRectangle2D.Float(x + i, y + i, width - (2 * i), height - (2 * i), arcW, arcH);
                g2.fill(rp);
            }
            g2.setColor(new Color(180, 180, 180));
            g2.setStroke(new BasicStroke(UIManagerUtil.getInstance().getScaledSizeFloat(0.7f)));
            g2.draw(rr);
        }

        public void paintFocusStrokes(Graphics2D g2, RoundRectangle2D rr) {
            Color color2 = new Color(focusColor.getRed(), focusColor.getGreen(), focusColor.getBlue(), 150);
            g2.setColor(color2);
            for (BasicStroke s : strokes) {
                g2.setStroke(s);
                g2.draw(rr);
            }
        }

        public boolean hasFocusChild(Component c) {
            if (!c.hasFocus()) {
                if (c instanceof Container) {
                    return Arrays.stream(((Container) c).getComponents())
                            .anyMatch(this::hasFocusChild);
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }
}
