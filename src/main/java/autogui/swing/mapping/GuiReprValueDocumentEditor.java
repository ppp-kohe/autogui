package autogui.swing.mapping;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

/** the representation depends on some Swing classes(java.desktop module)  */
public class GuiReprValueDocumentEditor extends GuiReprValue {
    @Override
    public boolean matchValueType(Class<?> cls) {
        return Document.class.isAssignableFrom(cls) ||
                AbstractDocument.Content.class.isAssignableFrom(cls) ||
                StringBuilder.class.isAssignableFrom(cls);
    }

    @Override
    public Object getUpdatedValue(GuiMappingContext context, boolean executeParent) throws Throwable {
        return new SwingInvoker(() -> super.getUpdatedValue(context, executeParent)).run();
    }

    public interface Task {
        Object call() throws Throwable;
    }

    public static class SwingInvoker {
        public Object result;
        public Throwable exception;

        protected Task runnable;

        public SwingInvoker(Task runnable) {
            this.runnable = runnable;
        }

        public Object run() throws Throwable {
            if (SwingUtilities.isEventDispatchThread()) {
                return runnable.call();
            } else {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        result = runnable.call();
                    } catch (Throwable ex) {
                        exception = ex;
                    }
                });
                if (exception != null) {
                    throw exception;
                } else {
                    return result;
                }
            }
        }

        public Object runNoError() {
            try {
                return run();
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public Document toUpdateValue(GuiMappingContext context, Object value) {
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
        return StyledDocument.class.isAssignableFrom(getValueType(context));
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

    public static class ContentWrappingDocument extends DefaultStyledDocument {
        protected Content value;

        public static TabSet tabSet;

        public ContentWrappingDocument(Content c) {
            super(c, new StyleContext());
            this.value = c;
            initDefaultStyle();
        }

        public void initDefaultStyle() {
            if (tabSet == null) {
                tabSet = new TabSet(IntStream.range(0, 100)
                        .mapToObj(i -> new TabStop(i * 32))
                        .toArray(TabStop[]::new));
            }
            Style style = getStyle(StyleContext.DEFAULT_STYLE);

            StyleConstants.setTabSet(style, tabSet);
            StyleConstants.setFontSize(style, 14);
            String os = System.getProperty("os.name", "?").toLowerCase();
            if (os.contains("mac")) {
                StyleConstants.setFontFamily(style, "Menlo");
            }
            setParagraphAttributes(0, getLength(), style, true);
        }

        public Content getContentValue() {
            return value;
        }

        /** compare values by equals(Object) for detecting change of a property:
         *   default impl. of equals of Content only compares their references */
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
         * compare only StringBuilder references for detecting change of the property */
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

    public interface EditSupplier<T> {
        T get() throws BadLocationException;
    }

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
