package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingView;

import java.util.function.Supplier;

/**
 * a column factory
 */
public interface GuiSwingTableColumn extends GuiSwingElement {
    ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                   GuiSwingView.SpecifierManager parentSpecifier);


    class SpecifierManagerIndex implements GuiSwingView.SpecifierManager {
        protected Supplier<GuiReprValue.ObjectSpecifier> tableSpecifier;
        protected int index;

        public SpecifierManagerIndex(Supplier<GuiReprValue.ObjectSpecifier> tableSpecifier) {
            this.tableSpecifier = tableSpecifier;
        }

        public SpecifierManagerIndex(Supplier<GuiReprValue.ObjectSpecifier> tableSpecifier, int index) {
            this.tableSpecifier = tableSpecifier;
            this.index = index;
        }

        public GuiReprValue.ObjectSpecifier getTableSpecifier() {
            return tableSpecifier.get();
        }

        public void setIndex(int index) {
            this.index = index;
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return new GuiReprValue.ObjectSpecifierIndex(tableSpecifier.get(), index);
        }

        public GuiReprValue.ObjectSpecifier getSpecifierWithSettingIndex(int index) {
            setIndex(index);
            return new GuiReprValue.ObjectSpecifierIndex(tableSpecifier.get(), index);
        }
    }

    /** interface for {@link ObjectTableColumn} */
    interface ObjectTableColumnWithContext {
        GuiMappingContext getContext();
    }
}
