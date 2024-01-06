package caching;
import java.util.ArrayList;
import java.util.List;


public class CacheQueue<K, V> {
    private CacheNode<K, V> first;
    private CacheNode<K, V> last;

    public CacheQueue() {
        this.first = null;
        this.last = null;
    }

    public CacheNode<K, V> enqueue(K key, V val) {
        CacheNode<K, V> newNode = new CacheNode<K, V>(key, val);
        
        if (this.first == null) {
            this.first = newNode;
        } else {
            CacheNode<K, V> prevNode = this.last;
            newNode.setPrev(prevNode);
            prevNode.setNext(newNode);
        }
        this.last = newNode;
        return newNode;
    }

    public CacheNode<K, V> dequeue() {
        if (this.first == null) {
            return null;
        }
        
        CacheNode<K, V> evictedNode = this.first;
        this.first = this.first.getNext();
        if (this.first != null) {
            this.first.setPrev(null);
        }

        evictedNode.setPrev(null);
        evictedNode.setNext(null);
        return evictedNode;
    }

    public boolean isEmpty() {
        return this.first == null;
    }

    public void putToTop(CacheNode<K, V> cacheNode) {
        CacheNode<K, V> prevNode = cacheNode.getPrev();
        CacheNode<K, V> nextNode = cacheNode.getNext();

        if (prevNode == null && nextNode == null) {
            // No prev or next nodes
            return;
        }

        if (this.first == cacheNode) {
            this.first = nextNode;
        } else if (this.last == cacheNode) {
            // Don't do anything, already at top!
            return;
        }

        if (prevNode != null) {
            // Set Prev -> Next
            prevNode.setNext(nextNode);
        }
        if (nextNode != null) {
            // Set Prev <- Next
            nextNode.setPrev(prevNode);
        }

        cacheNode.setNext(null);
        cacheNode.setPrev(this.last);
        this.last.setNext(cacheNode);
        this.last = cacheNode;
    }

    public List<CacheNode<K, V>> dumpMem() {
        // TEMP FUNCTION TO DELETE
        List<CacheNode<K, V>> l = new ArrayList<>();

        CacheNode<K, V> cur = this.first;
        while (cur != null) {
            l.add(cur);
            cur = cur.getNext();
        }
        return l;
    }
}
