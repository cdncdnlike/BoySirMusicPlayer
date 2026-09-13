module lezi.boysir.musicplayer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.net.http;
    requires java.desktop;
    requires MaterialFX;

    opens lezi.boysir.musicplayer to javafx.fxml;
    exports lezi.boysir.musicplayer;
}
