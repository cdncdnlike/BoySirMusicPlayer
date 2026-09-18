package lezi.boysir.musicplayer;

import javafx.scene.paint.Color;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AppState {
    public double volume = .8, speed = 1.0, floatingSize = 28, translationSize = 16, floatingOpacity = 1, lyricOffset = 0;
    public int sleepTimerMinutes;
    public String floatingColor = "#FFFFFF", translationColor = "#EBEBEB", source = "网易云";
    public boolean showTranslation = true, floatingTop;
    public boolean floatingVisible;
    public boolean minimizeToTray = true;
    public boolean autoStart;
    public boolean keyboardShortcuts = true;
    public String shortcutPlay = "SPACE", shortcutPrevious = "LEFT", shortcutNext = "RIGHT";
    public String theme = "跟随系统", backgroundPath = "";
    public final List<Song> favorites = new ArrayList<>();
    public final List<Song> queue = new ArrayList<>();
    public final List<Song> history = new ArrayList<>();
    public final List<Song> localSongs = new ArrayList<>();

    public static Path configDirectory() {
        return Path.of(System.getProperty("user.home"), ".boysir-music-player");
    }
    public static Path configFile() {
        return configDirectory().resolve("config.json");
    }
    private static Path file() {
        return configFile();
    }

    public static void clearFile() throws IOException {
        Files.deleteIfExists(configFile());
    }

    public static AppState load() {
        AppState state = new AppState();
        try {
            if (!Files.exists(file())) return state;
            String json = Files.readString(file(), StandardCharsets.UTF_8);
            state.volume = number(json, "volume", state.volume);
            state.speed = number(json, "speed", state.speed);
            state.floatingSize = number(json, "floatingSize", state.floatingSize);
            state.translationSize = number(json, "translationSize", state.translationSize);
            state.floatingOpacity = number(json, "floatingOpacity", state.floatingOpacity);
            state.lyricOffset = number(json, "lyricOffset", state.lyricOffset);
            state.sleepTimerMinutes = (int) number(json, "sleepTimerMinutes", state.sleepTimerMinutes);
            state.floatingColor = string(json, "floatingColor", state.floatingColor);
            state.translationColor = string(json, "translationColor", state.translationColor);
            state.source = string(json, "source", state.source);
            state.showTranslation = bool(json, "showTranslation", state.showTranslation);
            state.floatingTop = bool(json, "floatingTop", state.floatingTop);
            state.floatingVisible = bool(json, "floatingVisible", state.floatingVisible);
            state.minimizeToTray = bool(json, "minimizeToTray", state.minimizeToTray);
            state.autoStart = bool(json, "autoStart", state.autoStart);
            state.keyboardShortcuts = bool(json, "keyboardShortcuts", state.keyboardShortcuts);
            state.shortcutPlay = string(json, "shortcutPlay", state.shortcutPlay);
            state.shortcutPrevious = string(json, "shortcutPrevious", state.shortcutPrevious);
            state.shortcutNext = string(json, "shortcutNext", state.shortcutNext);
            state.theme = string(json, "theme", state.theme);
            state.backgroundPath = string(json, "backgroundPath", state.backgroundPath);
            readSongs(json, "favorites", state.favorites);
            readSongs(json, "queue", state.queue);
            readSongs(json, "history", state.history);
            readSongs(json, "localSongs", state.localSongs);
            state.localSongs.removeIf(s -> s.id() == null || !s.id().startsWith("local:") || !Files.isRegularFile(Path.of(s.id().substring(6))));
            // Older builds stored percentage-like values for these controls.
            // MFXSlider uses normalized values, so migrate those files once on load.
            state.volume = normalized(state.volume, .8);
            state.floatingOpacity = normalized(state.floatingOpacity, 1);
        } catch (Exception ignored) {
            // A malformed preference file should never prevent the player from starting.
        }
        return state;
    }

    private static double normalized(double value, double fallback) {
        if (!Double.isFinite(value)) return fallback;
        if (value > 1.0 && value <= 100.0) value /= 100.0;
        return Math.max(0.0, Math.min(1.0, value));
    }

    public void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), toJson(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Preferences are best-effort; playback must continue if the disk is read-only.
        }
    }

    private String toJson() {
        return "{\n" +
                "  \"volume\": " + volume + ",\n" +
                "  \"speed\": " + speed + ",\n" +
                "  \"floatingSize\": " + floatingSize + ",\n" +
                "  \"translationSize\": " + translationSize + ",\n" +
                "  \"floatingOpacity\": " + floatingOpacity + ",\n" +
                "  \"lyricOffset\": " + lyricOffset + ",\n" +
                "  \"sleepTimerMinutes\": " + sleepTimerMinutes + ",\n" +
                "  \"floatingColor\": \"" + escape(floatingColor) + "\",\n" +
                "  \"translationColor\": \"" + escape(translationColor) + "\",\n" +
                "  \"source\": \"" + escape(source) + "\",\n" +
                "  \"showTranslation\": " + showTranslation + ",\n" +
                "  \"floatingTop\": " + floatingTop + ",\n" +
                "  \"floatingVisible\": " + floatingVisible + ",\n" +
                "  \"minimizeToTray\": " + minimizeToTray + ",\n" +
                "  \"autoStart\": " + autoStart + ",\n" +
                "  \"keyboardShortcuts\": " + keyboardShortcuts + ",\n" +
                "  \"shortcutPlay\": \"" + escape(shortcutPlay) + "\",\n" +
                "  \"shortcutPrevious\": \"" + escape(shortcutPrevious) + "\",\n" +
                "  \"shortcutNext\": \"" + escape(shortcutNext) + "\",\n" +
                "  \"theme\": \"" + escape(theme) + "\",\n" +
                "  \"backgroundPath\": \"" + escape(backgroundPath) + "\",\n" +
                "  \"favorites\": " + songsJson(favorites) + ",\n" +
                "  \"queue\": " + songsJson(queue) + ",\n" +
                "  \"history\": " + songsJson(history) + ",\n" +
                "  \"localSongs\": " + songsJson(localSongs) + "\n" +
                "}\n";
    }

    private static String songsJson(List<Song> songs) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < songs.size(); i++) {
            if (i > 0) out.append(',');
            Song s = songs.get(i);
            out.append("{\"id\":\"").append(escape(s.id())).append("\",\"name\":\"")
                    .append(escape(s.name())).append("\",\"artist\":\"").append(escape(s.artist()))
                    .append("\",\"album\":\"").append(escape(s.album())).append("\",\"coverUrl\":\"")
                    .append(escape(s.coverUrl())).append("\",\"lyric\":\"").append(escape(s.lyric())).append("\"}");
        }
        return out.append(']').toString();
    }

    private static void readSongs(String json, String key, List<Song> target) {
        int keyAt = json.indexOf("\"" + key + "\"");
        if (keyAt < 0) return;
        int start = json.indexOf('[', keyAt);
        if (start < 0) return;
        int depth = 0, end = -1;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']' && --depth == 0) { end = i; break; }
        }
        if (end < 0) return;
        String section = json.substring(start + 1, end);
        int cursor = 0;
        while ((cursor = section.indexOf("{\"id\"", cursor)) >= 0) {
            int close = section.indexOf("\"}", cursor);
            if (close < 0) break;
            String object = section.substring(cursor, close + 1);
            target.add(new Song(string(object, "id", ""), string(object, "name", ""), string(object, "artist", ""),
                    string(object, "album", ""), string(object, "coverUrl", ""), string(object, "lyric", "")));
            cursor = close + 2;
        }
    }

    private static String string(String json, String key, String fallback) {
        String marker = "\"" + key + "\"";
        int keyAt = json.indexOf(marker);
        if (keyAt < 0) return fallback;
        int colon = json.indexOf(':', keyAt + marker.length());
        if (colon < 0) return fallback;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length() || json.charAt(start) != '"') return fallback;
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    default -> value.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }
        return fallback;
    }
    private static double number(String json, String key, double fallback) {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)").matcher(json);
        try { return m.find() ? Double.parseDouble(m.group(1)) : fallback; } catch (NumberFormatException e) { return fallback; }
    }
    private static boolean bool(String json, String key, boolean fallback) {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(true|false)").matcher(json);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : fallback;
    }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n"); }
    private static String unescape(String value) { return value.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"").replace("\\\\", "\\"); }
}
