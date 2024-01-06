package caching;
public class CacheNode<K, V> {
    private K key;
    private V val;

    private CacheNode<K, V> prev;
    private CacheNode<K, V> next;


    public CacheNode(K key, V val) {
        this.key = key;
        this.val = val;
    }

    public CacheNode(K key, V val, CacheNode<K, V> prev, CacheNode<K, V> next) {
        this.key = key;
        this.val = val;

        this.prev = prev;
        this.next = next;
    }


    public K getKey() {
        return key;
    }
    public V getVal() {
        return val;
    }
    public CacheNode<K, V> getPrev() {
        return prev;
    }
    public CacheNode<K, V> getNext() {
        return next;
    }

    
    public void setKey(K key) {
        this.key = key;
    }
    public void setVal(V val) {
        this.val = val;
    }
    public void setPrev(CacheNode<K, V> prev) {
        this.prev = prev;
    }
    public void setNext(CacheNode<K, V> next) {
        this.next = next;
    }    
}
