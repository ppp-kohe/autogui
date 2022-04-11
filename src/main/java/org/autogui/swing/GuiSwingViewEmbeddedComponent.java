package org.autogui.swing;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.mapping.GuiReprEmbeddedComponent;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.util.UIManagerUtil;
import org.autogui.swing.util.SwingDeferredRunner;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * a swing view for {@link GuiReprEmbeddedComponent}
 *
 * <h2>swing-value</h2>
 * {@link PropertyEmbeddedPane#getSwingViewValue()}:
 * {@link JComponent} or
 *   {@link SwingDeferredRunner.TaskResultFuture} unwrapped by
 *      {@link GuiReprEmbeddedComponent#toUpdateValue(GuiMappingContext, Object, Consumer)}
 *
 * <h2>history-value</h2>
 * unsupported
 *
 * <h2>string-transfer</h2>
 * unsupported.
 */
public class GuiSwingViewEmbeddedComponent implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        ValuePane<Object> comp = new PropertyEmbeddedPane(context, new SpecifierManagerDefault(parentSpecifier));
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
        private static final long serialVersionUID = 1L;

        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;

        protected PopupExtension popup;
        protected JComponent component;

        protected GuiTaskClock viewClock = new GuiTaskClock(true);
        /** @since 1.5 */
        protected Object initPrefsJson;

        public PropertyEmbeddedPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            init();
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
            update(context, context.getSource().getValue(), context.getContextClock().copy());
        }

        public void initSize() {
            UIManagerUtil ui = UIManagerUtil.getInstance();
            setPreferredSize(new Dimension(ui.getScaledSizeInt(300), ui.getScaledSizeInt(200)));
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
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            if (viewClock.isOlderWithSet(contextClock)) { //the source from target precedes other GUI generated values
                SwingUtilities.invokeLater(() ->
                        setSwingViewValueWithoutClockIncrement(newValue));
            }
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
            viewClock.increment();
            setSwingViewValueWithoutClockIncrement(value);
        }

        private void setSwingViewValueWithoutClockIncrement(Object value) {
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
                consumeInitPrefsJson(comp);
                add(comp, 0);
                setPreferredSize(comp.getPreferredSize());
                component = comp;
                revalidate();
            }
            repaint();
        }

        /**
         * set and clear if it has {@link #initPrefsJson} for
         *    the given component as {@link GuiPreferences.PreferencesJsonSupport}
         * @param comp the new component value
         * @since 1.5
         */
        protected void consumeInitPrefsJson(JComponent comp) {
            Object initPrefsJson = this.initPrefsJson;
            if (comp instanceof GuiPreferences.PreferencesJsonSupport &&
                initPrefsJson != null) {
                ((GuiPreferences.PreferencesJsonSupport) comp).setPrefsJson(initPrefsJson);
                this.initPrefsJson = null;
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            GuiSwingView.updateViewClockSync(viewClock, context);
            setSwingViewValue(value);
            updateFromGui(value, viewClock);
        }

        @Override
        public void setSwingViewValue(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                setSwingViewValueWithoutClockIncrement(value);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                setSwingViewValueWithoutClockIncrement(value);
                updateFromGui(value, viewClock);
            }
        }

        public void updateFromGui(Object v, GuiTaskClock viewClock) {
            GuiSwingView.updateFromGui(this, v, viewClock);
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
        }

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
        }

        @Override
        public Object getPrefsJsonSupported() {
            if (initPrefsJson != null) {
                //the prefs json is not yet consumed.
                return initPrefsJson;
            } else {
                return ValuePane.super.getPrefsJsonSupported();
            }
        }

        @Override
        public void setPrefsJsonSupported(Object json) {
            if (getSwingViewValue() != null) {
                ValuePane.super.setPrefsJsonSupported(json);
            } else {
                this.initPrefsJson = json;
            }
        }
    }
}
