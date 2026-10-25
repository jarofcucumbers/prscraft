package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CircuitLayoutTest{
    @Test void twoSourcesBranchIntoAdderAndThenReceiver(){var p=CircuitLayout.arrange(4,List.of(new CircuitLayout.Link(2,1),new CircuitLayout.Link(3,1),new CircuitLayout.Link(1,0)));assertTrue(p.points().get(2).x()<p.points().get(1).x());assertTrue(p.points().get(1).x()<p.points().get(0).x());assertNotEquals(p.points().get(2).y(),p.points().get(3).y());assertTrue(p.points().get(1).y()>p.points().get(2).y()&&p.points().get(1).y()<p.points().get(3).y());}
    @Test void cyclesAndDisconnectedNodesHaveFiniteNonoverlappingPositions(){var p=CircuitLayout.arrange(5,List.of(new CircuitLayout.Link(0,1),new CircuitLayout.Link(1,2),new CircuitLayout.Link(2,0),new CircuitLayout.Link(3,0)));assertEquals(5,new HashSet<>(p.points()).size());assertEquals(p.points().get(0).x(),p.points().get(2).x());assertTrue(p.points().get(3).x()<p.points().get(0).x());for(var point:p.points())assertTrue(Double.isFinite(point.x())&&Double.isFinite(point.y()));}
    @Test void fitContainsEveryNodeInALargeCircuit(){var links=new ArrayList<CircuitLayout.Link>();for(int i=0;i<31;i++)links.add(new CircuitLayout.Link(i,i+1));var p=CircuitLayout.arrange(32,links);var view=new MapViewport();view.fit(p.width()+50,p.height()+20,872,388);for(var point:p.points()){assertTrue(view.x()+point.x()*view.zoom()>=0);assertTrue(view.x()+(point.x()+180)*view.zoom()<=872);assertTrue(view.y()+point.y()*view.zoom()>=0);assertTrue(view.y()+(point.y()+88)*view.zoom()<=388);}}
    @Test void wheelZoomKeepsPointerAnchoredAndPanningIsInvertible(){var v=new MapViewport();v.fit(1200,700,872,388);double x=v.worldX(137),y=v.worldY(211);v.zoomAt(1.2,137,211);assertEquals(x,v.worldX(137),1e-9);assertEquals(y,v.worldY(211),1e-9);v.pan(30,-20);assertEquals(x,v.worldX(167),1e-9);assertEquals(y,v.worldY(191),1e-9);}
}
