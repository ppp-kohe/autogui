package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.swing.GuiSwingRootPane;
import org.autogui.swing.GuiSwingView;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

public class ObjectEmbeddedDemo {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new ObjectEmbeddedDemo()::run);
    }

    Hello hello;
    File openedFile;

    public void run() {
        JFrame frame = new JFrame("Object Embedded Demo");
        {
            JPanel pane = new JPanel(new BorderLayout());

            hello = new Hello(); //create your object

            GuiSwingRootPane helloPane = GuiSwingRootPane.createForObject(hello);
             //create GUI for the object
            //Note: to customize the pane,
            // you can use GuiSwingRootPane.GuiSwingRootPaneCreator
            //  obtained by GuiSwingRootPane.creator().
            pane.add(helloPane, BorderLayout.NORTH); //adds the created GUI to the pane

            frame.setJMenuBar(helloPane.getMenuBar());
             //you can obtain menu-bar

            GuiSwingView.ValuePane<Object> filePane = helloPane.getChildByName("file");
             // the created pane from the property
             System.err.println("filePane: " + filePane.getClass().getName());

            JTextArea text = new JTextArea(20, 80);
            pane.add(new JScrollPane(text));

            //callback from setter of the property
            hello.setFileCallback(() -> {
                File file = hello.file;

                if (!Objects.equals(openedFile, file)) {
                    if (file != null && !file.toString().isEmpty()) {
                        try {
                            text.setText(String.join("\n", Files.readAllLines(file.toPath())));
                        } catch (Exception ex) {
                            helloPane.getLogWindow().getManager().logError(ex);
                            //display the error
                        }
                    } else {
                        text.setText("");
                    }
                    openedFile = file;
                }
            });

            JButton clearButton = new JButton("Clear");
            clearButton.addActionListener(e -> {
                hello.file = null;
                helloPane.getContext().updateSourceFromRoot();
                 //notifies change of the property to GUI components
            });
            pane.add(clearButton, BorderLayout.SOUTH);

            frame.add(pane);
        }
        frame.pack();
        frame.setVisible(true);
    }

    @GuiIncluded
    public static class Hello {
        private File file;
        private Runnable fileCallback;

        @GuiIncluded
        public File getFile() {
            return file;
        }

        @GuiIncluded
        public void setFile(File file) {
            this.file = file;
            if (fileCallback != null) {
                fileCallback.run();
            }
        }

        public void setFileCallback(Runnable fileCallback) {
            this.fileCallback = fileCallback;
        }

        @GuiIncluded
        public void check() {
            if (file != null && file.exists()) {
                System.err.println(file + ": exists");
            } else {
                System.err.println(file + ": no such file");
            }
        }
    }
}
