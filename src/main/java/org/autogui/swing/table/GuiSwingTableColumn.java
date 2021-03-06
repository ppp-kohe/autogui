package org.autogui.swing.table;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue.ObjectSpecifier;
import org.autogui.base.mapping.GuiReprValue.ObjectSpecifierIndex;
import org.autogui.swing.GuiSwingElement;
import org.autogui.swing.GuiSwingPreferences;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingView.SpecifierManager;

import java.util.function.Supplier;

/**
 * the interface for a column factory
 */
public interface GuiSwingTableColumn extends GuiSwingElement {
    ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                   SpecifierManager parentSpecifier);

    /** a specifier factory for a list index: having a mutable index and creating {@link ObjectSpecifierIndex}.
     *  For columns of nested lists, the managers are also nested.
     *    In order to specify the indices Map&lt;SpecifierManagerIndex,Integer&gt; is used.
     *     This means that instances of the class are created for each nested structure
     *      and the identify of them are used. */
    class SpecifierManagerIndex implements SpecifierManager {
        protected Supplier<ObjectSpecifier> tableSpecifier;
        protected int index;

        public SpecifierManagerIndex(Supplier<ObjectSpecifier> tableSpecifier) {
            this.tableSpecifier = tableSpecifier;
        }

        public SpecifierManagerIndex(Supplier<ObjectSpecifier> tableSpecifier, int index) {
            this.tableSpecifier = tableSpecifier;
            this.index = index;
        }

        public ObjectSpecifier getTableSpecifier() {
            return tableSpecifier.get();
        }

        public void setIndex(int index) {
            this.index = index;
        }

        @Override
        public ObjectSpecifier getSpecifier() {
            return new ObjectSpecifierIndex(tableSpecifier.get(), index);
        }

        public ObjectSpecifier getSpecifierWithSettingIndex(int index) {
            setIndex(index);
            return new ObjectSpecifierIndex(tableSpecifier.get(), index);
        }

        @Override
        public String toString() {
            return String.format("[%x]", System.identityHashCode(this));
        }
    }

    /** interface for {@link ObjectTableColumn} */
    interface ObjectTableColumnWithContext
            extends GuiSwingPreferences.PreferencesUpdateSupport, GuiSwingView.SettingsWindowClient {
        GuiMappingContext getContext();

        SpecifierManager getSpecifierManager();

        /**
         * @return always this
         */
        default ObjectTableColumn asColumn() {
            return (ObjectTableColumn) this;
        }

        void loadSwingPreferences(GuiPreferences prefs);

        void saveSwingPreferences(GuiPreferences prefs);
    }
}
