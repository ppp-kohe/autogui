package autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <pre>
 *     [ icon: JButton | textField | popupButton ]
 *       [ popupMenu:
 *             model.getCandidates(textField.text,...) ]
 * </pre>
 */
public class SearchTextField extends JComponent {
    protected SearchTextFieldModel model;
    protected JButton icon;
    protected JTextField field;
    protected ScheduledTaskRunner.EditingRunner editingRunner;

    protected PopupExtensionText popup;
    protected JButton popupButton;

    protected List<PopupCategorized.CategorizedPopupItem> currentSearchedItems;
    protected SearchTask currentTask;
    protected List<SearchedItemsListener> searchedItemsListeners;

    protected static ImageIcon emptyIcon;

    protected SearchBackgroundPainter backgroundPainter;

    public interface SearchTextFieldModel {
        /** the method is executed under the background thread of {@link SwingWorker}.
         *   The publisher is passed for checking cancellation and publishing intermediate result.
         *   <pre>
         *       List&lt;CategorizedPopupItem&gt results = new ArrayList&lt;&gt();
         *       for (CategorizedPopupItem item : ...) {
         *           if (publisher.isSearchCancelled()) {
         *               break;
         *           }
         *           results.add(item);
         *           publisher.publishSearch(results);
         *       }
         *       return results;
         *   </pre>
         * */
        List<PopupCategorized.CategorizedPopupItem> getCandidates(String text, boolean editable, SearchTextFieldPublisher publisher);

        /** The method is executed under the event dispatching thread.
         *   The user selects the item from a menu and then this method will be called. */
        void select(PopupCategorized.CategorizedPopupItem item);

        /** After {@link #getCandidates(String, boolean, SearchTextFieldPublisher)},
         *    an exact matching item might be found, and then the method returns the item.
         *    The method is executed under the event dispatching thread. */
        PopupCategorized.CategorizedPopupItem getSelection();

        default boolean isFixedCategorySize() {
            return false;
        }
    }

    public interface SearchTextFieldPublisher {
        boolean isSearchCancelled();
        void publishSearch(List<PopupCategorized.CategorizedPopupItem> intermediateResult);
    }

    public interface SearchedItemsListener {
        void updateCurrentSearchedItems(List<PopupCategorized.CategorizedPopupItem> items, boolean done);
    }

    public static class SearchTextFieldModelEmpty implements SearchTextFieldModel {
        @Override
        public List<PopupCategorized.CategorizedPopupItem> getCandidates(String text, boolean editable, SearchTextFieldPublisher publisher) {
            return new ArrayList<>();
        }

        @Override
        public void select(PopupCategorized.CategorizedPopupItem item) { }

        @Override
        public PopupCategorized.CategorizedPopupItem getSelection() {
            return null;
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
        icon.setOpaque(false);
        icon.setFocusable(false);
    }

    public Icon getEmptyIcon() {
        if (emptyIcon == null) {
            BufferedImage img = new BufferedImage(16,16, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics g = img.createGraphics();
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            g.dispose();
            emptyIcon = new ImageIcon(img);
        }
        return emptyIcon;
    }

    public void initField() {
        editingRunner = new ScheduledTaskRunner.EditingRunner(500, this::updateField);
        field = new JTextField();
        field.setOpaque(false);
        setOpaque(false);

        field.getDocument().addDocumentListener(editingRunner);
        field.addActionListener(editingRunner);
        field.addFocusListener(editingRunner);
    }


    public void initPopup() {
        PopupCategorized categorized = initPopupCategorized(
                PopupCategorized.getSupplierWithMenuItems(
                        getPopupEditMenuItems(),
                        this::getSearchedItems),
                this::selectSearchedItemFromGui);

        popup = new PopupExtensionText(field, PopupExtensionText.getDefaultKeyMatcher(), categorized);
        popupButton = new JButton(popup.getAction());
        addSearchItemsListener(getPopupUpdateListener(popup, categorized));
    }

    public PopupCategorized initPopupCategorized(Supplier<? extends Collection<PopupCategorized.CategorizedPopupItem>> itemSupplier,
                                                 Consumer<PopupCategorized.CategorizedPopupItem> itemConsumer) {
        return model.isFixedCategorySize() ?
                new PopupCategorized.PopupCategorizedFixed(itemSupplier, itemConsumer) :
                new PopupCategorized(itemSupplier, itemConsumer);

    }

    public List<? extends JComponent> getPopupEditMenuItems() {
        return getPopupEditActions().stream()
                .map(JMenuItem::new)
                .collect(Collectors.toList());
    }

    public List<Action> getPopupEditActions() {
        return PopupExtensionText.getEditActions(field);
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
        super.setBackground(Color.white);
        backgroundPainter = new SearchBackgroundPainter(this);

        setLayout(new BorderLayout());
        add(icon, BorderLayout.WEST);
        add(field, BorderLayout.CENTER);
        add(popupButton, BorderLayout.EAST);
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

    public SearchTextFieldModel getModel() {
        return model;
    }

    public PopupExtensionText getPopup() {
        return popup;
    }

    public JButton getPopupButton() {
        return popupButton;
    }

    public ScheduledTaskRunner.EditingRunner getEditingRunner() {
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

    public boolean isEditable() {
        return isEnabled() && getField().isEnabled() && getField().isEditable();
    }

    ////////////////////

    /** After editing text or action performed, this method will be executed under the scheduler thread */
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
     *  start a new search task in background */
    public void updateFieldInEvent(boolean modified) {
        if (modified || currentSearchedItems == null) {
            String text = field.getText();
            if (currentTask != null && !currentTask.isDone()) {
                currentTask.cancel(true);
            }
            currentTask = createSearchTask(text);
            currentTask.execute();
        }
    }

    public SearchTask createSearchTask(String text) {
        return new SearchTask(this, text);
    }

    /** set the searched items from the background task: it might be an intermediate result */
    public void setCurrentSearchedItems(List<PopupCategorized.CategorizedPopupItem> currentSearchedItems, boolean done) {
        this.currentSearchedItems = currentSearchedItems;
        searchedItemsListeners.forEach(l -> l.updateCurrentSearchedItems(currentSearchedItems, done));
    }

    /** called once when the search is done */
    public void setCurrentSearchedItems(List<PopupCategorized.CategorizedPopupItem> currentSearchedItems,
                                        PopupCategorized.CategorizedPopupItem selection) {
        this.currentSearchedItems = currentSearchedItems;
        selectSearchedItemFromModel(selection);
        searchedItemsListeners.forEach(l -> l.updateCurrentSearchedItems(currentSearchedItems, true));
    }

    public List<PopupCategorized.CategorizedPopupItem> getSearchedItems() {
        List<PopupCategorized.CategorizedPopupItem> items = currentSearchedItems;
        if (items == null) {
            return Collections.emptyList();
        } else {
            return items;
        }
    }

    /** update model selection and also GUI display */
    public void selectSearchedItemFromGui(PopupCategorized.CategorizedPopupItem item) {
        model.select(item);
        setIconFromSearchedItem(item);
        setTextFromSearchedItem(item);
    }

    /** called when the search is done, and update only the icon */
    public void selectSearchedItemFromModel(PopupCategorized.CategorizedPopupItem item) {
        setIconFromSearchedItem(item);
    }

    public void setIconFromSearchedItem(PopupCategorized.CategorizedPopupItem item) {
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
            } else {
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
                icon.paintIcon(this.icon, img.getGraphics(), 0, 0);
                icon = new ImageIcon(img.getScaledInstance(ew, eh, Image.SCALE_SMOOTH));
            }
        }
        return icon;
    }

    public void setTextFromSearchedItem(PopupCategorized.CategorizedPopupItem item) {
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


    public static class SearchTask extends SwingWorker<List<PopupCategorized.CategorizedPopupItem>, List<PopupCategorized.CategorizedPopupItem>>
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

        @Override
        protected List<PopupCategorized.CategorizedPopupItem> doInBackground() throws Exception {
            try{
                return field.getModel().getCandidates(text, field.isEditable(), this);
            } catch (CancellationException ex) {
                return new ArrayList<>();
            }
        }

        @Override
        protected void process(List<List<PopupCategorized.CategorizedPopupItem>> chunks) {
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
        public void publishSearch(List<PopupCategorized.CategorizedPopupItem> intermediateResult) {
            publish(new ArrayList<>(intermediateResult));
        }
    }

    public static class SearchBackgroundPainter {
        protected JComponent component;
        protected Color[] gradientColors;
        protected Color focusColor;
        protected BasicStroke[] strokes;

        public SearchBackgroundPainter(JComponent component) {
            this.component = component;
            component.setLayout(new BorderLayout());
            component.setBorder(BorderFactory.createEmptyBorder(7, 5, 7, 5));
        }

        public void init() {
            initGradientColors();
            initFocusColor();
            initStrokes();
        }

        public void initGradientColors() {
            int gradMax = 4;

            List<Color> cs = new ArrayList<Color>();
            for (int i = 0; i < gradMax; ++i) {
                float p = (((float) i + 1) / ((float) gradMax));
                cs.add(getGradientColor(p));
            }
            gradientColors = cs.toArray(new Color[cs.size()]);
        }

        public void initFocusColor() {
            focusColor = UIManager.getColor("Focus.color");
            if (focusColor == null) {
                focusColor = new Color(100, 100, 100);
            } else {
                float[] hsb = new float[3];
                hsb = Color.RGBtoHSB(focusColor.getRed(), focusColor.getGreen(), focusColor.getBlue(), hsb);
                focusColor = Color.getHSBColor(hsb[0] * 0.97f, hsb[1] * 0.72f, hsb[2]);
            }
        }

        public void initStrokes() {
            strokes = new BasicStroke[2];
            for (int i = 0; i < strokes.length; ++i) {
                strokes[i] = new BasicStroke(strokes.length - i);
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
            int x = 2;
            int y = 2;
            int width = component.getWidth() - 4;
            int height = component.getHeight() - 4;
            int arc = 5;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D rr = new RoundRectangle2D.Float(x, y, width - 1, height - 1, arc, arc);

            paintGradientColors(g2, rr);

            if (hasFocusChild(component)) {
                paintFocusStrokes(g2, rr);
            }
        }

        public void paintGradientColors(Graphics2D g2, RoundRectangle2D rr) {
            int x = (int) rr.getX();
            int y = (int) rr.getY();
            int width = (int) rr.getWidth() + 1;
            int height = (int) rr.getHeight() + 1;
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
            g2.setColor(new Color(150, 150, 150));
            g2.setStroke(new BasicStroke(1));
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
