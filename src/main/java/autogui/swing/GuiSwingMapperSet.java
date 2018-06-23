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
        boolean match(GuiMappingContext context, MapperMatchType matchType);
        GuiSwingElement view(GuiMappingContext context);
    }

    public GuiSwingMapperSet addReprClass(Class<? extends GuiRepresentation> reprClass, GuiSwingElement view) {
        mappers.add(new MapperReprClass(reprClass, view, MapperMatchTypeDefault.View));
        return this;
    }

    public GuiSwingMapperSet addReprClassTableColumn(Class<? extends GuiRepresentation> reprClass, GuiSwingElement view) {
        mappers.add(new MapperReprClass(reprClass, view, MapperMatchTypeDefault.TableColumn));
        return this;
    }

    public GuiSwingMapperSet add(Mapper... mappers) {
        this.mappers.addAll(Arrays.asList(mappers));
        return this;
    }

    public List<Mapper> getMappers() {
        return mappers;
    }

    /**
     * @param context the target context
     * @return view as a regular component factory
     */
    public GuiSwingElement view(GuiMappingContext context) {
        return view(context, MapperMatchTypeDefault.View);
    }

    /**
     * @param context the target context
     * @return a {@link GuiSwingTableColumn}, a {@link GuiSwingTableColumnSet} or a {@link GuiSwingAction}
     */
    public GuiSwingElement viewTableColumn(GuiMappingContext context) {
        return view(context, MapperMatchTypeDefault.TableColumn);
    }

    public GuiSwingElement view(GuiMappingContext context, MapperMatchType matchType) {
        for (Mapper m : mappers) {
            if (m.match(context, matchType)) {
                return m.view(context);
            }
        }
        return null;
    }

    public interface MapperMatchType {
        default boolean match(MapperMatchType type) {
            return type.equals(this);
        }
    }

    public enum MapperMatchTypeDefault implements MapperMatchType {
        View,
        TableColumn
    }

    public static class MapperReprClass implements Mapper {
        protected Class<? extends GuiRepresentation> reprClass;
        protected GuiSwingElement view;
        protected MapperMatchType matchType;

        public MapperReprClass(Class<? extends GuiRepresentation> reprClass, GuiSwingElement view,
                               MapperMatchType matchType) {
            this.reprClass = reprClass;
            this.view = view;
            this.matchType = matchType;
        }

        @Override
        public boolean match(GuiMappingContext context, MapperMatchType matchType) {
            return this.matchType.match(matchType) &&
                    reprClass.isAssignableFrom(context.getRepresentation().getClass());
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
            return getClass().getSimpleName() + "(" + getReprClass().getName() + "," + matchType + "->" + getView()  +")";
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
                .addReprClassTableColumn(GuiReprObjectPane.class, new GuiSwingTableColumnSetDefault(viewSet))
                .addReprClassTableColumn(GuiReprCollectionElement.class, new GuiSwingTableColumnSetDefault(viewSet))
                .addReprClassTableColumn(GuiReprCollectionTable.class, new GuiSwingTableColumnCollection(viewSet))
                .addReprClassTableColumn(GuiReprPropertyPane.class, new GuiSwingTableColumnCollection.GuiSwingTableColumnProperty(viewSet))
                .addReprClassTableColumn(GuiReprValue.class, new GuiSwingTableColumnLabel())
                .addReprClass(GuiReprActionList.class, null) //nothing: handled by a sibling GuiSwingViewCollectionTable
                .addReprClassTableColumn(GuiReprAction.class, new GuiSwingActionDefault());

        viewSet.addReprClass(GuiReprValueBooleanCheckBox.class, new GuiSwingViewBooleanCheckBox())
                .addReprClass(GuiReprEmbeddedComponent.class, new GuiSwingViewEmbeddedComponent())
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
