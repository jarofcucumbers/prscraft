package dev.jarofcucumbers.prscraft;

import java.util.*;
import java.util.function.Function;

/** Bounded breadth-first traversal, also used by the in-world cable graph. */
public final class NetworkWalk {
    private NetworkWalk() {}
    public static <N> Map<N, Integer> distances(N start, Function<N, List<N>> neighbors, int maxNodes) {
        if (maxNodes < 1) throw new IllegalArgumentException("maxNodes");
        Map<N, Integer> distances = new LinkedHashMap<>();
        ArrayDeque<N> queue = new ArrayDeque<>();
        distances.put(start, 0); queue.add(start);
        while (!queue.isEmpty()) {
            N node = queue.remove();
            for (N next : neighbors.apply(node)) {
                if (!distances.containsKey(next) && distances.size() < maxNodes) {
                    distances.put(next, distances.get(node) + 1); queue.add(next);
                }
            }
        }
        return distances;
    }
}
