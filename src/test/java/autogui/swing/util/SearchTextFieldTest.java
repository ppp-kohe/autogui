package autogui.swing.util;

import autogui.swing.GuiSwingTestCase;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SearchTextFieldTest extends GuiSwingTestCase {

    public static void main(String[] args) throws Exception {
        new SearchTextFieldTest().test();
    }

    AtomicInteger cancelCount = new AtomicInteger();
    AtomicInteger startCount = new AtomicInteger();
    AtomicInteger finishCount = new AtomicInteger();
    PopupCategorized.CategorizedMenuItem selectedItem;

    String selectedName;

    @Test
    public void test() {
        SearchTextField fld = runGet(() -> {
            JPanel pane = new JPanel();

            SearchTextField f = new SearchTextField(new TestModel()) {
                @Override
                public void updateField(List<Object> events) {
                    System.err.println("update : " + events.size() + " : " +
                        events.stream().map(e -> e.getClass().getSimpleName())
                                .collect(Collectors.joining(", ")));
                    super.updateField(events);
                }
            };
            //pane.setLayout(new BorderLayout());
            f.getField().setText("hello, world");
            pane.add(f);

            testFrame(pane);
            return f;
        });

        fld.getPopup().show(fld.getField(), 0, 0);

        int i = 0;
        while (finishCount.get() == 0 && i < 100) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            ++i;
        }

        JMenuItem item = PopupExtensionTextTest.getMenu(fld.getPopup().getMenu(), "item-10");
        run(item::doClick);

        fld.getPopup().getMenu().setVisible(false);

        Assert.assertEquals("item-10", selectedName);
    }

    public class TestModel implements SearchTextField.SearchTextFieldModel {
        @Override
        public List<PopupCategorized.CategorizedMenuItem> getCandidates(String text, boolean editable, SearchTextField.SearchTextFieldPublisher publisher) {
            startCount.incrementAndGet();
            finishCount.set(0);
            List<PopupCategorized.CategorizedMenuItem> items = new ArrayList<>();
            for (int i = 0; i < 100; ++i) {
                if (publisher.isSearchCancelled()) {
                    cancelCount.incrementAndGet();
                    break;
                }
                items.add(new TestItem("item-" + i, "category-" + (i % 10)));

                if (i % 5 == 0) {
                    System.err.println("publish " + i);
                    publisher.publishSearch(items);
                }

                try {
                    Thread.sleep(30);
                } catch (Exception ex) {
                    System.err.println("interrupt " + i);
                }
            }
            finishCount.incrementAndGet();
            return items;
        }

        @Override
        public void select(PopupCategorized.CategorizedMenuItem item) {
            selectedItem = item;
        }

        @Override
        public PopupCategorized.CategorizedMenuItem getSelection() {
            return selectedItem;
        }
    }

    public class TestItem implements PopupCategorized.CategorizedMenuItemComponent {
        protected String name;
        protected String category;

        public TestItem(String name, String category) {
            this.name = name;
            this.category = category;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public JComponent getMenuItem(PopupCategorized sender) {
            return new JMenuItem(new TestAction(name));
        }
    }

    public class TestAction extends AbstractAction {
        protected String name;

        public TestAction(String name) {
            putValue(NAME, name);
            this.name = name;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            selectedName = name;
        }
    }
}
