package autogui.base.mapping;

import javax.swing.text.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.util.*;
import java.util.function.Supplier;

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
            super(c, StyleContext.getDefaultStyleContext());
            this.value = c;
        }
        public Content getContentValue() {
            return value;
        }
    }

    public static class StringBuilderContent implements AbstractDocument.Content {
        protected final StringBuilder buffer;
        protected List<ContentPosition> positions = new ArrayList<>();

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
            int n = Collections.binarySearch(positions, pos);
            if (n < 0) {
                positions.add(-n - 1, pos);
                return pos;
            } else {
                return positions.get(n);
            }
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
                updatePositions(where, str.length());
                return new ContentInsertEdit(this, createPosition(where), str.length(), str);
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
                updatePositions(where + nItems, -nItems);
                return new ContentRemoveEdit(this, createPosition(where), nItems, removed);
            });
        }

        protected void updatePositions(int pos, int adj) {
            int n = Collections.binarySearch(positions, new ContentPosition(pos));
            if (n < 0) {
                n = -n - 1;
            } else {
                n++;
            }
            for (int i = n, l = positions.size(); i < l; ++i) {
                positions.get(i).offset += adj;
            }
        }

        @Override
        public String getString(int where, int len) throws BadLocationException {
            if (where + len > length()) {
                throw new BadLocationException("Invalid range", length());
            }
            return run(() -> buffer.substring(where, where + len));
        }

        @Override
        public void getChars(int where, int len, Segment txt) throws BadLocationException {
            if (where + len > length()) {
                throw new BadLocationException("Invalid range", length());
            }
            //TODO optimize
            run(() -> txt.array = buffer.toString().toCharArray());
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

    public static class ContentPosition implements Position, Comparable<ContentPosition> {
        public int offset;

        public ContentPosition(int offset) {
            this.offset = offset;
        }

        @Override
        public int getOffset() {
            return offset;
        }

        @Override
        public int compareTo(ContentPosition o) {
            return Integer.compare(offset, o.offset);
        }
    }

    public static class ContentInsertEdit extends AbstractUndoableEdit {
        protected StringBuilderContent content;
        protected Position position;
        protected int length;
        protected String str;

        public ContentInsertEdit(StringBuilderContent content, Position position, int length, String str) {
            this.content = content;
            this.position = position;
            this.length = length;
            this.str = str;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            try {
                str = content.getString(position.getOffset(), length);
                content.remove(position.getOffset(), length);
            } catch (Exception ex){
                throw new CannotUndoException();
            }
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            try {
                content.insertString(position.getOffset(), str);
            } catch (Exception ex) {
                throw new CannotRedoException();
            }
        }
    }

    public static class ContentRemoveEdit extends AbstractUndoableEdit {
        protected StringBuilderContent content;
        protected Position position;
        protected int length;
        protected String str;

        public ContentRemoveEdit(StringBuilderContent content, Position position, int length, String str) {
            this.content = content;
            this.position = position;
            this.length = length;
            this.str = str;
        }
        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            try {
                content.insertString(position.getOffset(), str);
            } catch (Exception ex){
                throw new CannotUndoException();
            }
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            try {
                str = content.getString(position.getOffset(), length);
                content.remove(position.getOffset(), length);
            } catch (Exception ex) {
                throw new CannotRedoException();
            }
        }
    }
}
