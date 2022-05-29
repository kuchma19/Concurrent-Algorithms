package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private AtomicRef<Node> head;
    private AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0);
        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node node = new Node(x);
        while (true) {
            Node curTail = tail.getValue();
            AtomicRef<Node> next = curTail.next;
            if (curTail.equals(tail.getValue())) {
                if (next.getValue() == null) {
                    if (next.compareAndSet(null, node)) {
                        break;
                    }
                } else {
                    tail.compareAndSet(curTail, next.getValue());
                }
            }
        }
    }


    private int dequeueOrPeek(boolean isDequeue) {
        while (true) {
            Node curHead = head.getValue();
            Node curTail = tail.getValue();
            AtomicRef<Node> next = curHead.next;
            if (curHead.equals(head.getValue())) {
                if (curHead.equals(curTail)) {
                    if (next.getValue() == null) {
                        return Integer.MIN_VALUE;
                    }
                    tail.compareAndSet(curTail, next.getValue());
                } else {
                    if (head.compareAndSet(curHead, isDequeue ? next.getValue() : curHead)) {
                        return next.getValue().x;
                    }
                }
            }
        }
    }

    @Override
    public int dequeue() {
        return dequeueOrPeek(true);
    }

    @Override
    public int peek() {
        return dequeueOrPeek(false);
    }

    private static class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x) {
            this.next = new AtomicRef<>(null);
            this.x = x;
        }
    }
}