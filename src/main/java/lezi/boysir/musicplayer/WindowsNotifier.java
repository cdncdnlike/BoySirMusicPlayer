package lezi.boysir.musicplayer;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Font;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.AWTException;
import java.io.IOException;
import javafx.application.Platform;
import javafx.stage.Stage;

/** Uses the Windows shell notification balloon through the desktop tray API. */
public final class WindowsNotifier {
    private static TrayIcon appTrayIcon;
    private static MenuItem playPauseItem;

    private WindowsNotifier() {}

    /** Installs the app's persistent Windows notification-area icon and menu. */
    public static void installAppTray(Stage stage, Runnable exitAction, Runnable restoreAction,
                                      Runnable playPauseAction, Runnable previousAction, Runnable nextAction) {
        if (!SystemTray.isSupported() || appTrayIcon != null) return;
        try {
            Image image = ImageIO.read(WindowsNotifier.class.getResourceAsStream("app-logo.png"));
            PopupMenu menu = new PopupMenu();
            MenuItem showMain = new MenuItem("Show Main Window");
            MenuItem previous = new MenuItem("Previous");
            MenuItem playPause = new MenuItem("Play");
            MenuItem next = new MenuItem("Next");
            playPauseItem = playPause;
            MenuItem exit = new MenuItem("Exit");
            Font menuFont = new Font("Microsoft YaHei UI", Font.PLAIN, 12);
            menu.setFont(menuFont);
            showMain.setFont(menuFont);
            previous.setFont(menuFont);
            playPause.setFont(menuFont);
            next.setFont(menuFont);
            exit.setFont(menuFont);
            showMain.addActionListener(event -> Platform.runLater(() -> { showMainWindow(stage); restoreAction.run(); }));
            previous.addActionListener(event -> Platform.runLater(previousAction));
            playPause.addActionListener(event -> Platform.runLater(playPauseAction));
            next.addActionListener(event -> Platform.runLater(nextAction));
            exit.addActionListener(event -> Platform.runLater(exitAction));
            menu.add(showMain);
            menu.addSeparator();
            menu.add(previous);
            menu.add(playPause);
            menu.add(next);
            menu.addSeparator();
            menu.add(exit);

            appTrayIcon = new TrayIcon(image, "BoySir Music Player", menu);
            appTrayIcon.setImageAutoSize(true);
            appTrayIcon.addActionListener(event -> Platform.runLater(() -> { showMainWindow(stage); restoreAction.run(); }));
            SystemTray.getSystemTray().add(appTrayIcon);
        } catch (IOException | AWTException | IllegalArgumentException | NullPointerException ignored) {
            appTrayIcon = null;
        }
    }

    public static void removeAppTray() {
        if (appTrayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(appTrayIcon);
            appTrayIcon = null;
        }
        playPauseItem = null;
    }

    /** Updates the tray play/pause label to reflect the current playback state. */
    public static void setPlaybackPlaying(boolean playing) {
        MenuItem item = playPauseItem;
        if (item != null) java.awt.EventQueue.invokeLater(() -> item.setLabel(playing ? "Pause" : "Play"));
    }

    private static void showMainWindow(Stage stage) {
        if (!stage.isShowing()) stage.show();
        stage.setIconified(false);
        stage.toFront();
        stage.requestFocus();
    }

    public static void downloadFinished(String name) {
        show("下载完成", name);
    }

    public static void show(String title, String message) {
        if (appTrayIcon != null) {
            appTrayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
            return;
        }
        try {
            if (!SystemTray.isSupported()) return;
            Image image = ImageIO.read(WindowsNotifier.class.getResourceAsStream("app-logo.png"));
            TrayIcon icon = new TrayIcon(image, "BoySir Music");
            icon.setImageAutoSize(true);
            SystemTray tray = SystemTray.getSystemTray();
            tray.add(icon);
            icon.displayMessage(title, message, TrayIcon.MessageType.INFO);
            new Thread(() -> {
                try { Thread.sleep(5000); tray.remove(icon); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }, "download-notification-cleanup").start();
        } catch (IOException | AWTException | IllegalArgumentException | NullPointerException ignored) {
            // Notifications are optional and must not affect a successful download.
        }
    }
}
