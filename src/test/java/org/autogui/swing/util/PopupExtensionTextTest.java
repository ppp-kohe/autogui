package org.autogui.swing.util;

import org.autogui.swing.GuiSwingTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class PopupExtensionTextTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        PopupExtensionTextTest t = new PopupExtensionTextTest();
        t.testCut();
    }

    protected JTextField field;
    protected JFrame frame;
    protected PopupExtensionText ext;

    @After
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
        }
    }

    public void create() {
        field = new JTextField(20);

        ext = PopupExtensionText.installDefault(field);

        frame = createFrame(field);
    }

    @Test
    public void testCut() {
        run(this::create);

        run(() -> field.setText("hello, world"));

        run(() -> field.setSelectionStart(3));
        run(() -> field.setSelectionEnd(10));

        //show menu
        run(() -> field.dispatchEvent(new KeyEvent(field, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_ENTER, '\n')));

        //cut
        run(() -> field.dispatchEvent(new KeyEvent(field, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                PopupExtension.getMenuShortcutKeyMask(), KeyEvent.VK_X, 'x')));

        Assert.assertEquals("after cut",
                "helld",
                runGet(field::getText));

        Assert.assertEquals("after cut: clipboard",
                "lo, wor",
                getClipboardText());
    }

    @Test
    public void testCopy() {
        run(this::create);

        run(() -> field.setText("hello, world"));

        run(() -> field.setSelectionStart(3));
        run(() -> field.setSelectionEnd(10));

        //show menu
        run(() -> field.dispatchEvent(new KeyEvent(field, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_ENTER, '\n')));

        //copy
        run(() -> field.dispatchEvent(new KeyEvent(field, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                PopupExtension.getMenuShortcutKeyMask(), KeyEvent.VK_C, 'x')));

        Assert.assertEquals("after copy",
                "hello, world",
                runGet(field::getText));

        Assert.assertEquals("after copy: clipboard",
                "lo, wor",
                getClipboardText());

    }
}
