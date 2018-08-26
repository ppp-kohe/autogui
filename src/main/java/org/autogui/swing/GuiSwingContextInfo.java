package org.autogui.swing;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.swing.util.MenuBuilder;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.base.type.*;

/**
 * a label factory for context target info.
 */
public class GuiSwingContextInfo {
    protected static GuiSwingContextInfo instance = new GuiSwingContextInfo();

    public static GuiSwingContextInfo get() {
        return instance;
    }

    public MenuBuilder.MenuLabel getInfoLabel(GuiMappingContext context) {
        return MenuBuilder.get().createLabel(getInfo(context), PopupCategorized.SUB_CATEGORY_LABEL_TYPE);
    }

    public String getInfo(GuiMappingContext context) {
        GuiTypeElement e = context.getTypeElement();
        return getName(e);
    }

    public String getName(GuiTypeElement e) {
        if (e == null) {
            return "?";
        } else if (e instanceof GuiTypeCollection) {
            return getNameCollection((GuiTypeCollection) e);
        } else if (e instanceof GuiTypeValue) {
            return getName(e.getName());
        } else if (e instanceof GuiTypeMember) {
            return getNameMember((GuiTypeMember) e);
        } else {
            return e.toString();
        }
    }

    public String getNameCollection(GuiTypeCollection collection) {
        return getName(collection.getName()) + "<" + getName(collection.getElementType()) + ">";
    }

    public String getName(String str) {
        int i = str.lastIndexOf(".");
        if (i != -1) {
            return str.substring(i + 1);
        } else {
            return str;
        }
    }

    public String getNameMember(GuiTypeMember m) {
        if (m instanceof GuiTypeMemberAction) {
            if (m instanceof GuiTypeMemberActionList) {
                return getNameActionList((GuiTypeMemberActionList) m);
            } else {
                return getNameAction((GuiTypeMemberAction) m);
            }
        } else if (m instanceof GuiTypeMemberProperty) {
            return getNameProperty((GuiTypeMemberProperty) m);
        } else {
            return m.getName();
        }
    }

    public String getNameAction(GuiTypeMemberAction m) {
        String mName = m.getName() + "()";
        if (m.getOwner() != null) {
            return getName(m.getOwner()) + "." + mName;
        } else {
            return mName;
        }
    }

    public String getNameActionList(GuiTypeMemberActionList m) {
        String mName = m.getName() + "(" + "List<" + getName(m.getElementType()) + ">" + ")";
        if (m.getOwner() != null) {
            return getName(m.getOwner());
        } else {
            return mName;
        }
    }

    public String getNameProperty(GuiTypeMemberProperty p) {
        String typeName = getName(p.getType());
        if (p.getOwner() != null) {
            return typeName + " " + getName(p.getOwner()) + "." + p.getName();
        } else {
            return typeName + " " + p.getName();
        }
    }
}
