package org.antlr.jetbrains.sample;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AngularHtmlGotoDeclarationHandler implements GotoDeclarationHandler {
    private static final Logger LOG = Logger.getInstance(AngularHtmlGotoDeclarationHandler.class);
    private static final Pattern SELECTOR_PATTERN = Pattern.compile("selector\\s*:\\s*(['\"`])([^'\"]+)\\1");

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                             int offset,
                                                             Editor editor) {
        LOG.debug("[AngularGoto] getGotoDeclaratioinTargets sourceElement=" +
                (sourceElement == null ? "null" : sourceElement.getClass().getName()) + ", offset=" + offset);
        if (sourceElement == null) {
            LOG.debug("[AngularGoto] skip: source is null");
            return null;
        }
        PsiElement tagNameElement = findTagNameElement(sourceElement, offset, editor);
        String selector;
        PsiFile htmlFile;
        if (tagNameElement != null) {
            IElementType sourceType = tagNameElement.getNode() != null ? tagNameElement.getNode().getElementType() : null;
            if (sourceType != XmlTokenType.XML_NAME && sourceType != XmlTokenType.XML_TAG_NAME) {
                LOG.debug("[AngularGoto] skip: tokenType=" + sourceType);
                return null;
            }
            XmlTag tag = PsiTreeUtil.getParentOfType(tagNameElement, XmlTag.class, false);
            if (tag == null) {
                LOG.debug("[AngularGoto] skip: no XmlTag parent");
                return null;
            }
            htmlFile = tagNameElement.getContainingFile();
            selector = tagNameElement.getText();
            LOG.debug("[AngularGoto] token text=" + selector + ", tag=" + tag.getName());
        } else {
            htmlFile = PsiDocumentManager.getInstance(sourceElement.getProject()).getPsiFile(editor.getDocument());
            if (htmlFile == null) {
                htmlFile = sourceElement.getContainingFile();
            }
            selector = extractTagNameFromText(editor, offset);
            LOG.debug("[AngularGoto] fallback selector from text=" + selector);
            if (selector == null) {
                LOG.debug("[AngularGoto] skip: no tag-name element near offset");
                return null;
            }
        }

        if (htmlFile == null || !htmlFile.getName().endsWith(".html")) {
            LOG.debug("[AngularGoto] skip: file in not html: " + (htmlFile == null ? "null" : htmlFile.getName()));
            return null;
        }
        if (!isComponentTag(selector)) {
            LOG.debug("[AngularGoto] skip: not component tag");
            return null;
        }

        PsiElement target = findSelectorDeclaration(sourceElement.getProject(), selector);
        if (target == null) {
            LOG.debug("[AngularGoto] selector not resolved: " + selector);
            return null;
        }
        LOG.debug("[AngularGoto] resolved selector " + selector + " -> " +
                target.getContainingFile().getName() + ":" + target.getTextOffset());
        return new PsiElement[]{target};
    }

    private static boolean isComponentTag(@Nullable String tagName) {
        return tagName != null && !tagName.isEmpty() && tagName.indexOf('-') > 0;
    }

    private static @Nullable PsiElement findSelectorDeclaration(@NotNull Project project, @NotNull String selector) {
        Collection<VirtualFile> files = FilenameIndex.getAllFilesByExt(project, "ts", GlobalSearchScope.projectScope(project));
        LOG.debug("[AngularGoto] scanning ts files count=" + files.size() + " for selector=" + selector);
        PsiManager psiManager = PsiManager.getInstance(project);
        int fileCount = 0;
        for (VirtualFile vf : files) {
            fileCount++;
            PsiFile tsFile = psiManager.findFile(vf);
            if (tsFile == null) {
                continue;
            }
            LOG.debug("[AngularGoto] checking file " + fileCount + ": " + tsFile.getName());
            PsiElement target = findSelectorInFile(tsFile, selector);
            if (target != null) {
                LOG.debug("[AngularGoto] found selector in file " + tsFile.getName());
                return target;
            }
        }
        LOG.debug("[AngularGoto] selector not found in ts files: " + selector + ", total files checked=" + fileCount);
        return null;
    }

    private static @Nullable PsiElement findTagNameElement(@NotNull PsiElement sourceElement, int offset, @NotNull Editor editor) {
        if (isTagNameToken(sourceElement)) {
            return sourceElement;
        }
        Project project = sourceElement.getProject();
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null) {
            file = sourceElement.getContainingFile();
        }
        if (file == null) {
            return null;
        }
        int textLength = file.getTextLength();
        int[] candidates = new int[]{offset, offset - 1, offset + 1, offset - 2, offset + 2};
        for (int candidate : candidates) {
            if (candidate < 0 || candidate >= textLength) {
                continue;
            }
            PsiElement e = file.findElementAt(candidate);
            if (isTagNameToken(e)) {
                return e;
            }
        }
        return null;
    }

    private static boolean isTagNameToken(@Nullable PsiElement element) {
        if (element == null || element.getNode() == null) {
            return false;
        }
        IElementType type = element.getNode().getElementType();
        return type == XmlTokenType.XML_NAME || type == XmlTokenType.XML_TAG_NAME;
    }

    private static @Nullable String extractTagNameFromText(@NotNull Editor editor, int offset) {
        CharSequence text = editor.getDocument().getCharsSequence();
        int n = text.length();
        if (n == 0) {
            return null;
        }
        int pos = Math.max(0, Math.min(offset, n - 1));

        int left = pos;
        while (left >= 0) {
            char c = text.charAt(left);
            if (c == '<') {
                break;
            }
            if (c == '>') {
                return null;
            }
            left--;
        }
        if (left < 0 || text.charAt(left) != '<') {
            return null;
        }

        int right = pos;
        while (right < n) {
            char c = text.charAt(right);
            if (c == '>') {
                break;
            }
            if (right != left && c == '<') {
                return null;
            }
            right++;
        }
        if (right >= n || text.charAt(right) != '>') {
            return null;
        }

        int i = left + 1;
        while (i < right && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        if (i < right && text.charAt(i) == '/') {
            i++;
        }
        while (i < right && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        int start = i;
        while (i < right) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ':') {
                i++;
                continue;
            }
            break;
        }
        if (start >= i) {
            return null;
        }
        return text.subSequence(start, i).toString();
    }

    private static @Nullable PsiElement findSelectorInFile(@NotNull PsiFile tsFile, @NotNull String targetSelector) {
        String text = tsFile.getText();
        Matcher selectorMatcher = SELECTOR_PATTERN.matcher(text);
        int foundCount = 0;
        int skippedCount = 0;
        while (selectorMatcher.find()) {
            int matchStart = selectorMatcher.start(2);
            int matchEnd = selectorMatcher.end(2);
            String matchedText = selectorMatcher.group(2);
            // Check if this match is inside a comment
            if (isInsideComment(text, matchStart, matchEnd)) {
                LOG.debug("[AngularGoto] skipping selector '" + matchedText + "' at [" + matchStart + "," + matchEnd + ") - inside comment");
                skippedCount++;
                continue;
            }
            String selectorValue = selectorMatcher.group(2);
            if (!targetSelector.equals(selectorValue)) {
                continue;
            }
            PsiElement selectorLeaf = tsFile.findElementAt(matchStart);
            foundCount++;
            if (selectorLeaf != null) {
                LOG.debug("[AngularGoto] found selector '" + targetSelector + "' at offset " + matchStart + ", element: " + selectorLeaf.getClass().getName());
                return selectorLeaf;
            } else {
                LOG.debug("[AngularGoto] null element at matchStart=" + matchStart);
            }
        }
        LOG.debug("[AngularGoto] findSelectorInFile: tsFile=" + tsFile.getName() + ", targetSelector='" + targetSelector + "', found=" + foundCount + ", skipped=" + skippedCount);
        return null;
    }

    private static boolean isInsideComment(@NotNull String text, int start, int end) {
        int i = 0;
        while (i < text.length()) {
            // Check for block comment
            if (i + 1 < text.length() && text.charAt(i) == '/' && text.charAt(i + 1) == '*') {
                int commentStart = i;
                i += 2;
                while (i + 1 < text.length() && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                    i++;
                }
                if (i + 1 < text.length()) {
                    i += 2; // Skip */
                }
                int commentEnd = i;
                // Check if [start, end) overlaps with [commentStart, commentEnd)
                if (start < commentEnd && end > commentStart) {
                    return true;
                }
                continue;
            }
            // Check for line comment
            if (i + 1 < text.length() && text.charAt(i) == '/' && text.charAt(i + 1) == '/') {
                int commentStart = i;
                // Skip until end of line
                while (i < text.length() && text.charAt(i) != '\n') {
                    i++;
                }
                if (i < text.length()) {
                    i++; // Skip newline
                }
                int commentEnd = i;
                // Check if [start, end) overlaps with [commentStart, commentEnd)
                if (start < commentEnd && end > commentStart) {
                    return true;
                }
                continue;
            }
            // Skip strings - we don't need to check them because the regex already matches
            // only content inside quotes, and those quotes are not inside comments.
            if (text.charAt(i) == '`' || text.charAt(i) == '\'' || text.charAt(i) == '"') {
                char quote = text.charAt(i);
                i++;
                while (i < text.length()) {
                    char c = text.charAt(i);
                    if (c == '\\' && i + 1 < text.length()) {
                        i += 2; // Skip escaped char
                    } else if (c == quote) {
                        i++;
                        break;
                    } else {
                        i++;
                    }
                }
                continue;
            }
            i++;
        }
        return false;
    }
}
