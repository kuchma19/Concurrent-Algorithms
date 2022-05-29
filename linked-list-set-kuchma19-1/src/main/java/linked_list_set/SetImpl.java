package linked_list_set;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class SetImpl implements Set {
    private class Node {
        AtomicMarkableReference<Node> next;
        int x;

        Node() {
            this.next = new AtomicMarkableReference<>(new Node(Integer.MAX_VALUE, null), false);
            this.x = Integer.MIN_VALUE;
        }

        Node(int x, Node next) {
            this.next = new AtomicMarkableReference<>(next, false);
            this.x = x;
        }
        @Override
        public String toString() {
            String ans = String.valueOf(x);
            if (next.getReference() != null) {
                ans = ans + " " + next.getReference().toString();
            }
            return ans;
        }
    }

    private class Window {
        Node cur, next;
        Window(Node cur, Node next) {
            this.cur = cur;
            this.next = next;
        }
    }

    private final Node head = new Node();

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        retry: while (true) {
            Node cur = head;
            Node next = cur.next.getReference();
            boolean[] removed = new boolean[1];
            while (next != null && next.x < x) {
                Node next_next = next.next.get(removed);
                if (removed[0]) {
                    while (removed[0]) {
                        if (!cur.next.compareAndSet(next, next_next, false, false)) {
                            continue retry;
                        }
                        next = next_next;
                        next_next = next.next.get(removed);
                    }
                } else {
                    cur = next;
                    next = cur.next.getReference();
                }
            }
            if (next != null) {
                Node next_next = next.next.get(removed);
                while (removed[0]) {
                    if (!cur.next.compareAndSet(next, next_next, false, false)) {
                        continue retry;
                    }
                    next = next_next;
                    next_next = next.next.get(removed);
                }
            }


            return new Window(cur, next);
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next == null || w.next.x == x) {
                return false;
            }
            Node new_node = new Node(x, w.next);
            if (w.cur.next.compareAndSet(w.next, new_node, false, false)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next == null || w.next.x != x) {
                return false;
            }
            if (w.next.next.compareAndSet(w.next.next.getReference(), w.next.next.getReference(), false, true)) {
                w.cur.next.compareAndSet(w.next, w.next.next.getReference(), false, false);
                return true;
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        boolean res = w.next != null && w.next.x == x;
        return res;
    }
}