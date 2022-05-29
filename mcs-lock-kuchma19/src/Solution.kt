import java.util.concurrent.atomic.AtomicReference

class Solution(val env: Environment) : Lock<Solution.Node> {
    private val tail: AtomicReference<Node?> = AtomicReference(null)

    override fun lock(): Node {
        val my = Node() // сделали узел
        my.isLock.set(true)
        val pred = tail.getAndSet(my)
        if (pred != null) {
            pred.nextNode.set(my)
            while (my.isLock.get()) {
                env.park()
            }
        }
        return my // вернули узел
    }

    override fun unlock(node: Node) {
        if (node.nextNode.get() == null) {
            if (tail.compareAndSet(node, null)) {
                return
            } else {
                while (node.nextNode.get() == null) {
                    // pass
                }
            }
        }
        node.nextNode.value!!.isLock.set(false)
        env.unpark(node.nextNode.get()!!.thread)
    }

    class Node {
        val thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val nextNode: AtomicReference<Node?> = AtomicReference(null)
        val isLock: AtomicReference<Boolean> = AtomicReference(false)
        // todo: необходимые поля (val, используем AtomicReference)
    }
}