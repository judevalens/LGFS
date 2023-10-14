package gfs;

 class File (var metadata: FileMetadata) {
    var chunks: ArrayList<ChunkMetadata>? = null

     init {
         chunks = ArrayList()
     }
 }
