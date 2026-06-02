package org.antlr.jetbrains.sample;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AngularHtmlTagCompletionContributor extends CompletionContributor {
    public AngularHtmlTagCompletionContributor() {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            new CompletionProvider<>() {
                @Override
                protected void addCompletions(@NotNull CompletionParameters parameters,
                                              @NotNull ProcessingContext context,
                                              @NotNull CompletionResultSet result) {
                    PsiFile file = parameters.getOriginalFile();
                    if (file == null || !file.getName().endsWith(".html")) {
                        return;
                    }
                    Editor editor = parameters.getEditor();
                    int offset = editor.getCaretModel().getOffset();
                    CharSequence text = editor.getDocument().getCharsSequence();
                    TagNameContext tagCtx = getTagNameContext(text, offset);
                    if (!tagCtx.insideTagName) {
                        return;
                    }
                    CompletionResultSet prefixed = result.withPrefixMatcher(tagCtx.prefix);
                    List<String> selectors = AngularSelectorIndex.getAllSelectors(file.getProject());
                    for (String selector : selectors) {
                        prefixed.addElement(
                                LookupElementBuilder.create(selector)
                                        .withTypeText("Angular selector", true)
                        );
                    }
                }
            }
        );
    }

    private static @NotNull TagNameContext getTagNameContext(@NotNull CharSequence text, int offset) {
        int n = text.length();
        int pos = Math.max(0, Math.min(offset, n));
        int lt = -1;
        for (int i = pos -1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '>') {
                return TagNameContext.notInside();
            }
            if (c == '<') {
                lt = i;
                break;
            }
        }
        if (lt < 0) {
            return TagNameContext.notInside();
        }
        if (lt + 1 < pos && text.charAt(lt + 1) == '/') {
            return TagNameContext.notInside();
        }
        for (int i = lt + 1; i < pos; i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                return TagNameContext.notInside();
            }
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ':')) {
                return TagNameContext.notInside();
            }
        }
        String prefix = text.subSequence(lt + 1, pos).toString();
        return new TagNameContext(true, prefix);
    }

    private static final class TagNameContext {
        private final boolean insideTagName;
        private final String prefix;

        private TagNameContext(boolean insideTagName, @NotNull String prefix) {
            this.insideTagName = insideTagName;
            this.prefix = prefix;
        }

        private static @NotNull TagNameContext notInside() {
            return new TagNameContext(false, "");
        }
    }
}
