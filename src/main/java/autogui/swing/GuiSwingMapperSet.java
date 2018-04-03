package autogui.swing;

import autogui.base.mapping.*;
import autogui.swing.mapping.GuiReprEmbeddedComponent;
import autogui.swing.mapping.GuiReprValueDocumentEditor;
import autogui.swing.mapping.GuiReprValueImagePane;
import autogui.swing.table.*;

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

    public GuiSwingMapperSet addReprClassTableColumn(Class<? extends GuiRepresentation> reprClass, GuiSwingElement view) {
        mappers.add(new MapperReprClassTableColumn(reprClass, view));
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

        public Class<? extends GuiRepresentation> getReprClass() {
            return reprClass;
        }

        public GuiSwingElement getView() {
            return view;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(" + getReprClass().getName() + "->" + getView()  +")";
        }
    }

    public static class MapperReprClassTableColumn extends MapperReprClass {
        public MapperReprClassTableColumn(Class<? extends GuiRepresentation> reprClass, GuiSwingElement view) {
            super(reprClass, view);
        }

        @Override
        public boolean match(GuiMappingContext context) {
            return context.isParentCollectionElement() && super.match(context);
        }
    }

    /**
     * @return the default set of representation including swing-based items.
     *    {@link GuiRepresentation#getDefaultSet()} + reprs in autogui.swing.mapping.*
     */
    public static GuiReprSet getReprDefaultSet() {
        return reprDefaultSet;
    }

    public static GuiSwingMapperSet getDefaultMapperSet() {
        return defaultMapperSet;
    }

    protected static GuiReprSet reprDefaultSet;

    protected static GuiSwingMapperSet defaultMapperSet;

    static {
        GuiReprSet reprSet = new GuiReprSet();
        addDefaultReprSetTo(reprSet);
        reprDefaultSet = reprSet;


        GuiSwingMapperSet viewSet = new GuiSwingMapperSet();
        addDefaultReprClassesTo(viewSet);
        defaultMapperSet = viewSet;
    }

    public static void addDefaultReprSetTo(GuiReprSet reprSet) {
        reprSet.add(new GuiReprCollectionElement(reprSet));

        reprSet.add(new GuiReprValueBooleanCheckBox(),
                new GuiReprValueEnumComboBox(),
                new GuiReprValueFilePathField(),
                new GuiReprValueNumberSpinner(),
                new GuiReprValueStringField());

        reprSet.add(new GuiReprEmbeddedComponent(),
                new GuiReprValueDocumentEditor(),
                new GuiReprValueImagePane());

        reprSet.add(new GuiReprCollectionTable(reprSet),
                new GuiReprObjectTabbedPane(reprSet),
                new GuiReprObjectPane(reprSet),
                new GuiReprPropertyPane(reprSet),
                new GuiReprAction(),
                new GuiReprActionList());

        reprSet.add(new GuiReprValueLabel());
    }

    public static void addDefaultReprClassesTo(GuiSwingMapperSet viewSet) {
        viewSet.addReprClassTableColumn(GuiReprValueBooleanCheckBox.class, new GuiSwingTableColumnBoolean())
                .addReprClassTableColumn(GuiReprValueStringField.class, new GuiSwingTableColumnString())
                .addReprClassTableColumn(GuiReprValueEnumComboBox.class, new GuiSwingTableColumnEnum())
                .addReprClassTableColumn(GuiReprValueFilePathField.class, new GuiSwingTableColumnFilePath())
                .addReprClassTableColumn(GuiReprValueNumberSpinner.class, new GuiSwingTableColumnNumber())
                .addReprClassTableColumn(GuiReprValueImagePane.class, new GuiSwingTableColumnImage())
                .addReprClassTableColumn(GuiReprValue.class, new GuiSwingTableColumnLabel())
                .addReprClass(GuiReprCollectionElement.class, new GuiSwingTableColumnSetDefault(viewSet))
                .addReprClass(GuiReprActionList.class, null); //nothing: handled by a sibling GuiSwingViewCollectionTable

        viewSet.addReprClass(GuiReprValueBooleanCheckBox.class, new GuiSwingViewBooleanCheckBox())
                .addReprClass(GuiReprValueDocumentEditor.class, new GuiSwingViewDocumentEditor())
                .addReprClass(GuiReprValueEnumComboBox.class, new GuiSwingViewEnumComboBox())
                .addReprClass(GuiReprValueFilePathField.class, new GuiSwingViewFilePathField())
                .addReprClass(GuiReprValueImagePane.class, new GuiSwingViewImagePane())
                .addReprClass(GuiReprValueNumberSpinner.class, new GuiSwingViewNumberSpinner())
                .addReprClass(GuiReprValueStringField.class, new GuiSwingViewStringField())
                .addReprClass(GuiReprCollectionTable.class, new GuiSwingViewCollectionTable(viewSet))
                .addReprClass(GuiReprPropertyPane.class, new GuiSwingViewPropertyPane(viewSet))
                .addReprClass(GuiReprObjectTabbedPane.class, new GuiSwingViewTabbedPane(viewSet))
                .addReprClass(GuiReprObjectPane.class, new GuiSwingViewObjectPane(viewSet))
                .addReprClass(GuiReprAction.class, new GuiSwingActionDefault())
                .addReprClass(GuiReprValue.class, new GuiSwingViewLabel());
    }
}
