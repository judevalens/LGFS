package utils.radixtree.concurrent

import java.lang.NullPointerException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

class Lock {

    private val maxPermit = 100;
    private val mutex : Semaphore = Semaphore(maxPermit,true)

    private val permitHolder : ConcurrentHashMap<Long,Boolean> = ConcurrentHashMap();

    private val deleteSemaphore = Semaphore(1,true);

    fun readLock() {
        val id  = Thread.currentThread().threadId()
        if (permitHolder.contains(id)) {
            throw IllegalStateException("Reentrant locks arent allowed")
        }
        mutex.acquire()
    }

    fun unlockRead() {
        mutex.release()
    }

    fun writeLock() {
        val id  = Thread.currentThread().threadId()
        if (permitHolder.contains(id)) {
            throw IllegalStateException("Reentrant locks arent allowed")
        }
        mutex.acquire(maxPermit)
    }

    fun unlockWrite() {
        val id  = Thread.currentThread().threadId()
        mutex.release(maxPermit)
        permitHolder.remove(id)
    }

    fun lockDelete(){
        deleteSemaphore.acquire()
    }

    fun unlockDelete() {
        deleteSemaphore.release()
    }

}