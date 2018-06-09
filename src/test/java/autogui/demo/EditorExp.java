package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;

import javax.swing.text.*;

@GuiIncluded
public class EditorExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new EditorExp());
    }


    PlainPane plainPane = new PlainPane();
    StyledPane styledPane = new StyledPane();
    ContentPane contentPane = new ContentPane();
    StringBuilderPane builderPane = new StringBuilderPane();

    @GuiIncluded(index = 0)
    public PlainPane getPlainPane() {
        return plainPane;
    }

    @GuiIncluded(index = 1)
    public StyledPane getStyledPane() {
        return styledPane;
    }

    @GuiIncluded(index = 2)
    public ContentPane getContentPane() {
        return contentPane;
    }

    @GuiIncluded(index = 3)
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
    public static class ContentPane {
        AbstractDocument.Content content;

        @GuiIncluded
        public AbstractDocument.Content getContent() {
            if (content == null) {
                content = new StringContent(); //the content becomes read-only. changes from non-GUI code will break the undo-manager of the view
            }
            return content;
        }
    }

    @GuiIncluded
    public static class StringBuilderPane {
        StringBuilder builder = new StringBuilder();

        @GuiIncluded
        public StringBuilder getBuilder() {
            return builder; //the builder becomes read-only. changes from non-GUI code will break the text-view
        }

        @GuiIncluded
        public void clear() {
            builder = new StringBuilder(); //we can change the builder instance itself.
        }

    }
}
