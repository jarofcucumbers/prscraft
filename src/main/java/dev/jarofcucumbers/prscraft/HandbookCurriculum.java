package dev.jarofcucumbers.prscraft;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
/** Content-only navigation and task diagrams; no client classes or player state. */
public final class HandbookCurriculum{
    private HandbookCurriculum(){}
    public enum Chapter{FOUNDATIONS,COMPONENTS,TASKS}
    public record Lesson(String id,String diagram,List<String> formulas){}
    public record ComponentEntry(String id,List<String> items,String diagram,List<String> formulas){}
    public record Node(String item,List<String> parameters,int x,int y){}
    public record Edge(int from,int to,String label){}
    public record Task(String id,List<Node> nodes,List<Edge> edges){}
    private record Data(List<String> foundations,List<Lesson> lessons,List<ComponentEntry> components,List<Task> tasks){}
    private static final Data DATA=load();
    private static Data load(){try(var stream=HandbookCurriculum.class.getResourceAsStream("/assets/prscraft/handbook/curriculum.json")){
        if(stream==null)throw new IllegalStateException("Missing handbook curriculum");return new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),Data.class);
    }catch(IOException e){throw new IllegalStateException("Cannot read handbook curriculum",e);}}
    public static List<String> entries(Chapter chapter){return switch(chapter){case FOUNDATIONS->List.copyOf(DATA.foundations());case COMPONENTS->DATA.components().stream().map(c->"component_"+c.id()).toList();case TASKS->DATA.tasks().stream().map(Task::id).toList();};}
    public static List<ComponentEntry> components(){return List.copyOf(DATA.components());}
    public static Task task(String id){return DATA.tasks().stream().filter(t->t.id().equals(id)).findFirst().orElseThrow();}
    public static List<HandbookContent.Page> extraPages(){var pages=new ArrayList<HandbookContent.Page>();for(var l:DATA.lessons())pages.add(new HandbookContent.Page(l.id(),l.diagram(),l.formulas()));for(var c:DATA.components())pages.add(new HandbookContent.Page("component_"+c.id(),c.diagram(),c.formulas()));return List.copyOf(pages);}
    public static HandbookContent.Page page(String id){return HandbookContent.PAGES.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow();}
    public static String titleKey(Chapter chapter,String entry){return chapter==Chapter.TASKS?"advancements.prscraft."+entry+".title":"book.prscraft."+entry+".title";}
}
