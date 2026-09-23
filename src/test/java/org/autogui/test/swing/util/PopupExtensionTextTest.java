package org.autogui.test.swing.util;

import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.util.PopupExtensionText;
import org.autogui.test.swing.GuiSwingTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PopupExtensionTextTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        PopupExtensionTextTest t = new PopupExtensionTextTest();
        t.testCut();
    }

    protected JTextField field;
    protected JFrame frame;
    protected PopupExtensionText ext;

    public PopupExtensionTextTest() {
    }

    @AfterEach
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
        withClipLock(() -> {
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

            assertEquals(
                    "helld",
                    runGet(field::getText),"after cut");

            assertEquals(
                    "lo, wor",
                    getClipboardText(), "after cut: clipboard");
        });
    }

    @Test
    public void testCopy() {
        withClipLock(() -> {
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

            assertEquals(
                    "hello, world",
                    runGet(field::getText),
                    "after copy");

            assertEquals(
                    "lo, wor",
                    getClipboardText(),"after copy: clipboard");
        });
    }
}
