package gfs;

import java.util.concurrent.atomic.AtomicLong;

public class FileSystem {
    AtomicLong lastChunkId = new AtomicLong();
    final static int CHUNK_SIZE = 64*1000000;
}
