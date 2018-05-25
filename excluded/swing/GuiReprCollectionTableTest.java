package autogui.base.excluded;

import autogui.GuiIncluded;
import autogui.base.mapping.*;
import autogui.base.type.GuiTypeBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.Callable;

@SuppressWarnings("unchecked")
public class GuiReprCollectionTableTest {

    public GuiReprValue.ObjectSpecifier getSpecifier() {
        return GuiReprValue.NONE;
    }


    @SuppressWarnings("all")
    @Test
    public void testList() {
        GuiMappingContext context = new GuiMappingContext(new GuiTypeBuilder().get(TestList.class)) {
            @Override
            public <T> T execute(Callable<T> task) throws Throwable {
                return task.call();
            }
        };
        Assert.assertTrue(GuiRepresentation.getDefaultSet().match(context));
        context.setPreferences(new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context));

        TestList list = new TestList(); //class TestList { List<String> stringList; //[a,b,c] }
        context.setSource(list);
        context.updateSourceFromRoot();

        GuiMappingContext prop = context.getChildren().stream()
                .filter(e -> e.getName().equals("stringList"))
                .findFirst().orElse(null);

        GuiMappingContext ctx = prop.getChildren().get(0); //GMC(stringList,CollectionTable(CollectionElement(StringField)))

        GuiReprCollectionTable table = (GuiReprCollectionTable) ctx.getRepresentation();

        Assert.assertFalse(table.isJsonSetter());

        Assert.assertEquals(Collections.emptyList(), table.toUpdateValue(context, null));
        Assert.assertEquals(Arrays.asList("a", "b", "c"), table.toUpdateValue(context, list.stringList));

        List<String> json = (List<String>) table.toJson(ctx, Arrays.asList("hello", "world"));
        Assert.assertEquals(Arrays.asList("hello", "world"), json);

        List<String> listObj = (List<String>) table.fromJson(ctx, null, Arrays.asList("hello", "world"));
        Assert.assertEquals(Arrays.asList("hello", "world"), listObj);

        Assert.assertEquals("hello\nworld", table.toHumanReadableString(ctx, Arrays.asList("hello", "world")));

        GuiMappingContext elementCtx = ctx.getChildren().get(0);
        GuiReprCollectionElement elem = (GuiReprCollectionElement) elementCtx.getRepresentation();

        GuiMappingContext elementValueCtx = elementCtx.getChildren().get(0);
        GuiReprValueStringField elemValue = (GuiReprValueStringField) elementValueCtx.getReprValue();

        GuiReprValue.ObjectSpecifier elemValueSpec =
                GuiReprValue.NONE
                        .child(false) //TestList
                        .child(false) //List
                        .childIndex(1) //get(0)
                        .child(false); //String

        try {
            Assert.assertEquals(3,
                    elem.getValueCollectionSize(elementCtx, list.stringList,
                            elemValueSpec.getParent().getParent()));
        } catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail();
        }

        Object e = null;
        try {
            e = elemValue.getUpdatedValueWithoutNoUpdate(elementValueCtx, elemValueSpec);
        } catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail();
        }
        Assert.assertEquals("b", e);

        elemValue.updateFromGui(elementValueCtx, "hello", elemValueSpec);

        Assert.assertEquals("hello", list.stringList.get(1));

        Assert.assertEquals(1, elem.getFixedColumnSize(elementCtx));
        Assert.assertEquals(0, elem.getFixedColumnIndex(elementCtx, elementValueCtx));

    }

    @SuppressWarnings("all")
    @Test
    public void testListObj() {
        GuiMappingContext context = new GuiMappingContext(new GuiTypeBuilder().get(TestListObj.class));
        Assert.assertTrue(GuiRepresentation.getDefaultSet().match(context));
        context.setPreferences(new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context));

        TestListObj list = new TestListObj();
        context.setSource(list);
        context.updateSourceFromRoot();


        GuiMappingContext prop = context.getChildren().stream()
                .filter(e -> e.getName().equals("list"))
                .findFirst().orElse(null);

        GuiMappingContext ctx = prop.getChildren().get(0);
        GuiReprCollectionTable table = (GuiReprCollectionTable) ctx.getRepresentation();


        List<TestElement> es = Arrays.asList(new TestElement("hello", 123), new TestElement("world", 456));
        List<Map<String,Object>> json = (List<Map<String,Object>>) table.toJson(ctx,
                es);
        Map<String,Object> m1 = json.get(0);
        Assert.assertEquals("hello", m1.get("str"));
        Assert.assertEquals(123, m1.get("i"));
        Map<String,Object> m2 = json.get(1);
        Assert.assertEquals("world", m2.get("str"));
        Assert.assertEquals(456, m2.get("i"));


        List<TestElement> es2 = (List<TestElement>) table.fromJson(ctx, null, json);
        Assert.assertEquals("hello", es2.get(0).str);
        Assert.assertEquals(123, es2.get(0).i);
        Assert.assertEquals("world", es2.get(1).str);
        Assert.assertEquals(456, es2.get(1).i);

        String str= table.toHumanReadableString(ctx, es);
        Assert.assertEquals("hello\t123\nworld\t456", str);

        GuiMappingContext elemCtx = ctx.getChildren().get(0);
        GuiReprCollectionElement elem = (GuiReprCollectionElement) elemCtx.getRepresentation();

        GuiMappingContext strCtx = elemCtx.getChildren().stream()
                .filter(e -> e.getName().equals("i"))
                .findFirst().orElse(null);

        //TODO elem.updateCellFromGui(elemCtx, strCtx, es.get(0), 0, 1, 1000);
        Assert.assertEquals(1000, es.get(0).i);

        try {
            //TODO Object v = elem.getCellValue(elemCtx, strCtx, es.get(0), 0, 1);
            //TODO Assert.assertEquals(1000, v);
        } catch (Throwable ex) {
            Assert.fail();
        }
    }

    @GuiIncluded
    public static class TestList {
        @GuiIncluded
        public List<String> stringList = new ArrayList<>(Arrays.asList("a", "b", "c"));
    }

    @GuiIncluded
    public static class TestListObj {
        @GuiIncluded
        public List<TestElement> list = new ArrayList<>();
    }

    @GuiIncluded
    public static class TestElement {
        @GuiIncluded(index = 0) public String str;
        @GuiIncluded(index = 1) public int i;

        public TestElement() {}

        public TestElement(String str, int i) {
            this.str = str;
            this.i = i;
        }

        @Override
        public String toString() {
            return "E(" + str + "," + i + ")";
        }
    }
}
