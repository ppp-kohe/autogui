package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiRepresentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiSwingMapperSet {
    protected List<Mapper> mappers = new ArrayList<>();

    public interface Mapper {
        boolean match(GuiMappingContext context);
        GuiSwingElement view(GuiMappingContext context);
    }

    public GuiSwingMapperSet addReprClass(Class<? extends GuiRepresentation> reprClass, GuiSwingElement view) {
        mappers.add(new MapperReprClass(reprClass, view));
        return this;
    }

    public GuiSwingMapperSet add(Mapper... mappers) {
        this.mappers.addAll(Arrays.asList(mappers));
        return this;
    }

    public List<Mapper> getMappers() {
        return mappers;
    }

    public GuiSwingElement view(GuiMappingContext context) {
        for (Mapper m : mappers) {
            if (m.match(context)) {
                return m.view(context);
            }
        }
        return null;
    }

    public static class MapperReprClass implements Mapper {
        protected Class<? extends GuiRepresentation> reprClass;
        protected GuiSwingElement view;

        public MapperReprClass(Class<? extends GuiRepresentation> reprClass, GuiSwingElement view) {
            this.reprClass = reprClass;
            this.view = view;
        }

        @Override
        public boolean match(GuiMappingContext context) {
            return reprClass.isAssignableFrom(context.getRepresentation().getClass());
        }

        @Override
        public GuiSwingElement view(GuiMappingContext context) {
            return view;
        }
    }
}
