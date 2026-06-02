package org.antlr.jetbrains.sample;

import org.junit.Test;

import static org.junit.Assert.*;

public class AngularSelectorIndexTest {

    @Test
    public void testIsInsideComment_BlockComment() {
        String text = "/* comment */ selector: 'my-component'";
        assertTrue(AngularSelectorIndex.isInsideComment(text, 3, 11));
        assertFalse(AngularSelectorIndex.isInsideComment(text, 22, 35));
    }

    @Test
    public void testIsInsideComment_LineComment() {
        String text = "// comment\nselector: 'my-component'";
        assertTrue(AngularSelectorIndex.isInsideComment(text, 3, 11));
        assertFalse(AngularSelectorIndex.isInsideComment(text, 21, 34));
    }

    @Test
    public void testIsInsideComment_MultipleComments() {
        String text = """
            // line comment
            /* block comment */
            selector: 'my-component'
            """;
        assertFalse(AngularSelectorIndex.isInsideComment(text, 100, 113));
    }

    @Test
    public void testIsInsideComment_CommentsWithSelectorKeyword() {
        String text = """
            // selector: 'ignored'
            /* selector: 'also-ignored' */
            @Component({
                selector: 'my-component',
                template: '...'
            })
            """;
        assertTrue(AngularSelectorIndex.isInsideComment(text, 13, 25));
        assertTrue(AngularSelectorIndex.isInsideComment(text, 41, 61));
        assertFalse(AngularSelectorIndex.isInsideComment(text, 100, 113));
    }

    @Test
    public void testIsInsideComment_NoMatch() {
        String text = "selector: 'my-component'";
        assertFalse(AngularSelectorIndex.isInsideComment(text, 0, 9));
        assertFalse(AngularSelectorIndex.isInsideComment(text, 25, 30));
    }
}
