package caching;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import configs.GeneralConfigs;

public class Main {
    // TEMP CLASS FOR DEMONSTRATION ONLY
    public static void main(String[] args) {
        URLCache<Integer, Integer> urlCache = new URLCache<Integer, Integer>(GeneralConfigs.CACHE_SIZE);

        urlCache.put(1, 11);
        urlCache.put(2, 12);
        urlCache.put(3, 13);
        urlCache.put(4, 14);
        urlCache.put(5, 15);
        urlCache.put(6, 16);


        // Play with cache elements here. Uncomment lines
//        urlCache.get(0);
//        urlCache.get(1);
//        urlCache.get(2);
//        urlCache.get(3);
//        urlCache.get(4);
//        urlCache.get(5);
//        urlCache.get(6);
//        urlCache.get(7);

        // Dump cache memory for inspection.
        urlCache.dumpMem();
    }
}