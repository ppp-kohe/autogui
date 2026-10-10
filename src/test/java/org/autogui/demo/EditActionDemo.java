package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.swing.AutoGuiShell;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@GuiIncluded
public class EditActionDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new EditActionDemo());
    }

    public EditActionDemo() {}

    @GuiIncluded(editor = true)
    public Message newMessage() {
        return new Message("Message");
    }

    @GuiIncluded
    public void completeNewMessage(Message value, boolean ok) {
        if (ok) {
            messages = new ArrayList<>(messages);
            messages.add(value);
        } else {
            System.err.printf("The item <%s> is discarded%n", value);
        }
    }

    @GuiIncluded(editor = true)
    public GroupingMessage groupMessages(List<Message> messages) {
        return messages.isEmpty() ? null : new GroupingMessage(messages);
    }

    @GuiIncluded
    public void completeGroupMessages(GroupingMessage msg, boolean ok) {
        if (ok) {
            messages = new ArrayList<>(messages);
            if (msg.isDeleteOriginals()) {
                messages.removeAll(msg.getMessages());
            }
            messages.add(msg.getMessage());
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

    @GuiIncluded
    public static class GroupingMessage {

        protected Message message;
        protected boolean deleteOriginals;
        protected List<Message> messages;

        public GroupingMessage(List<Message> messages) {
            var text = messages.stream()
                    .map(Message::getMessage)
                    .collect(Collectors.joining("\n"));
            this.message = new Message(text);
            this.deleteOriginals = true;
            this.messages = messages;
        }
        @GuiIncluded
        public Message getMessage() {
            return message;
        }
        @GuiIncluded
        public boolean isDeleteOriginals() {
            return deleteOriginals;
        }
        @GuiIncluded
        public void setDeleteOriginals(boolean deleteOriginals) {
            this.deleteOriginals = deleteOriginals;
        }

        public List<Message> getMessages() {
            return messages;
        }

        @GuiIncluded
        public String getInfo() {
            return "Grouping " + messages.size() + " messages";
        }
    }
}
