package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.swing.AutoGuiShell;

import java.util.ArrayList;
import java.util.List;

@GuiIncluded
public class EditActionDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new EditActionDemo());
    }

    @GuiIncluded(action = true)
    public Message editMessage() {
        return new Message("Message");
    }

    @GuiIncluded
    public void editedMessage(Message value, boolean ok) {
        if (ok) {
            messages = new ArrayList<>(messages);
            messages.add(value);
        } else {
            System.err.printf("The item <%s> is discarded%n", value);
        }
    }

    protected List<Message> messages = new ArrayList<>();
    @GuiIncluded
    public List<Message> getMessages() {
        return messages;
    }

    @GuiIncluded
    public static class Message {
        protected String message;

        public Message(String message) {
            this.message = message;
        }

        @GuiIncluded
        public String getMessage() {
            return message;
        }

        @GuiIncluded
        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "Message(" + message + ")";
        }
    }


}
