/*
 * Copyright 2017 - 2023 Thomas Kuenneth
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.Preferences;

public class Main extends Application implements Pausable {

    public static final String VERSION =
            ResourceBundle.getBundle("version").getString("VERSION");

    private static final Logger LOGGER = Logger.getGlobal();
    private static final String KEY_CANNOT_READ = "cannot_read";
    private static final String KEY_CANNOT_WRITE = "cannot_write";
    private static final String KEY_FILE_FROM = "fileFrom";
    private static final String KEY_FILE_TO = "fileTo";
    private static final String KEY_IGNORES = "ignores";
    private static final String DELETE_ORPHANS = "deleteOrphanedFiles";
    private static final String KEY_NO_OVERLAP = "no_overlap";
    private static final String EMPTY_STRING = "";
    private static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance();

    private enum STATE {
        IDLE, COPYING, COPY_PAUSED, DELETING, DELETE_PAUSED, FINISHED
    }

    private STATE state;

    private final FileCopier copier = new FileCopier();

    private final MD5 mdFrom = new MD5();
    private final StringBuilder sbFrom = new StringBuilder();

    private final MD5 mdTo = new MD5();
    private final StringBuilder sbTo = new StringBuilder();

    private final Preferences prefs = Preferences.userNodeForPackage(Main.class);

    private final ResourceBundle messages
            = ResourceBundle.getBundle("com.thomaskuenneth.monicopy.messages");

    private final Object lock = new Object();

    private final ObservableList<String> ignores = FXCollections.observableArrayList();

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
        updateIgnoresFromPreferences();
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
        root = new BorderPane(new HBox(20, center, createIgnores()));
        root.setPadding(new Insets(20, 20, 20, 20));
        root.setBottom(bottom);
        Scene scene = new Scene(root);
        primaryStage.setTitle(String.format("%s %s",
                getString("title"), VERSION));
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("app.png")));
        primaryStage.setOnCloseRequest((event) -> {
            updatePreferencesFromIgnores();
            System.exit(0);
        });
        primaryStage.show();
    }

    private Pane createIgnores() {
        final var listView = new ListView(ignores);
        final var model = listView.getSelectionModel();
        model.setSelectionMode(SelectionMode.MULTIPLE);
        var add = new Button(getString("add_ignore"));
        add.setOnAction((ActionEvent event) -> {
            File result = selectDir(fileFrom, getString("add_ignored_directory"));
            if ((result != null) && (!ignores.contains(result.getAbsolutePath()))) {
                ignores.add(result.getAbsolutePath());
            }
        });
        HBox.setHgrow(add, Priority.ALWAYS);
        add.setMaxWidth(Double.MAX_VALUE);
        var delete = new Button(getString("delete_ignore"));
        delete.setDisable(model.getSelectedItems().size() < 1);
        delete.setOnAction((ActionEvent event) -> {
            ignores.removeAll(model.getSelectedItems());
        });
        model.selectedItemProperty().addListener((ov, oldValue, newValue) -> {
            delete.setDisable(newValue == null);
        });
        HBox.setHgrow(delete, Priority.ALWAYS);
        delete.setMaxWidth(Double.MAX_VALUE);
        var buttons = new VBox(10, add, delete);
        var box = new HBox(10, listView, buttons);
        return new VBox(2, new Text(getString("ignored_directories")), box);
    }

    private void updatePreferencesFromIgnores() {
        prefs.put(KEY_IGNORES, String.join("\n", ignores));
    }

    private void updateIgnoresFromPreferences() {
        var lines = prefs.get(KEY_IGNORES, "");
        for (var line : lines.split("\n")) {
            var dir = new File(line);
            if (dir.isDirectory()) {
                ignores.add(line);
            }
        }
    }

    private CheckBox createAndConfigureCheckBox() {
        var cb = new CheckBox(getString(DELETE_ORPHANS));
        cb.setSelected(prefs.getBoolean(DELETE_ORPHANS, false));
        cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            prefs.putBoolean(DELETE_ORPHANS, newVal);
        });
        return cb;
    }

    private Button createAndConfigureButton() {
        var b = new Button();
        b.setOnAction((ActionEvent event) -> {
            switch (state) {
                case IDLE -> {
                    state = STATE.COPYING;
                    ta = new TextArea();
                    ta.setEditable(false);
                    root.setCenter(ta);
                    copy(fileFrom, fileTo);
                }
                case COPYING -> state = STATE.COPY_PAUSED;
                case COPY_PAUSED -> {
                    state = STATE.COPYING;
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
                case DELETING -> state = STATE.DELETE_PAUSED;
                case DELETE_PAUSED -> {
                    state = STATE.DELETING;
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
                case FINISHED -> Platform.exit();
            }
            updateCopyButton();
        });
        updateCopyButton();
        return b;
    }

    private void copy(File from, File to) {
        Thread t = new Thread(() -> {
            int offset = from.getAbsolutePath().length() + 1;
            message(getString("started_copying"));
            message(getString("find_files"));
            FileStore store = new FileStore(this);
            List<File> files = store.fill(from, ignores);
            if (files == null) {
                return;
            }
            long numberOfFiles = store.getNumberOfFiles();
            long numberOfProcessedFiles = 0;
            float ratio = (float) 100 / (float) numberOfFiles;
            message(String.format(getString("number_of_files_and_directories"),
                    numberOfFiles,
                    // base directory should not be counted
                    store.getNumberOfDirectories() - 1));
            int lastPrinted = -1;
            for (File fileToCopy : files) {
                checkForPause();
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
                int percent = (int) (ratio * (float) ++numberOfProcessedFiles);
                if ((percent % 10) == 0) {
                    if (lastPrinted != percent) {
                        message(String.format("%d percent done", percent));
                        lastPrinted = percent;
                    }
                }
            }
            message(getString("finished_copying"));
            nextStep();
        });
        t.start();
    }

    private void deleteOrphans(File sourceDir, File destiDir) {
        Thread t = new Thread(() -> {
            int offset = destiDir.getAbsolutePath().length();
            message(getString("started_deleting"));
            FileStore store = new FileStore(this);
            List<File> files = store.fill(destiDir, ignores);
            if (files == null) {
                return;
            }
            for (File fileToDelete : files) {
                checkForPause();
                final String filename = fileToDelete.getAbsolutePath();
                if (filename.charAt(offset) == File.separatorChar) {
                    offset += 1;
                }
                String name = filename.substring(offset);
                File sourceFile = new File(sourceDir, name);
                if (!sourceFile.exists()) {
                    boolean deleted = fileToDelete.delete();
                    if (!deleted) {
                        message(String.format(getString("could_not_delete"),
                                filename, sourceFile.getAbsolutePath()));
                    }
                }
            }
            deleteOrphanedDirs(destiDir);
            message(getString("finished_deleting"));
            nextStep();
        });
        t.start();
    }

    private void deleteOrphanedDirs(File base) {
        FolderMap folders = new FolderMap();
        folders.fill(base);
        Iterator<File> i = folders.getIterator();
        while (i.hasNext()) {
            checkForPause();
            File f = i.next();
            String absolutePath = f.getAbsolutePath();
            if (!f.isDirectory()) {
                LOGGER.log(Level.SEVERE,
                        String.format("%s is not a directory", absolutePath));
                continue;
            }
            String[] files = f.list();
            if ((files != null) && (files.length == 0)) {
                LOGGER.log(Level.INFO,
                        String.format("deleting directory %s", absolutePath));
                boolean ok = f.delete();
                if (!ok) {
                    message(String.format("could not delete %s", absolutePath));
                }
            }
        }
    }

    private void nextStep() {
        switch (state) {
            case COPYING -> {
                if (cbDelOrphanedFiles.isSelected()) {
                    state = STATE.DELETING;
                    deleteOrphans(fileFrom, fileTo);
                } else {
                    state = STATE.FINISHED;
                }
            }
            case DELETING -> state = STATE.FINISHED;
        }
        updateCopyButton();
    }

    @Override
    public void checkForPause() {
        synchronized (lock) {
            if ((state == STATE.COPY_PAUSED) || (state == STATE.DELETE_PAUSED)) {
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
                fileFrom.mkdirs();
                fileTo.mkdirs();
                if (!fileFrom.canRead()) {
                    strWarning = getString(KEY_CANNOT_READ);
                    disable = true;
                } else if (!fileTo.canWrite()) {
                    strWarning = getString(KEY_CANNOT_WRITE);
                    disable = true;
                } else {
                    disable = fileTo.getAbsolutePath().contains(fileFrom.getAbsolutePath());
                    if (disable) {
                        strWarning = getString(KEY_NO_OVERLAP);
                    }
                }
            }
            warning.setText(strWarning);
            button.setDisable(disable);
            switch (state) {
                case IDLE -> button.setText(getString("start"));
                case COPYING, DELETING -> button.setText(getString("pause"));
                case COPY_PAUSED, DELETE_PAUSED -> button.setText(getString("continue"));
                case FINISHED -> button.setText(getString("close"));
                default -> throw new IllegalStateException("unhandled state: " + state);
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
                    "not found in destination");
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
            final String checksum = mdFrom.getChecksum(fileToCopy);
            if (checksum != null) {
                sbFrom.append(checksum);
            }
        });
        Thread tTo = new Thread(() -> {
            sbTo.setLength(0);
            final String checksum = mdTo.getChecksum(destination);
            if (checksum != null) {
                sbTo.append(checksum);
            }
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
            try {
                var succeeded = destination.setLastModified(fileToCopy.lastModified());
                LOGGER.log(Level.INFO, String.format("%s setLastModified(): %b",
                        destination.getAbsolutePath(), succeeded));
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.SEVERE, "setLastModified()", e);
            }
        }
        return copy;
    }

    public static void main(String[] args) {
        // FIXME: wrap this in a if macos...
        System.setProperty("prism.order", "j2d");
        try {
            File f = new File(System.getProperty("user.home", "."), "MoniCopy.log");
            FileHandler handler = new FileHandler(f.getAbsolutePath(), false);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
            LOGGER.addHandler(handler);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not create file handler", e);
        }
        launch(args);
    }
}
