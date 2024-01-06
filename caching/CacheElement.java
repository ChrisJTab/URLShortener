package caching;
public class CacheElement<K, V> {
    private K key;
    private V val;

    public CacheElement(K key, V val) {
        this.key = key;
        this.val = val;
    }

    public void setKey(K key) {
        this.key = key;
    }
    public void setVal(V val) {
        this.val = val;
    }

    public K getKey() {
        return this.key;
    }
    public V getVal() {
        return this.val;
    }
}