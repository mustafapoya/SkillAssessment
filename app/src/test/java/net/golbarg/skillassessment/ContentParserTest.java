package net.golbarg.skillassessment;

import static org.junit.Assert.assertEquals;

import net.golbarg.skillassessment.models.ContentBlock;
import net.golbarg.skillassessment.util.ContentParser;

import org.junit.Test;

import java.util.List;

public class ContentParserTest {

    @Test
    public void plainText() {
        List<ContentBlock> blocks = ContentParser.parse("  What is Git?  ");
        assertEquals(1, blocks.size());
        assertEquals(ContentBlock.Type.TEXT, blocks.get(0).getType());
        assertEquals("What is Git?", blocks.get(0).getContent());
    }

    @Test
    public void keepsOrderOfTextAndCode() {
        List<ContentBlock> blocks = ContentParser.parse("What would this return?\n\n```python\nx = [1, 2]\nreturn len(x)\n```\n\nPick one.");
        assertEquals(3, blocks.size());
        assertEquals(ContentBlock.Type.TEXT, blocks.get(0).getType());
        assertEquals(ContentBlock.Type.CODE, blocks.get(1).getType());
        assertEquals("python", blocks.get(1).getLanguage());
        assertEquals("x = [1, 2]\nreturn len(x)", blocks.get(1).getContent());
        assertEquals("Pick one.", blocks.get(2).getContent());
    }

    @Test
    public void unterminatedFenceIsStillCode() {
        List<ContentBlock> blocks = ContentParser.parse("```js\nlet a = 1;");
        assertEquals(1, blocks.size());
        assertEquals(ContentBlock.Type.CODE, blocks.get(0).getType());
        assertEquals("let a = 1;", blocks.get(0).getContent());
    }

    @Test
    public void singleLineFence() {
        List<ContentBlock> blocks = ContentParser.parse("```a = 1```");
        assertEquals(1, blocks.size());
        assertEquals("a = 1", blocks.get(0).getContent());
        assertEquals("", blocks.get(0).getLanguage());
    }

    @Test
    public void extractsImages() {
        List<ContentBlock> blocks = ContentParser.parse("Which shape?\n\n![image](images/shape.png)\nChoose.");
        assertEquals(3, blocks.size());
        assertEquals(ContentBlock.Type.IMAGE, blocks.get(1).getType());
        assertEquals("images/shape.png", blocks.get(1).getContent());
    }

    @Test
    public void unescapesJsonSlashesInImageUrls() {
        List<ContentBlock> blocks = ContentParser.parse("![image](https:\\/\\/example.com\\/a.png)");
        assertEquals("https://example.com/a.png", blocks.get(0).getContent());
    }

    @Test
    public void tabsBecomeSpaces() {
        List<ContentBlock> blocks = ContentParser.parse("```\n\tint a;\n```");
        assertEquals("    int a;", blocks.get(0).getContent());
    }

    @Test
    public void previewDropsCodeAndBackticks() {
        assertEquals("What does git stash do?", ContentParser.preview("What does `git stash` do?\n```bash\ngit stash\n```"));
    }
}
