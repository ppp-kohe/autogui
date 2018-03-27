package autogui.swing.mapping;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.util.SwingDeferredRunner;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
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
    public Object getValue(GuiMappingContext context, Object parentSource, Object prev) throws Throwable {
        return SwingDeferredRunner.run(() -> super.getValue(context, parentSource, prev));
    }

    @Override
    public Object update(GuiMappingContext context, Object parentSource, Object newValue) {
        try {
            return SwingDeferredRunner.run(() -> super.update(context, parentSource, newValue));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean isHistoryValueStored() {
        return false;
    }

    @Override
    public void setSource(GuiMappingContext context, Object value) {
        if (value instanceof StyledDocument) {
            StyledDocument doc = (StyledDocument) value;
            Style style = doc.getStyle(StyleContext.DEFAULT_STYLE);
            if (setUpStyle(style)) {
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
        if (value instanceof Document) {
            return (Document) value;
        } else if (value instanceof AbstractDocument.Content) {
            return new ContentWrappingDocument((AbstractDocument.Content) value);
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
            tabSet = new TabSet(IntStream.range(0, 100)
                    .mapToObj(i -> new TabStop(i * 32))
                    .toArray(TabStop[]::new));
        }
        return tabSet;
    }

    public static boolean setUpStyle(Style style) {
        boolean set = false;
        if (StyleConstants.getTabSet(style) == null) {
            StyleConstants.setTabSet(style, getTabSet());
            set = true;
        }
        if (style.getAttribute(StyleConstants.FontSize) == null) {
            StyleConstants.setFontSize(style, 14);
            set = true;
        }
        if (style.getAttribute(StyleConstants.FontFamily) == null) {
            String os = System.getProperty("os.name", "?").toLowerCase();
            if (os.contains("mac")) {
                StyleConstants.setFontFamily(style, "Menlo");
                set = true;
            }
        }
        return set;
    }

    public static void setUpStyle(StyledDocument doc) {
        Style style = doc.getStyle(StyleContext.DEFAULT_STYLE);
        if (setUpStyle(style)) {
            doc.setParagraphAttributes(0, doc.getLength(), style, true);
        }
    }

    /**
     * a styled-document impl. for {@link javax.swing.text.AbstractDocument.Content}
     */
    public static class ContentWrappingDocument extends DefaultStyledDocument {
        protected Content value;

        public ContentWrappingDocument(Content c) {
            super(c, new StyleContext());
            this.value = c;
            initDefaultStyle();
        }

        public void initDefaultStyle() {
            setUpStyle(this);
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

    //TODO this is a worst way for implementing a text editor.
    /**
     *  a content for {@link StringBuilder}
     */
    public static class StringBuilderContent implements AbstractDocument.Content, Serializable {
        protected final StringBuilder buffer;
        transient protected char[] array;
        transient protected List<WeakReference<ContentPosition>> positions = new ArrayList<>();

        public StringBuilderContent(StringBuilder buffer) {
            this.buffer = buffer;
            if (buffer.length() == 0) {
                buffer.append('\n');
            }
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
                return run(buffer::length);
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
                updatePositions(where, str.length());
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
                updatePositions(where, -nItems);
                return new ContentRemoveEdit(this, where, nItems, removed);
            });
        }

        protected void updatePositions(int pos, int adj) {
            if (adj >= 0 && pos == 0) {
                pos = 1; //it seems important
            }
            for (Iterator<WeakReference<ContentPosition>> iter = positions.iterator(); iter.hasNext();) {
                ContentPosition existing = iter.next().get();
                if (existing != null) {
                    if (adj < 0) {
                        //remove
                        if (existing.offset >= (pos - adj)) {
                            existing.offset += adj;
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
                String s = buffer.substring(where, where + len);
                return s;
            });
        }

        @Override
        public void getChars(int where, int len, Segment txt) throws BadLocationException {
            if (where + len > length()) {
                throw new BadLocationException("Invalid range", length());
            }
            if (array == null) {
                array = buffer.toString().toCharArray();
            }
            run(() -> txt.array = array);
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
            return buffer != null ? buffer.hashCode() : 0;
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

    /**
     * an undoable insertion operation for {@link StringBuilderContent}
     */
    public static class ContentInsertEdit extends AbstractUndoableEdit {
        protected StringBuilderContent content;
        protected int offset;
        protected int length;
        protected String str;

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
            } catch (Exception ex){
                throw new CannotUndoException();
            }
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            try {
                str = content.getString(offset, length);
                content.remove(offset, length);
            } catch (Exception ex) {
                throw new CannotRedoException();
            }
        }
    }
}
