package org.autogui.base.mapping;

import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GuiReprValueFilePathFieldTest {

    GuiReprValueFilePathField fld;

    GuiTypeBuilder builder;
    GuiTypeValue typeFile;
    GuiTypeValue typePath;

    GuiMappingContext contextFile;
    GuiMappingContext contextPath;

    File file;
    Path path;

    @Before
    public void setUp() {
        fld = new GuiReprValueFilePathField();

        builder = new GuiTypeBuilder();
        typeFile = (GuiTypeValue) builder.get(File.class);
        typePath = (GuiTypeValue) builder.get(Path.class);

        file = new File("src", "main");
        path = Paths.get("src", "main", "java");

        contextFile = new GuiReprObjectPaneTest.GuiMappingContextForDebug(typeFile, fld);
        contextPath = new GuiReprObjectPaneTest.GuiMappingContextForDebug(typePath, fld);
    }


    @Test
    public void testValueMatch() {
        GuiMappingContext ctx = new GuiMappingContext(typeFile, file);
        Assert.assertTrue("match with File",
                fld.match(ctx));

        GuiMappingContext ctx2 = new GuiMappingContext(typePath, path);
        Assert.assertTrue("match with Path",
                fld.match(ctx2));
    }

    @Test
    public void testValueUpdate() throws Throwable {
        contextFile.setSource(GuiMappingContext.GuiSourceValue.of(null));
        Assert.assertEquals("updated value File obj",
                file,
                fld.update(contextFile, GuiMappingContext.NO_SOURCE,
                        file, GuiReprValue.NONE));
        contextPath.setSource(GuiMappingContext.GuiSourceValue.of(null));
        Assert.assertEquals("updated value Path obj",
                path,
                fld.update(contextPath, GuiMappingContext.NO_SOURCE,
                        path, GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdateNull() throws Throwable {
        contextFile.setSource(GuiMappingContext.GuiSourceValue.of(null));
        Assert.assertNull("updated value null",
                fld.update(contextFile, GuiMappingContext.NO_SOURCE,
                        null, GuiReprValue.NONE));
    }

    @Test
    public void testValueToValueFromPath() {
        Assert.assertEquals("toValueFromPath with File context converts Path to File",
                file,
                fld.toValueFromPath(contextFile, file.toPath()));

        Assert.assertEquals("toValueFromPath with Path context returns arg Path",
                path,
                fld.toValueFromPath(contextPath, path));
    }
    //////////

    @Test
    public void testValueToJson() {
        Assert.assertEquals("toJson returns path of arg",
                file.toPath().toString(),
                fld.toJson(contextFile, file));

        Assert.assertEquals("toJson returns path of arg",
                path.toString(),
                fld.toJson(contextPath, path));

        Assert.assertEquals("toJson returns path of arg",
                path.toFile().getPath(),
                fld.toJson(contextFile, path));
    }

    @Test
    public void testValueToJsonIllegal() {
        Assert.assertNull("toJson returns null for illegal arg",
                fld.toJson(contextFile, 123));
    }

    @Test
    public void testValueFromJson() {
        Assert.assertFalse("isJsonSetter is false for boolean",
                fld.isJsonSetter());
        Assert.assertEquals("fromJson returns File",
                file,
                fld.fromJson(contextFile, null, file.getPath()));

        Assert.assertEquals("fromJson returns Path",
                file.toPath(),
                fld.fromJson(contextPath, null, file.getPath()));
    }

    @Test
    public void testValueFromJsonIllegal() {
        Assert.assertNull("fromJson returns null for illegal arg",
                fld.fromJson(contextFile, null, 123));
    }


    @Test
    public void testValueToHumanReadableString() {
        Assert.assertEquals("toHumanReadableString File returns path of Path",
                path.toString(),
                fld.toHumanReadableString(contextFile, path));

        Assert.assertEquals("toHumanReadableString File returns path of File",
                file.getPath(),
                fld.toHumanReadableString(contextFile, file));

        Assert.assertEquals("toHumanReadableString Path returns path of File",
                file.getPath(),
                fld.toHumanReadableString(contextPath, file));

        Assert.assertEquals("toHumanReadableString Path returns path of Path",
                path.toString(),
                fld.toHumanReadableString(contextPath, path));
    }

    @Test
    public void testValueToHumanReadableStringNull() {
        Assert.assertEquals("toHumanReadableString returns null for null",
                "null",
                fld.toHumanReadableString(contextFile, null));
    }

    @Test
    public void testValueFromHumanReadableString() {
        Assert.assertEquals("fromHumanReadableString File returns File",
                file,
                fld.fromHumanReadableString(contextFile, file.getPath()));

        Assert.assertEquals("fromHumanReadableString Path returns Path",
                file.toPath(),
                fld.fromHumanReadableString(contextPath, file.getPath()));

    }

    @Test
    public void testValueFromHumanReadableStringNull() {
        Assert.assertNull("fromHumanReadableString returns null for null",
                fld.fromHumanReadableString(contextFile, null));
    }


}
