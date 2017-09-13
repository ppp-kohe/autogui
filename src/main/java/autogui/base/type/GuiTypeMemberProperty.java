package autogui.base.type;

import java.util.Collections;
import java.util.List;

public class GuiTypeMemberProperty extends GuiTypeMember {
    protected String setterName;
    protected String getterName;
    protected String fieldName;
    protected GuiTypeElement type;

    public GuiTypeMemberProperty(String name) {
        super(name);
    }

    public GuiTypeMemberProperty(String name, String setterName, String getterName, String fieldName, GuiTypeElement type) {
        super(name);
        this.setterName = setterName;
        this.getterName = getterName;
        this.fieldName = fieldName;
        this.type = type;
    }

    public void setSetterName(String setterName) {
        this.setterName = setterName;
    }

    public String getSetterName() {
        return setterName;
    }

    public void setGetterName(String getterName) {
        this.getterName = getterName;
    }

    public String getGetterName() {
        return getterName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setType(GuiTypeElement type) {
        this.type = type;
    }

    public GuiTypeElement getType() {
        return type;
    }

    @Override
    public List<GuiTypeElement> getChildren() {
        if (type == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(type);
        }
    }
}
