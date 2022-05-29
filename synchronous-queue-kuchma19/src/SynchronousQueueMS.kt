import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import java.lang.RuntimeException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private class NodeQ<E>(continuation: Continuation<Boolean>?, element: E?, isSend: Boolean, next: NodeQ<E>?) {
    val cont: AtomicRef<Continuation<Boolean>?> = atomic(continuation)
    val elem: AtomicRef<E?> = atomic(element)
    val isSend: AtomicBoolean = atomic(isSend)
    val next: AtomicRef<NodeQ<E>?> = atomic(next)
}

private class MSQueue<E> {
    val head: AtomicRef<NodeQ<E>>
    val tail: AtomicRef<NodeQ<E>>

    init {
        val dummy = NodeQ<E>(null, null, false, null)
        head = atomic(dummy)
        tail = atomic(dummy)
    }

    fun enqueue(x: E, currentHead: NodeQ<E>): Boolean {
        return enqueue(null, x, true, currentHead)
    }

    fun enqueue(x: Continuation<Boolean>, currentHead: NodeQ<E>): Boolean {
        return enqueue(x, null, false, currentHead)
    }

    fun enqueue(x: Continuation<Boolean>?, y: E?, isSend: Boolean, currentHead: NodeQ<E>): Boolean {
        val newHead: NodeQ<E> = NodeQ(x, y, isSend, null)
        return if (currentHead.next.compareAndSet(null, newHead)) {
            head.compareAndSet(currentHead, newHead)
            true
        } else {
            head.compareAndSet(currentHead, currentHead.next.value!!)
            false;
        }
    }

    fun dequeue(currentTail: NodeQ<E>): NodeQ<E>? {
        val nextTail = currentTail.next.value ?: return null
        return if (tail.compareAndSet(currentTail, nextTail)) {
            nextTail
        } else {
            null
        }
    }


}


class SynchronousQueueMS<E> : SynchronousQueue<E> {
    private val q = MSQueue<E>()

    override suspend fun send(element: E) {
        while (true) {
            val t = q.tail.value
            val h = q.head.value
            if (h == t || h.isSend.value) {
                val resSuspend = suspendCoroutine<Boolean> suspend@{ cont ->
                    if (!q.enqueue(cont, element, true, h)) {
                        cont.resume(false)
                        return@suspend
                    }
                }
                if (resSuspend) {
                    return
                } else {
                    continue
                }
            } else {
                val el = q.dequeue(t) ?: continue
                el.elem.getAndSet(element)
                val valCont = el.cont.value ?: continue
                valCont.resume(true)
                return
            }
        }
    }

    override suspend fun receive(): E {
        while (true) {
            val t = q.tail.value
            val h = q.head.value
            if (h == t || !h.isSend.value) {
                val resSuspend = suspendCoroutine<Boolean> suspend@{ cont ->
                    if (!q.enqueue(cont, h)) {
                        cont.resume(false)
                        return@suspend
                    }
                }
                if (resSuspend) {
                    return h.next.value?.elem?.value ?: throw RuntimeException()
                }
            } else {
                val el = q.dequeue(t) ?: continue
                val valCont = el.cont.value ?: continue
                valCont.resume(true)
                return el.elem.value ?: throw RuntimeException()
            }
        }
    }
}





