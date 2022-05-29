import kotlinx.atomicfu.*

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<E>(INITIAL_CAPACITY, null, 0))

    override fun get(index: Int): E {
        val curCore: Core<E> = core.value
        if (index < curCore.length.value) {
            val curElement: E? = curCore.getElement(index)
            if (curElement != null) {
                return curElement;
            }
            throw NullPointerException()
        } else {
            throw IllegalArgumentException()
        }
    }

    override fun put(index: Int, element: E) {
        var curCore: Core<E> = core.value
        if (index < curCore.length.value) {
            while (true) {
                curCore.setElement(index, element)
                curCore = curCore.next.value ?: break
            }
        } else {
            throw IllegalArgumentException()
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val curCore: Core<E> = core.value
            val curLen: Int = curCore.getLen()
            val curCap: Int = curCore.getCap()
            if (curLen < curCap) {
                if (curCore.compareAndSetInIndex(curLen, null, element)) {
                    curCore.length.compareAndSet(curLen, curLen + 1)
                    break
                } else {
                    curCore.length.compareAndSet(curLen, curLen + 1)
                }
            } else {
                val newNextCore = Core<E>(curCap * 2, null, curLen)
                if (curCore.next.compareAndSet(null, newNextCore)) {
                    for (i in 0 until curLen) {
                        newNextCore.compareAndSetInIndex(i, null, curCore.getElement(i))
                    }
                    core.compareAndSet(curCore, newNextCore)
                } else {
                    val nextCore = curCore.next.value
                    if (nextCore != null) {
                        for (i in 0 until curLen) {
                            nextCore.compareAndSetInIndex(i, null, curCore.getElement(i))
                        }
                    }
                }
            }
        }
    }

    override val size: Int get() {
        return core.value.length.value
    }
}

private class Core<E>(
    capacity: Int,
    nextCore: Core<E>?,
    len: Int,
) {
    private val array: AtomicArray<E?> = atomicArrayOfNulls<E>(capacity)
    val cap: AtomicInt = atomic(capacity)
    val next: AtomicRef<Core<E>?> = atomic(nextCore)
    val length: AtomicInt = atomic(len)

    fun getElement(index: Int) : E? {
        return array[index].value
    }

    fun setElement(index: Int, element: E) {
        array[index].getAndSet(element)
    }

    fun compareAndSetInIndex(index: Int, currentElement: E?, setElement: E?) : Boolean {
        return array[index].compareAndSet(currentElement, setElement)
    }

    fun getLen() : Int {
        return length.value
    }

    fun getCap() : Int {
        return cap.value
    }

}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME