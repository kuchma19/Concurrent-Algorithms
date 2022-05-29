package stack;

import java.util.Random;

import kotlinx.atomicfu.AtomicArray;
import kotlinx.atomicfu.AtomicRef;

public class StackImpl implements Stack {
    private static final int NUMBER_ITERATIONS = 10;
    private static final int SIZE_ARRAY = 16;

    private final AtomicRef<Node> head = new AtomicRef<>(null);
    private final AtomicArray<Node> array = new AtomicArray<>(SIZE_ARRAY);
    private final Random random = new Random();

    @Override
    public void push(int x) {
        if (waitPop(x)) {
            return;
        }
        while (true) {
            Node curNode = head.getValue();
            if (head.compareAndSet(curNode, new Node(x, curNode))) {
                return;
            }
        }
    }

    private boolean waitPop(int x) {
        int randIndex = random.nextInt(SIZE_ARRAY);
        return waitPop(x, randIndex - 1) || waitPop(x, randIndex) || waitPop(x, randIndex + 1);
    }

    private boolean waitPop(int x, int i) {
        i = inArray(i);
        AtomicRef<Node> curNode = array.get(i);
        Node newNode = new Node(x, null);
        if (curNode.compareAndSet(null, newNode)) {
            for (int j = 0; j < NUMBER_ITERATIONS; j++) {
                if (curNode.getValue() == null) {
                    return true;
                }
            }
            return !curNode.compareAndSet(newNode, null);
        }
        return false;
    }

    private int inArray(int i) {
        return (i + SIZE_ARRAY) % SIZE_ARRAY;
    }

    @Override
    public int pop() {
        Node find = findPush();
        if (find != null) {
            return find.x;
        }
        while (true) {
            Node curNode = head.getValue();
            if (curNode == null) {
                return Integer.MIN_VALUE;
            }
            if (head.compareAndSet(curNode, curNode.next.getValue())) {
                return curNode.x;
            }
        }
    }

    private Node findPush() {
        for (int i = 0; i < 5; i++) {
            AtomicRef<Node> curNode = array.get(i);
            Node curValue = curNode.getValue();
            if (curValue == null) {
                continue;
            }
            if (curNode.compareAndSet(curValue, null)) {
                return curValue;
            }
        }
        return null;
    }

    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }
}
