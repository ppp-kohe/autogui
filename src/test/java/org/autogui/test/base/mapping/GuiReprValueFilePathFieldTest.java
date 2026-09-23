package org.autogui.test.base.mapping;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiReprValueFilePathField;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class GuiReprValueFilePathFieldTest {

    GuiReprValueFilePathField fld;

    GuiTypeBuilder builder;
    GuiTypeValue typeFile;
    GuiTypeValue typePath;

    GuiMappingContext contextFile;
    GuiMappingContext contextPath;

    File file;
    Path path;

    public GuiReprValueFilePathFieldTest() {
    }

    @BeforeEach
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
        assertTrue(fld.match(ctx), "match with File");

        GuiMappingContext ctx2 = new GuiMappingContext(typePath, path);
        assertTrue(fld.match(ctx2), "match with Path");
    }

    @Test
    public void testValueUpdate() throws Throwable {
        contextFile.setSource(GuiMappingContext.GuiSourceValue.of(null));
        assertEquals(file, fld.update(contextFile, GuiMappingContext.NO_SOURCE, file, GuiReprValue.NONE), "updated value File obj");
        contextPath.setSource(GuiMappingContext.GuiSourceValue.of(null));
        assertEquals(path, fld.update(contextPath, GuiMappingContext.NO_SOURCE, path, GuiReprValue.NONE), "updated value Path obj");
    }

    @Test
    public void testValueUpdateNull() throws Throwable {
        contextFile.setSource(GuiMappingContext.GuiSourceValue.of(null));
        assertNull(fld.update(contextFile, GuiMappingContext.NO_SOURCE, null, GuiReprValue.NONE), "updated value null");
    }

    @Test
    public void testValueToValueFromPath() {
        assertEquals(file, fld.toValueFromPath(contextFile, file.toPath()), "toValueFromPath with File context converts Path to File");

        assertEquals(path, fld.toValueFromPath(contextPath, path), "toValueFromPath with Path context returns arg Path");
    }

    /// ///////

    @Test
    public void testValueToJson() {
        assertEquals(file.toPath().toString(), fld.toJson(contextFile, file), "toJson returns path of arg");

        assertEquals(path.toString(), fld.toJson(contextPath, path), "toJson returns path of arg");

        assertEquals(path.toFile().getPath(), fld.toJson(contextFile, path), "toJson returns path of arg");
    }

    @Test
    public void testValueToJsonIllegal() {
        assertNull(fld.toJson(contextFile, 123), "toJson returns null for illegal arg");
    }

    @Test
    public void testValueFromJson() {
        assertFalse(fld.isJsonSetter(), "isJsonSetter is false for boolean");
        assertEquals(file, fld.fromJson(contextFile, null, file.getPath()), "fromJson returns File");

        assertEquals(file.toPath(), fld.fromJson(contextPath, null, file.getPath()), "fromJson returns Path");
    }

    @Test
    public void testValueFromJsonIllegal() {
        assertNull(fld.fromJson(contextFile, null, 123), "fromJson returns null for illegal arg");
    }


    @Test
    public void testValueToHumanReadableString() {
        assertEquals(path.toString(), fld.toHumanReadableString(contextFile, path), "toHumanReadableString File returns path of Path");

        assertEquals(file.getPath(), fld.toHumanReadableString(contextFile, file), "toHumanReadableString File returns path of File");

        assertEquals(file.getPath(), fld.toHumanReadableString(contextPath, file), "toHumanReadableString Path returns path of File");

        assertEquals(path.toString(), fld.toHumanReadableString(contextPath, path), "toHumanReadableString Path returns path of Path");
    }

    @Test
    public void testValueToHumanReadableStringNull() {
        assertEquals("null", fld.toHumanReadableString(contextFile, null), "toHumanReadableString returns null for null");
    }

    @Test
    public void testValueFromHumanReadableString() {
        assertEquals(file, fld.fromHumanReadableString(contextFile, file.getPath()), "fromHumanReadableString File returns File");

        assertEquals(file.toPath(), fld.fromHumanReadableString(contextPath, file.getPath()), "fromHumanReadableString Path returns Path");

    }

    @Test
    public void testValueFromHumanReadableStringNull() {
        assertNull(fld.fromHumanReadableString(contextFile, null), "fromHumanReadableString returns null for null");
    }


}
