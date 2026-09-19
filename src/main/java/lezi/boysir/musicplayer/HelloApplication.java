package lezi.boysir.musicplayer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.File;
import java.util.Arrays;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        javafx.application.Platform.setImplicitExit(false);
        loadIconFonts();
        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();
        HelloController controller = new HelloController();
        Scene scene = new Scene(controller.view(), 1180, 760);
        scene.getStylesheets().add(HelloApplication.class.getResource("app.css").toExternalForm());
        stage.setTitle("BoySir Music Player (development version)");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        var iconUrl = HelloApplication.class.getResource("app-logo.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }
        Runnable exitApplication = () -> {
            controller.shutdown();
            WindowsNotifier.removeAppTray();
            javafx.application.Platform.exit();
            System.exit(0);
        };
        WindowsNotifier.installAppTray(stage, exitApplication, controller::restoreFloatingLyrics,
                controller::togglePlayback, controller::playPrevious, controller::playNext);
        stage.iconifiedProperty().addListener((observable, wasIconified, iconified) -> {
            if (iconified) controller.keepFloatingLyricsVisible();
        });
        stage.setOnCloseRequest(event -> {
            if (controller.shouldMinimizeToTray()) {
                event.consume();
                stage.hide();
                controller.keepFloatingLyricsVisible();
            } else {
                exitApplication.run();
            }
        });
        stage.show();
        if (getParameters() != null && getParameters().getRaw() != null && !getParameters().getRaw().isEmpty()) {
            controller.openFiles(getParameters().getRaw().stream().map(String::trim).filter(s -> !s.isBlank()).toList());
        }
    }
    /** Loads the bundled Font Awesome icon fonts so the transport/favorite glyphs render
     *  identically on every machine instead of depending on system icon fonts. */
    private void loadIconFonts() {
        for (String path : new String[]{"fonts/fa-solid-900.ttf", "fonts/fa-regular-400.ttf"}) {
            try (var in = HelloApplication.class.getResourceAsStream(path)) {
                if (in != null) javafx.scene.text.Font.loadFont(in, 12);
            } catch (Exception ignored) { }
        }
    }

    public static void main(String[] args) { launch(args); }
}
