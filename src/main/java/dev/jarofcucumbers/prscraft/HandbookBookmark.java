package dev.jarofcucumbers.prscraft;
import com.google.gson.Gson;
/** Answer visibility is intentionally absent: every task starts without spoilers. */
public record HandbookBookmark(HandbookCurriculum.Chapter chapter,String entry,boolean contents,int contentsPage){
    public static HandbookBookmark defaults(){return new HandbookBookmark(HandbookCurriculum.Chapter.FOUNDATIONS,"intro",true,0);}
    public static HandbookBookmark read(String json){try{var state=new Gson().fromJson(json,HandbookBookmark.class);if(state==null||state.chapter()==null||state.contentsPage()<0||!HandbookCurriculum.entries(state.chapter()).contains(state.entry()))return defaults();return new HandbookBookmark(state.chapter(),state.entry(),state.contents(),Math.min(state.contentsPage(),(HandbookCurriculum.entries(state.chapter()).size()-1)/18));}catch(RuntimeException e){return defaults();}}
    public String json(){return new Gson().toJson(this);}
}
