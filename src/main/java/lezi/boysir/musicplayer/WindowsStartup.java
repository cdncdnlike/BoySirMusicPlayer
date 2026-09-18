package lezi.boysir.musicplayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Enables the packaged application in the current Windows user's startup registry. */
public final class WindowsStartup {
    private static final String RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String VALUE_NAME = "BoySirMusicPlayer";

    private WindowsStartup() { }

    public static void setEnabled(boolean enabled) {
        if (!isWindows()) return;
        try {
            if (enabled) {
                String command = launchCommand();
                if (command.isBlank()) return;
                run("reg", "add", RUN_KEY, "/v", VALUE_NAME, "/t", "REG_SZ", "/d", command, "/f");
            } else {
                run("reg", "delete", RUN_KEY, "/v", VALUE_NAME, "/f");
            }
        } catch (Exception ignored) { }
    }

    private static String launchCommand() {
        String packagedPath = System.getProperty("jpackage.app-path", "");
        if (!packagedPath.isBlank() && Files.exists(Path.of(packagedPath))) return quote(packagedPath);
        String command = ProcessHandle.current().info().command().orElse("");
        return command.isBlank() ? "" : quote(command);
    }

    private static void run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        process.waitFor();
    }

    private static String quote(String value) { return "\"" + value.replace("\"", "\\\"") + "\""; }
    private static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }
}
