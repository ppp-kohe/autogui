package org.autogui.base;

import java.io.File;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * a simple JSON writer.
 * <pre>
 *     String jsonSource = JsonWriter.create().write(obj).toSource();
 * </pre>
 */
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
        switch (o) {
            case null -> emit("null");
            case Map<?, ?> map -> writeMap(map);
            case String s -> writeString(s);
            case List<?> objects -> writeArray(objects);
            case Number number -> writeNumber(number);
            case Boolean b -> emit(o.toString());
            default -> writeObject(o);
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
            error(ex);
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
        return "\n\r\f\t\b\"'\\".indexOf(c) != -1;
    }
    protected char toEscape(char c) {
        return switch (c) {
            case '\n' -> 'n';
            case '\r' -> 'r';
            case '\f' -> 'f';
            case '\t' -> 't';
            case '\b' -> 'b';
            default -> c;
        };
    }
    public String toSource() {
        return source.toString();
    }
}
