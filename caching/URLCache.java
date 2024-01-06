package caching;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class URLCache<K, V> {
    private final Map<K, CacheNode<K, V>> cacheMap;
    private final CacheQueue<K, V> cacheQueue;
    private final Integer maxSize;

    public URLCache(Integer size) {
        this.maxSize = size;
        this.cacheMap = new HashMap<K, CacheNode<K, V>>(size);
        this.cacheQueue = new CacheQueue<K, V>();
    }

    public synchronized boolean put(K key, V val) {
        if (cacheMap.containsKey(key) && cacheMap.get(key).getVal() == val) {
            // (Key, Val) pair exist already. Don't write to memory
            return false;
        } else if (cacheMap.containsKey(key)) {
            // (Key, !Val) pair exist already. Only write val to key.
            cacheMap.get(key).setVal(val);
            return true;
        }
        

        if (cacheMap.size() == this.maxSize) {
            // Cache is full. Evict least access entry!
            CacheNode<K, V> evictedNode = this.cacheQueue.dequeue();
            this.cacheMap.remove(evictedNode.getKey());
        }

        CacheNode<K, V> newCacheNode = this.cacheQueue.enqueue(key, val);
        this.cacheMap.put(key, newCacheNode);
        return true;
    }

    public synchronized V get(K key) {
        if (!this.cacheMap.containsKey(key)) {
            return null;
        }

        CacheNode<K, V> cacheNode = this.cacheMap.get(key);
        this.cacheQueue.putToTop(cacheNode);
        return cacheNode.getVal();
    }

    public void dumpMem() {
        System.out.println("--------------------- CACHE MAP ---------------------");
        for (Map.Entry<K, CacheNode<K, V>> entry : cacheMap.entrySet()) {
            K key = entry.getKey();
            V val = entry.getValue().getVal();
            
            // Do something with the key and value here
            System.out.println("Key: " + key + ", Value: " + val);
        }
        System.out.println("--------------------- CACHE MAP ---------------------");
        
        
        System.out.println("\n\n");


        System.out.println("--------------------- CACHE QUEUE ---------------------");
        List<CacheNode<K, V>> a = this.cacheQueue.dumpMem();
        
        for (CacheNode<K, V> cacheNode : a) {
            System.out.println("(" + cacheNode.getKey() + ", " + cacheNode.getVal() + ")");
        }
        System.out.println("--------------------- CACHE QUEUE ---------------------");
    }
}
