package org.autogui.swing;

import org.autogui.swing.mapping.GuiReprEmbeddedComponent;
import org.autogui.swing.mapping.GuiReprValueDocumentEditor;
import org.autogui.swing.mapping.GuiReprValueImagePane;
import org.autogui.swing.table.*;
import org.autogui.swing.table.GuiSwingTableColumnCollection.GuiSwingTableColumnProperty;
import org.autogui.base.mapping.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * a set of mapping between {@link GuiRepresentation} and {@link GuiSwingElement}.
 * The default set is provided by {@link #getDefaultMapperSet()}.
 * <p>
 * The class also provides default set of {@link GuiRepresentation}s as {@link #getReprDefaultSet()},
 *     which is an extended version of {@link GuiRepresentation#getDefaultSet()}.
 */
public class GuiSwingMapperSet {
    protected List<Mapper> mappers = new ArrayList<>();

    /** a mapping between {@link GuiRepresentation} bound to a {@link GuiMappingContext} and {@link GuiSwingElement}.
     *    the mapping can be varied by a mapping type {@link MapperMatchType}. */
    public interface Mapper {
        boolean match(GuiMappingContext context, MapperMatchType matchType);
        GuiSwingElement view(GuiMappingContext context);
    }

    /**
     * register a mapping of reprClass and a regular view with the type {@link MapperMatchTypeDefault#View}
     * @param reprClass a representation class
     * @param view a view factory
     * @return this
     */
    public GuiSwingMapperSet addReprClass(Class<? extends GuiRepresentation> reprClass, GuiSwingElement view) {
        mappers.add(new MapperReprClass(reprClass, view, MapperMatchTypeDefault.View));
        return this;
    }

    /**
     * register a mapping of reprClass and a column view with the type {@link MapperMatchTypeDefault#TableColumn}
     * @param reprClass a representation class
     * @param view a column view factory
     * @return this
     */
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

    /** the interface definition of mapping type */
    public interface MapperMatchType {
        default boolean match(MapperMatchType type) {
            return type.equals(this);
        }
    }

    /** a concrete definition of mapper type */
    public enum MapperMatchTypeDefault implements MapperMatchType {
        View,
        TableColumn
    }

    /**
     * a mapping between a representation of a class and a view with a mapping type
     */
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
     *    setup by {@link #addDefaultReprSetTo(GuiReprSet)}
     *
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

    /**
     * provide a default set of representations.
     * {@link GuiRepresentation#getDefaultSet()} + reprs in autogui.swing.mapping.*
     * <ol>
     *     <li> {@link GuiReprCollectionElement} : precedes {@link GuiReprCollectionTable}</li>
     *     <li> {@link GuiReprValueBooleanCheckBox}</li>
     *     <li> {@link GuiReprValueEnumComboBox}</li>
     *     <li> {@link GuiReprValueFilePathField}</li>
     *     <li> {@link GuiReprValueNumberSpinner}</li>
     *     <li> {@link GuiReprValueStringField}</li>
     *     <li> {@link GuiReprEmbeddedComponent}</li>
     *     <li> {@link GuiReprValueDocumentEditor}</li>
     *     <li> {@link GuiReprValueImagePane}</li>
     *     <li> {@link GuiReprCollectionTable}</li>
     *     <li> {@link GuiReprObjectTabbedPane} : precedes {@link GuiReprObjectPane}</li>
     *     <li> {@link GuiReprObjectPane}</li>
     *     <li> {@link GuiReprPropertyPane} : follows other property supported panes like {@link GuiReprValueBooleanCheckBox}</li>
     *     <li> {@link GuiReprAction}</li>
     *     <li> {@link GuiReprActionList}</li>
     *     <li> {@link GuiReprValueLabel} : matches any type of representations</li>
     * </ol>
     * @param reprSet a target set for adding
     */
    public static void addDefaultReprSetTo(GuiReprSet reprSet) {
        reprSet.add(new GuiReprCollectionElement(reprSet));

        reprSet.add(new GuiReprValueBooleanCheckBox(),
                new GuiReprValueEnumComboBox(),
                new GuiReprValueFilePathField(),
                new GuiReprValueNumberSpinner(),
                new GuiReprValueStringField());

        reprSet.add(new GuiReprEmbeddedComponent(),
                new GuiReprValueDocumentEditor(),
                new GuiReprValueImagePane(true));

        reprSet.add(new GuiReprCollectionTable(reprSet),
                new GuiReprObjectTabbedPane(reprSet),
                new GuiReprObjectPane(reprSet),
                new GuiReprPropertyPane(reprSet),
                new GuiReprAction(),
                new GuiReprActionList());

        reprSet.add(new GuiReprValueLabel());
    }

    /**
     * provide a default set of mappings between a representation and a view.
     * <table>
     *     <caption>mappings for table columns </caption>
     *     <tr><th>representation</th> <th>column view</th></tr>
     *     <tr><td>{@link GuiReprValueBooleanCheckBox}</td> <td>{@link GuiSwingTableColumnBoolean}</td> </tr>
     *     <tr><td>{@link GuiReprValueStringField}</td>     <td>{@link GuiSwingTableColumnString}</td> </tr>
     *     <tr><td>{@link GuiReprValueEnumComboBox}</td>    <td>{@link GuiSwingTableColumnEnum}</td> </tr>
     *     <tr><td>{@link GuiReprValueFilePathField}</td>   <td>{@link GuiSwingTableColumnFilePath}</td> </tr>
     *     <tr><td>{@link GuiReprValueNumberSpinner}</td>   <td>{@link GuiSwingTableColumnNumber}</td> </tr>
     *     <tr><td>{@link GuiReprValueImagePane}</td>       <td>{@link GuiSwingTableColumnImage}</td> </tr>
     *     <tr><td>{@link GuiReprObjectPane}</td>           <td>{@link GuiSwingTableColumnSetDefault}</td> </tr>
     *     <tr><td>{@link GuiReprCollectionElement}</td>    <td>{@link GuiSwingTableColumnSetDefault}</td> </tr>
     *     <tr><td>{@link GuiReprCollectionTable} </td>     <td>{@link GuiSwingTableColumnCollection} </td> </tr>
     *     <tr><td>{@link GuiReprPropertyPane} </td>        <td>{@link GuiSwingTableColumnProperty} </td> </tr>
     *     <tr><td>{@link GuiReprValue} </td>               <td>{@link GuiSwingTableColumnLabel} </td> </tr>
     *     <tr><td>{@link GuiReprAction} </td>              <td>{@link GuiSwingActionDefault} </td> </tr>
     * </table>
     * <table>
     *     <caption>mappings for values </caption>
     *     <tr><th>representation</th>                      <th>view</th></tr>
     *     <tr><td>{@link GuiReprActionList} </td>          <td>null (handled by a sibling {@link GuiSwingViewCollectionTable})</td> </tr>
     *     <tr><td>{@link GuiReprValueBooleanCheckBox}</td> <td>{@link GuiSwingViewBooleanCheckBox}</td> </tr>
     *     <tr><td>{@link GuiReprEmbeddedComponent} </td>   <td>{@link GuiSwingViewEmbeddedComponent} </td> </tr>
     *     <tr><td>{@link GuiReprValueDocumentEditor} </td> <td>{@link GuiSwingViewDocumentEditor} </td> </tr>
     *     <tr><td>{@link GuiReprValueEnumComboBox} </td>   <td>{@link GuiSwingViewEnumComboBox} </td> </tr>
     *     <tr><td>{@link GuiReprValueFilePathField} </td>  <td>{@link GuiSwingViewFilePathField} </td> </tr>
     *     <tr><td>{@link GuiReprValueImagePane} </td>      <td>{@link GuiSwingViewImagePane} </td> </tr>
     *     <tr><td>{@link GuiReprValueNumberSpinner} </td>  <td>{@link GuiSwingViewNumberSpinner} </td> </tr>
     *     <tr><td>{@link GuiReprValueStringField} </td>    <td>{@link GuiSwingViewStringField} </td> </tr>
     *     <tr><td>{@link GuiReprCollectionTable} </td>     <td>{@link GuiSwingViewCollectionTable} </td> </tr>
     *     <tr><td>{@link GuiReprPropertyPane} </td>        <td>{@link GuiSwingViewPropertyPane} </td> </tr>
     *     <tr><td>{@link GuiReprObjectTabbedPane} </td>    <td>{@link GuiSwingViewTabbedPane} </td> </tr>
     *     <tr><td>{@link GuiReprObjectPane} </td>          <td>{@link GuiSwingViewObjectPane} </td> </tr>
     *     <tr><td>{@link GuiReprAction} </td>              <td>{@link GuiSwingActionDefault} </td> </tr>
     *     <tr><td>{@link GuiReprValue} </td>               <td>{@link GuiSwingViewLabel} </td> </tr>
     * </table>
      * @param viewSet a target set for adding
     */
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
                .addReprClassTableColumn(GuiReprPropertyPane.class, new GuiSwingTableColumnProperty(viewSet))
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
