package lezi.boysir.musicplayer;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.io.IOException;
import java.time.Duration;
import java.util.regex.*;

public class MusicApi {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final Map<String,String> qqLyrics = new HashMap<>(), qqUrls = new HashMap<>();
    public record UpdateInfo(String version, String downloadUrl, String releaseNotes, boolean forceUpdate) { }
    public CompletableFuture<UpdateInfo> checkUpdate() {
        return get("https://www.alicemana.cn/checkupdate/version.json").thenApply(j -> new UpdateInfo(
                find(j, "version"), find(j, "downloadUrl"), find(j, "releaseNotes"), Pattern.compile("\\\"forceUpdate\\\"\\s*:\\s*true", Pattern.CASE_INSENSITIVE).matcher(j).find())).orTimeout(8, java.util.concurrent.TimeUnit.SECONDS);
    }
    public CompletableFuture<List<Song>> search(String key, String source) { return "腾讯云".equals(source) ? get("https://cyapi.top/API/qq_music.php?apikey=62ccfd8be755cc5850046044c6348d6cac5ef31bd5874c1352287facc06f94c4&msg=" + enc(key) + "&num=30&type=json").thenApply(this::qqSongs) : get("https://msapi.awup.cn/search?keywords=" + enc(key)).thenApply(this::songs); }
    public CompletableFuture<List<Song>> search(String key) { return search(key, "网易云"); }
    public CompletableFuture<Song> details(Song song) { return song.id().startsWith("qq:") ? qqDetails(song) : get("https://msapi.awup.cn/song/detail?ids=" + enc(song.id())).thenApply(j -> song.withDetails(find(j, "picUrl"), "").withAlbum(findAfter(j, "\\\"al\\\"", "name"))); }
    public CompletableFuture<String> lyric(Song song) { return song.id().startsWith("qq:") ? qqDetails(song).thenApply(Song::lyric) : lyric(song.id()); }
    public CompletableFuture<String> lyric(String id) { if (id.startsWith("qq:")) return CompletableFuture.completedFuture(qqLyrics.getOrDefault(id, "")); return get("https://msapi.awup.cn/lyric?id=" + enc(id)).thenApply(j -> findNested(j, "lrc", "lyric") + "\n---TRANSLATION---\n" + findNested(j, "tlyric", "lyric")); }
    public CompletableFuture<String> audioUrl(String id) {
        if (id.startsWith("qq:")) return CompletableFuture.completedFuture(qqUrls.getOrDefault(id, ""));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.qijieya.cn/meting/?type=url&id=" + enc(id))).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).thenApply(response -> {
            String finalUrl = response.uri().toString();
            if (!finalUrl.contains("api.qijieya.cn")) return finalUrl;
            return url(new String(response.body(), StandardCharsets.UTF_8));
        });
    }
    public CompletableFuture<String> audioUrl(Song song) { return song.id().startsWith("qq:") ? qqAudio(song) : audioUrl(song.id()); }
    public CompletableFuture<String> randomSaying() { return get("https://uapis.cn/api/v1/saying/random").thenApply(j -> { String content=find(j,"content"), source=find(j,"source"), author=find(j,"author"); if(content.isBlank()) return "愿每一次播放，都有新的发现"; return content + (source.isBlank() ? "" : "  ·  " + source) + (author.isBlank() ? "" : "  ——" + author); }); }
    public CompletableFuture<Path> download(Song song, Path target) {
        return audioUrl(song).thenCompose(url -> {
            if (url == null || url.isBlank()) return CompletableFuture.failedFuture(new IOException("没有可用的音频地址"));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).thenApply(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) throw new CompletionException(new IOException("下载失败: HTTP " + response.statusCode()));
                try { Files.write(target, response.body()); return target; } catch (IOException e) { throw new CompletionException(e); }
            });
        });
    }
    public CompletableFuture<Song> qqDetails(Song song) { String[] p=song.id().split(":",3); String n=p.length>1?p[1]:"1"; return get("https://cyapi.top/API/qq_music.php?apikey=62ccfd8be755cc5850046044c6348d6cac5ef31bd5874c1352287facc06f94c4&msg="+enc(song.name())+"&num=30&n="+n+"&type=json").thenApply(j -> { String lyric=findNested(j,"lyric","text"), audio=find(j,"url"); qqLyrics.put(song.id(),lyric); qqUrls.put(song.id(),audio); return song.withDetails(findNested(j,"cover","large"), lyric); }); }
    public CompletableFuture<String> qqAudio(Song song) { String[] p=song.id().split(":",3); String n=p.length>1?p[1]:"1"; return get("https://cyapi.top/API/qq_music.php?apikey=62ccfd8be755cc5850046044c6348d6cac5ef31bd5874c1352287facc06f94c4&msg="+enc(song.name())+"&num=30&n="+n+"&type=json").thenApply(j -> find(j,"url")); }
    private String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private CompletableFuture<String> get(String url) { return client.sendAsync(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).header("Accept", "application/json").GET().build(), HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body); }
    private List<Song> songs(String json) {
        List<Song> result = new ArrayList<>();
        Matcher item = Pattern.compile("\\\"fee\\\"\\s*:[\\s\\S]*?(?=\\\"fee\\\"\\s*:|\\\"hasMore\\\")").matcher(json);
        while (item.find() && result.size() < 30) {
            String block = item.group();
            Matcher song = Pattern.compile("\\\"artists\\\"\\s*:\\s*\\[(.*?)\\][\\s\\S]*?\\\"name\\\"\\s*:\\s*\\\"(.*?)\\\"[\\s\\S]*?\\\"id\\\"\\s*:\\s*(\\d+)").matcher(block);
            if (!song.find()) continue;
            String artist = song.group(1).replaceAll("[\\s\\S]*?\\\"name\\\"\\s*:\\s*\\\"(.*?)\\\"[\\s\\S]*", "$1");
            result.add(new Song(song.group(3), clean(song.group(2)), clean(artist), ""));
        }
        return result;
    }
    private List<Song> qqSongs(String json) { List<Song> out=new ArrayList<>(); Matcher m=Pattern.compile("\\{\\s*\\\"name\\\"\\s*:\\s*\\\"(.*?)\\\"[\\s\\S]*?\\\"artists\\\"\\s*:\\s*\\\"(.*?)\\\"[\\s\\S]*?\\\"id\\\"\\s*:\\s*\\\"(.*?)\\\"[\\s\\S]*?\\\"cover\\\"\\s*:\\s*\\\"(.*?)\\\"\\s*\\}").matcher(json); int n=1; while(m.find()&&out.size()<30) out.add(new Song("qq:"+n+++":"+clean(m.group(3)),clean(m.group(1)),clean(m.group(2)),"",clean(m.group(4)),"")); return out; }
    private String url(String json) { Matcher m = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"(.*?)\\\"").matcher(json); return m.find() ? clean(m.group(1)) : (json.startsWith("http") ? json.trim() : ""); }
    private String find(String json, String key) { Matcher m = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(json); return m.find() ? clean(m.group(1)) : ""; }
    private String findAfter(String json, String anchor, String key) { int at = json.indexOf(anchor); return at < 0 ? "" : find(json.substring(at), key); }
    private String findNested(String json, String object, String key) { int at = json.indexOf("\"" + object + "\""); return at < 0 ? "" : find(json.substring(at), key); }
    private String clean(String value) { return value.replace("\\n", "\n").replace("\\r", "\r").replace("\\/", "/").replace("\\\"", "\""); }
}
