package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.mapping.GuiReprEmbeddedComponent;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * <h3>representation</h3>
 * {@link autogui.swing.mapping.GuiReprEmbeddedComponent}
 *
 * <h3>{@link PropertyEmbeddedPane#getSwingViewValue()}</h3>
 * {@link JComponent}
 *
 * <h3>history-value</h3>
 * unsupported
 *
 * <h3>string-transfer</h3>
 * unsupported.
 */
public class GuiSwingViewEmbeddedComponent implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        ValuePane<Object> comp = new PropertyEmbeddedPane(context, parentSpecifier);
        if (context.isTypeElementProperty()) {
            return comp.wrapSwingProperty();
        } else {
            return comp.asSwingViewComponent();
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    public static class PropertyEmbeddedPane extends JComponent
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object> {
        protected GuiMappingContext context;
        protected Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier;
        protected GuiReprValue.ObjectSpecifier specifierCache;

        protected PopupExtension popup;
        protected JComponent component;

        public PropertyEmbeddedPane(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
            this.context = context;
            this.parentSpecifier = parentSpecifier;
        }

        public void init() {
            initName();
            initLayout();
            initContextUpdate();
            initValue();
            initSize();
        }

        public void initName() {
            setName(context.getName());
            GuiSwingView.setDescriptionToolTipText(context, this);
        }

        public void initLayout() {
            setLayout(new BorderLayout());
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initValue() {
            update(context, context.getSource());
        }

        public void initSize() {
            setPreferredSize(new Dimension(300, 200));
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            return Collections.emptyList();
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return new PopupExtension.PopupMenuBuilderEmpty();
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            if (getComponentCount() == 0) {
                return null;
            } else {
                return getComponent(0);
            }
        }

        @Override
        public void setSwingViewValue(Object value) {
            GuiReprEmbeddedComponent embeddedComponent = (GuiReprEmbeddedComponent) getSwingViewContext().getRepresentation();

            JComponent comp = embeddedComponent.toUpdateValue(getSwingViewContext(), value, this::setSwingViewValueComponent);
            setSwingViewValueComponent(comp);
        }

        public void setSwingViewValueComponent(JComponent comp) {
            if (comp != null && comp != component) {
//                if (getComponentCount() > 0) {
//                    remove(0);
//                }
//            } else {
                if (component != null && getComponentCount() > 0) {
                    remove(component);
                }
                add(comp, 0);
                setPreferredSize(comp.getPreferredSize());
                component = comp;
                revalidate();
            }
            repaint();
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            setSwingViewValue(value);
            GuiReprEmbeddedComponent repr = (GuiReprEmbeddedComponent) getSwingViewContext().getRepresentation();
            if (repr.isEditable(getSwingViewContext())) {
                repr.updateFromGui(getSwingViewContext(), value);
            }
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return GuiSwingView.getSpecifierDefault(parentSpecifier, this.specifierCache, this::setSpecifierCache);
        }

        public void setSpecifierCache(GuiReprValue.ObjectSpecifier specifierCache) {
            this.specifierCache = specifierCache;
        }
    }
}
