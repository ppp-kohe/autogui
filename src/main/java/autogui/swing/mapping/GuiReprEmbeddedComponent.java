package autogui.swing.mapping;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.type.GuiUpdatedValue;
import autogui.swing.util.SwingDeferredRunner;

import javax.swing.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * a GUI component for a property holding a {@link JComponent}
 */
public class GuiReprEmbeddedComponent extends GuiReprValue {
    @Override
    public boolean matchValueType(Class<?> cls) {
        return JComponent.class.isAssignableFrom(cls);
    }

    @Override
    public boolean isTaskRunnerUsedFor(Callable<?> task) {
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

    @Override
    public JComponent toUpdateValue(GuiMappingContext context, Object value) {
        return toUpdateValue(context, value, null);
    }

    public JComponent toUpdateValue(GuiMappingContext context, Object value, Consumer<JComponent> delayed) {
        if (value instanceof SwingDeferredRunner.TaskResultFuture) {
            Future<Object> f = ((SwingDeferredRunner.TaskResultFuture) value).getFuture();
            if (f.isDone()) {
                try {
                    value = f.get();
                } catch (Exception ex) {
                    return null;
                }
            } else {
                if (delayed != null) {
                    SwingDeferredRunner.defaultService.execute(() -> {
                        try {
                            delayed.accept(toUpdateValue(context, f.get()));
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                }
                return null;
            }
        }
        if (value instanceof GuiUpdatedValue) {
            return toUpdateValue(context, ((GuiUpdatedValue) value).getValue(), delayed);
        } else if (value instanceof JComponent) {
            return (JComponent) value;
        } else {
            return null;
        }
    }

    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        return null;
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        return target;
    }

    @Override
    public boolean isHistoryValueSupported() {
        return false;
    }
}
