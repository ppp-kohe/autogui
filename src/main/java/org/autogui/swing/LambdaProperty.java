package org.autogui.swing;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprSet;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiRepresentation;
import org.autogui.base.type.*;
import org.autogui.swing.mapping.GuiReprValueImagePane;
import org.autogui.swing.table.*;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Lambda-based property panes.
 * The LambdaProperty class itself is a property for a pair of {@link Supplier} and {@link Consumer}, and
 * member types of the class are concrete property panes.
 * Example:
 *  <pre>
 *      public class StrObj {
 *          private String str;
 *          public void setStr(String s) { str = s; }
 *          public String getStr() { return str; }
 *      }
 *
 *      StrObj s = new StrObj();
 *      LambdaStringPane strPane = new LambdaStringPane(s::getStr, s::setStr);
 *                     //a pane creating holding a LambdaProperty with Supplier and Consumer
 *
 *      strPane.setText("update1"); //this will cause s.setStr("update")
 *
 *      s.setStr("update2");
 *      strPane.updateSwingViewSource(); //update the pane with s.getStr()
 *  </pre>
 *
 * @param <T> the value type
 */
@SuppressWarnings("this-escape")
public class LambdaProperty<T> extends GuiTypeMemberProperty {
    protected Supplier<T> getter;
    protected Consumer<T> setter;

    private static final String UNNAMED = "_";

    public LambdaProperty(Class<?> type, Supplier<T> getter, Consumer<T> setter) {
        this(UNNAMED, type, getter, setter);
    }
    @SuppressWarnings("rawtypes")
    public LambdaProperty(Class<? extends java.util.List> listType, Class<?> elementType, Supplier<T> getter, Consumer<T> setter) {
        this(UNNAMED, listType, elementType, getter, setter);
    }

    /**
     * @param name the property name
     * @param type the property type
     * @param getter the supplier for the property
     * @param setter the consumer for the property
     * @since 1.6.3
     */
    public LambdaProperty(String name, Class<?> type, Supplier<T> getter, Consumer<T> setter) {
        super(name);
        setType(new GuiTypeValue(type));
        this.getter = getter;
        this.setter = setter;
    }

    /**
     * @param name the property name
     * @param listType the property list type
     * @param elementType the explicit element type of the list, will be constructed as {@link GuiTypeValue}
     * @param getter the supplier for the entire list property
     * @param setter the consumer for the entire list property
     * @since 1.6.3
     */
    @SuppressWarnings("rawtypes")
    public LambdaProperty(String name, Class<? extends java.util.List> listType, Class<?> elementType, Supplier<T> getter, Consumer<T> setter) {
        super(name);
        setType(new GuiTypeCollection(listType, new GuiTypeValue(elementType)));
        this.getter = getter;
        this.setter = setter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object executeSet(Object target, Object value) {
        setter.accept((T) value);
        return null;
    }

    @Override
    public GuiUpdatedValue executeGet(Object target) {
        return GuiUpdatedValue.of(getter.get());
    }

    @Override
    public boolean isWritable() {
        return setter != null;
    }

    /**
     *
     * @param type the value type
     * @param getter supplier for a value of the type
     * @param setter consumer for the type and update the value returned by the getter
     * @return the context recursively constructed by {@link GuiSwingMapperSet#getReprDefaultSet()}. {@link GuiRepresentation#matchAndSetNotifiersAsInit(GuiMappingContext)}
     * @param <T> the value type; T.class can be {@code Class<T>}
     * @since 1.6.3
     */
    public static <T> GuiMappingContext createAndBuild(String name, Class<?> type, Supplier<T> getter, Consumer<T> setter) {
        GuiReprSet reprSet = GuiSwingMapperSet.getReprDefaultSet();
        GuiMappingContext context = create(name, type, getter, setter, reprSet);
        reprSet.matchAndSetNotifiersAsInit(context);
        context.updateSourceSubTree();
        return context;
    }

    public static <T> GuiMappingContext create(Class<?> type, Supplier<T> getter, Consumer<T> setter, GuiRepresentation repr) {
        return create(UNNAMED, type, getter, setter, repr);
    }

    /**
     * @param name the proeprty name
     * @param type the value-type of the property
     * @param getter the getter for the property value
     * @param setter the setter for the property value
     * @param repr the representation for the property
     * @return a new context for the property
     * @param <T> the value-type of the property
     * @since 1.6.3
     */
    public static <T> GuiMappingContext create(String name, Class<?> type, Supplier<T> getter, Consumer<T> setter, GuiRepresentation repr) {
        GuiMappingContext context = new GuiMappingContext(new LambdaProperty<>(name, type, getter, setter), repr);
        context.updateSourceSubTree();
        return context;
    }

    @SuppressWarnings("rawtypes")
    public static <T> GuiMappingContext createList(Class<? extends java.util.List> listType, Class<?> elementType, Supplier<T> getter, Consumer<T> setter, GuiRepresentation repr) {
        return createList(UNNAMED, listType, elementType, getter, setter, repr);
    }

    /**
     *
     * @param name the property name
     * @param listType the list type of the property
     * @param elementType the element type of the property
     * @param getter the getter for the entire list
     * @param setter the setter for the entire list
     * @param repr the representation of the element of the list
     * @return a context for the list property
     * @param <T> the list type
     * @since 1.6.3
     */
    @SuppressWarnings("rawtypes")
    public static <T> GuiMappingContext createList(String name, Class<? extends java.util.List> listType, Class<?> elementType, Supplier<T> getter, Consumer<T> setter, GuiRepresentation repr) {
        GuiMappingContext context = new GuiMappingContext(new LambdaProperty<>(name, listType, elementType, getter, setter), GuiRepresentation.createPropertyPane(repr));

        GuiMappingContext listContext = context.createChildCandidate(context.getTypeElementAsProperty().getType());
        listContext.setRepresentation(repr);
        listContext.addToParent();

        GuiMappingContext elementContext = listContext.createChildCandidate(listContext.getTypeElement().getChildren().getFirst());
        elementContext.setRepresentation(GuiRepresentation.createCollectionElement(null));
        elementContext.addToParent();

        GuiMappingContext elemObjContext = elementContext.createChildCandidate(elementContext.getTypeElement()); //same type as parent
        elemObjContext.setRepresentation(GuiRepresentation.createValue());
        elemObjContext.addToParent();

        context.updateSourceSubTree();
        return listContext;
    }

    public static SpecifierManagerNone NONE = new SpecifierManagerNone(); //deprecated: use GuiSwingView.specifierManagerRoot()

    public static class SpecifierManagerNone implements GuiSwingView.SpecifierManager {
        public SpecifierManagerNone() {}
        @Override
        public GuiReprValue.ObjectSpecifier get() {
            return GuiReprValue.NONE;
        }

        @Override
        public String toString() {
            return UNNAMED;
        }
    }

    ////////////

    /**
     * the lambda-based version of string-pane
     * @since 1.1
     */
    public static class LambdaStringPane extends GuiSwingViewStringField.PropertyStringPane {
        @Serial private static final long serialVersionUID = 1L;

        public LambdaStringPane(Supplier<String> getter, Consumer<String> setter) {
            this(UNNAMED, getter, setter);
        }

        /**
         * @param name the property name
         * @param getter the property getter
         * @param setter the property setter
         * @since 1.6.3
         */
        public LambdaStringPane(String name, Supplier<String> getter, Consumer<String> setter) {
            super(create(name, String.class, getter, setter, GuiRepresentation.createValueStringField()), GuiSwingView.specifierManagerRoot());
            SwingUtilities.invokeLater(() ->
                setPreferredSize(new Dimension(UIManagerUtil.getInstance().getScaledSizeInt(100), getPreferredSize().height )));
             //try to prevent dead lockking
        }
    }

    public static class LambdaNumberSpinner extends GuiSwingViewNumberSpinner.PropertyNumberSpinner {
        @Serial private static final long serialVersionUID = 1L;

        public <T extends Number> LambdaNumberSpinner(Class<T> type, Supplier<T> getter, Consumer<T> setter) {
            this(UNNAMED, type, getter, setter);
        }

        /**
         * @param name the property name
         * @param type the number type of the property
         * @param getter the property getter
         * @param setter the property setter
         * @param <T>  the number type
         * @since 1.6.3
         */
        public <T extends Number> LambdaNumberSpinner(String name, Class<T> type, Supplier<T> getter, Consumer<T> setter) {
            super(create(name, type, getter, setter, GuiRepresentation.createValueNumberSpinner()), GuiSwingView.specifierManagerRoot());
        }
    }

    public static class LambdaLabel extends GuiSwingViewLabel.PropertyLabel {
        @Serial private static final long serialVersionUID = 1L;

        public LambdaLabel(Supplier<?> getter) {
            this(UNNAMED, getter);
        }

        /**
         * @param name the property name
         * @param getter the property getter
         * @since 1.6.3
         */
        public LambdaLabel(String name, Supplier<?> getter) {
            super(create(name, Object.class, getter, null, GuiRepresentation.createValueLabel()), GuiSwingView.specifierManagerRoot());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    context.updateSourceSubTree();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    context.updateSourceSubTree();
                }
            });
        }
    }

    public static class LambdaImagePane extends GuiSwingViewImagePane.PropertyImagePane {
        @Serial private static final long serialVersionUID = 1L;

        public <T extends Image> LambdaImagePane(Supplier<T> getter, Consumer<T> setter) {
            this(UNNAMED, getter, setter);
        }

        public <T extends Image> LambdaImagePane(Class<? extends Image> imageType, Supplier<T> getter, Consumer<T> setter) {
            this(UNNAMED, imageType, getter, setter);
        }

        /**
         * @param name the property name
         * @param getter the property getter
         * @param setter the setter
         * @param <T> the image type
         * @since 1.6.3
         */
        public <T extends Image> LambdaImagePane(String name, Supplier<T> getter, Consumer<T> setter) {
            this(name, BufferedImage.class, getter, setter);
        }

        /**
         * @param name the property name
         * @param imageType the property image type
         * @param getter the property getter
         * @param setter the setter
         * @param <T> the image type
         * @since 1.6.3
         */
        public <T extends Image> LambdaImagePane(String name, Class<? extends Image> imageType, Supplier<T> getter, Consumer<T> setter) {
            super(create(name, imageType, getter, setter, new GuiReprValueImagePane()), GuiSwingView.specifierManagerRoot());
        }
    }

    public static class LambdaFilePathPane extends GuiSwingViewFilePathField.PropertyFilePathPane {
        @Serial private static final long serialVersionUID = 1L;

        public LambdaFilePathPane(Supplier<Path> getter, Consumer<Path> setter) {
            this(UNNAMED, getter, setter);
        }

        /**
         * @param name the property name
         * @param getter the property getter
         * @param setter the setter
         * @since 1.6.3
         */
        public LambdaFilePathPane(String name, Supplier<Path> getter, Consumer<Path> setter) {
            super(create(name, Path.class, getter, setter, GuiRepresentation.createValueFilePathField()), GuiSwingView.specifierManagerRoot());
            SwingUtilities.invokeLater(() ->
                setPreferredSize(new Dimension(UIManagerUtil.getInstance().getScaledSizeInt(200), getPreferredSize().height )));
        }
    }

    public static class LambdaEnumComboBox extends GuiSwingViewEnumComboBox.PropertyEnumComboBox {
        @Serial private static final long serialVersionUID = 1L;

        public <T extends Enum<?>> LambdaEnumComboBox(Class<T> enumType, Supplier<T> getter, Consumer<T> setter) {
            this(UNNAMED, enumType, getter, setter);
        }

        /**
         * @param name the property name
         * @param enumType  the property enum type
         * @param getter the property getter
         * @param setter the setter
         * @param <T> the enum type
         * @since 1.6.3
         */
        public <T extends Enum<?>> LambdaEnumComboBox(String name, Class<T> enumType, Supplier<T> getter, Consumer<T> setter) {
            super(create(name, enumType, getter, setter, GuiRepresentation.createValueEnumComboBox()), GuiSwingView.specifierManagerRoot());
        }
    }

    public static class LambdaBooleanCheckBox extends GuiSwingViewBooleanCheckBox.PropertyCheckBox {
        @Serial private static final long serialVersionUID = 1L;

        public LambdaBooleanCheckBox(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            this(UNNAMED, label, getter, setter);
        }

        /**
         * @param name the property name
         * @param label  the label for the check-box
         * @param getter the property getter
         * @param setter the setter
         * @since 1.6.3
         */
        public LambdaBooleanCheckBox(String name, String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            super(create(name, Boolean.class, getter, setter, GuiRepresentation.createValueBooleanCheckBox()), GuiSwingView.specifierManagerRoot());
            setText(label);
        }
    }

    public static class LambdaDocumentPlainEditorPane extends GuiSwingViewDocumentEditor.PropertyDocumentEditorPane {
        @Serial private static final long serialVersionUID = 1L;

        public LambdaDocumentPlainEditorPane(Supplier<? extends Document> getter) {
            this(UNNAMED, getter);
        }

        /**
         * @param name the property name
         * @param getter the property getter
         * @since 1.6.3
         */
        public LambdaDocumentPlainEditorPane(String name, Supplier<? extends Document> getter) {
            super(create(name, Document.class, getter, null, GuiSwingMapperSet.createValueDocumentEditor()), GuiSwingView.specifierManagerRoot());
        }
    }

    public static class LambdaDocumentTextPane extends GuiSwingViewDocumentEditor.PropertyDocumentTextPane {
        @Serial private static final long serialVersionUID = 1L;

        /** @param getter the getter returns {@link StringBuilder},
         *  {@link javax.swing.text.AbstractDocument.Content}, or {@link StyledDocument} */
        public LambdaDocumentTextPane(Supplier<?> getter) {
            this(UNNAMED, getter);
        }

        /**
         * @param name the property name
         * @param getter the getter returns {@link StringBuilder},
         *  {@link javax.swing.text.AbstractDocument.Content}, or {@link StyledDocument}
         * @since 1.6.3
         *  */
        public LambdaDocumentTextPane(String name, Supplier<?> getter) {
            super(create(name, Document.class, getter, null, GuiSwingMapperSet.createValueDocumentEditor()), GuiSwingView.specifierManagerRoot());
        }
    }


    public static class LambdaCollectionTable extends GuiSwingViewCollectionTable.CollectionTable {
        @Serial private static final long serialVersionUID = 1L;
        private boolean finishAdding = false;

        public <E> LambdaCollectionTable(Class<E> elementType, Supplier<java.util.List<E>> getter) {
            this(UNNAMED, elementType, getter);
        }

        /**
         * @param name the property name
         * @param elementType the list ellement type
         * @param getter the property getter
         * @param <E> the element type
         */
        public <E> LambdaCollectionTable(String name, Class<E> elementType, Supplier<java.util.List<E>> getter) {
            super(createList(name, List.class, elementType, getter, null,
                    GuiRepresentation.createCollectionTable(GuiSwingMapperSet.getReprDefaultSet())), GuiSwingView.specifierManagerRoot());
            initRowIndex();
        }

        public void initRowIndex() {
            getObjectTableModel().getColumns().addColumnRowIndex();
        }

        public LambdaCollectionTable addColumn(ObjectTableColumn column) {
            getObjectTableModel().getColumns().addColumnStatic(column);
            getObjectTableModel().initTableWithoutScrollPane(this);
            return this;
        }

        public <E,T> GuiMappingContext addColumnContext(String name, Class<?> type,
                                                      Function<E, T> getter, BiConsumer<E, T> setter,
                                                      GuiRepresentation representation) {
            GuiMappingContext columnContext = context.getChildren().getFirst() // table -> collectionElement
                    .getChildren().getFirst()   // -> value
                    .createChildCandidate(new LambdaElementProperty<>(name, type, getter, setter));
            columnContext.setRepresentation(representation);
            columnContext.addToParent();
            return columnContext;
        }

        ///// boolean

        public <E> LambdaCollectionTable addColumnBoolean(String name,
                                                          Function<E, Boolean> getter, BiConsumer<E, Boolean> setter) {
            return addColumn(new GuiSwingTableColumnBoolean()
                    .createColumn(addColumnContext(name, Boolean.class, getter, setter, GuiRepresentation.createValueBooleanCheckBox()),
                            getRowSpecifier(), getRowSpecifier()));
        }

        public LambdaCollectionTable addColumnBoolean(String name,
                                                      Supplier<Boolean> getter, Consumer<Boolean> setter) {
            return addColumnBoolean(name, (e) -> getter.get(), (e,v) -> setter.accept(v));
        }

        ///// String

        public <E> LambdaCollectionTable addColumnString(String name,
                                                         Function<E, String> getter, BiConsumer<E, String> setter) {
            return addColumn(new GuiSwingTableColumnString()
                    .createColumn(addColumnContext(name, String.class, getter, setter, GuiRepresentation.createValueStringField()),
                            getRowSpecifier(), getRowSpecifier()));
        }

        public LambdaCollectionTable addColumnString(String name,
                                                      Supplier<String> getter, Consumer<String> setter) {
            return addColumnString(name, (e) -> getter.get(), (e,v) -> setter.accept(v));
        }

        ///// Number


        public <E, T extends Number> LambdaCollectionTable addColumnNumber(String name, Class<T> numType,
                                                                           Function<E, T> getter, BiConsumer<E, T> setter) {
            return addColumn(new GuiSwingTableColumnNumber()
                    .createColumn(addColumnContext(name, numType, getter, setter, GuiRepresentation.createValueNumberSpinner()),
                            getRowSpecifier(), getRowSpecifier()));
        }

        public <T extends Number> LambdaCollectionTable addColumnNumber(String name, Class<T> numType,
                                                     Supplier<T> getter, Consumer<T> setter) {
            return addColumnNumber(name, numType, (e) -> getter.get(), (e,v) -> setter.accept(v));
        }

        ///// Enum

        public <E, T extends Enum<?>> LambdaCollectionTable addColumnEnum(String name, Class<T> enumType,
                                                                          Function<E, T> getter, BiConsumer<E, T> setter) {
            return addColumn(new GuiSwingTableColumnEnum()
                    .createColumn(addColumnContext(name, enumType, getter, setter, GuiRepresentation.createValueEnumComboBox()),
                            getRowSpecifier(), getRowSpecifier()));
        }

        public <T extends Enum<?>> LambdaCollectionTable addColumnEnum(String name, Class<T> enumType,
                                                                        Supplier<T> getter, Consumer<T> setter) {
            return addColumnEnum(name, enumType, (e) -> getter.get(), (e,v) -> setter.accept(v));
        }

        ///// Path

        public <E> LambdaCollectionTable addColumnFilePath(String name,
                                                           Function<E, Path> getter, BiConsumer<E, Path> setter) {
            return addColumn(new GuiSwingTableColumnFilePath()
                    .createColumn(addColumnContext(name, Path.class, getter, setter, GuiRepresentation.createValueFilePathField()),
                            getRowSpecifier(), getRowSpecifier()));
        }

        public <T extends Number> LambdaCollectionTable addColumnFilePath(String name,
                                                                        Supplier<Path> getter, Consumer<Path> setter) {
            return addColumnFilePath(name, (e) -> getter.get(), (e,v) -> setter.accept(v));
        }

        ///// Image

        public <E, T extends Image> LambdaCollectionTable addColumnImage(String name, Class<T> imageType,
                                                                         Function<E, T> getter, BiConsumer<E, T> setter) {
            return addColumn(new GuiSwingTableColumnImage()
                    .createColumn(addColumnContext(name, imageType, getter, setter, GuiSwingMapperSet.createValueImagePane(false)),
                            getRowSpecifier(), getRowSpecifier()));
        }

        public <T extends Image> LambdaCollectionTable addColumnImage(String name, Class<T> imageType,
                                                                        Supplier<T> getter, Consumer<T> setter) {
            return addColumnImage(name, imageType, (e) -> getter.get(), (e,v) -> setter.accept(v));
        }

        //// label

        public <E> LambdaCollectionTable addColumnLabel(String name,
                                                          Function<E, ?> getter) {
            return addColumn(new GuiSwingTableColumnLabel()
                    .createColumn(addColumnContext(name, Object.class, getter, null, GuiRepresentation.createValueLabel()),
                            getRowSpecifier(), getRowSpecifier()));
        }

        public LambdaCollectionTable addColumnLabel(String name,
                                                      Supplier<?> getter) {
            return addColumnLabel(name, (e) -> getter.get());
        }

        //// action

        public <E> LambdaCollectionTable addAction(String name, Consumer<List<E>> action) {
            GuiMappingContext context = getSwingViewContext().createChildCandidate(new GuiTypeMemberActionList(name,
                    new GuiTypeValue(void.class),
                    getSwingViewContext().getTypeElementCollection().getElementType(), (String) null, false));
            context.setRepresentation(GuiRepresentation.createActionList());
            context.addToParent();

            GuiSwingTableColumnSetDefault.TableSelectionListAction a = new GuiSwingTableColumnSetDefault.TableSelectionListAction(context, this) {
                @Serial private static final long serialVersionUID = 1L;

                @SuppressWarnings("unchecked")
                @Override
                public void actionPerformed(ActionEvent e) {
                    setEnabled(false);
                    try {
                        action.accept((List<E>) source.getSelectedItems());
                    } finally {
                        setEnabled(true);
                    }
                }
            };

            actions.add(a);
            return this;
        }

        /**
         * after adding columns and actions, this method sets up relating components for the table
         *   by calling {@link #setupAfterAddingColumns()}.
         *  The method will be automatically called by {@link #wrapPane(boolean, boolean)}.
         * @return this
         * @since 1.1
         */
        public LambdaCollectionTable finishAdding() {
            if (!finishAdding) {
                setupAfterAddingColumns();
                finishAdding = true;
            }
            return this;
        }

        /**
         * @return wrapping pane for the table with scroll-bars and tool-bar. call {@link #wrapPane(boolean, boolean)} with (true, true)
         */
        public GuiSwingViewWrapper.ValueWrappingPane<List<?>> wrapSwingPane() {
            return wrapPane(true, true);
        }

        /**
         * calls {@link #finishAdding()}, if not yet.
         * calls {@link #initTableScrollPane()} and {@link #initActionToolBar(List)}.
         *
         * @param verticalAlways flag for always displaying vertical scroll-bar
         * @param horizontalAlways flag for always displaying horizontal scroll-bar
         * @return wrapping pane with scroll-bars and tool-bar.
         */
        public GuiSwingViewWrapper.ValueWrappingPane<List<?>> wrapPane(boolean verticalAlways, boolean horizontalAlways) {
            if (!finishAdding) {
                finishAdding();
            }
            GuiSwingViewWrapper.ValueScrollPane<List<?>> s = new GuiSwingViewWrapper.ValueScrollPane<>(this,
                    verticalAlways ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    horizontalAlways ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            initTableScrollPane(s);
            GuiSwingViewWrapper.ValueWrappingPane<List<?>> pane = s.wrapSwingPane();
            pane.add(initActionToolBar(getActions()), BorderLayout.PAGE_START);
            return pane;
        }
    }

    public static class LambdaElementProperty<E,T> extends GuiTypeMemberProperty {
        protected Function<E,T> getter;
        protected BiConsumer<E,T> setter;

        public LambdaElementProperty(String name, Class<?> type, Function<E, T> getter, BiConsumer<E, T> setter) {
            super(name);
            setType(new GuiTypeValue(type));
            this.getter = getter;
            this.setter = setter;
        }

        @SuppressWarnings("unchecked")
        @Override
        public GuiUpdatedValue executeGet(Object target) {
            return GuiUpdatedValue.of(getter.apply((E) target));
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object executeSet(Object target, Object value) {
            setter.accept((E) target, (T) value);
            return null;
        }

        @Override
        public boolean isWritable() {
            return setter != null;
        }
    }
}
