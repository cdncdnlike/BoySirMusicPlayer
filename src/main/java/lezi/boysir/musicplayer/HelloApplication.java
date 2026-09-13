package lezi.boysir.musicplayer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        javafx.application.Platform.setImplicitExit(false);
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
        WindowsNotifier.installAppTray(stage, exitApplication, controller::restoreFloatingLyrics);
        stage.setOnCloseRequest(event -> {
            if (controller.shouldMinimizeToTray()) {
                event.consume();
                stage.hide();
            } else {
                exitApplication.run();
            }
        });
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
