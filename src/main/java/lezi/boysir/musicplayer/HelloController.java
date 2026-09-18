package lezi.boysir.musicplayer;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXToggleButton;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import io.github.palexdev.materialfx.controls.MFXSlider;
import io.github.palexdev.materialfx.enums.FloatMode;
import io.github.palexdev.materialfx.enums.SliderEnums.SliderMode;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.FileChooser;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.nio.file.Path;
import java.io.File;
import java.awt.Desktop;
import java.nio.file.Files;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Collectors;

public class HelloController {
    private final AppState savedState = AppState.load();
    private final MusicApi api = new MusicApi();
    private final ObservableList<Song> songs = FXCollections.observableArrayList();
    private final ObservableList<Song> localSongs = FXCollections.observableArrayList();
    private final ListView<Song> list = new ListView<>(songs);
    private final VBox lyricLines = new VBox(14);
    private final List<LyricEntry> lyricEntries = new ArrayList<>();
    private ScrollPane lyricScroll;
    private final MFXProgressSpinner searchSpinner = new MFXProgressSpinner();
    private final MFXProgressBar updateProgress = new MFXProgressBar();
    private final Label title = new Label("还没有播放歌曲"), artist = new Label("搜索歌曲开始播放"), status = new Label("准备就绪");
    private final Label coverFallback = new Label("♫");
    private final ImageView cover = new ImageView();
    // The transport controls intentionally use the lightweight JavaFX slider
    // style used by the original player. Settings sliders remain MFXSlider.
    private final Slider progress = new Slider(0, 1, 0);
    private final Label elapsed = new Label("0:00"), duration = new Label("0:00"), volumePercent = new Label("80%");
    private final MFXToggleButton showTranslation = new MFXToggleButton("翻译");
    private final StackPane centerHost = new StackPane();
    private final Slider volume = new Slider(0, 1, .8);
    private final MFXComboBox<Double> speed = new MFXComboBox<>();
    private final MFXComboBox<String> source = new MFXComboBox<>(FXCollections.observableArrayList("网易云", "腾讯云"));
    private Stage floatingStage;
    private boolean shuttingDown;
    private Label floatingLyric, floatingTranslation;
    private double floatDragX, floatDragY;
    private final MFXSlider floatingSize = new MFXSlider();
    private final MFXSlider translationSize = new MFXSlider();
    private final MFXSlider lyricOffsetSlider = new MFXSlider();
    private final MFXComboBox<String> sleepTimerSelect = new MFXComboBox<>(FXCollections.observableArrayList("关闭", "15 分钟", "30 分钟", "45 分钟", "60 分钟", "当前歌曲结束"));
    private final ColorPicker floatingColor = new ColorPicker(Color.WHITE);
    private final ColorPicker translationColor = new ColorPicker(Color.rgb(235,235,235));
    private final MFXSlider floatingOpacity = new MFXSlider();
    private final MFXToggleButton floatingTop = new MFXToggleButton("始终置顶");
    private final MFXToggleButton minimizeToTray = new MFXToggleButton("关闭时最小化到托盘");
    private final MFXToggleButton autoStart = new MFXToggleButton("开机启动");
    private final MFXToggleButton keyboardShortcuts = new MFXToggleButton("启用键盘快捷键");
    private final MFXTextField shortcutPlayField = new MFXTextField();
    private final MFXTextField shortcutPreviousField = new MFXTextField();
    private final MFXTextField shortcutNextField = new MFXTextField();
    private final MFXComboBox<String> themeSelect = new MFXComboBox<>(FXCollections.observableArrayList("跟随系统", "明亮", "黑暗"));
    private final MFXButton backgroundButton = new MFXButton("选择背景图片");
    private MFXButton floatingToggleButton;
    private BorderPane rootPane;
    private final ObservableList<Song> recentSongs = FXCollections.observableArrayList();
    private final ObservableList<Song> favoriteSongs = FXCollections.observableArrayList();
    private final ObservableList<Song> historySongs = FXCollections.observableArrayList();
    private final MFXButton favorite = new MFXButton("♡");
    private final Label saying = new Label("加载一言…");
    private final Button shuffle = new MFXButton("⤨"), repeat = new MFXButton("↻");
    private final Button toggle = new MFXButton("▶");
    private MediaPlayer player;
    private Song currentSong;
    private long playGeneration;
    private boolean shuffleEnabled;
    private int repeatMode;
    private Timeline sleepTimeline;

    public HelloController() {
        configureSliders();
        loadSavedFloatingSettings();
        favoriteSongs.addAll(savedState.favorites);
        localSongs.addAll(savedState.localSongs);
        recentSongs.addAll(savedState.queue); deduplicate(favoriteSongs); deduplicate(recentSongs); if (!recentSongs.isEmpty()) currentSong = recentSongs.get(0);
        historySongs.addAll(savedState.history); deduplicate(historySongs);
        if (savedState.autoStart) WindowsStartup.setEnabled(true);
        favoriteSongs.addListener((javafx.collections.ListChangeListener<Song>) change -> {
            savedState.favorites.clear();
            savedState.favorites.addAll(favoriteSongs);
            savedState.save();
        });
        recentSongs.addListener((javafx.collections.ListChangeListener<Song>) change -> {
            savedState.queue.clear();
            savedState.queue.addAll(recentSongs);
            savedState.save();
        });
        localSongs.addListener((javafx.collections.ListChangeListener<Song>) change -> {
            savedState.localSongs.clear();
            savedState.localSongs.addAll(localSongs);
            savedState.save();
        });
        historySongs.addListener((javafx.collections.ListChangeListener<Song>) change -> { savedState.history.clear(); savedState.history.addAll(historySongs); savedState.save(); });
        lyricOffsetSlider.setValue(savedState.lyricOffset);
        lyricOffsetSlider.valueProperty().addListener((o, a, b) -> { savedState.lyricOffset = b.doubleValue(); savedState.save(); if (player != null) updateLyric(player.getCurrentTime().toSeconds()); });
        sleepTimerSelect.setValue(sleepTimerLabel(savedState.sleepTimerMinutes));
        sleepTimerSelect.setText(sleepTimerSelect.getValue());
        sleepTimerSelect.setOnAction(e -> configureSleepTimer(sleepTimerValue(sleepTimerSelect.getValue())));
        configureShortcutField(shortcutPlayField, savedState.shortcutPlay, value -> { savedState.shortcutPlay = value; savedState.save(); });
        configureShortcutField(shortcutPreviousField, savedState.shortcutPrevious, value -> { savedState.shortcutPrevious = value; savedState.save(); });
        configureShortcutField(shortcutNextField, savedState.shortcutNext, value -> { savedState.shortcutNext = value; savedState.save(); });
    }

    private void configureSliders() {
        configureSlider(floatingOpacity, 0, 1, 1, SliderMode.DEFAULT, false, 0.01);
        configureSlider(floatingSize, 18, 44, 28, SliderMode.SNAP_TO_TICKS, false, 1);
        configureSlider(translationSize, 10, 28, 16, SliderMode.SNAP_TO_TICKS, false, 1);
        configureSlider(lyricOffsetSlider, -5, 5, 0, SliderMode.SNAP_TO_TICKS, true, 1);
    }

    private void configureSlider(MFXSlider slider, double min, double max, double value, SliderMode mode, boolean bidirectional, double tickUnit) {
        // Keep the order documented by MaterialFX: min, max, then value.
        slider.setMin(min);
        slider.setMax(max);
        slider.setValue(Math.max(min, Math.min(max, value)));
        slider.setSliderMode(mode);
        slider.setBidirectional(bidirectional);
        slider.setAnimateOnPress(true);
        slider.setTickUnit(tickUnit);
        // Keep snapping behavior without drawing the font-based tick glyphs.
        // On systems where the MaterialFX icon font is unavailable those glyphs
        // appear as square blocks along the track.
        slider.setShowMajorTicks(false);
        slider.setShowMinorTicks(false);
        slider.setShowTicksAtEdges(true);
        slider.setDecimalPrecision(tickUnit < 1 ? 2 : 0);
        // Use the built-in MaterialFX value popup and its fade animation.
        // Its supplier is installed by the MFXSlider constructor.
        slider.setThumbSupplier(() -> {
            // MFXSliderSkin expects a sized container as the draggable thumb.
            // Keep that container, but use only one geometric circle: no radius
            // glyph and no ripple node that could render a square placeholder.
            Circle thumb = new Circle(6.5);
            Circle thumbRadius = new Circle(15);
            thumb.getStyleClass().setAll("thumb", "clean-slider-thumb");
            thumbRadius.getStyleClass().setAll("thumb-radius", "clean-slider-thumb-radius");
            thumb.setMouseTransparent(true);
            thumbRadius.setMouseTransparent(true);
            StackPane container = new StackPane(thumbRadius, thumb);
            container.getStyleClass().add("thumb-container");
            container.setFocusTraversable(false);
            container.setMinSize(13, 13);
            container.setPrefSize(13, 13);
            container.setMaxSize(13, 13);
            return container;
        });
        // MFXSlider's skin binds the track/bar height to the control height. The
        // official skin uses a 6px control height; the thumb remains larger and
        // provides the visible hit target.
        slider.setMinHeight(6);
        slider.setPrefHeight(6);
        slider.setMaxHeight(6);
        slider.getStyleClass().add("player-slider");
    }

    private void loadSavedFloatingSettings() {
        floatingSize.setValue(savedState.floatingSize);
        translationSize.setValue(savedState.translationSize);
        floatingOpacity.setValue(savedState.floatingOpacity);
        try {
            floatingColor.setValue(Color.web(savedState.floatingColor));
            translationColor.setValue(Color.web(savedState.translationColor));
        } catch (IllegalArgumentException ignored) {
            floatingColor.setValue(Color.WHITE);
            translationColor.setValue(Color.rgb(235, 235, 235));
        }
        floatingTop.setSelected(savedState.floatingTop);
        showTranslation.setSelected(savedState.showTranslation);
    }

    private void deduplicate(ObservableList<Song> items) {
        java.util.HashSet<String> ids = new java.util.HashSet<>();
        items.removeIf(song -> song == null || song.id() == null || !ids.add(song.id()));
    }

    private String sleepTimerLabel(int minutes) {
        return switch (minutes) { case 15 -> "15 分钟"; case 30 -> "30 分钟"; case 45 -> "45 分钟"; case 60 -> "60 分钟"; case -1 -> "当前歌曲结束"; default -> "关闭"; };
    }
    private int sleepTimerValue(String label) {
        if (label == null || label.equals("关闭")) return 0;
        if (label.equals("当前歌曲结束")) return -1;
        try { return Integer.parseInt(label.replaceAll("[^0-9]", "")); } catch (NumberFormatException e) { return 0; }
    }
    private void configureSleepTimer(int minutes) {
        if (sleepTimeline != null) sleepTimeline.stop();
        savedState.sleepTimerMinutes = minutes;
        savedState.save();
        if (minutes <= 0 || minutes == -1) return;
        sleepTimeline = new Timeline(new KeyFrame(Duration.minutes(minutes), e -> stopForSleepTimer()));
        sleepTimeline.setCycleCount(1);
        sleepTimeline.play();
    }
    private void stopForSleepTimer() {
        if (player != null) { player.pause(); toggle.setText("▶"); status.setText("睡眠定时器已停止播放"); }
        savedState.sleepTimerMinutes = 0;
        savedState.save();
        if (sleepTimerSelect != null) { sleepTimerSelect.setValue("关闭"); sleepTimerSelect.setText("关闭"); }
    }

    private void configureShortcutField(MFXTextField field, String value, java.util.function.Consumer<String> onChange) {
        field.setText(value == null || value.isBlank() ? "未设置" : value);
        field.setEditable(true);
        field.setFocusTraversable(true);
        field.setPrefWidth(150);
        field.setAlignment(Pos.CENTER);
        field.getStyleClass().add("shortcut-field");
        field.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) { field.setText(value); event.consume(); return; }
            if (event.getCode().isModifierKey()) return;
            String shortcut = shortcutText(event);
            field.setText(shortcut);
            onChange.accept(shortcut);
            event.consume();
        });
        field.addEventFilter(KeyEvent.KEY_TYPED, KeyEvent::consume);
        field.setOnMouseClicked(event -> { field.requestFocus(); field.selectAll(); });
    }
    private String shortcutText(KeyEvent event) {
        StringBuilder text = new StringBuilder();
        if (event.isControlDown()) text.append("CTRL+");
        if (event.isAltDown()) text.append("ALT+");
        if (event.isShiftDown()) text.append("SHIFT+");
        text.append(event.getCode().getName().toUpperCase(java.util.Locale.ROOT));
        return text.toString();
    }
    private boolean matchesShortcut(KeyEvent event, String shortcut) {
        if (shortcut == null || shortcut.isBlank() || "未设置".equals(shortcut)) return false;
        return shortcutText(event).equalsIgnoreCase(shortcut);
    }

    public Parent view() {
        BorderPane root = new BorderPane(); rootPane = root; root.getStyleClass().add("root"); applyTheme();
        root.setLeft(navigation()); updateProgress.setProgress(0); updateProgress.setVisible(false); updateProgress.setManaged(false); updateProgress.setMaxWidth(Double.MAX_VALUE); updateProgress.getStyleClass().add("update-progress"); VBox top = new VBox(updateProgress, header()); VBox.setVgrow(updateProgress, Priority.NEVER); root.setTop(top); centerHost.getChildren().setAll(mainContent()); root.setCenter(centerHost); root.setBottom(playerBar()); if (savedState.floatingVisible) Platform.runLater(this::toggleFloatingLyrics); Platform.runLater(() -> checkForUpdate(false));
        root.setFocusTraversable(true);
        root.setOnDragOver(e -> { if (e.getDragboard().hasFiles()) e.acceptTransferModes(javafx.scene.input.TransferMode.COPY); e.consume(); });
        root.setOnDragDropped(e -> { if (e.getDragboard().hasFiles()) { openFiles(e.getDragboard().getFiles().stream().map(File::getAbsolutePath).collect(Collectors.toList())); e.setDropCompleted(true); } else e.setDropCompleted(false); e.consume(); });
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> { if (e.getTarget() instanceof javafx.scene.control.TextInputControl) return; if (!savedState.keyboardShortcuts) return; if (matchesShortcut(e, savedState.shortcutPlay)) { toggle(); e.consume(); } else if (matchesShortcut(e, savedState.shortcutPrevious)) { previous(); e.consume(); } else if (matchesShortcut(e, savedState.shortcutNext)) { next(); e.consume(); } }); return root;
    }

    public void shutdown() {
        shuttingDown = true;
        savedState.save();
        if (player != null) { player.stop(); player.dispose(); player = null; }
        if (floatingStage != null) { floatingStage.hide(); floatingStage.close(); floatingStage = null; }
    }

    public boolean shouldMinimizeToTray() { return savedState.minimizeToTray; }

    public void restoreFloatingLyrics() {
        if (savedState.floatingVisible && floatingStage != null) {
            floatingStage.show();
            if (floatingToggleButton != null) setActive(floatingToggleButton, true);
        }
    }

    /** Re-shows the owned lyric window after Windows hides owned stages on minimize/hide. */
    public void keepFloatingLyricsVisible() {
        if (!shuttingDown && savedState.floatingVisible && floatingStage != null) {
            Platform.runLater(() -> {
                if (!shuttingDown && savedState.floatingVisible && !floatingStage.isShowing()) floatingStage.show();
            });
        }
    }

    private VBox navigation() {
        Label logo = new Label("♪"); logo.getStyleClass().add("nav-logo");
        Button home = navButton("⌂", "首页"), library = navButton("♫", "音乐库"), playlists = navButton("\uE14C", "歌单"), settings = navButton("⚙", "设置"), about = navButton("ⓘ", "关于");
        home.getStyleClass().add("nav-selected");
        home.setOnAction(e -> { select(home, library, playlists, settings, about); centerHost.getChildren().setAll(mainContent()); }); library.setOnAction(e -> { select(library, home, playlists, settings, about); centerHost.getChildren().setAll(libraryView()); }); playlists.setOnAction(e -> { select(playlists, home, library, settings, about); centerHost.getChildren().setAll(playlistView()); }); settings.setOnAction(e -> { select(settings, home, library, playlists, about); centerHost.getChildren().setAll(settingsView()); }); about.setOnAction(e -> { select(about, home, library, playlists, settings); centerHost.getChildren().setAll(aboutView()); });
        VBox rail = new VBox(18, logo, home, library, playlists, settings, about); rail.getStyleClass().add("navigation-rail"); rail.setAlignment(Pos.TOP_CENTER); rail.setPadding(new Insets(24, 12, 24, 12)); return rail;
    }
    private Button navButton(String iconText, String caption) { Label icon = new Label(iconText); icon.getStyleClass().add("nav-icon"); Label text = new Label(caption); text.getStyleClass().add("nav-caption"); VBox graphic = new VBox(3, icon, text); graphic.setAlignment(Pos.CENTER); MFXButton button = new MFXButton(); button.setGraphic(graphic); button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY); button.getStyleClass().add("nav-item"); button.setPrefSize(88, 64); configureRipple(button, 44); addPressAnimation(button); return button; }
    private void select(Button selected, Button... others) { selected.getStyleClass().add("nav-selected"); Arrays.stream(others).forEach(b -> b.getStyleClass().remove("nav-selected")); }
    private Node libraryView() {
        Label h=new Label("音乐库"); h.getStyleClass().add("page-title");
        Label sub=new Label("在线搜索结果与本地音乐"); sub.getStyleClass().add("muted");
        MFXButton addLocal = new MFXButton("添加本地音乐"); addLocal.getStyleClass().add("config-button"); configureRipple(addLocal, 16); addPressAnimation(addLocal); addLocal.setOnAction(e -> chooseLocalFiles());
        HBox heading = new HBox(new VBox(2, h, sub), new Region(), addLocal); HBox.setHgrow(heading.getChildren().get(1), Priority.ALWAYS); heading.setAlignment(Pos.CENTER_LEFT);
        ObservableList<Song> librarySongs = FXCollections.observableArrayList(); librarySongs.addAll(songs); librarySongs.addAll(localSongs);
        ListView<Song> copy=new ListView<>(librarySongs); copy.setPlaceholder(new Label("还没有收录歌曲"));
        copy.setCellFactory(v->new ListCell<>() {
            @Override protected void updateItem(Song s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    setContextMenu(null);
                    return;
                }
                Label n = new Label(s.name()); n.getStyleClass().add("song-name");
                Label a = new Label(s.artist()); a.getStyleClass().add("song-artist");
                VBox info = new VBox(2, n, a);
                Button b = new Button("▶"); b.getStyleClass().add("row-play"); addPressAnimation(b);
                b.setOnAction(x -> play(s));
                setGraphic(new HBox(14, info, b));
                MenuItem remove = new MenuItem("从音乐库删除");
                remove.setOnAction(x -> {
                    removeFromLibrary(s);
                    librarySongs.removeIf(existing -> existing.id().equals(s.id()));
                });
                setContextMenu(new ContextMenu(remove));
            }
        });
        copy.setOnMouseClicked(e->{if(e.getClickCount()==2&&copy.getSelectionModel().getSelectedItem()!=null)play(copy.getSelectionModel().getSelectedItem());});
        VBox card=new VBox(8,heading,copy); card.getStyleClass().add("content-card"); card.setPadding(new Insets(26)); VBox.setVgrow(copy,Priority.ALWAYS); VBox wrap=new VBox(card);wrap.setPadding(new Insets(18,28,20,20));VBox.setVgrow(card,Priority.ALWAYS);return wrap;
    }
    private void removeFromLibrary(Song song) {
        if (song == null) return;
        if (song.id() != null && song.id().startsWith("local:")) {
            localSongs.removeIf(existing -> existing.id().equals(song.id()));
            savedState.localSongs.removeIf(existing -> existing.id().equals(song.id()));
        } else {
            songs.removeIf(existing -> existing.id().equals(song.id()));
        }
        savedState.save();
        status.setText("已从音乐库移除：" + song.name());
    }
    private Node playlistView() {
        Label h=new Label("歌单");h.getStyleClass().add("page-title");
        Label sub=new Label("本地歌单 · 最近播放与收藏");sub.getStyleClass().add("muted");
        MFXButton clear=new MFXButton("清空歌单"); clear.getStyleClass().add("clear-playlist"); configureRipple(clear, 18); addPressAnimation(clear); clear.setOnAction(e -> { recentSongs.clear(); savedState.queue.clear(); savedState.save(); });
        HBox heading=new HBox(new VBox(2,h,sub), new Region(), clear); HBox.setHgrow(heading.getChildren().get(1), Priority.ALWAYS); heading.setAlignment(Pos.CENTER_LEFT);
        ListView<Song> copy=new ListView<>(recentSongs); copy.setPlaceholder(new Label("播放歌曲后会出现在这里"));
        copy.setCellFactory(v->new ListCell<>(){
            private double dragY;
            protected void updateItem(Song s,boolean e){super.updateItem(s,e); if(e||s==null){setText(null);setContextMenu(null);return;} setText(s.name()+"  ·  "+s.artist()); setOnDragDetected(event -> { if (getItem() == null) return; dragY = event.getY(); javafx.scene.input.Dragboard board = startDragAndDrop(javafx.scene.input.TransferMode.MOVE); board.setContent(java.util.Map.of(javafx.scene.input.DataFormat.PLAIN_TEXT, getItem().id())); event.consume(); }); setOnDragOver(event -> { if (event.getGestureSource() != this && event.getDragboard().hasContent(javafx.scene.input.DataFormat.PLAIN_TEXT)) event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE); event.consume(); }); setOnDragDropped(event -> { javafx.scene.input.Dragboard board=event.getDragboard(); boolean success=false; if(board.hasContent(javafx.scene.input.DataFormat.PLAIN_TEXT)){ String id=(String)board.getContent(javafx.scene.input.DataFormat.PLAIN_TEXT); int from=indexOfSong(recentSongs,id), to=getIndex(); if(from>=0&&to>=0&&from!=to){ Song moved=recentSongs.remove(from); recentSongs.add(Math.min(to,recentSongs.size()),moved); success=true; } } event.setDropCompleted(success); event.consume(); }); MenuItem remove=new MenuItem("从歌单删除"); remove.setOnAction(x -> { recentSongs.remove(s); savedState.queue.removeIf(q -> q.id().equals(s.id())); savedState.save(); }); setContextMenu(new ContextMenu(remove)); }
        });
        copy.setOnMouseClicked(e->{if(e.getClickCount()==2&&copy.getSelectionModel().getSelectedItem()!=null)play(copy.getSelectionModel().getSelectedItem());});
        ListView<Song> fav=new ListView<>(favoriteSongs); fav.setPlaceholder(new Label("还没有收藏歌曲")); fav.setCellFactory(v->new ListCell<>(){protected void updateItem(Song s,boolean e){super.updateItem(s,e);setText(e||s==null?null:"★  "+s.name()+"  ·  "+s.artist());}}); fav.setOnMouseClicked(e->{if(e.getClickCount()==2&&fav.getSelectionModel().getSelectedItem()!=null)play(fav.getSelectionModel().getSelectedItem());});
        ListView<Song> history=new ListView<>(historySongs); history.setPlaceholder(new Label("还没有播放历史")); history.setCellFactory(v->new ListCell<>(){protected void updateItem(Song s,boolean e){super.updateItem(s,e);setText(e||s==null?null:s.name()+"  ·  "+s.artist());}}); history.setOnMouseClicked(e->{if(e.getClickCount()==2&&history.getSelectionModel().getSelectedItem()!=null)play(history.getSelectionModel().getSelectedItem());});
        Label fh=new Label("我的收藏"); fh.getStyleClass().add("section-title"); Label hh=new Label("播放历史"); hh.getStyleClass().add("section-title"); VBox card=new VBox(12,heading,copy,fh,fav,hh,history);card.getStyleClass().add("content-card");card.setPadding(new Insets(26));VBox.setVgrow(copy,Priority.ALWAYS);VBox.setVgrow(fav,Priority.ALWAYS);VBox.setVgrow(history,Priority.ALWAYS);VBox wrap=new VBox(card);wrap.setPadding(new Insets(18,28,20,20));VBox.setVgrow(card,Priority.ALWAYS);return wrap;
    }
    private int indexOfSong(List<Song> items, String id) { for (int i=0;i<items.size();i++) if (id.equals(items.get(i).id())) return i; return -1; }
    private Node settingsView() {
        Label h = new Label("设置"); h.getStyleClass().add("page-title"); themeSelect.setValue(savedState.theme); themeSelect.setText(savedState.theme); themeSelect.setFloatMode(FloatMode.DISABLED); themeSelect.getStyleClass().add("source-select"); themeSelect.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> Platform.runLater(() -> { if (!themeSelect.isShowing()) themeSelect.show(); })); themeSelect.setOnAction(e -> { savedState.theme = themeSelect.getValue(); savedState.save(); applyTheme(); }); backgroundButton.setPrefHeight(34); backgroundButton.setPrefWidth(116); backgroundButton.getStyleClass().add("config-button"); backgroundButton.setOnAction(e -> chooseBackground());
        floatingSize.setValue(savedState.floatingSize); translationSize.setValue(savedState.translationSize); floatingOpacity.setValue(savedState.floatingOpacity); floatingColor.setValue(Color.web(savedState.floatingColor)); translationColor.setValue(Color.web(savedState.translationColor)); floatingTop.setSelected(savedState.floatingTop); minimizeToTray.setSelected(savedState.minimizeToTray); minimizeToTray.selectedProperty().addListener((o,a,b)->{ savedState.minimizeToTray=b; savedState.save(); }); autoStart.setSelected(savedState.autoStart); autoStart.selectedProperty().addListener((o,a,b)->{ savedState.autoStart=b; savedState.save(); WindowsStartup.setEnabled(b); }); keyboardShortcuts.setSelected(savedState.keyboardShortcuts); keyboardShortcuts.selectedProperty().addListener((o,a,b)->{ savedState.keyboardShortcuts=b; savedState.save(); }); Label f = new Label("悬浮歌词"); f.getStyleClass().add("section-title");
        floatingSize.setSliderMode(SliderMode.SNAP_TO_TICKS); floatingSize.setTickUnit(1); floatingSize.setShowMajorTicks(false); floatingSize.setShowMinorTicks(false); floatingSize.setPrefWidth(300); translationSize.setSliderMode(SliderMode.SNAP_TO_TICKS); translationSize.setTickUnit(1); translationSize.setShowMajorTicks(false); translationSize.setShowMinorTicks(false); translationSize.setPrefWidth(300); floatingOpacity.setSliderMode(SliderMode.DEFAULT); floatingOpacity.setUnitIncrement(.01); floatingOpacity.setPrefWidth(300); floatingOpacity.valueProperty().addListener((o,a,b)->{ savedState.floatingOpacity=b.doubleValue(); savedState.save(); applyFloatingSettings(); }); floatingSize.valueProperty().addListener((o,a,b)->{ savedState.floatingSize=b.doubleValue(); savedState.save(); applyFloatingSettings(); }); translationSize.valueProperty().addListener((o,a,b)->{ savedState.translationSize=b.doubleValue(); savedState.save(); applyFloatingSettings(); }); floatingColor.setOnAction(e->{ savedState.floatingColor=floatingColor.getValue().toString(); savedState.save(); applyFloatingSettings(); }); translationColor.setOnAction(e->{ savedState.translationColor=translationColor.getValue().toString(); savedState.save(); applyFloatingSettings(); }); floatingTop.selectedProperty().addListener((o,a,b)->{savedState.floatingTop=b; savedState.save(); if(floatingStage!=null)floatingStage.setAlwaysOnTop(b);});
        lyricOffsetSlider.setSliderMode(SliderMode.SNAP_TO_TICKS); lyricOffsetSlider.setTickUnit(1); lyricOffsetSlider.setShowMajorTicks(false); lyricOffsetSlider.setShowMinorTicks(false); lyricOffsetSlider.setPrefWidth(300); lyricOffsetSlider.setValue(savedState.lyricOffset); sleepTimerSelect.setValue(sleepTimerLabel(savedState.sleepTimerMinutes)); sleepTimerSelect.setText(sleepTimerSelect.getValue()); sleepTimerSelect.setFloatMode(FloatMode.DISABLED); sleepTimerSelect.getStyleClass().add("source-select"); sleepTimerSelect.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> Platform.runLater(() -> { if (!sleepTimerSelect.isShowing()) sleepTimerSelect.show(); }));
        GridPane grid = new GridPane(); grid.setHgap(18); grid.setVgap(16); grid.addRow(0,new Label("原文大小"),floatingSize); grid.addRow(1,new Label("翻译大小"),translationSize); grid.addRow(2,new Label("原文颜色"),floatingColor); grid.addRow(3,new Label("翻译颜色"),translationColor); grid.addRow(4,new Label("文字透明度"),floatingOpacity); grid.addRow(5,floatingTop); ColumnConstraints cc=new ColumnConstraints();cc.setMinWidth(130);grid.getColumnConstraints().add(cc); grid.getStyleClass().add("settings-grid");
        GridPane playbackSettings = new GridPane(); playbackSettings.setHgap(18); playbackSettings.setVgap(16); playbackSettings.addRow(0, new Label("歌词偏移（秒）"), lyricOffsetSlider); playbackSettings.addRow(1, new Label("睡眠定时器"), sleepTimerSelect); playbackSettings.getStyleClass().add("settings-grid");
        MFXButton openConfig = new MFXButton("打开配置文件夹"); openConfig.setPrefHeight(34); openConfig.setPrefWidth(132); openConfig.getStyleClass().add("config-button"); configureRipple(openConfig, 16); addPressAnimation(openConfig); openConfig.setOnAction(e -> openConfigFolder());
        MFXButton clearConfig = new MFXButton("清空配置文件"); clearConfig.setPrefHeight(34); clearConfig.setPrefWidth(116); clearConfig.getStyleClass().add("danger-button"); configureRipple(clearConfig, 16); addPressAnimation(clearConfig); clearConfig.setOnAction(e -> clearConfig());
        MFXButton clearBackground = new MFXButton("清除背景"); clearBackground.setPrefHeight(34); clearBackground.setPrefWidth(116); clearBackground.getStyleClass().add("config-button"); configureRipple(clearBackground, 16); addPressAnimation(clearBackground); clearBackground.setOnAction(e -> { savedState.backgroundPath = ""; savedState.save(); applyTheme(); });
        MFXButton exportConfig = new MFXButton("导出配置"), importConfig = new MFXButton("导入配置"); exportConfig.setPrefHeight(34); exportConfig.setPrefWidth(116); importConfig.setPrefHeight(34); importConfig.setPrefWidth(116); exportConfig.getStyleClass().add("config-button"); importConfig.getStyleClass().add("config-button"); configureRipple(exportConfig, 16); configureRipple(importConfig, 16); addPressAnimation(exportConfig); addPressAnimation(importConfig); exportConfig.setOnAction(e -> exportConfig()); importConfig.setOnAction(e -> importConfig());
        HBox appearance = new HBox(8, new Label("播放器风格"), themeSelect); appearance.setAlignment(Pos.CENTER_LEFT);
        Region appearanceGap = new Region(); appearanceGap.setMinHeight(12);
        HBox configTransfer = new HBox(8, exportConfig, importConfig); VBox switchOptions = new VBox(4, minimizeToTray, autoStart, keyboardShortcuts); GridPane shortcutGrid = new GridPane(); shortcutGrid.setHgap(8); shortcutGrid.setVgap(6); shortcutGrid.addRow(0, new Label("播放/暂停"), shortcutPlayField); shortcutGrid.addRow(1, new Label("上一首"), shortcutPreviousField); shortcutGrid.addRow(2, new Label("下一首"), shortcutNextField); VBox shortcutOptions = new VBox(8, new Label("自定义快捷键"), shortcutGrid); shortcutOptions.getStyleClass().add("settings-switch-group"); VBox startupOptions = new VBox(8, new Label("启动与窗口"), switchOptions); startupOptions.getStyleClass().add("settings-switch-group"); VBox dataActions = new VBox(10, new Label("外观与数据"), appearance, backgroundButton, clearBackground, appearanceGap, openConfig, clearConfig, configTransfer, startupOptions); dataActions.setAlignment(Pos.TOP_LEFT); dataActions.setMinWidth(180);
        VBox leftSettings = new VBox(18, new Label("悬浮歌词"), grid, new Label("播放设置"), playbackSettings, shortcutOptions); leftSettings.getStyleClass().add("settings-column"); VBox rightSettings = new VBox(dataActions); rightSettings.getStyleClass().add("settings-column"); leftSettings.setPrefWidth(0); leftSettings.setMaxWidth(Double.MAX_VALUE); rightSettings.setPrefWidth(0); rightSettings.setMaxWidth(Double.MAX_VALUE); HBox settingsBody = new HBox(30, leftSettings, rightSettings); settingsBody.setAlignment(Pos.TOP_LEFT); HBox.setHgrow(leftSettings, Priority.ALWAYS); HBox.setHgrow(rightSettings, Priority.ALWAYS);
        VBox card = new VBox(18,h,new Label("播放、歌词和界面设置"),settingsBody); card.getStyleClass().add("content-card"); card.setPadding(new Insets(30)); card.setMinHeight(Region.USE_PREF_SIZE);
        ScrollPane scroll = new ScrollPane(card); scroll.setFitToWidth(true); scroll.setFitToHeight(false); scroll.setPannable(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); scroll.getStyleClass().add("settings-scroll");
        VBox wrap=new VBox(scroll); wrap.setPadding(new Insets(18,28,20,20)); VBox.setVgrow(scroll,Priority.ALWAYS); return wrap;
    }
    private Node aboutView() {
        Label title = new Label("关于 BoySir Music"); title.getStyleClass().add("page-title");
        Label version = new Label("版本 " + CURRENT_VERSION); version.getStyleClass().add("section-title");
        Label description = new Label("一个基于 JavaFX 与 MaterialFX 的桌面音乐播放器。\n支持网易云音乐与腾讯音乐源、歌词滚动、翻译、悬浮歌词、收藏、播放模式和一言。\n\n音乐数据由第三方公开 API 提供，内容版权归原平台及其权利人所有。\n\n©2026 BoySir Network Ltd. All Rights Reserved."); description.getStyleClass().add("muted"); description.setWrapText(true);
        MFXButton terms = new MFXButton("用户协议"), privacy = new MFXButton("隐私政策"), update = new MFXButton("检查更新"), exit = new MFXButton("退出程序"); terms.setPrefHeight(32); terms.setPrefWidth(92); privacy.setPrefHeight(32); privacy.setPrefWidth(92); update.setPrefHeight(32); update.setPrefWidth(92); exit.setPrefHeight(32); exit.setPrefWidth(92); terms.getStyleClass().add("policy-button"); privacy.getStyleClass().add("policy-button"); update.getStyleClass().add("policy-button"); exit.getStyleClass().add("policy-button"); configureRipple(terms, 16); configureRipple(privacy, 16); configureRipple(update, 16); configureRipple(exit, 16); addPressAnimation(exit); terms.setOnAction(e -> showDocument("用户协议", TERMS)); privacy.setOnAction(e -> showDocument("隐私政策", PRIVACY)); update.setOnAction(e -> checkForUpdate(true)); exit.setOnAction(e -> terminateApplication()); HBox actions = new HBox(10, new Region(), update, exit, terms, privacy); HBox.setHgrow(actions.getChildren().get(0), Priority.ALWAYS); actions.setAlignment(Pos.CENTER_RIGHT);
        Region bottomSpace = new Region(); VBox.setVgrow(bottomSpace, Priority.ALWAYS);
        VBox card = new VBox(18, title, version, description, bottomSpace, actions); card.getStyleClass().add("content-card"); card.setPadding(new Insets(32)); VBox wrap = new VBox(card); wrap.setPadding(new Insets(18,28,20,20)); VBox.setVgrow(card, Priority.ALWAYS); return wrap;
    }

    private static final String TERMS = "用户协议\n生效日期：2026年9月12日\n适用软件：BoySirMusicPlayer\n开发者：Boy_Sir\n\n服务说明\n本应用是一款本地音乐播放软件，提供音频播放、播放列表管理等功能。当前版本无需注册账号。\n\n使用许可\n我们授予您个人、非商业、不可转让的使用许可。您不得将本应用用于违法用途，不得反向工程、破解或传播恶意版本。\n\n用户责任\n您应确保所播放、导入的音乐文件来源合法，并自行承担相关版权责任。\n\n知识产权\n本应用本身的代码、界面、图标等归开发者所有。\n\n免责声明\n本应用按现状提供，开发者不对间接损失承担责任。\n\n联系方式\n1879872864@qq.com";
    private static final String PRIVACY = "隐私政策\n生效日期：2026年9月12日\n适用软件：BoySirMusicPlayer\n\n信息收集\n当前版本不主动收集、不上传您的个人信息，不要求注册账号。\n\n权限使用\n本应用可能读取您选择的本地音频文件，仅用于扫描和播放。\n\n数据存储\n播放列表、设置等数据默认保存在您的本地设备中。\n\n第三方服务\n当前版本不集成广告或统计服务。\n\n信息共享\n我们不会出售、出租或向第三方共享您的个人信息，除非法律法规要求。\n\n联系方式\n1879872864@qq.com";
    private static final String CURRENT_VERSION = loadCurrentVersion();
    private static String loadCurrentVersion() {
        Properties properties = new Properties();
        try (var input = HelloController.class.getResourceAsStream("version.properties")) {
            if (input != null) {
                properties.load(input);
                String version = properties.getProperty("version");
                if (version != null && !version.isBlank() && !version.startsWith("@")) return version.trim();
            }
        } catch (IOException ignored) { }
        return "0.0.0";
    }
    private void checkForUpdate(boolean manual) {
        if (!manual) setStartupUiDisabled(true);
        if (updateProgress != null) { updateProgress.setManaged(true); updateProgress.setVisible(true); updateProgress.setProgress(-1); }
        api.checkUpdate().thenAccept(info -> Platform.runLater(() -> {
            if (updateProgress != null) { updateProgress.setProgress(1); updateProgress.setVisible(false); updateProgress.setManaged(false); }
            if (info.version() == null || info.version().isBlank() || compareVersions(info.version(), CURRENT_VERSION) <= 0) {
                if (manual) showInfo("检查更新", "当前已是最新版本（" + CURRENT_VERSION + "）");
                if (!manual) setStartupUiDisabled(false);
                return;
            }
            String notes = info.releaseNotes() == null ? "" : info.releaseNotes();
            WindowsNotifier.show("发现新版本 " + info.version(), notes.isBlank() ? "BoySir Music Player 有新版本可用" : notes.replace('\n', ' '));
            Alert alert = new Alert(info.forceUpdate() ? Alert.AlertType.WARNING : Alert.AlertType.CONFIRMATION);
            alert.setTitle(info.forceUpdate() ? "需要更新" : "发现新版本");
            alert.setHeaderText("BoySir Music Player " + info.version());
            alert.setContentText((notes.isBlank() ? "发现新版本。" : notes) + "\n\n是否打开下载页面？");
            ButtonType open = new ButtonType("打开下载");
            ButtonType later = new ButtonType(info.forceUpdate() ? "退出" : "稍后", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(open, later);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == open) {
                openUpdateUrl(info.downloadUrl());
                if (info.forceUpdate()) terminateApplication();
            }
            else if (info.forceUpdate()) {
                terminateApplication();
            }
            if (!manual) setStartupUiDisabled(false);
        })).exceptionally(error -> { Platform.runLater(() -> { if (updateProgress != null) { updateProgress.setProgress(1); updateProgress.setVisible(false); updateProgress.setManaged(false); } if (manual) showInfo("检查更新", "检查更新失败，请稍后重试。"); else { setStartupUiDisabled(false); startupUpdateCheckFailed(); } }); return null; });
    }
    private void setStartupUiDisabled(boolean disabled) {
        if (rootPane == null) return;
        if (rootPane.getLeft() != null) rootPane.getLeft().setDisable(disabled);
        if (rootPane.getCenter() != null) rootPane.getCenter().setDisable(disabled);
        if (rootPane.getBottom() != null) rootPane.getBottom().setDisable(disabled);
        if (rootPane.getTop() != null && rootPane.getTop() instanceof VBox top && top.getChildren().size() > 1) top.getChildren().get(1).setDisable(disabled);
    }
    private void startupUpdateCheckFailed() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("无法检查更新");
        alert.setHeaderText("启动前必须完成版本检查");
        alert.setContentText("当前无法连接更新服务器，请连接网络后重试。\n选择“退出”将关闭播放器。");
        ButtonType retry = new ButtonType("重试");
        ButtonType exit = new ButtonType("退出", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(retry, exit);
        if (alert.showAndWait().orElse(exit) == retry) checkForUpdate(false);
        else terminateApplication();
    }
    private int compareVersions(String a, String b) {
        try { String[] x = a.replaceAll("[^0-9.]", "").split("\\."), y = b.replaceAll("[^0-9.]", "").split("\\."); for (int i=0;i<Math.max(x.length,y.length);i++) { int xi=i<x.length&&!x[i].isBlank()?Integer.parseInt(x[i]):0, yi=i<y.length&&!y[i].isBlank()?Integer.parseInt(y[i]):0; if (xi!=yi) return Integer.compare(xi,yi); } } catch (RuntimeException ignored) { } return a.compareTo(b);
    }
    private void openUpdateUrl(String url) { try { if (url != null && !url.isBlank() && Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url)); } catch (Exception ignored) { } }
    private void terminateApplication() { shuttingDown = true; shutdown(); WindowsNotifier.removeAppTray(); Platform.exit(); System.exit(0); }
    private void showInfo(String header, String message) { Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK); alert.setTitle(header); alert.setHeaderText(null); alert.showAndWait(); }
    private void openConfigFolder() { try { Files.createDirectories(AppState.configDirectory()); if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(AppState.configDirectory().toFile()); } catch (Exception e) { status.setText("无法打开配置文件夹"); } }
    private void exportConfig() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("导出播放器配置"); chooser.setInitialFileName("boysir-music-player-config.json"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 配置", "*.json"));
        File target = chooser.showSaveDialog(rootPane == null ? null : rootPane.getScene().getWindow());
        if (target == null) return;
        try {
            savedState.save();
            Files.copy(AppState.configFile(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            status.setText("配置已导出");
            WindowsNotifier.show("配置导出完成", "配置已保存到：" + target.getName());
        } catch (IOException e) {
            status.setText("配置导出失败");
            WindowsNotifier.show("配置导出失败", "无法保存播放器配置，请检查目标位置是否可写");
        }
    }
    private void importConfig() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("导入播放器配置"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 配置", "*.json"));
        File sourceFile = chooser.showOpenDialog(rootPane == null ? null : rootPane.getScene().getWindow());
        if (sourceFile == null) return;
        try {
            Files.createDirectories(AppState.configDirectory());
            Files.copy(sourceFile.toPath(), AppState.configFile(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            WindowsNotifier.show("配置导入完成", "配置已导入，重启播放器后生效");
            showInfo("导入配置", "配置已导入，重启播放器后生效。");
        } catch (IOException e) {
            status.setText("配置导入失败");
            WindowsNotifier.show("配置导入失败", "无法读取或写入配置文件");
        }
    }
    private void clearConfig() { Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "清空后收藏、播放队列和所有设置都会被删除，是否继续？", ButtonType.CANCEL, ButtonType.OK); alert.setTitle("清空配置文件"); alert.setHeaderText("确认清空配置文件"); alert.showAndWait().filter(ButtonType.OK::equals).ifPresent(ok -> { try { AppState.clearFile(); savedState.favorites.clear(); savedState.queue.clear(); savedState.autoStart = false; WindowsStartup.setEnabled(false); favoriteSongs.clear(); recentSongs.clear(); WindowsNotifier.show("配置已清空", "收藏、播放队列和设置已清空"); status.setText("配置已清空"); } catch (IOException e) { status.setText("清空配置失败"); } }); }
    private void showDocument(String titleText, String content) { Stage window = new Stage(); window.initOwner(title.getScene().getWindow()); window.setTitle(titleText); TextArea text = new TextArea(content); text.setWrapText(true); text.setEditable(false); text.getStyleClass().add("document-text"); VBox box = new VBox(text); box.setPadding(new Insets(18)); VBox.setVgrow(text, Priority.ALWAYS); window.setScene(new javafx.scene.Scene(box, 640, 560)); window.show(); }
    private void chooseBackground() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("选择播放器背景图片");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        File file = chooser.showOpenDialog(rootPane == null ? null : rootPane.getScene().getWindow());
        if (file != null) { savedState.backgroundPath = file.getAbsolutePath(); savedState.save(); applyTheme(); }
    }

    private void applyTheme() {
        if (rootPane == null) return;
        rootPane.getStyleClass().remove("dark-theme");
        rootPane.getStyleClass().remove("custom-background"); rootPane.setBackground(null); rootPane.setStyle("");
        if ("黑暗".equals(savedState.theme)) rootPane.getStyleClass().add("dark-theme");
        if (!savedState.backgroundPath.isBlank() && Files.exists(Path.of(savedState.backgroundPath))) {
            rootPane.getStyleClass().add("custom-background");
            Image image = new Image(Path.of(savedState.backgroundPath).toUri().toString(), false);
            rootPane.setBackground(new Background(new BackgroundImage(image, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, new BackgroundSize(100, 100, true, true, false, true))));
            rootPane.setStyle("-fx-background-color: transparent; -fx-background-image: url('" + Path.of(savedState.backgroundPath).toUri() + "'); -fx-background-repeat: no-repeat; -fx-background-position: center center; -fx-background-size: cover;");
        } else if (savedState.backgroundPath.isBlank()) { rootPane.setBackground(null); rootPane.setStyle(""); }
    }

    private HBox header() {
        Label brand = new Label("♪  BoySir Music"); brand.getStyleClass().add("brand"); Label sub = new Label("Material music player"); sub.getStyleClass().add("subtitle");
        MFXTextField input = new MFXTextField(); input.setPromptText("搜索歌曲、歌手或专辑"); input.setFloatMode(FloatMode.DISABLED); input.setAlignment(Pos.CENTER_LEFT); input.setPrefHeight(48); input.setPadding(new Insets(0, 18, 0, 18)); input.getStyleClass().add("search-field"); addFieldFeedback(input);
        MFXButton search = new MFXButton("⌕"); search.getStyleClass().add("icon-button"); search.setPrefSize(48, 48); configureRipple(search, 24); addPressAnimation(search); search.setOnAction(e -> search(input.getText())); input.setOnAction(e -> search(input.getText()));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS); HBox searchBox = new HBox(8, input, search); searchBox.setPrefWidth(420); searchBox.setAlignment(Pos.CENTER_RIGHT);
        source.setValue(savedState.source.equals("腾讯云") ? "腾讯云" : "网易云"); source.setText(source.getValue()); source.setFloatMode(FloatMode.DISABLED); source.getStyleClass().add("source-select"); source.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> Platform.runLater(() -> { if (!source.isShowing()) source.show(); })); source.setOnAction(e -> { if (source.getValue()!=null) { source.setText(source.getValue()); savedState.source = source.getValue(); savedState.save(); } }); HBox bar = new HBox(22, new VBox(2, brand, sub), spacer, new Label("音乐源"), source, searchBox); bar.getStyleClass().add("top-app-bar"); bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(18, 32, 18, 28)); return bar;
    }

    private Node mainContent() {
        Label heading = new Label("发现音乐"); heading.getStyleClass().add("page-title"); Label hint = new Label("搜索音乐"); hint.getStyleClass().add("muted");
        list.setPlaceholder(new Label("搜索歌曲后，结果会显示在这里")); list.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(Song s, boolean empty) { super.updateItem(s, empty); if (empty || s == null) { setGraphic(null); return; }
                Label no = new Label(String.format("%02d", getIndex() + 1)); no.getStyleClass().add("index"); Label name = new Label(s.name()); name.getStyleClass().add("song-name"); Label by = new Label(s.artist()); by.getStyleClass().add("song-artist");
                VBox info = new VBox(3, name, by); HBox.setHgrow(info, Priority.ALWAYS); MFXButton play = new MFXButton("▶"); play.getStyleClass().add("row-play"); configureRipple(play, 20); addPressAnimation(play); play.setOnAction(e -> play(s)); HBox row = new HBox(18, no, info, play); row.setAlignment(Pos.CENTER_LEFT); setGraphic(row);
            }
        }); list.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() != null) play(list.getSelectionModel().getSelectedItem()); });
        searchSpinner.setRadius(11); searchSpinner.setColor1(Color.web("#6750A4")); searchSpinner.setColor2(Color.web("#7D5260")); searchSpinner.setColor3(Color.web("#386A20")); searchSpinner.setColor4(Color.web("#9A4520")); searchSpinner.setVisible(false); searchSpinner.setMouseTransparent(true); searchSpinner.getStyleClass().add("search-spinner");
        StackPane resultList = new StackPane(list, searchSpinner); resultList.setAlignment(Pos.CENTER); VBox.setVgrow(resultList, Priority.ALWAYS);
        VBox resultCard = new VBox(10, heading, hint, resultList); resultCard.getStyleClass().add("content-card"); resultCard.setPadding(new Insets(26, 30, 26, 30)); VBox.setVgrow(resultList, Priority.ALWAYS);
        Label lyricsTitle = new Label("歌词"); lyricsTitle.getStyleClass().add("section-title"); showTranslation.setSelected(savedState.showTranslation); showTranslation.selectedProperty().addListener((o,a,b)->{savedState.showTranslation=b; savedState.save(); if(currentSong!=null){updateDetails(currentSong); if(player!=null)updateLyric(player.getCurrentTime().toSeconds());}}); lyricLines.getStyleClass().add("lyric-lines"); lyricLines.getChildren().add(new Label("播放歌曲后，歌词会显示在这里"));
        lyricScroll = new ScrollPane(lyricLines); lyricScroll.setFitToWidth(true); lyricScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); lyricScroll.getStyleClass().add("lyrics-scroll"); HBox lyricHeader=new HBox(lyricsTitle,new Region(),showTranslation); HBox.setHgrow(lyricHeader.getChildren().get(1),Priority.ALWAYS); lyricHeader.setAlignment(Pos.CENTER_LEFT); VBox lyricsCard = new VBox(12, lyricHeader, lyricScroll); lyricsCard.getStyleClass().add("lyrics-card"); lyricsCard.setPadding(new Insets(26)); VBox.setVgrow(lyricScroll, Priority.ALWAYS); if (currentSong != null) { updateDetails(currentSong); if (player != null) Platform.runLater(() -> updateLyric(player.getCurrentTime().toSeconds())); }
        SplitPane split = new SplitPane(resultCard, lyricsCard); split.setDividerPositions(0.64); split.getStyleClass().add("main-split"); VBox.setVgrow(split, Priority.ALWAYS); VBox wrapper = new VBox(split); wrapper.setPadding(new Insets(18, 28, 20, 20)); VBox.setVgrow(split, Priority.ALWAYS); return wrapper;
    }

    private Node playerBar() {
        cover.setFitWidth(54); cover.setFitHeight(54); cover.setPreserveRatio(true); StackPane art = new StackPane(coverFallback, cover); Rectangle artClip = new Rectangle(54, 54); artClip.setArcWidth(28); artClip.setArcHeight(28); art.setClip(artClip); art.getStyleClass().add("artwork"); coverFallback.getStyleClass().add("cover-note");
        title.getStyleClass().add("playing-name"); artist.getStyleClass().add("playing-artist"); HBox now = new HBox(14, art, new VBox(3, title, artist)); now.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(now, Priority.ALWAYS);
        MFXButton prev = new MFXButton("\uE100"), next = new MFXButton("\uE101"); prev.setAccessibleText("上一首 \uE100"); next.setAccessibleText("下一首 \uE101"); prev.setUserData("\uE100"); next.setUserData("\uE101"); prev.getStyleClass().add("transport"); next.getStyleClass().add("transport"); toggle.getStyleClass().add("play-button"); repeat.getStyleClass().add("transport"); for (Button b : new Button[]{prev, next, toggle, repeat}) { if (b instanceof MFXButton m) configureRipple(m, b == toggle ? 24 : 22); addPressAnimation(b); if (b == toggle) b.setPrefSize(56, 56); else b.setPrefSize(44, 44); b.setMinSize(b.getPrefWidth(), b.getPrefHeight()); } toggle.setOnAction(e -> toggle()); prev.setOnAction(e -> previous()); next.setOnAction(e -> next()); repeat.setOnAction(e -> { repeatMode = (repeatMode + 1) % 4; shuffleEnabled = repeatMode == 3; String label = switch (repeatMode) { case 1 -> "↻"; case 2 -> "↻1"; case 3 -> "⤨"; default -> "↻"; }; repeat.setText(label); setActive(repeat, repeatMode != 0); });
        speed.getItems().setAll(.75, 1.0, 1.25, 1.5, 2.0); speed.setValue(savedState.speed); speed.selectItem(savedState.speed); speed.setText(savedState.speed + "x"); speed.setFloatMode(FloatMode.DISABLED); speed.setConverter(new javafx.util.StringConverter<>() { public String toString(Double v){return v==null?"1.0x":v+"x";} public Double fromString(String s){return Double.valueOf(s.replace("x", ""));} }); speed.getStyleClass().add("speed-select"); speed.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> Platform.runLater(() -> { if (!speed.isShowing()) speed.show(); })); speed.setOnAction(e -> { if (speed.getValue()!=null) speed.setText(speed.getConverter().toString(speed.getValue())); savedState.speed = speed.getValue(); savedState.save(); if (player != null) player.setRate(speed.getValue()); });
        volume.setValue(savedState.volume); volumePercent.setText(String.format("%d%%", Math.round(savedState.volume * 100))); volume.setPrefWidth(110); volume.getStyleClass().add("volume-slider"); volume.valueProperty().addListener((o,a,b) -> { volumePercent.setText(String.format("%d%%", Math.round(b.doubleValue()*100))); savedState.volume = b.doubleValue(); savedState.save(); if (player != null) player.setVolume(b.doubleValue()); });
        progress.setMaxWidth(Double.MAX_VALUE); progress.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> { e.consume(); setProgressFromMouse(e.getX()); }); progress.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> { e.consume(); setProgressFromMouse(e.getX()); }); progress.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> e.consume()); HBox timeline=new HBox(8,elapsed,progress,duration); timeline.getStyleClass().add("timeline"); timeline.setAlignment(Pos.CENTER); HBox.setHgrow(progress,Priority.ALWAYS); VBox state = new VBox(4, status); state.setAlignment(Pos.CENTER_RIGHT);
        favorite.getStyleClass().add("favorite-button"); favorite.setMinSize(38,38); favorite.setPrefSize(38,38); favorite.setMaxSize(38,38); favorite.setAlignment(Pos.CENTER); favorite.setPadding(Insets.EMPTY); configureRipple(favorite, 18); favorite.setOnAction(e -> toggleFavorite()); MFXButton downloadButton = new MFXButton("\uE118"); downloadButton.getStyleClass().add("transport"); configureRipple(downloadButton, 18); downloadButton.setOnAction(e -> downloadCurrent()); MFXButton floatButton = new MFXButton("词"); floatingToggleButton = floatButton; floatButton.getStyleClass().add("transport"); configureRipple(floatButton, 18); floatButton.setOnAction(e -> toggleFloatingLyrics()); HBox controls = new HBox(10, prev, toggle, next, repeat); controls.setAlignment(Pos.CENTER); HBox options = new HBox(10, favorite, downloadButton, floatButton, new Label("速度"), speed, new Label("音量"), volume, volumePercent); options.setAlignment(Pos.CENTER_RIGHT); VBox right = new VBox(5, state, options); right.setAlignment(Pos.CENTER_RIGHT);
        saying.getStyleClass().add("saying"); api.randomSaying().thenAccept(text -> Platform.runLater(() -> saying.setText(text))).exceptionally(e -> null);
        BorderPane bar = new BorderPane(); bar.getStyleClass().add("player-controls"); bar.setLeft(now); bar.setCenter(controls); bar.setRight(right); BorderPane.setAlignment(now, Pos.CENTER_LEFT); BorderPane.setAlignment(controls, Pos.CENTER); BorderPane.setAlignment(right, Pos.CENTER_RIGHT); bar.setPadding(new Insets(10, 34, 14, 34));
        VBox shell=new VBox(timeline,bar,saying); shell.getStyleClass().add("player-bar"); shell.setPadding(new Insets(6,0,0,0)); HBox.setHgrow(progress,Priority.ALWAYS); timeline.setPadding(new Insets(0,34,0,34)); return shell;
    }

    private void search(String keyword) { if (keyword == null || keyword.isBlank()) return; status.setText("正在搜索…"); searchSpinner.setVisible(true); api.search(keyword.trim(), source.getValue()).thenAccept(r -> Platform.runLater(() -> { songs.setAll(r); searchSpinner.setVisible(false); status.setText("找到 " + r.size() + " 首歌曲"); animateResults(); })).exceptionally(e -> { Platform.runLater(() -> { searchSpinner.setVisible(false); status.setText("搜索失败，请检查网络"); }); return null; }); }
    private void chooseLocalFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("添加本地音乐");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("音频文件", "*.mp3", "*.m4a", "*.aac", "*.wav", "*.aiff", "*.flac"));
        List<File> files = chooser.showOpenMultipleDialog(rootPane == null ? null : rootPane.getScene().getWindow());
        int before = localSongs.size();
        if (files != null) files.forEach(this::addLocalFile);
        int added = localSongs.size() - before;
        if (added > 0) {
            status.setText("已添加 " + added + " 首本地歌曲");
            centerHost.getChildren().setAll(libraryView());
        }
    }
    private void addLocalFile(File file) {
        if (file == null || !file.isFile() || !isSupportedAudio(file.toPath())) return;
        String id = localId(file.toPath());
        if (localSongs.stream().anyMatch(s -> s.id().equals(id))) return;
        String base = file.getName().replaceFirst("(?i)\\.[^.]+$", "");
        String lyric = readLocalLyric(file);
        localSongs.add(new Song(id, base, "本地音乐", "", "", lyric));
    }
    private String localId(Path path) { return "local:" + path.toAbsolutePath().normalize(); }
    private boolean isSupportedAudio(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".aac")
                || name.endsWith(".wav") || name.endsWith(".aiff") || name.endsWith(".flac");
    }
    private String localPath(String id) { return id != null && id.startsWith("local:") ? id.substring(6) : ""; }
    private String readLocalLyric(File audio) {
        Path lrc = Path.of(audio.getAbsolutePath().replaceFirst("(?i)\\.[^.]+$", "") + ".lrc");
        try { return Files.exists(lrc) ? Files.readString(lrc) : ""; } catch (IOException ignored) { return ""; }
    }
    public void openFiles(List<String> paths) {
        if (paths == null) return;
        int before = localSongs.size();
        paths.stream().map(Path::of).filter(this::isSupportedAudio).map(Path::toFile).filter(File::isFile).forEach(this::addLocalFile);
        if (localSongs.size() > before) Platform.runLater(() -> {
            centerHost.getChildren().setAll(libraryView());
            if (!localSongs.isEmpty()) play(localSongs.get(localSongs.size() - 1));
        });
    }
    private void animateResults() { list.setOpacity(0); list.setTranslateY(14); FadeTransition fade = new FadeTransition(Duration.millis(360), list); fade.setToValue(1); TranslateTransition slide = new TranslateTransition(Duration.millis(360), list); slide.setToY(0); new ParallelTransition(fade, slide).play(); }
    private void play(Song song) {
        if (song != null && song.id().startsWith("local:")) { playLocal(song); return; }
        final long request = ++playGeneration; currentSong = song; favorite.setText(hasFavorite(song.id()) ? "♥" : "♡"); int index = songs.indexOf(song); if (index >= 0) list.getSelectionModel().select(index); if (recentSongs.stream().noneMatch(existing -> existing.id().equals(song.id()))) { recentSongs.add(song); savedState.save(); } historySongs.removeIf(existing -> existing.id().equals(song.id())); historySongs.add(0, song); title.setText(song.name()); artist.setText(song.artist()); status.setText("正在加载歌曲…"); progress.setValue(0); elapsed.setText("0:00"); duration.setText("0:00"); if (player != null) { player.stop(); player.dispose(); player = null; } api.details(song).thenCombine(api.lyric(song), (detail, lyric) -> detail.withDetails(detail.coverUrl(), lyric)).thenCompose(detail -> { if (request != playGeneration) return CompletableFuture.completedFuture(null); currentSong = detail; Platform.runLater(() -> { if (request == playGeneration) { favorite.setText(hasFavorite(detail.id()) ? "♥" : "♡"); updateDetails(detail); } }); return api.audioUrl(detail); }).thenAccept(url -> Platform.runLater(() -> startMedia(url, request))).exceptionally(e -> { if (request == playGeneration) Platform.runLater(() -> status.setText("歌曲详情或播放地址获取失败")); return null; }); }
    private void playLocal(Song song) {
        final long request = ++playGeneration;
        currentSong = song; favorite.setText(hasFavorite(song.id()) ? "♥" : "♡");
        enqueueSong(song);
        title.setText(song.name()); artist.setText("本地音乐"); status.setText("正在加载本地歌曲…"); progress.setValue(0); elapsed.setText("0:00"); duration.setText("0:00");
        if (player != null) { player.stop(); player.dispose(); player = null; }
        updateDetails(song);
        startMedia(Path.of(localPath(song.id())).toUri().toString(), request);
    }

    private void enqueueSong(Song song) {
        if (song == null) return;
        if (recentSongs.stream().noneMatch(existing -> existing.id().equals(song.id()))) recentSongs.add(song);
        historySongs.removeIf(existing -> existing.id().equals(song.id()));
        historySongs.add(0, song);
    }
    private void startMedia(String url, long request) {
        if (request != playGeneration || url == null || url.isBlank()) return;
        try { player = new MediaPlayer(new Media(url)); } catch (RuntimeException e) { status.setText("无法打开本地音频文件"); return; }
        player.setVolume(volume.getValue()); player.setRate(speed.getValue());
        player.setOnReady(() -> { if (request != playGeneration) return; status.setText("播放中"); duration.setText(formatTime(player.getTotalDuration())); player.play(); toggle.setText("⏸"); });
        player.currentTimeProperty().addListener((o, a, b) -> { if (request != playGeneration || player == null) return; if (player.getTotalDuration().toSeconds() > 0) progress.setValue(b.toSeconds() / player.getTotalDuration().toSeconds()); elapsed.setText(formatTime(b)); updateLyric(b.toSeconds()); });
        player.setOnEndOfMedia(() -> { if (request != playGeneration) return; if (repeatMode == 2) { player.seek(Duration.ZERO); player.play(); } else if (repeatMode == 1 || shuffleEnabled) next(); else toggle.setText("▶"); });
    }
    private void seekProgress(double value) { if (player != null && player.getTotalDuration() != null && !player.getTotalDuration().isUnknown() && player.getTotalDuration().toSeconds() > 0) { player.seek(Duration.seconds(Math.max(0, Math.min(1, value)) * player.getTotalDuration().toSeconds())); } }
    private void setProgressFromMouse(double x) { double value = Math.max(0, Math.min(1, x / Math.max(1, progress.getWidth()))); progress.setValue(value); seekProgress(value); }
    private String formatTime(Duration value) { int total=(int)Math.max(0,value.toSeconds()); return String.format("%d:%02d", total/60,total%60); }
    private void updateDetails(Song song) { if (!song.coverUrl().isBlank()) { cover.setImage(new Image(song.coverUrl(), true)); coverFallback.setVisible(false); } lyricEntries.clear(); lyricLines.getChildren().clear(); if (lyricScroll != null) lyricScroll.setVvalue(0); String[] sections = song.lyric().split("\\n---TRANSLATION---\\n", 2); java.util.Map<String,String> translations = new java.util.HashMap<>(); if (sections.length > 1) for (String line : sections[1].split("\\n")) { java.util.regex.Matcher tm = java.util.regex.Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)").matcher(line); if (tm.matches()) translations.put(tm.group(1)+":"+tm.group(2), tm.group(3).trim()); } if (sections.length == 0 || sections[0].isBlank()) { lyricLines.getChildren().add(new Label("暂无歌词")); return; } for (String line : sections[0].replace("\\r", "").split("\\n")) { java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)").matcher(line); if (m.matches()) { double time = Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2)); String key=m.group(1)+":"+m.group(2); VBox pair=new VBox(3); pair.setAlignment(Pos.CENTER); Label original=new Label(m.group(3).trim()); original.getStyleClass().add("lyric-line"); pair.getChildren().add(original); String translated=translations.get(key); if(showTranslation.isSelected()&&translated!=null&&!translated.isBlank()){Label trans=new Label(translated);trans.getStyleClass().add("lyric-translation");pair.getChildren().add(trans);} lyricEntries.add(new LyricEntry(time, original)); lyricLines.getChildren().add(pair); } } if (lyricEntries.isEmpty()) lyricLines.getChildren().add(new Label("暂无可解析歌词")); }
    private void updateLyric(double seconds) { seconds += savedState.lyricOffset; int activeIndex = -1; for (int i = 0; i < lyricEntries.size(); i++) { LyricEntry e = lyricEntries.get(i); boolean active = seconds >= e.time && (i == lyricEntries.size() - 1 || seconds < lyricEntries.get(i + 1).time); e.label.getStyleClass().removeAll("lyric-active"); if (active) activeIndex = i; } if (activeIndex < 0) { if (lyricScroll != null) { Timeline t = new Timeline(new KeyFrame(Duration.millis(260), new KeyValue(lyricScroll.vvalueProperty(), 0))); t.play(); } setFloatingText("等待歌词…", ""); return; } LyricEntry active = lyricEntries.get(activeIndex); active.label.getStyleClass().add("lyric-active"); String translation = ""; if (showTranslation.isSelected() && active.label.getParent() instanceof VBox pair && pair.getChildren().size() > 1) translation = ((Label) pair.getChildren().get(1)).getText(); setFloatingText(active.label.getText(), translation); if (lyricScroll != null && lyricEntries.size() > 1) { double target = Math.max(0, Math.min(1, (activeIndex - 3.0) / Math.max(1, lyricEntries.size() - 7.0))); Timeline t=new Timeline(new KeyFrame(Duration.millis(320),new KeyValue(lyricScroll.vvalueProperty(),target))); t.play(); } }
    private void toggleFloatingLyrics() { if (floatingStage != null && floatingStage.isShowing()) { savedState.floatingVisible = false; floatingStage.hide(); if (floatingToggleButton != null) setActive(floatingToggleButton, false); savedState.save(); return; } if (floatingStage == null) { floatingLyric = new Label("等待歌词…"); floatingTranslation = new Label(); floatingLyric.getStyleClass().add("floating-lyric"); floatingTranslation.getStyleClass().add("floating-translation"); VBox text = new VBox(3, floatingLyric, floatingTranslation); text.setAlignment(Pos.CENTER); StackPane pane = new StackPane(text); pane.getStyleClass().add("floating-lyric-pane"); pane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-border-color: transparent;"); pane.setOnMousePressed(e -> { floatDragX = e.getScreenX() - floatingStage.getX(); floatDragY = e.getScreenY() - floatingStage.getY(); }); pane.setOnMouseDragged(e -> { floatingStage.setX(e.getScreenX() - floatDragX); floatingStage.setY(e.getScreenY() - floatDragY); }); floatingStage = new Stage(StageStyle.TRANSPARENT); floatingStage.setOnHidden(e -> { if (savedState.floatingVisible && !shuttingDown) Platform.runLater(() -> { if (!floatingStage.isShowing()) floatingStage.show(); }); }); javafx.scene.Scene floatScene = new javafx.scene.Scene(pane, 600, 112, Color.TRANSPARENT); floatScene.setFill(Color.TRANSPARENT); floatingStage.setScene(floatScene); floatingStage.setAlwaysOnTop(floatingTop.isSelected()); floatingStage.setX(300); floatingStage.setY(30); applyFloatingSettings(); } floatingStage.show(); savedState.floatingVisible = true; if (floatingToggleButton != null) setActive(floatingToggleButton, true); savedState.save(); }
    private void setFloatingText(String text, String translation) { if (floatingLyric != null) floatingLyric.setText(text == null || text.isBlank() ? "♪" : text); if (floatingTranslation != null) { floatingTranslation.setText(translation == null ? "" : translation); floatingTranslation.setVisible(!translation.isBlank()); } }
    private void downloadCurrent() {
        if (currentSong == null) { status.setText("请先播放歌曲"); return; }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("保存歌曲");
        chooser.setInitialFileName(currentSong.name().replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp3");
        File file = chooser.showSaveDialog(title.getScene().getWindow());
        if (file == null) return;
        status.setText("正在下载…");
        api.download(currentSong, Path.of(file.toURI())).thenAccept(saved -> Platform.runLater(() -> {
            status.setText("下载完成"); WindowsNotifier.downloadFinished(saved.getFileName().toString());
        })).exceptionally(error -> { Platform.runLater(() -> status.setText("下载失败")); return null; });
    }
    private void toggleFavorite() { if (currentSong == null) return; Song found = favoriteSongs.stream().filter(s -> s.id().equals(currentSong.id())).findFirst().orElse(null); if (found != null) { favoriteSongs.remove(found); savedState.favorites.removeIf(s -> s.id().equals(currentSong.id())); savedState.save(); favorite.setText("♡"); } else { favoriteSongs.add(currentSong); savedState.favorites.add(currentSong); savedState.save(); favorite.setText("♥"); } }
    private boolean hasFavorite(String id) { return favoriteSongs.stream().anyMatch(s -> s.id().equals(id)); }
    private void applyFloatingSettings() { if (floatingLyric == null || floatingTranslation == null) return; floatingLyric.setTextFill(floatingColor.getValue()); floatingTranslation.setTextFill(translationColor.getValue()); floatingLyric.setOpacity(floatingOpacity.getValue()); floatingTranslation.setOpacity(floatingOpacity.getValue()); String family = "'Microsoft YaHei UI', 'Microsoft YaHei', sans-serif"; floatingLyric.setStyle("-fx-font-family: " + family + "; -fx-font-size: " + Math.round(floatingSize.getValue()) + "px; -fx-font-weight: 800; -fx-effect: dropshadow(gaussian, rgba(0,0,0,.8), 5, .65, 0, 2);"); floatingTranslation.setStyle("-fx-font-family: " + family + "; -fx-font-size: " + Math.round(translationSize.getValue()) + "px; -fx-font-weight: 600; -fx-effect: dropshadow(gaussian, rgba(0,0,0,.8), 4, .65, 0, 1);"); if (floatingStage != null) floatingStage.setAlwaysOnTop(floatingTop.isSelected()); }
    private void toggle() { if (player == null) return; if ("⏸".equals(toggle.getText())) { player.pause(); toggle.setText("▶"); status.setText("已暂停"); } else { player.play(); toggle.setText("⏸"); status.setText("播放中"); } }
    private void previous() { switchTrack(-1); }
    private void next() { switchTrack(1); }
    private void switchTrack(int direction) {
        ObservableList<Song> source = recentSongs.isEmpty() ? songs : recentSongs;
        if (source.isEmpty()) return;
        int current = -1;
        if (currentSong != null) {
            for (int i = 0; i < source.size(); i++) {
                if (currentSong.id().equals(source.get(i).id())) { current = i; break; }
            }
        }
        if (current < 0) current = direction > 0 ? -1 : 0;
        int target;
        if (shuffleEnabled && source.size() > 1) {
            do { target = ThreadLocalRandom.current().nextInt(source.size()); } while (target == current);
        } else {
            target = current + direction;
            if (target < 0 || target >= source.size()) {
                if (repeatMode == 1) target = direction > 0 ? 0 : source.size() - 1;
                else return;
            }
        }
        Song song = source.get(target);
        if (!recentSongs.isEmpty()) {
            int resultIndex = songs.indexOf(song);
            if (resultIndex >= 0) list.getSelectionModel().select(resultIndex);
        } else {
            list.getSelectionModel().select(target);
        }
        play(song);
    }
    private void setActive(Button b, boolean active) { b.getStyleClass().removeAll("transport-active"); if (active) { b.getStyleClass().add("transport-active"); b.setStyle("-fx-background-color: #e8def8; -fx-text-fill: #4f378b;"); } else { b.setStyle("-fx-background-color: transparent; -fx-text-fill: #6750a4;"); } b.applyCss(); }
    private void transparentPopup(Window popup) { if (popup != null && popup.getScene() != null) popup.getScene().setFill(Color.TRANSPARENT); }
    private void addPressAnimation(Button button) { button.setOnMousePressed(e -> { button.setOpacity(.78); ScaleTransition t = new ScaleTransition(Duration.millis(90), button); t.setToX(.96); t.setToY(.96); t.playFromStart(); }); button.setOnMouseReleased(e -> release(button)); button.setOnMouseExited(e -> release(button)); }
    private void configureRipple(MFXButton button, double radius) { button.setComputeRadiusMultiplier(false); button.setRippleRadius(radius); button.setRippleAnimateBackground(true); button.setRippleAnimateShadow(false); button.setRippleBackgroundOpacity(.16); button.getRippleGenerator().setAutoClip(true); button.getRippleGenerator().setClipSupplier(() -> { Rectangle clip = new Rectangle(button.getWidth(), button.getHeight()); clip.setArcWidth(Math.min(button.getWidth(), button.getHeight())); clip.setArcHeight(Math.min(button.getWidth(), button.getHeight())); return clip; }); }
    private void addFieldFeedback(MFXTextField field) { field.setOnMouseEntered(e -> { ScaleTransition t = new ScaleTransition(Duration.millis(140), field); t.setToX(1.015); t.setToY(1.04); t.playFromStart(); }); field.setOnMouseExited(e -> { ScaleTransition t = new ScaleTransition(Duration.millis(140), field); t.setToX(1); t.setToY(1); t.playFromStart(); }); }
    private void release(Button button) { button.setOpacity(1); ScaleTransition t = new ScaleTransition(Duration.millis(130), button); t.setToX(1); t.setToY(1); t.playFromStart(); }
    private record LyricEntry(double time, Label label) { }
}
