package org.autogui.swing.util;

import org.autogui.base.mapping.ScheduledTaskRunner;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.util.List;
import java.util.function.Consumer;

/** the subclass of the executor with implementing various listeners */
public class EditingRunner extends ScheduledTaskRunner<Object>
        implements DocumentListener, KeyListener, ActionListener, FocusListener, ChangeListener, InputMethodListener {
    public EditingRunner(long delay, Consumer<List<Object>> consumer) {
        super(delay, consumer);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        schedule(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        schedule(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        schedule(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        schedule(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        schedule(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        schedule(e);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //enter the key
        runImmediately(e);
    }

    @Override
    public void focusGained(FocusEvent e) {
        //nothing happen
    }

    @Override
    public void focusLost(FocusEvent e) {
        //leave the editing target
        runImmediately(e);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        schedule(e);
    }

    @Override
    public void inputMethodTextChanged(InputMethodEvent event) {
        schedule(event);
    }

    @Override
    public void caretPositionChanged(InputMethodEvent event) {
        schedule(event);
    }
}
