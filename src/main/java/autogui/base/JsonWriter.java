package autogui.base;

import java.io.File;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class JsonWriter {
    protected Appendable source;
    protected int level;
    protected boolean newLines = true;

    public static void write(Object json, File file) {
        write(json, file.toPath());
    }
    
    public static void write(Object json, Path file) {
        try (Writer w = Files.newBufferedWriter(file)) {
            new JsonWriter(w).write(json);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static JsonWriter create() {
        return new JsonWriter();
    }

    public JsonWriter(Appendable source) {
        this.source = source;
    }
    
    public JsonWriter() {
        this(new StringBuilder(4096));
    }

    public JsonWriter withNewLines(boolean newLines) {
        this.newLines = newLines;
        return this;
    }

    public JsonWriter write(Object o) {
        if (o == null) {
            emit("null");
        } else if (o instanceof Map<?,?>) {
            writeMap((Map<?,?>) o);
        } else if (o instanceof String) {
            writeString((String) o);
        } else if (o instanceof List<?>) {
            writeArray((List<?>) o);
        } else if ( o instanceof Number) {
            writeNumber((Number) o);
        } else if (o instanceof Boolean) {
            emit(o.toString());
        } else {
            writeObject(o);
        }
        return this;
    }
    protected void writeObject(Object o) {
        writeString(o.toString());
    }

    public JsonWriter writeMap(Map<?,?> m) {
        emit('{'); 
        emitIndentUp();
        boolean first = true;
        for (Map.Entry<?,?> e : m.entrySet()) {
            if (first) {
                first = false;
            } else {
                emit(',');
                emitIndentReturn();
            }
            writeString((String) e.getKey());
            emit(':');
            write(e.getValue());
        }
        emitIndentDown();
        emit('}');
        return this;
    }
    public JsonWriter writeString(String s) {
        emit('"');
        for (char c : s.toCharArray()) {
            if (isEscape(c)) {
                emit('\\');
                emit(toEscape(c));
            } else {
                emit(c);
            }
        }
        emit('"');
        return this;
    }
    public JsonWriter writeArray(List<?> a) {
        emit('[');
        boolean first = true;
        for (Object o : a) {
            if (first) {
                first = false;
            } else {
                emit(',');
            }
            write(o);
        }
        emit(']');
        return this;
    }
    public JsonWriter writeNumber(Number n) {
        emit(n.toString());
        return this;
    }
    protected void emit(String str) {
        try {
            source.append(str);
        } catch (Exception ex) {
            error(ex);
        }
    }
    protected void emit(char c) {
        try{
            source.append(c);
        } catch (Exception ex) {
            error(ex);;
        }
    }

    protected void error(Exception ex) {
        throw new RuntimeException(ex);
    }

    protected void emitIndentUp() {
        level++;
        emitIndentReturn();
    }
    protected void emitIndentDown() {
        level--;
        emitIndentReturn();
    }
    protected void emitIndentReturn() {
        if (newLines) {
            try {
                source.append('\n');
                for (int i = 0; i < level; ++i) {
                    source.append("  ");
                }
            } catch (Exception ex) {
                error(ex);
            }
        }
    }
    protected boolean isEscape(char c) {
        return "\n\r\f\t\b\"\'\\".indexOf(c) != -1;
    }
    protected char toEscape(char c) {
        switch (c) {
        case '\n':
            return 'n';
        case '\r':
            return 'r';
        case '\f':
            return 'f';
        case '\t':
            return 't';
        case '\b':
            return 'b';
        default:
            return c;
        }
    }
    public String toSource() {
        return source.toString();
    }
}
