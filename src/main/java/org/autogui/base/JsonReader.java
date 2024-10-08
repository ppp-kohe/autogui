package org.autogui.base;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** a simple JSON reader.
 *  <pre>
 *      Object value = JsonReader.create(jsonSource).parseValue();
 *  </pre>
 * */
public class JsonReader {
    protected String source;
    protected int index;
    protected int lineNumber;
    protected int columnNumber;
    protected int sourceLength;

    public static Object read(File file) {
        if (file.exists()) {
            try{
                var bytes = Files.readAllBytes(file.toPath());
                var str = new String(bytes, StandardCharsets.UTF_8);
                return JsonReader.create(str).parseValue();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }


    public static JsonReader create(String source) {
        return new JsonReader(source);
    }

    public JsonReader(String source) {
        this.source = source;
        this.sourceLength = (source == null ? 0 : source.length());
        index = 0;
        lineNumber = 0;
        columnNumber = 0;
    }

    public Object parseValue() {
        eatSpaces();
        if (!hasNext()) {
            throw error("[empty]");
        }
        char c = next();
        switch (c) {
            case '\"':
                return parseString();
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return parseNumber();
            case 't':
                if (eat("true")) {
                    return Boolean.TRUE;
                } else {
                    throw error(c);
                }
            case 'f':
                if (eat("false")) {
                    return Boolean.FALSE;
                } else {
                    throw error(c);
                }
            case 'n':
                if (eat("null")) {
                    return null;
                } else {
                    throw error(c);
                }
            default:
                throw error(c);
        }
    }
    public RuntimeException error(char c) {
        return error("'" + c + "'");
    }

    public RuntimeException error(String c) {
        return new RuntimeException("JSON parsing error " + c +
                " at " + getCurrentLineInfo());
    }

    public String getCurrentLineInfo() {
        int start = Math.max(0, index - columnNumber);
        int len = (source == null ? 0 : Math.max(0, source.indexOf('\n', index))) - start;
        String targetLine = "";
        if (len > 80) {
            targetLine = " : '" + (source == null ? "" : source.substring(start, start + 80)) + "...'";
        } else if (len > 0) {
            targetLine = " : '" + (source == null ? "" : source.substring(start, start + len)) + "'";
        }
        return "line " + lineNumber + " column " + columnNumber + targetLine;
    }

    public String parseString() {
        eatSpaces();
        if (!eat('"')) {
            throw error("No \"");
        }
        StringBuilder buf = new StringBuilder();
        while (canEatNextOrEat('"')) {
            if (eat('\\')) {
                char c = eatNext();
                switch (c) {
                    case 'b':
                        buf.append("\b");
                        break;
                    case 'f':
                        buf.append("\f");
                        break;
                    case 'n':
                        buf.append("\n");
                        break;
                    case 'r':
                        buf.append("\r");
                        break;
                    case 't':
                        buf.append("\t");
                        break;
                    case 'u':
                        int i = Integer.parseInt("" +
                                eatNext() +
                                eatNext() +
                                eatNext() +
                                eatNext(), 16);
                        buf.append((char) i);
                        break;
                    default:
                        buf.append(c);
                }
            } else {
                buf.append(eatNext());
            }
        }
        return buf.toString();
    }

    public Map<String, Object> parseObject() {
        eatSpaces();
        if (eat('{')) {
            HashMap<String, Object> obj = new HashMap<>();
            eatSpaces();
            while (canEatNextOrEat('}')) {
                String str = parseString();
                eatSpaces();
                if (!eat(':')) {
                    throw error("No :");
                }
                Object v = parseValue();
                obj.put(str, v);
                eatSpaces();
                eat(','); //
                eatSpaces();
            }
            return obj;
        } else {
            return null;
        }
    }

    public List<Object> parseArray() {
        eatSpaces();
        if (eat('[')) {
            List<Object> ary = new ArrayList<>();
            eatSpaces();
            while (canEatNextOrEat(']')) {
                ary.add(parseValue());
                eatSpaces();
                eat(','); //non-strict parsing
                eatSpaces();
            }
            return ary;
        } else {
            return null;
        }
    }

    public Number parseNumber() {
        eatSpaces();
        StringBuilder buf = new StringBuilder();
        if (eat('-')) {
            buf.append('-');
        }
        int width = 0;
        if (eat('0')) {
            buf.append('0');
            ++width;
        } else {
            while (canEatDigit()) {
                buf.append(eatNext());
                ++width;
            }
        }
        boolean floating = false;
        if (eat('.')) {
            buf.append('.');
            floating = true;
            while (canEatDigit()) {
                buf.append(eatNext());
                ++width;
            }
        }
        boolean exp = false;
        if (eat('e') || eat('E')) {
            exp = true;
            buf.append('E');
            if (eat('+')) {
                buf.append('+');
            } else if (eat('-')) {
                buf.append('-');
            }
            while (canEatDigit()) {
                buf.append(eatNext());
            }
        }
        String numStr = buf.toString();
        if (!floating && !exp) {
            if (width > 18) {
                return new BigInteger(numStr);
            } else if (width > 9) {
                try {
                    return Long.parseLong(numStr);
                } catch (NumberFormatException nfe) {
                    return new BigInteger(numStr);
                }
            } else {
                try {
                    return Integer.parseInt(numStr);
                } catch (NumberFormatException nfe) {
                    return Long.parseLong(numStr);
                }
            }
        } else {
            if (width > 15) {
                return new BigDecimal(numStr);
            } else if (width > 6) {
                double d = Double.parseDouble(numStr);
                if (Double.isFinite(d)) {
                    return new BigDecimal(numStr);
                } else {
                    return d;
                }
            } else {
                float f = Float.parseFloat(numStr);
                if (Float.isFinite(f)) {
                    return new BigDecimal(numStr);
                } else {
                    return f;
                }
            }
        }
    }

    public char next() {
        return source.charAt(index);
    }

    public boolean hasNext() {
        return index < sourceLength;
    }

    public void eatSpaces() {
        while (hasNext()) {
            char c = next();
            if (c == ' ' || c == '\n' || c == '\t' || c == '\r') {
                proceedNext(c);
            } else {
                break;
            }
        }
    }

    public boolean eat(char c) {
        if (hasNext()) {
            if (next() == c) {
                proceedNext(c);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * @param str must not contain a newline
     * @return true if successfully eaten the str
     */
    public boolean eat(String str) {
        if (hasNext() && source.startsWith(str, index)) {
            index += str.length();
            return true;
        } else {
            return false;
        }
    }

    public char eatNext() {
        if (hasNext()) {
            char c = next();
            proceedNext(c);
            return c;
        } else {
            throw error("EOF");
        }
    }

    public boolean canEatNextOrEat(char c) {
        if (hasNext()) {
            return !eat(c);
        } else {
            throw error("No " + c);
        }
    }

    public boolean canEat(char c) {
        return hasNext() && next() == c;
    }

    public boolean canEatDigit() {
        if (hasNext()) {
            char c = next();
            return '0' <= c && c <= '9';
        } else {
            return false;
        }
    }

    public void proceedNext(char theNextChar) {
        ++index;
        if (theNextChar == '\n') {
            ++lineNumber;
            columnNumber = 0;
        } else {
            ++columnNumber;
        }
    }
}