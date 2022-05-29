import kotlinx.atomicfu.AtomicArray
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import java.util.*

class FCPriorityQueue<E : Comparable<E>> {
    private val q = PriorityQueue<E>()
    private val ARRAY_SIZE = 16
    private val array: AtomicArray<ConditionAndValue<E>?> = atomicArrayOfNulls(ARRAY_SIZE)
    private val isLocked: AtomicBoolean = atomic(false)
    private val random: Random = Random()

    private fun doArrayConditions() {
        for (i in 0 until ARRAY_SIZE) {
            val curCondition = array[i].value ?: continue
            when (curCondition.condition) {
                TypeCondition.ADD -> {
                    if (array[i].compareAndSet(curCondition, ConditionAndValue(null, TypeCondition.DONE))) {
                        q.add(curCondition.value)
                    }
                }
                TypeCondition.PEEK -> {
                    array[i].compareAndSet(curCondition, ConditionAndValue(q.peek(), TypeCondition.DONE))
                }
                TypeCondition.POLL -> {
                    val value = q.peek()
                    if (array[i].compareAndSet(curCondition, ConditionAndValue(value, TypeCondition.DONE))) {
                        q.poll()
                    }
                }
                else -> {
                    continue
                }
            }
        }
    }

    private fun addToArrayCondition(condition: ConditionAndValue<E>?): ConditionAndValue<E>? {
        while (true) {
            val rnd = random.nextInt(ARRAY_SIZE)
            val l = rnd - 1
            val r = rnd + 1
            for (I in l..r) {
                val i =
                    if (I >= ARRAY_SIZE) I - ARRAY_SIZE
                    else if (I < 0) I + ARRAY_SIZE
                    else I
                if (array[i].compareAndSet(null, condition)) {
                    while (true) {
                        val curCondition = array[i].value ?: return null
                        if (curCondition.condition == TypeCondition.DONE) {
                            array[i].compareAndSet(curCondition, null)
                            return curCondition
                        } else {
                            if (array[i].compareAndSet(curCondition, null)) {
                                return null
                            }
                        }
                    }
                }
            }
        }
    }

    private fun lock(): Boolean {
        return isLocked.compareAndSet(expect = false, update = true)
    }

    private fun unlock() {
        isLocked.getAndSet(false)
    }

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        while (true) {
            return if (lock()) {
                try {
                    doArrayConditions()
                    q.poll()
                } finally {
                    unlock()
                }
            } else {
                val conditionAndValue = addToArrayCondition(ConditionAndValue(null, TypeCondition.POLL)) ?: continue
                conditionAndValue.value
            }
        }
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        while (true) {
            return if (lock()) {
                try {
                    doArrayConditions()
                    q.peek()
                } finally {
                    unlock()
                }
            } else {
                val conditionAndValue = addToArrayCondition(ConditionAndValue(null, TypeCondition.PEEK)) ?: continue
                conditionAndValue.value
            }
        }
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        while (true) {
            if (lock()) {
                doArrayConditions()
                q.add(element)
                unlock()
            } else {
                addToArrayCondition(ConditionAndValue(element, TypeCondition.ADD)) ?: continue
            }
            break
        }
    }

    private enum class TypeCondition {
        ADD,
        POLL,
        PEEK,
        DONE,
    }

    private class ConditionAndValue<E>(
        val value: E?,
        val condition: TypeCondition,
    )
}