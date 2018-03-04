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
import java.text.DateFormat;
import java.util.Date;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
    private static final String KEY_DELETE_ORPHANED_FILES = "deleteOrphanedFiles";
    private static final String KEY_NO_OVERLAP = "no_overlap";
    private static final String EMPTY_STRING = "";
    private static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance();

    private enum STATE {
        IDLE, COPYING, COPY_PAUSED, DELETING, DELETE_PAUSED, FINISHED
    }
    private STATE state;

    private final FileCopier copier = new FileCopier();
    private final FileStore store = new FileStore();

    private final MD5 mdFrom = new MD5();
    private final StringBuilder sbFrom = new StringBuilder();

    private final MD5 mdTo = new MD5();
    private final StringBuilder sbTo = new StringBuilder();

    private final Preferences prefs = Preferences.userNodeForPackage(Main.class);

    private final ResourceBundle messages
            = ResourceBundle.getBundle("com.thomaskuenneth.monicopy.messages");

    private final Object lock = new Object();

    private Stage primaryStage = null;
    private File fileFrom = null;
    private File fileTo = null;
    private Button button = null;
    private TextArea ta = null;
    private BorderPane root = null;
    private CheckBox cbDelOrphanedFiles = null;
    private Text warning = null;

    @Override
    public void start(Stage primaryStage) {
        state = STATE.IDLE;
        this.primaryStage = primaryStage;
        fileFrom = getFileFromPreferences(KEY_FILE_FROM);
        fileTo = getFileFromPreferences(KEY_FILE_TO);
        Text t1 = new Text(getString("string1"));
        Hyperlink t2 = new Hyperlink();
        updateHyperLink(t2, fileFrom);
        t2.setOnAction((ActionEvent event) -> {
            File result = selectDir(fileFrom, getString("sourceDir"));
            if (result != null) {
                fileFrom = result;
                updateHyperLink(t2, fileFrom);
                updateCopyButton();
                setPreferencesFromFile(fileFrom, KEY_FILE_FROM);
            }
        });
        Text t3 = new Text(getString("to"));
        Hyperlink t4 = new Hyperlink();
        updateHyperLink(t4, fileTo);
        t4.setOnAction((ActionEvent event) -> {
            File result = selectDir(fileTo, getString("destinationDir"));
            if (result != null) {
                fileTo = result;
                updateHyperLink(t4, fileTo);
                updateCopyButton();
                setPreferencesFromFile(fileTo, KEY_FILE_TO);
            }
        });

        cbDelOrphanedFiles = createAndConfigureCheckBox();
        button = createAndConfigureButton();

        warning = new Text(EMPTY_STRING);
        warning.setFill(Color.RED);
        VBox texts = new VBox(t1, t2, t3, t4, warning);
        texts.setPadding(new Insets(0, 0, 20, 0));

        VBox center = new VBox(texts, cbDelOrphanedFiles);
        HBox bottom = new HBox(button);
        BorderPane.setMargin(bottom, new Insets(20, 0, 0, 0));
        bottom.setAlignment(Pos.CENTER);
        root = new BorderPane(center);
        root.setPadding(new Insets(20, 20, 20, 20));
        root.setBottom(bottom);
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle(getString("title"));
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("app.png")));
        primaryStage.show();
    }

    private CheckBox createAndConfigureCheckBox() {
        CheckBox cb = new CheckBox(getString("delete_orphaned_files"));
        cb.setSelected(prefs.getBoolean(KEY_DELETE_ORPHANED_FILES, false));
        cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            prefs.putBoolean(KEY_DELETE_ORPHANED_FILES, newVal);
        });
        return cb;
    }

    private Button createAndConfigureButton() {
        Button b = new Button();
        b.setOnAction((ActionEvent event) -> {
            switch (state) {
                case IDLE:
                    state = STATE.COPYING;
                    ta = new TextArea();
                    ta.setEditable(false);
                    root.setCenter(ta);
                    copy(fileFrom, fileTo);
                    break;
                case COPYING:
                    state = STATE.COPY_PAUSED;
                    break;
                case DELETING:
                    state = STATE.DELETE_PAUSED;
                    break;
                case COPY_PAUSED:
                    state = STATE.COPYING;
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                    break;
                case DELETE_PAUSED:
                    state = STATE.DELETING;
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                    break;
                case FINISHED:
                    Platform.exit();
                    break;
            }
            updateCopyButton();
        });
        updateCopyButton();
        return b;
    }

    private void copy(File from, File to) {
        Thread t = new Thread(() -> {
            int offset = from.getAbsolutePath().length() + 1;
            store.fill(from);
            File fileToCopy;
            message(getString("started_copying"));
            while (((fileToCopy = store.poll()) != null)
                    || (store.isFilling())) {
                if (fileToCopy == null) {
                    continue;
                }
                synchronized (lock) {
                    if (state == STATE.COPY_PAUSED) {
                        try {
                            LOGGER.log(Level.INFO, "pausing");
                            lock.wait();
                        } catch (InterruptedException ex) {
                            LOGGER.log(Level.SEVERE, "interruption while waiting to resume", ex);
                        } finally {
                            LOGGER.log(Level.INFO, "resuming");
                        }
                    }
                }
                File destination = new File(to,
                        fileToCopy.getAbsolutePath().substring(offset));
                if (mustBeCopied(fileToCopy, destination)) {
                    boolean ok;
                    boolean readFromBuffer = mdFrom.canReadFromBuffer();
                    LOGGER.log(Level.INFO,
                            String.format("copying %s (readFromBuffer is %b)",
                                    fileToCopy.getAbsolutePath(), readFromBuffer));
                    if (readFromBuffer) {
                        ok = copier.copy(mdFrom.getBuffer(),
                                mdFrom.getLengthOfFile(),
                                destination);
                    } else {
                        ok = copier.copy(fileToCopy, destination);
                    }
                    if (!ok) {
                        String msg = String.format(getString("could_not_copy"),
                                fileToCopy.getAbsolutePath(),
                                copier.getLastLocalizedMessage());
                        message(msg);
                    } else {
                        destination.setLastModified(fileToCopy.lastModified());
                    }
                } else {
                    LOGGER.log(Level.INFO, "no need to copy");
                }
            }
            message(getString("finished_copying"));
            nextStep();
        });
        t.start();
    }

    private void deleteOrphanedFiles(File _from, File _to) {
        final File from = _to;
        final File to = _from;
        if (!store.isEmpty()) {
            LOGGER.log(Level.SEVERE, "file store not empty");
            nextStep();
            return;
        }
        Thread t = new Thread(() -> {
            int offset = from.getAbsolutePath().length();
            store.fill(from);
            File fileToDelete;
            message(getString("started_deleting"));
            while (((fileToDelete = store.poll()) != null)
                    || (store.isFilling())) {
                if (fileToDelete == null) {
                    continue;
                }
                synchronized (lock) {
                    if (state == STATE.COPY_PAUSED) {
                        try {
                            LOGGER.log(Level.INFO, "pausing");
                            lock.wait();
                        } catch (InterruptedException ex) {
                            LOGGER.log(Level.SEVERE, "interruption while waiting to resume", ex);
                        } finally {
                            LOGGER.log(Level.INFO, "resuming");
                        }
                    }
                }
                final String filename = fileToDelete.getAbsolutePath();
                if (filename.charAt(offset) == File.separatorChar) {
                    offset += 1;
                }
                String name = filename.substring(offset);
                File sourceFile = new File(to,
                        name);
                if (!sourceFile.exists()) {
                    boolean deleted = fileToDelete.delete();
                    if (!deleted) {
                        message(String.format(getString("could_not_delete"),
                                filename, sourceFile.getAbsolutePath()));
                    }
                }
            }
            message(getString("finished_deleting"));
            nextStep();
        });
        t.start();
    }

    private void nextStep() {
        switch (state) {
            case COPYING:
                if (cbDelOrphanedFiles.isSelected()) {
                    state = STATE.DELETING;
                    deleteOrphanedFiles(fileFrom, fileTo);
                } else {
                    state = STATE.FINISHED;
                }
                break;
            case DELETING:
                state = STATE.FINISHED;
                break;
        }
        updateCopyButton();
    }

    private void message(String msg) {
        LOGGER.log(Level.INFO, msg);
        Platform.runLater(() -> {
            String time = TIME_FORMAT.format(new Date());
            ta.appendText(String.format(getString("message_template"), time, msg));
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

    private File selectDir(File current, String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        if (current != null) {
            if (!current.exists() || !current.isDirectory()) {
                current = null;
            }
        }
        chooser.setInitialDirectory(current);
        File selectedDirectory = chooser.showDialog(primaryStage);
        return selectedDirectory;
    }

    private void updateHyperLink(Hyperlink link, File dir) {
        link.setText(dir == null ? "\u2026" : dir.getAbsolutePath());
    }

    private void updateCopyButton() {
        Platform.runLater(() -> {
            boolean disable = fileFrom == null || fileTo == null;
            String strWarning = "";
            if (!disable) {
                disable = fileTo.getAbsolutePath().contains(fileFrom.getAbsolutePath());
                if (disable) {
                    strWarning = getString(KEY_NO_OVERLAP);
                }
            }
            warning.setText(strWarning);
            button.setDisable(disable);
            switch (state) {
                case IDLE:
                    button.setText(getString("start"));
                    break;
                case COPYING:
                case DELETING:
                    button.setText(getString("pause"));
                    break;
                case COPY_PAUSED:
                case DELETE_PAUSED:
                    button.setText(getString("continue"));
                    break;
                case FINISHED:
                    button.setText(getString("close"));
                    break;
                default:
                    throw new IllegalStateException("unhandled state: " + state);
            }
        });
    }

    private synchronized boolean mustBeCopied(File fileToCopy, File destination) {
        LOGGER.log(Level.INFO,
                String.format("preparing to copy %s",
                        fileToCopy.getAbsolutePath()));
        mdFrom.reset();
        if (!destination.exists()) {
            LOGGER.log(Level.INFO,
                    String.format("not found in destination"));
            return true;
        }
        long lenFileToCopy = fileToCopy.length();
        long lenDestination = destination.length();
        if (lenFileToCopy != lenDestination) {
            LOGGER.log(Level.INFO,
                    String.format("different size in destination: %d != %d",
                            lenFileToCopy, lenDestination));
            return true;
        }
        long lastModifiedfileToCopy = fileToCopy.lastModified();
        long lastModifieddestination = destination.lastModified();
        if (lastModifiedfileToCopy == lastModifieddestination) {
            return false;
        }
        LOGGER.log(Level.INFO,
                String.format("different modification date: %tc != %tc",
                        lastModifiedfileToCopy, lastModifieddestination));
        Thread tFrom = new Thread(() -> {
            sbFrom.setLength(0);
            sbFrom.append(mdFrom.getChecksum(fileToCopy));
        });
        Thread tTo = new Thread(() -> {
            sbTo.setLength(0);
            sbTo.append(mdTo.getChecksum(destination));
        });
        tFrom.start();
        tTo.start();
        try {
            tFrom.join();
            tTo.join();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "interruption while joining threads", e);
            return true;
        }
        String strSbFrom = sbFrom.toString();
        String strSbTo = sbTo.toString();
        boolean copy = !strSbFrom.equals(strSbTo);
        if (copy) {
            LOGGER.log(Level.INFO, String.format("different md5 hashes: %s != %s",
                    strSbFrom, strSbTo));
        } else {
            destination.setLastModified(fileToCopy.lastModified());
        }
        return copy;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
