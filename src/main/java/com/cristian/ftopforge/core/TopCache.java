package com.cristian.ftopforge.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TopCache {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private List<FactionSnapshot> sorted = Collections.emptyList();
    private Map<String, FactionSnapshot> byId = Collections.emptyMap();

    public void rebuild(List<FactionSnapshot> snapshots) {
        List<FactionSnapshot> copy = new ArrayList<>(snapshots);
        copy.sort(Comparator.comparingLong(FactionSnapshot::totalValue).reversed());
        Map<String, FactionSnapshot> idx = new HashMap<>();
        for (FactionSnapshot s : copy) idx.put(s.factionId(), s);
        lock.writeLock().lock();
        try {
            this.sorted = Collections.unmodifiableList(copy);
            this.byId = Collections.unmodifiableMap(idx);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<FactionSnapshot> top(int n) {
        lock.readLock().lock();
        try {
            if (n <= 0 || sorted.isEmpty()) return Collections.emptyList();
            return sorted.subList(0, Math.min(n, sorted.size()));
        } finally {
            lock.readLock().unlock();
        }
    }

    public int rankOf(String factionId) {
        lock.readLock().lock();
        try {
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).factionId().equals(factionId)) return i + 1;
            }
            return 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    public FactionSnapshot byId(String factionId) {
        lock.readLock().lock();
        try {
            return byId.get(factionId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try { return sorted.size(); } finally { lock.readLock().unlock(); }
    }

    public List<FactionSnapshot> topRange(int fromInclusive, int toExclusive) {
        lock.readLock().lock();
        try {
            int size = sorted.size();
            int from = Math.max(0, fromInclusive);
            int to = Math.min(size, toExclusive);
            if (from >= to) return Collections.emptyList();
            return Collections.unmodifiableList(new ArrayList<>(sorted.subList(from, to)));
        } finally {
            lock.readLock().unlock();
        }
    }

    public AvgBreakdown averagesTop10() {
        lock.readLock().lock();
        try {
            int n = Math.min(10, sorted.size());
            if (n == 0) return AvgBreakdown.empty();
            long c = 0, sp = 0, it = 0, bl = 0, ba = 0;
            for (int i = 0; i < n; i++) {
                FactionSnapshot s = sorted.get(i);
                c += s.chunksValue();
                sp += s.spawnersValue();
                it += s.itemsValue();
                bl += s.blocksValue();
                ba += s.balanceValue();
            }
            return new AvgBreakdown(c / n, sp / n, it / n, bl / n, ba / n);
        } finally {
            lock.readLock().unlock();
        }
    }
}
