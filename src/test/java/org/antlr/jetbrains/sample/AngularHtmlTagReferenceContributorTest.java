package org.antlr.jetbrains.sample;

import org.junit.Test;

import static org.junit.Assert.*;

public class AngularHtmlTagReferenceContributorTest {

    @Test
    public void testIsInsideComment_BlockComment() {
        String text = "/* comment */ selector: 'my-component'";
        // Test match inside comment
        assertTrue(AngularHtmlTagReferenceContributor.isInsideComment(text, 3, 11));
        
        // Test match after comment (selector value position)
        assertFalse(AngularHtmlTagReferenceContributor.isInsideComment(text, 22, 35));
    }

    @Test
    public void testIsInsideComment_LineComment() {
        String text = "// comment\nselector: 'my-component'";
        // Test match inside line comment
        assertTrue(AngularHtmlTagReferenceContributor.isInsideComment(text, 3, 11));
        
        // Test match after line comment (selector value position)
        assertFalse(AngularHtmlTagReferenceContributor.isInsideComment(text, 21, 34));
    }

    @Test
    public void testIsInsideComment_MultipleComments() {
        String text = """
            // line comment
            /* block comment */
            selector: 'my-component'
            """;
        // Match after all comments (selector value position)
        assertFalse(AngularHtmlTagReferenceContributor.isInsideComment(text, 100, 113));
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
        // Match inside line comment
        assertTrue(AngularHtmlTagReferenceContributor.isInsideComment(text, 13, 25)); // line comment
        // Match inside block comment
        assertTrue(AngularHtmlTagReferenceContributor.isInsideComment(text, 41, 61)); // block comment
        // Match for real selector - not inside comment
        assertFalse(AngularHtmlTagReferenceContributor.isInsideComment(text, 100, 113));
    }

    @Test
    public void testIsInsideComment_NoMatch() {
        String text = "selector: 'my-component'";
        // Match before string starts
        assertFalse(AngularHtmlTagReferenceContributor.isInsideComment(text, 0, 9));
        // Match after string ends
        assertFalse(AngularHtmlTagReferenceContributor.isInsideComment(text, 25, 30));
    }
}
