package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HandbookBookmarkTest{
    @Test void remembersChapterAndExactPageWithoutRememberingAnswers(){var bookmark=new HandbookBookmark(HandbookCurriculum.Chapter.TASKS,"gray_levels",false,1);assertEquals(bookmark,HandbookBookmark.read(bookmark.json()));assertFalse(bookmark.json().contains("reveal"));}
    @Test void invalidAndOutdatedBookmarksFallBackSafely(){assertEquals(HandbookBookmark.defaults(),HandbookBookmark.read("bad json"));assertEquals(HandbookBookmark.defaults(),HandbookBookmark.read("{\"chapter\":\"TASKS\",\"entry\":\"removed\"}"));assertEquals(HandbookBookmark.defaults(),HandbookBookmark.read("null"));}
    @Test void everyChapterHasDistinctEntriesAndCatalogCoversAllItems(){for(var chapter:HandbookCurriculum.Chapter.values()){var entries=HandbookCurriculum.entries(chapter);assertFalse(entries.isEmpty());assertEquals(entries.size(),entries.stream().distinct().count());}var catalog=HandbookCurriculum.components().stream().flatMap(c->c.items().stream()).collect(java.util.stream.Collectors.toSet());assertTrue(catalog.containsAll(HandbookContent.COVERED_ITEMS));assertEquals(Milestones.IDS,java.util.Set.copyOf(HandbookCurriculum.entries(HandbookCurriculum.Chapter.TASKS)));}
}
