package autogui.swing.mapping;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.type.GuiUpdatedValue;
import autogui.swing.util.SwingDeferredRunner;
import autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** a GUI representation for a property holding
 * a {@link Document}, {@link javax.swing.text.AbstractDocument.Content}, or a {@link StringBuilder}.
 *  the representation depends on some Swing classes(java.desktop module)  */
public class GuiReprValueDocumentEditor extends GuiReprValue {
    @Override
    public boolean isTaskRunnerUsedFor(Callable<?> task) {
        return !SwingUtilities.isEventDispatchThread();
    }

    @Override
    public boolean matchValueType(Class<?> cls) {
        return Document.class.isAssignableFrom(cls) ||
                AbstractDocument.Content.class.isAssignableFrom(cls) ||
                StringBuilder.class.isAssignableFrom(cls);
    }

    @Override
    public GuiUpdatedValue getValue(GuiMappingContext context, GuiMappingContext.GuiSourceValue parentSource,
                                    ObjectSpecifier specifier, GuiMappingContext.GuiSourceValue prev) throws Throwable {
        Object v = SwingDeferredRunner.run(() -> super.getValue(context, parentSource, specifier, prev));
        if (v instanceof GuiUpdatedValue) {
            return (GuiUpdatedValue) v;
        } else {
            return GuiUpdatedValue.of(v); //future
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
    public boolean isHistoryValueStored(Object value) {
        return false;
    }

    @Override
    public void setSource(GuiMappingContext context, Object value) {
        if (value instanceof StyledDocument) {
            StyledDocument doc = (StyledDocument) value;
            Style style = doc.getStyle(StyleContext.DEFAULT_STYLE);
            if (setupStyle(style)) {
                doc.setParagraphAttributes(0, doc.getLength(), style, true);
            }
        }
        super.setSource(context, value);
    }

    public Document toUpdateValue(GuiMappingContext context, Object value) {
        return toUpdateValue(context, value, null);
    }

    public Document toUpdateValue(GuiMappingContext context, Object value, Consumer<Document> delayed) {
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
        } else if (value instanceof Document) {
            return (Document) value;
        } else if (value instanceof AbstractDocument.Content) {
            return ContentWrappingDocument.create((AbstractDocument.Content) value);
        } else if (value instanceof StringBuilder) {
            return toUpdateValue(context, new StringBuilderContent((StringBuilder) value));
        } else {
            return null;
        }
    }

    public Object toSourceValue(GuiMappingContext context, Document document) {
        Class<?> cls = this.getValueType(context);
        if (Document.class.isAssignableFrom(cls)) {
            return document;
        } else if (AbstractDocument.Content.class.isAssignableFrom(cls)) {
            return ((ContentWrappingDocument) document).getContentValue();
        } else if (StringBuilder.class.isAssignableFrom(cls)) {
            return ((StringBuilderContent) ((ContentWrappingDocument) document).getContentValue())
                    .getBuffer();
        } else {
            return document; //error
        }
    }

    public boolean isStyledDocument(GuiMappingContext context) {
        Class<?> cls = this.getValueType(context);
        return StyledDocument.class.isAssignableFrom(cls) ||
                //those wrapped documents are also StyledDocuments
                AbstractDocument.Content.class.isAssignableFrom(cls) ||
                StringBuilder.class.isAssignableFrom(cls);
        //return true; //enables style change
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return document text String
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        if (source instanceof Document) {
            Document doc = (Document) source;
            try {
                return doc.getText(0, doc.getLength());
            } catch (Exception ex) {
                return null;
            }
        } else if (source instanceof AbstractDocument.Content) {
            AbstractDocument.Content content = (AbstractDocument.Content) source;
            try {
                return content.getString(0, content.length());
            } catch (Exception ex) {
                return null;
            }
        } else if (source instanceof StringBuilder) {
            return source.toString();
        } else {
            return null;
        }
    }

    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        Object j = toJson(context, source);
        return j == null ? "" : j.toString();
    }

    @Override
    public Object fromHumanReadableString(GuiMappingContext context, String str) {
        return fromJson(context, null, str);
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        Class<?> cls = getValueType(context);
        if (json instanceof String) {
            String jsonStr = (String) json;

            if (fromJsonCheckType(Document.class, cls, target)) {
                Document doc = castOrMake(Document.class, target, DefaultStyledDocument::new);
                try {
                    doc.remove(0, doc.getLength());
                    doc.insertString(0, jsonStr, null);
                } catch (Exception ex) {
                    return null;
                }
                return doc;
            } else if (fromJsonCheckType(AbstractDocument.Content.class, cls, target)) {
                AbstractDocument.Content content = castOrMake(AbstractDocument.Content.class, target, GapContent::new);
                try {
                    content.remove(0, content.length());
                    content.insertString(0, jsonStr);
                } catch (Exception ex) {
                    return null;
                }
                return content;
            } else if (fromJsonCheckType(StringBuilder.class, cls, target)) {
                StringBuilder buf = castOrMake(StringBuilder.class, target, StringBuilder::new);
                buf.replace(0, buf.length(), jsonStr);
                return buf;
            }
        }
        return null;
    }

    private boolean fromJsonCheckType(Class<?> type, Class<?> valueType, Object target) {
        return target == null
                ? type.isAssignableFrom(valueType)
                : type.isInstance(target);
    }

    @Override
    public boolean isJsonSetter() {
        return true;
    }

    public static TabSet tabSet;

    public static TabSet getTabSet() {
        if (tabSet == null) {
            int pos = UIManagerUtil.getInstance().getScaledSizeInt(32);
            tabSet = new TabSet(IntStream.range(0, 100)
                    .mapToObj(i -> new TabStop(i * pos))
                    .toArray(TabStop[]::new));
        }
        return tabSet;
    }

    public static boolean setupStyle(Style style) {
        boolean set = false;
        if (StyleConstants.getTabSet(style) == null) {
            StyleConstants.setTabSet(style, getTabSet());
            set = true;
        }
        Font consoleFont = UIManagerUtil.getInstance().getConsoleFont();
        if (style.getAttribute(StyleConstants.FontSize) == null) {
            StyleConstants.setFontSize(style, consoleFont.getSize());
            set = true;
        }
        if (style.getAttribute(StyleConstants.FontFamily) == null) {
            StyleConstants.setFontFamily(style, consoleFont.getFamily());
            set = true;
        }
        return set;
    }

    public static void setupStyle(StyledDocument doc) {
        Style style = doc.getStyle(StyleContext.DEFAULT_STYLE);
        if (setupStyle(style)) {
            doc.setParagraphAttributes(0, doc.getLength(), style, true);
        }
    }

    /**
     * a styled-document impl. for {@link javax.swing.text.AbstractDocument.Content}
     */
    public static class ContentWrappingDocument extends DefaultStyledDocument {
        protected Content value;

        public static ContentWrappingDocument create(Content c) {
            if (c.length() > 1) {
                try {
                    String str = c.getString(0, c.length() - 1);
                    c.remove(0, c.length() - 1);

                    ContentWrappingDocument doc = new ContentWrappingDocument(c);

                    doc.insertString(0, str, null);
                    return doc;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return new ContentWrappingDocument(c);
                }
            } else {
                return new ContentWrappingDocument(c);
            }
        }

        /**
         * @param c an empty content: DefaultStyleDocument seems to require an empty content at initialization.
         *          To satisfy this, use {@link #create(Content)} which do removing, creating, and re-inserting
         */
        public ContentWrappingDocument(Content c) {
            super(c, new StyleContext());
            this.value = c;
            initDefaultStyle();
        }

        public void initDefaultStyle() {
            setupStyle(this);
        }

        public Content getContentValue() {
            return value;
        }

        /** compare values by equals(Object) for detecting change of a property:
         *   default impl. of equals of Content only compares their references
         * @param o compared value
         * @return true if o is this
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ContentWrappingDocument that = (ContentWrappingDocument) o;

            return value != null ? value.equals(that.value) : that.value == null;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    /**
     *  a content for {@link StringBuilder}
     */
    public static class StringBuilderContent implements AbstractDocument.Content, Serializable {
        protected final StringBuilder buffer;
        transient protected char[] array;
        transient protected List<WeakReference<ContentPosition>> positions = new ArrayList<>();

        public StringBuilderContent(StringBuilder buffer) {
            this.buffer = buffer;
        }

        public StringBuilder getBuffer() {
            return buffer;
        }

        @Override
        public Position createPosition(int offset) throws BadLocationException {
            ContentPosition pos = new ContentPosition(offset);
            positions.add(new WeakReference<>(pos));
            return pos;
        }

        @Override
        public int length() {
            try {
                return buffer.length() + 1; //virtually append a new-line
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public UndoableEdit insertString(int where, String str) throws BadLocationException {
            if (where >= length() || where < 0) {
                throw new BadLocationException("Invalid location", length());
            }
            return run(() -> {
                buffer.insert(where, str);
                array = null;
                updatePositions(where, str.length(), true);
                return new ContentInsertEdit(this, where, str.length(), str);
            });
        }

        @Override
        public UndoableEdit remove(int where, int nItems) throws BadLocationException {
            if (where + nItems >= length()) {
                throw new BadLocationException("Invalid range", length());
            }
            return run(() -> {
                String removed = buffer.substring(where, where + nItems);
                buffer.delete(where, where + nItems);
                array = null;
                updatePositions(where, nItems, false);
                return new ContentRemoveEdit(this, where, nItems, removed);
            });
        }

        protected void updatePositions(int pos, int adj, boolean insert) {
            if (insert && pos == 0) {
                pos = 1; //it seems important
            }
            for (Iterator<WeakReference<ContentPosition>> iter = positions.iterator(); iter.hasNext();) {
                ContentPosition existing = iter.next().get();
                if (existing != null) {
                    if (!insert) {
                        //remove
                        if (existing.offset >= (pos + adj)) {
                            existing.offset -= adj;
                        } else if (existing.offset >= pos) {
                            existing.offset = pos;
                        }
                    } else {
                        //insert
                        if (existing.offset >= pos) {
                            existing.offset += adj;
                        }
                    }
                } else {
                    iter.remove();
                }
            }
        }

        @Override
        public String getString(int where, int len) throws BadLocationException {
            if (where + len > length()) {
                throw new BadLocationException("Invalid range", length());
            }
            return run(() -> {
                int end = Math.min(buffer.length(), where + len);
                return buffer.substring(where, end) + (where + len > end ? "\n" : "");
            });
        }

        @Override
        public void getChars(int where, int len, Segment txt) throws BadLocationException {
            if (where + len > length()) {
                throw new BadLocationException("Invalid range", length());
            }
            txt.array = run(() -> {
                if (array == null || array.length != buffer.length() + 1) {
                    array = Arrays.copyOf(buffer.toString().toCharArray(), buffer.length() + 1);
                    array[array.length - 1] = '\n';
                }
                return array;
            });
            txt.offset = where;
            txt.count = len;
        }

        public <T> T run(EditSupplier<T> s) throws BadLocationException {
            synchronized (buffer) {
                return s.get();
            }
        }

        /**
         * compare only StringBuilder references for detecting change of the property
         * @param o compared value
         * @return true if o is this
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StringBuilderContent that = (StringBuilderContent) o;

            return buffer == that.buffer;
        }

        @Override
        public int hashCode() {
            return buffer != null ? System.identityHashCode(buffer) : 0;
        }

        public List<ContentPosition> getPositions() {
            return positions.stream()
                    .map(Reference::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        public List<UndoContentPosition> getPositionsAsUndo(int pos, int len) {
            return positions.stream()
                    .map(Reference::get)
                    .filter(Objects::nonNull)
                    .filter(e -> pos <= e.getOffset() && e.getOffset() <= pos + len)
                    .map(UndoContentPosition::new)
                    .collect(Collectors.toList());
        }
    }

    /**
     * an functional interface used in {@link StringBuilderContent}
     * @param <T> the returned type
     */
    public interface EditSupplier<T> {
        T get() throws BadLocationException;
    }

    /**
     * a position implementation with an offset
     */
    public static class ContentPosition implements Position {
        public int offset;

        public ContentPosition(int offset) {
            this.offset = offset;
        }

        @Override
        public int getOffset() {
            return offset;
        }

        @Override
        public String toString() {
            return Integer.toString(offset);
        }
    }

    public static class UndoContentPosition {
        protected ContentPosition position;
        protected int offset;

        public UndoContentPosition(ContentPosition position) {
            this.position = position;
            this.offset = position.getOffset();
        }

        public void restore() {
            position.offset = offset;
        }

        @Override
        public String toString() {
            return offset + "/" + position;
        }
    }

    /**
     * an undoable insertion operation for {@link StringBuilderContent}
     */
    public static class ContentInsertEdit extends AbstractUndoableEdit {
        protected StringBuilderContent content;
        protected int offset;
        protected int length;
        protected String str;
        protected List<UndoContentPosition> positions = Collections.emptyList();

        public ContentInsertEdit(StringBuilderContent content, int offset, int length, String str) {
            this.content = content;
            this.offset = offset;
            this.length = length;
            this.str = str;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            try {
                positions = content.getPositionsAsUndo(offset, length);
                str = content.getString(offset, length);
                content.remove(offset, length);
            } catch (Exception ex){
                throw new CannotUndoException();
            }
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            try {
                content.insertString(offset, str);
                positions.forEach(UndoContentPosition::restore);
            } catch (Exception ex) {
                throw new CannotRedoException();
            }
        }
    }

    /**
     * an undoable removing operation for {@link StringBuilderContent}
     */
    public static class ContentRemoveEdit extends AbstractUndoableEdit {
        protected StringBuilderContent content;
        protected int offset;
        protected int length;
        protected String str;
        protected List<UndoContentPosition> positions = Collections.emptyList();

        public ContentRemoveEdit(StringBuilderContent content, int offset, int length, String str) {
            this.content = content;
            this.offset = offset;
            this.length = length;
            this.str = str;
        }
        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            try {
                content.insertString(offset, str);
                positions.forEach(UndoContentPosition::restore);
            } catch (Exception ex){
                throw new CannotUndoException();
            }
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            try {
                positions = content.getPositionsAsUndo(offset, length);
                str = content.getString(offset, length);
                content.remove(offset, length);
            } catch (Exception ex) {
                throw new CannotRedoException();
            }
        }
    }
}
