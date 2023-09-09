package gfs;

import java.util.ArrayList;

public class FileMetadata {
    String key;
    long size;
    boolean isDir;
    ArrayList<Chunk> chunks;

    public FileMetadata(String key) {
        this.key = key;
    }

    public FileMetadata(int size) {
        var numChunks = (int) Math.ceil(size / (double) FileSystem.CHUNK_SIZE);
        chunks = new ArrayList<>(numChunks);
    }

}
