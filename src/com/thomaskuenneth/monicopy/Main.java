/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thomaskuenneth.monicopy;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 *
 * @author tkuen
 */
public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static TextArea ta = null;

    private final FileCopier copier = new FileCopier();
    private final FileStore store = new FileStore();

    private final MD5 md1 = new MD5();
    private final StringBuilder sb1 = new StringBuilder();

    private final MD5 md2 = new MD5();
    private final StringBuilder sb2 = new StringBuilder();

    @Override
    public void start(Stage primaryStage) {
        Text t1 = new Text("Copy all files and folders inside");
        Text t2 = new Text("...");
        Text t3 = new Text("to");
        Text t4 = new Text("...");
        Button button = new Button("Start");
        button.setOnAction((ActionEvent event) -> {
            ta = new TextArea();
            VBox root = new VBox(ta);
            root.setPadding(new Insets(20, 20, 20, 20));
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);

            File from = new File("C:\\Users\\tkuen");
            File to = new File("D:\\");
            Thread t = new Thread(() -> {
                copy(from, to);
            });
            t.start();
        });
        HBox box = new HBox(button);
        box.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, t1, t2, t3, t4, box);
        root.setPadding(new Insets(20, 20, 20, 20));
        Scene scene = new Scene(root);
        primaryStage.setTitle("MoniCopy");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void copy(File from, File to) {
        int offset = from.getAbsolutePath().length() + 1;
        store.fill(from);
        File fileToCopy;
        while (((fileToCopy = store.poll()) != null)
                || (store.isFilling())) {
            if (fileToCopy == null) {
                continue;
            }
            File destination = new File(to,
                    fileToCopy.getAbsolutePath().substring(offset));
            message(String.format("processing %s",
                    fileToCopy.getAbsolutePath()));
            if (mustBeCopied(fileToCopy, destination)) {
                boolean ok = copier.copy(fileToCopy, destination);
                if (!ok) {
                    message("error");
                }
            }
        }
    }

    private synchronized boolean mustBeCopied(File fileToCopy, File destination) {
        if (!destination.exists()) {
            return true;
        }
        long lenFileToCopy = fileToCopy.length();
        long lenDestination = destination.length();
        if (lenFileToCopy != lenDestination) {
            return true;
        }
        Thread t1 = new Thread(() -> {
            sb1.setLength(0);
            sb1.append(md1.getChecksum(fileToCopy));
        });
        Thread t2 = new Thread(() -> {
            sb2.setLength(0);
            sb2.append(md2.getChecksum(destination));
        });
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "interruption while joining threads", e);
            return true;
        }
        boolean notEqual = !sb1.toString().equals(sb2.toString());
        return notEqual;
    }

    public static void message(String msg) {
        if (ta == null) {
            System.out.println(msg);
        } else {
            Platform.runLater(() -> {
                ta.appendText(msg);
                ta.appendText("\n");
            });
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
