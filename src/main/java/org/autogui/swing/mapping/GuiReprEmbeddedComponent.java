package org.autogui.swing.mapping;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiUpdatedValue;
import org.autogui.swing.util.SwingDeferredRunner;

import javax.swing.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * a GUI component for a property holding a {@link JComponent}
 * <pre>
 *     private JComponent comp;
 *     &#64;GuiIncluded public JComponent getComp() {
 *         if (comp == null) {
 *             comp = new JLabel("hello");
 *         }
 *         return comp;
 *     }
 * </pre>
 * <p>
 *     the class executes obtaining and updating a target value under the event dispatching thread
 *      via {@link SwingDeferredRunner}.
 * <p>
 *      For the case of updating from GUI,
 *      a UI event (in the event thread) -&gt;
 *       executeContextTask
 *         and it observes that {@link #isTaskRunnerUsedFor(Supplier)} returns false and directly invoke the given task
 *       -&gt;
 *        updateFromGui -&gt; update -&gt;
 *         {@link SwingDeferredRunner#run(SwingDeferredRunner.Task)} with {@code super.update} (it directly invoke the task as in the event thread)
 *          -&gt; the method of the target object is invoked under the event thread.
 */
public class GuiReprEmbeddedComponent extends GuiReprValue {
    public GuiReprEmbeddedComponent() {}
    @Override
    public boolean matchValueType(Class<?> cls) {
        return JComponent.class.isAssignableFrom(cls);
    }

    @Override
    public boolean isTaskRunnerUsedFor(Supplier<?> task) {
        return false;
    }

    @Override
    public GuiUpdatedValue getValue(GuiMappingContext context, GuiMappingContext.GuiSourceValue parentSource,
                                    ObjectSpecifier specifier, GuiMappingContext.GuiSourceValue prev) throws Throwable {
        Object v = SwingDeferredRunner.run(() -> super.getValue(context, parentSource, specifier, prev));
        if (v instanceof GuiUpdatedValue) {
            return (GuiUpdatedValue) v;
        } else {
            return GuiUpdatedValue.of(v);
        }
    }

    @Override
    public Object update(GuiMappingContext context, GuiMappingContext.GuiSourceValue parentSource,
                                  Object newValue, ObjectSpecifier specifier) {
        try {
            return SwingDeferredRunner.run(() -> super.update(context, parentSource, newValue, specifier));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @param context the context of the repr.
     * @param value the current value
     * @return obtains a {@link JComponent} value, or if the value is a deferred future,
     *    then get the done value or null if it is not completed.
     *    To support the delayed completion, use {@link #toUpdateValue(GuiMappingContext, Object, Consumer)} instead.
     */
    @Override
    public JComponent toUpdateValue(GuiMappingContext context, Object value) {
        return toUpdateValue(context, value, null);
    }

    public JComponent toUpdateValue(GuiMappingContext context, Object value, Consumer<JComponent> delayed) {
        if (value instanceof SwingDeferredRunner.TaskResultFuture taskResult) {
            Future<Object> f = taskResult.getFuture();
            if (f.isDone()) {
                try {
                    value = f.get();
                } catch (Exception ex) {
                    System.err.println(this + " : " + ex);
                    return COMPONENT_NONE;
                }
            } else {
                if (delayed != null) {
                    SwingDeferredRunner.getDefaultService().execute(() -> {
                        try {
                            var newValue = toUpdateValue(context, f.get());
                            if (!newValue.equals(COMPONENT_NONE)) {
                                delayed.accept(newValue);
                            }
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                }
                return COMPONENT_NONE;
            }
        }
        switch (value) {
            case null -> {
                return null;
            }
            case GuiUpdatedValue updatedValue -> {
                if (updatedValue.isNone()) {
                    return COMPONENT_NONE;
                } else {
                    return toUpdateValue(context, updatedValue.getValue(), delayed);
                }
            }
            case JComponent jComponent -> {
                return jComponent;
            }
            default -> {
                return COMPONENT_NONE;
            }
        }
    }

    public static ComponentNone COMPONENT_NONE = new ComponentNone();

    /**
     * a dummy component class for representing no updating.
     */
    public static class ComponentNone extends JComponent {
        public ComponentNone() {}
    }

    @Override
    public boolean isHistoryValueSupported() {
        return false;
    }
}
