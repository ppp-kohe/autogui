package autogui.swing;

import autogui.base.mapping.*;
import autogui.base.type.*;
import autogui.swing.mapping.GuiReprValueDocumentEditor;
import autogui.swing.mapping.GuiReprValueImagePane;
import autogui.swing.table.*;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LambdaProperty<T> extends GuiTypeMemberProperty {
    protected Supplier<T> getter;
    protected Consumer<T> setter;

    public LambdaProperty(Class<?> type, Supplier<T> getter, Consumer<T> setter) {
        super("_");
        setType(new GuiTypeValue(type));
        this.getter = getter;
        this.setter = setter;
    }

    public LambdaProperty(Class<? extends java.util.List> listType, Class<?> elementType, Supplier<T> getter, Consumer<T> setter) {
        super("_");
        setType(new GuiTypeCollection(listType, new GuiTypeValue(elementType)));
        this.getter = getter;
        this.setter = setter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object executeSet(Object target, Object value) throws Exception {
        setter.accept((T) value);
        return null;
    }

    @Override
    public GuiUpdatedValue executeGet(Object target) throws Exception {
        return GuiUpdatedValue.of(getter.get());
    }

    @Override
    public boolean isWritable() {
        return setter != null;
    }

    public static <T> GuiMappingContext create(Class<?> type, Supplier<T> getter, Consumer<T> setter, GuiRepresentation repr) {
        GuiMappingContext context = new GuiMappingContext(new LambdaProperty<>(type, getter, setter), repr);
        context.updateSourceSubTree();
        return context;
    }

    public static <T> GuiMappingContext createList(Class<? extends java.util.List> listType, Class<?> elementType, Supplier<T> getter, Consumer<T> setter, GuiRepresentation repr) {
        GuiMappingContext context = new GuiMappingContext(new LambdaProperty<>(listType, elementType, getter, setter), repr);

        GuiMappingContext subContext = context.createChildCandidate(context.getTypeElement().getChildren().get(0));
        subContext.setRepresentation(new GuiReprCollectionElement(null));
        subContext.addToParent();

        context.updateSourceSubTree();
        return context;
    }

    public static SpecifierManagerNone NONE;

    public static class SpecifierManagerNone implements GuiSwingView.SpecifierManager {
        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return GuiReprValue.NONE;
        }
    }

    ////////////

    public static class LambdaTextPane extends GuiSwingViewStringField.PropertyStringPane {
        public LambdaTextPane(Supplier<String> getter, Consumer<String> setter) {
            super(create(String.class, getter, setter, new GuiReprValueStringField()), NONE);
            setPreferredSize(new Dimension(100, getPreferredSize().height ));
        }
    }

    public static class LambdaNumberSpinner extends GuiSwingViewNumberSpinner.PropertyNumberSpinner {
        public <T extends Number> LambdaNumberSpinner(Class<T> type, Supplier<T> getter, Consumer<T> setter) {
            super(create(type, getter, setter, new GuiReprValueNumberSpinner()), NONE);
        }
    }

    public static class LambdaLabel extends GuiSwingViewLabel.PropertyLabel {
        public LambdaLabel(Supplier<?> getter) {
            super(create(Object.class, getter, null, new GuiReprValueLabel()), NONE);
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
        public <T extends Image> LambdaImagePane(Supplier<T> getter, Consumer<T> setter) {
            this(BufferedImage.class, getter, setter);
        }

        public <T extends Image> LambdaImagePane(Class<? extends Image> imageType, Supplier<T> getter, Consumer<T> setter) {
            super(create(imageType, getter, setter, new GuiReprValueImagePane()), NONE);
        }
    }

    public static class LambdaFilePathPane extends GuiSwingViewFilePathField.PropertyFilePathPane {
        public LambdaFilePathPane(Supplier<Path> getter, Consumer<Path> setter) {
            super(create(Path.class, getter, setter, new GuiReprValueFilePathField()), NONE);
            setPreferredSize(new Dimension(200, getPreferredSize().height ));
        }
    }

    public static class LambdaEnumComboBox extends GuiSwingViewEnumComboBox.PropertyEnumComboBox {
        public <T extends Enum<?>> LambdaEnumComboBox(Class<T> enumType, Supplier<T> getter, Consumer<T> setter) {
            super(create(enumType, getter, setter, new GuiReprValueEnumComboBox()), NONE);
        }
    }

    public static class LambdaBooleanCheckBox extends GuiSwingViewBooleanCheckBox.PropertyCheckBox {
        public LambdaBooleanCheckBox(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            super(create(Boolean.class, getter, setter, new GuiReprValueBooleanCheckBox()), NONE);
            setText(label);
        }
    }

    public static class LambdaDocumentPlainEditorPane extends GuiSwingViewDocumentEditor.PropertyDocumentEditorPane {
        public LambdaDocumentPlainEditorPane(Supplier<? extends Document> getter) {
            super(create(Document.class, getter, null, new GuiReprValueDocumentEditor()), NONE);
        }
    }

    public static class LambdaDocumentTextPane extends GuiSwingViewDocumentEditor.PropertyDocumentTextPane {
        /** @param getter the getter returns {@link StringBuilder},
         *  {@link javax.swing.text.AbstractDocument.Content}, or {@link StyledDocument} */
        public LambdaDocumentTextPane(Supplier<?> getter) {
            super(create(Document.class, getter, null, new GuiReprValueDocumentEditor()), NONE);
        }
    }


    public static class LambdaCollectionTable extends GuiSwingViewCollectionTable.CollectionTable {
        public <E> LambdaCollectionTable(Class<E> elementType, Supplier<java.util.List<E>> getter) {
            super(createList(List.class, elementType, getter, null,
                    new GuiReprCollectionTable(GuiRepresentation.getDefaultSet())), NONE);
            initRowIndex();
        }

        public void initRowIndex() {
            getObjectTableModel().addColumnRowIndex();
        }

        public LambdaCollectionTable addColumn(ObjectTableColumn column) {
            getObjectTableModel().addColumn(column);
            getObjectTableModel().initTableWithoutScrollPane(this);;
            return this;
        }

        public <E,T> GuiMappingContext addColumnContext(String name, Class<?> type,
                                                      Function<E, T> getter, BiConsumer<E, T> setter,
                                                      GuiRepresentation representation) {
            GuiMappingContext columnContext = context.getChildren().get(0)
                    .createChildCandidate(new LambdaElementProperty<>(name, type, getter, setter));
            columnContext.setRepresentation(representation);
            columnContext.addToParent();
            return columnContext;
        }

        ///// boolean

        public <E> LambdaCollectionTable addColumnBoolean(String name,
                                                          Function<E, Boolean> getter, BiConsumer<E, Boolean> setter) {
            return addColumn(new GuiSwingTableColumnBoolean()
                    .createColumn(addColumnContext(name, Boolean.class, getter, setter, new GuiReprValueBooleanCheckBox()),
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
                    .createColumn(addColumnContext(name, String.class, getter, setter, new GuiReprValueStringField()),
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
                    .createColumn(addColumnContext(name, numType, getter, setter, new GuiReprValueNumberSpinner()),
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
                    .createColumn(addColumnContext(name, enumType, getter, setter, new GuiReprValueEnumComboBox()),
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
                    .createColumn(addColumnContext(name, Path.class, getter, setter, new GuiReprValueFilePathField()),
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
                    .createColumn(addColumnContext(name, imageType, getter, setter, new GuiReprValueImagePane()),
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
                    .createColumn(addColumnContext(name, Object.class, getter, null, new GuiReprValueLabel()),
                            getRowSpecifier(), getRowSpecifier()));
        }

        public LambdaCollectionTable addColumnLabel(String name,
                                                      Supplier<?> getter) {
            return addColumnLabel(name, (e) -> getter.get());
        }

        //// action

        public <E> LambdaCollectionTable addAction(String name, Consumer<List<E>> action) {
            GuiMappingContext context = getSwingViewContext().createChildCandidate(new GuiTypeMemberActionList(name,
                    ((GuiTypeCollection) getSwingViewContext().getTypeElementAsProperty().getType()).getElementType(), (String) null));
            context.setRepresentation(new GuiReprActionList());
            context.addToParent();

            Action a = new GuiSwingTableColumnSetDefault.TableSelectionListAction(context, this) {
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

        public GuiSwingView.ValueWrappingPane<List<?>> wrapSwingPane() {
            return wrapPane(true, true);
        }

        public GuiSwingView.ValueWrappingPane<List<?>> wrapPane(boolean verticalAlways, boolean horizontalAlways) {
            GuiSwingView.ValueScrollPane<List<?>> s = new GuiSwingView.ValueScrollPane<>(this,
                    verticalAlways ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    horizontalAlways ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            initTableScrollPane(s);
            GuiSwingView.ValueWrappingPane<List<?>> pane = s.wrapSwingPane();
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
        public GuiUpdatedValue executeGet(Object target) throws Exception {
            return GuiUpdatedValue.of(getter.apply((E) target));
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            setter.accept((E) target, (T) value);
            return null;
        }

        @Override
        public boolean isWritable() {
            return setter != null;
        }
    }
}
