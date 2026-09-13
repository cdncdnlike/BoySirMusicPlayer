package lezi.boysir.musicplayer;

public record Song(String id, String name, String artist, String album, String coverUrl, String lyric) {
    public Song(String id, String name, String artist, String album) { this(id, name, artist, album, "", ""); }
    public Song withDetails(String cover, String text) { return new Song(id, name, artist, album, cover, text); }
    public Song withAlbum(String value) { return new Song(id, name, artist, value, coverUrl, lyric); }
}
