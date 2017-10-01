package autogui.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonReader {
    protected String source;
    protected int index;
    protected int sourceLength;

    public static Object read(File file) {
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));) {
                String line;
                StringBuilder buf = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    buf.append(line);
                }

                String data = buf.toString();
                return JsonReader.create(data).parseValue();
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
    }

    public Object parseValue() {
        eatSpaces();
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
                eat("true");
                return Boolean.TRUE;
            case 'f':
                eat("false");
                return Boolean.FALSE;
            case 'n':
                eat("null");
                return null;
            default:
                throw new RuntimeException();
        }
    }

    public String parseString() {
        eatSpaces();
        eat('"');
        StringBuilder buf = new StringBuilder();
        while (!eat('"')) {
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
                        int i = Integer.parseInt(new StringBuilder()
                                .append(eatNext())
                                .append(eatNext())
                                .append(eatNext())
                                .append(eatNext()).toString(), 16);
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
            HashMap<String, Object> obj = new HashMap<String, Object>();
            eatSpaces();
            while (!eat('}')) {
                String str = parseString();
                eatSpaces();
                eat(':');
                Object v = parseValue();
                obj.put(str, v);
                eatSpaces();
                eat(',');
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
            List<Object> ary = new ArrayList<Object>();
            eatSpaces();
            while (!eat(']')) {
                ary.add(parseValue());
                eatSpaces();
                eat(',');
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
        if (!floating && !exp) {
            if (width > 18) {
                return new BigInteger(buf.toString());
            } else if (width > 9) {
                return Long.parseLong(buf.toString());
            } else {
                return Integer.parseInt(buf.toString());
            }
        } else {
            if (width > 6) {
                return Double.parseDouble(buf.toString());
            } else {
                return Float.parseFloat(buf.toString());
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
            if (c == ' ' || c == '\n' || c == '\t') {
                proceedNext();
            } else {
                break;
            }
        }
    }

    public boolean eat(char c) {
        if (hasNext()) {
            if (next() == c) {
                proceedNext();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

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
            proceedNext();
            return c;
        } else {
            return (char) 0;
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

    public void proceedNext() {
        ++index;
    }
}