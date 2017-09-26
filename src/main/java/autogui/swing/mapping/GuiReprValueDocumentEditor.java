package autogui.swing.mapping;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;

import javax.swing.text.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.*;

/** the representation depends on some Swing classes(java.desktop module)  */
public class GuiReprValueDocumentEditor extends GuiReprValue {
    @Override
    public boolean matchValueType(Class<?> cls) {
        return Document.class.isAssignableFrom(cls) ||
                AbstractDocument.Content.class.isAssignableFrom(cls) ||
                StringBuilder.class.isAssignableFrom(cls);
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
        Class<?> cls = context.getTypeElementValue().getType();
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
    }

    public static class ContentWrappingDocument extends DefaultStyledDocument {
        protected Content value;
        public ContentWrappingDocument(Content c) {
            super(c, new StyleContext());
            this.value = c;
        }
        public Content getContentValue() {
            return value;
        }
    }

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
                pos = 1;
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