/*
 * Copyright 2017 Thomas Kuenneth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thomaskuenneth.monicopy;

import java.io.File;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * This is the main class of MoniCopy.
 *
 * @author Thomas Kuenneth
 */
public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final String KEY_FILE_FROM = "fileFrom";
    private static final String KEY_FILE_TO = "fileTo";
    private static final String EMPTY_STRING = "";

    private final FileCopier copier = new FileCopier();
    private final FileStore store = new FileStore();

    private final MD5 md1 = new MD5();
    private final StringBuilder sb1 = new StringBuilder();

    private final MD5 md2 = new MD5();
    private final StringBuilder sb2 = new StringBuilder();

    private final Preferences prefs = Preferences.userNodeForPackage(Main.class);

    private final ResourceBundle messages
            = ResourceBundle.getBundle("com.thomaskuenneth.monicopy.messages");

    private Stage primaryStage = null;
    private File fileFrom = null;
    private File fileTo = null;
    private Button button = null;
    private TextArea ta = null;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        fileFrom = getFileFromPreferences(KEY_FILE_FROM);
        fileTo = getFileFromPreferences(KEY_FILE_TO);
        Text t1 = new Text(getString("string1"));
        Hyperlink t2 = new Hyperlink();
        updateHyperLink(t2, fileFrom);
        t2.setOnAction((ActionEvent event) -> {
            fileFrom = selectDir(fileFrom);
            updateHyperLink(t2, fileFrom);
            updateCopyButton();
            setPreferencesFromFile(fileFrom, KEY_FILE_FROM);
        });
        Text t3 = new Text(getString("to"));
        Hyperlink t4 = new Hyperlink();
        updateHyperLink(t4, fileTo);
        t4.setOnAction((ActionEvent event) -> {
            fileTo = selectDir(fileTo);
            updateHyperLink(t4, fileTo);
            updateCopyButton();
            setPreferencesFromFile(fileTo, KEY_FILE_TO);
        });
        button = new Button(getString("start"));
        button.setOnAction((ActionEvent event) -> {
            ta = new TextArea();
            ta.setEditable(false);
            VBox root = new VBox(ta);
            root.setPadding(new Insets(20, 20, 20, 20));
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);

            Thread t = new Thread(() -> {
                copy(fileFrom, fileTo);
            });
            t.start();
        });
        updateCopyButton();

        HBox box = new HBox(button);
        box.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, t1, t2, t3, t4, box);
        root.setPadding(new Insets(20, 20, 20, 20));
        Scene scene = new Scene(root);
        primaryStage.setTitle(getString("title"));
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
            final File _f = fileToCopy;
            File destination = new File(to,
                    fileToCopy.getAbsolutePath().substring(offset));
            if (mustBeCopied(fileToCopy, destination)) {
                boolean ok = copier.copy(fileToCopy, destination);
                if (!ok) {
                    String msg = String.format(getString("could_not_copy"),
                            _f.getAbsolutePath(),
                            copier.getLastLocalizedMessage());
                    message(msg);
                }
            }
        }
        message(getString("done"));
    }

    private void message(String msg) {
        LOGGER.log(Level.INFO, msg);
        Platform.runLater(() -> {
            ta.appendText(msg);
        });
    }

    private File getFileFromPreferences(String key) {
        File result = null;
        String val = prefs.get(key, EMPTY_STRING);
        if ((val != null) && (val.length() > 0)) {
            result = new File(val);
        }
        return result;
    }

    private String getString(String key) {
        return messages.getString(key);
    }

    private void setPreferencesFromFile(File dir, String key) {
        String val = EMPTY_STRING;
        if (dir != null) {
            val = dir.getAbsolutePath();
        }
        prefs.put(key, val);
    }

    private File selectDir(File current) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("JavaFX Projects");
        chooser.setInitialDirectory(current);
        File selectedDirectory = chooser.showDialog(primaryStage);
        return selectedDirectory;
    }

    private void updateHyperLink(Hyperlink link, File dir) {
        link.setText(dir == null ? "\u2026" : dir.getAbsolutePath());
    }

    private void updateCopyButton() {
        button.setDisable(fileFrom == null || fileTo == null);
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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
