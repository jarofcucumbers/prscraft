package dev.jarofcucumbers.prscraft;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NetworkWalkTest {
    @Test void followsTurnsBranchesAndCyclesUsingShortestPath() {
        Map<String,List<String>> graph=Map.of("port",List.of("a"),"a",List.of("port","corner","branch"),
            "corner",List.of("a","end"),"branch",List.of("a","end"),"end",List.of("corner","branch"));
        var distances=NetworkWalk.distances("port",n->graph.getOrDefault(n,List.of()),256);
        assertEquals(3,distances.get("end"));assertEquals(5,distances.size());
    }
    @Test void brokenLinkCannotReachTheOtherSide() {
        var distances=NetworkWalk.distances(0,n->n<3?List.of(n+1):List.of(),256);
        assertFalse(distances.containsKey(4));assertEquals(4,distances.size());
    }
    @Test void traversalIsBoundedEvenOnInfiniteInput() {
        var distances=NetworkWalk.distances(0,n->List.of(n+1,n+2),256);
        assertEquals(256,distances.size());
    }
}
