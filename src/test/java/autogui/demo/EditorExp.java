package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.StyledDocument;

@GuiIncluded
public class EditorExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new EditorExp());
    }


    StyledPane styledPane = new StyledPane();
    PlainPane plainPane = new PlainPane();
    StringBuilderPane builderPane = new StringBuilderPane();

    @GuiIncluded
    public StyledPane getStyledPane() {
        return styledPane;
    }

    @GuiIncluded
    public PlainPane getPlainPane() {
        return plainPane;
    }

    @GuiIncluded
    public StringBuilderPane getBuilderPane() {
        return builderPane;
    }

    @GuiIncluded
    public static class StyledPane {
        StyledDocument doc;

        @GuiIncluded
        public StyledDocument getDoc() {
            if (doc == null) {
                doc = new DefaultStyledDocument();
            }
            return doc;
        }
    }

    @GuiIncluded
    public static class PlainPane {
        Document doc;

        @GuiIncluded
        public Document getDoc() {
            if (doc == null) {
                doc = new PlainDocument();
            }
            return doc;
        }
    }

    @GuiIncluded
    public static class StringBuilderPane {
        StringBuilder builder = new StringBuilder();

        @GuiIncluded
        public StringBuilder getBuilder() {
            return builder;
        }
    }
}
