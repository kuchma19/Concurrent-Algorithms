import java.util.function.Supplier

/**
 * @author Kuchma Andrey
 */
class Solution : AtomicCounter {
    // объявите здесь нужные вам поля

    private val root: Node = Node(0)
    private val threadLocal: ThreadLocal<Node> = ThreadLocal.withInitial { root }

    override fun getAndAdd(x: Int): Int {
        while (true) {
            val y = threadLocal.get().x
            val newNode = Node(y + x)
            val node = threadLocal.get().consensus.decide(newNode)
            threadLocal.set(node)
            if (node == newNode) {
                return y
            }
        }
    }

    // вам наверняка потребуется дополнительный класс
    private class Node(val x: Int) {
        val consensus: Consensus<Node> = Consensus()
    }
}
