package dijkstra

import kotlinx.atomicfu.AtomicInt
import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

private val random = Random()

private var count: AtomicInteger = AtomicInteger(0);

fun addToMul(mulQueue: List<PriorityQueue<Node>>, node: Node) {
    val randVal: Int = random.nextInt(mulQueue.size)
    val queue: PriorityQueue<Node> = mulQueue[randVal]
    synchronized(queue) {
        count.incrementAndGet()
        queue.add(node);
    }
}

fun getTwoInt(bound: Int): Pair<Int, Int> {
    var firstRand: Int = -1
    var secondRand: Int = -1
    while (firstRand == secondRand) {
        firstRand = random.nextInt(bound)
        secondRand = random.nextInt(bound)
        if (firstRand > secondRand) {
            firstRand = secondRand.also { secondRand = firstRand }
        }
    }
    return Pair(firstRand, secondRand)
}

fun getElement(mulQueue: List<PriorityQueue<Node>>): Node? {
    while (!count.compareAndSet(0, 0)) {
        val indexes: Pair<Int, Int> = getTwoInt(mulQueue.size)
        val firstQueue: PriorityQueue<Node> = mulQueue[indexes.first]
        val secondQueue: PriorityQueue<Node> = mulQueue[indexes.second]
        var ans: Node?
        synchronized(firstQueue) {
            synchronized(secondQueue) {
                val fst: Node? = firstQueue.peek()
                val snd: Node? = secondQueue.peek()
                if (fst == null) {
                    ans = if (snd != null) {
                        secondQueue.remove()
                        snd
                    } else {
                        null
                    }
                } else {
                    ans = if (snd == null) {
                        firstQueue.remove()
                        fst
                    } else {
                        if (NODE_DISTANCE_COMPARATOR.compare(fst, snd) > 0) {
                            secondQueue.remove()
                            snd
                        } else {
                            firstQueue.remove()
                            fst
                        }
                    }
                }
            }
        }
        if (ans != null) {
            count.decrementAndGet()
            return ans
        }
    }
    return null
}

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    // val q = PriorityQueue(workers, NODE_DISTANCE_COMPARATOR) // TODO replace me with a multi-queue based PQ!
    val qs: MutableList<PriorityQueue<Node>> = mutableListOf()
    for (i in 0 until workers) {
        qs += listOf(PriorityQueue(workers, NODE_DISTANCE_COMPARATOR))
    }
    addToMul(qs, start)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (true) {
                // TODO Write the required algorithm here,
                // TODO break from this loop when there is no more node to process.
                // TODO Be careful, "empty queue" != "all nodes are processed".
               val cur: Node? = getElement(qs)
                if (cur == null) {
                    if (count.compareAndSet(0, 0)) {
                        break
                    }
                    continue
                }
                for (e in cur.outgoingEdges) {
                   if (e.to.distance > cur.distance + e.weight) {
                       var curDist = cur.distance + e.weight
                       var oldDist = e.to.distance
                       while (oldDist > curDist && !e.to.casDistance(oldDist, curDist)) {
                           oldDist = e.to.distance
                           curDist = cur.distance + e.weight
                           if (oldDist <= curDist) {
                               break
                           }
                       }
                       addToMul(qs, e.to)
                   }
               }
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}