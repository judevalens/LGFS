import gfs.FileMetadata
import gfs.FileSystem
import network.Manager
import utils.radixtree.RadixTree;
import utils.radixtree.StringKey

fun main() {
    val fs =  FileSystem()
    fs.addFile(FileMetadata.newFile("notes/math/calc/test.txt",1000))
    fs.addFile(FileMetadata.newFile("notes/math/calc/notes1.txt",1000))
    fs.addFile(FileMetadata.newFile("notes/chem/organic/notes1.txt",1000))
    Manager.launch()
}