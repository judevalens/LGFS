package lgfs.gfs.allocator

import lgfs.gfs.ChunkServerState
import lgfs.gfs.FileMetadata
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AllocatorTest {

    private lateinit var allocatorUnderTest: Allocator
    private lateinit var chunkServerStates: MutableList<ChunkServerState>

    @BeforeTest
    fun before() {
        allocatorUnderTest = Allocator()
        chunkServerStates = ArrayList()
        for (i in 0..5) {
            chunkServerStates.add(ChunkServerState("testHostname$i"))
        }
    }

    @Test
    fun `test allocating chunks with 1 chunk server available`() {

        allocatorUnderTest.updateState(ChunkServerState("testHostname"))
        val fileMetadata = FileMetadata("test.txt", false, 1000000);
        val res = allocatorUnderTest.allocateChunks(fileMetadata)
        assertEquals(true, res.first)
        assertEquals(1,res.second?.size)
    }
    @Test
    fun `test server state is not duplicated`() {

        allocatorUnderTest.updateState(ChunkServerState("testHostname"))
        allocatorUnderTest.updateState(ChunkServerState("testHostname"))
        allocatorUnderTest.updateState(ChunkServerState("testHostname"))
        val fileMetadata = FileMetadata("test.txt", false, 1000000);
        val res = allocatorUnderTest.allocateChunks(fileMetadata)
        assertEquals(true, res.first)
        assertEquals(1,res.second?.get(0)?.replicas?.size)
    }

    @Test
    fun `test allocating chunks with 0 chunk server available`() {
        val fileMetadata = FileMetadata("test.txt", false, 1000000);
        val res = allocatorUnderTest.allocateChunks(fileMetadata)
        assertEquals(false, res.first)
    }

    @Test
    fun `test allocating the correct number of chunks`() {
        val size = Allocator.CHUNK_SIZE * 5L
        val nChunks: Long = 5
        allocatorUnderTest.updateState(chunkServerStates[0])
        val fileMetadata = FileMetadata("test.txt", false, size);
        val res = allocatorUnderTest.allocateChunks(fileMetadata)
        assertEquals(true, res.first)
        assertEquals(nChunks.toInt(), res.second?.size)
    }

    @Test
    fun `test allocating replicas in the correct order`() {
        val size = Allocator.CHUNK_SIZE * 2L
        val nChunks: Long = 2
        val state1: ChunkServerState = mock()
        val state2: ChunkServerState = mock()
        val state3: ChunkServerState = mock()
        whenever(state1.chunkServerHostName).thenReturn("hostname1")
        whenever(state2.chunkServerHostName).thenReturn("hostname2")
        whenever(state3.chunkServerHostName).thenReturn("hostname3")
        whenever(state3.getRanking()).thenReturn(10.0)
        whenever(state2.getRanking()).thenReturn(5.0)
        whenever(state1.getRanking()).thenReturn(1.0)
        allocatorUnderTest.updateState(state1)
        allocatorUnderTest.updateState(state2)
        allocatorUnderTest.updateState(state3)
        val fileMetadata = FileMetadata("test.txt", false, size);
        val res = allocatorUnderTest.allocateChunks(fileMetadata)
        assertEquals(true, res.first)
        assertEquals(nChunks.toInt(), res.second?.size)
        assertNotNull(res.second)
        res.second?.forEach {
            assertEquals("hostname3", it.replicas[2])
        }
    }


}