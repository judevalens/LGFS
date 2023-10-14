package utils.radixtree

import java.util.concurrent.locks.ReentrantReadWriteLock

abstract class Lockable {
    val lock: ReentrantReadWriteLock = ReentrantReadWriteLock();

    fun isRWLocked() : Boolean {
        return lock.readLock().tryLock()
    }
}
