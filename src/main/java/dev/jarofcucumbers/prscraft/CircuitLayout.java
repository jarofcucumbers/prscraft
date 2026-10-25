package dev.jarofcucumbers.prscraft;
import java.util.*;
/** Directed layers over SCCs; branches remain separate and cycles cannot grow layers forever. */
public final class CircuitLayout{
    private CircuitLayout(){}
    public record Link(int from,int to){}
    public record Point(double x,double y){}
    public record Plan(List<Point> points,double width,double height){}
    public static Plan arrange(int count,List<Link> links){
        if(count<0||count>32)throw new IllegalArgumentException("nodes");if(count==0)return new Plan(List.of(),180,88);
        var graph=new ArrayList<List<Integer>>();for(int i=0;i<count;i++)graph.add(new ArrayList<>());
        for(var edge:links)if(edge.from()>=0&&edge.from()<count&&edge.to()>=0&&edge.to()<count&&edge.from()!=edge.to()&&!graph.get(edge.from()).contains(edge.to()))graph.get(edge.from()).add(edge.to());
        class Components{
            int time,total;int[] index=new int[count],low=new int[count],component=new int[count];boolean[] active=new boolean[count];ArrayDeque<Integer> stack=new ArrayDeque<>();
            Components(){Arrays.fill(index,-1);for(int i=0;i<count;i++)if(index[i]<0)visit(i);}
            void visit(int v){index[v]=low[v]=time++;stack.push(v);active[v]=true;for(int next:graph.get(v)){if(index[next]<0){visit(next);low[v]=Math.min(low[v],low[next]);}else if(active[next])low[v]=Math.min(low[v],index[next]);}if(low[v]==index[v]){int n;do{n=stack.pop();active[n]=false;component[n]=total;}while(n!=v);total++;}}
        }
        var components=new Components();var dag=new ArrayList<Set<Integer>>();int[] degree=new int[components.total],layer=new int[components.total];for(int i=0;i<components.total;i++)dag.add(new LinkedHashSet<>());
        for(int from=0;from<count;from++)for(int to:graph.get(from)){int a=components.component[from],b=components.component[to];if(a!=b&&dag.get(a).add(b))degree[b]++;}
        var queue=new ArrayDeque<Integer>();for(int i=0;i<degree.length;i++)if(degree[i]==0)queue.add(i);while(!queue.isEmpty()){int v=queue.remove();for(int next:dag.get(v)){layer[next]=Math.max(layer[next],layer[v]+1);if(--degree[next]==0)queue.add(next);}}
        int layers=Arrays.stream(layer).max().orElse(0)+1;var rows=new ArrayList<List<Integer>>();for(int i=0;i<layers;i++)rows.add(new ArrayList<>());for(int i=0;i<count;i++)rows.get(layer[components.component[i]]).add(i);
        int maxRows=rows.stream().mapToInt(List::size).max().orElse(1);Point[] result=new Point[count];
        for(int column=0;column<layers;column++){var entries=rows.get(column);for(int row=0;row<entries.size();row++)result[entries.get(row)]=new Point(24+column*310,24+row*128+(maxRows-entries.size())*64);}
        return new Plan(List.of(result),(layers-1)*310+228,(maxRows-1)*128+136);
    }
}
