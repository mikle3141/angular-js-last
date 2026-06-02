package org.antlr.jetbrains.sample;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AngularHtmlGotoDeclarationHandler implements GotoDeclarationHandler {
    private static final Logger LOG = Logger.getInstance(AngularHtmlGotoDeclarationHandler.class);

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                             int offset,
                                                             Editor editor) {
        if (sourceElement == null) {
            return null;
        }
        PsiElement tagNameElement = findTagNameElement(sourceElement, offset, editor);
        String selector;
        PsiFile htmlFile;
        if (tagNameElement != null) {
            IElementType sourceType = tagNameElement.getNode() != null ? tagNameElement.getNode().getElementType() : null;
            if (sourceType != XmlTokenType.XML_NAME && sourceType != XmlTokenType.XML_TAG_NAME) {
                return null;
            }
            XmlTag tag = PsiTreeUtil.getParentOfType(tagNameElement, XmlTag.class, false);
            if (tag == null) {
                return null;
            }
            htmlFile = tagNameElement.getContainingFile();
            selector = tagNameElement.getText();
        } else {
            htmlFile = PsiDocumentManager.getInstance(sourceElement.getProject()).getPsiFile(editor.getDocument());
            if (htmlFile == null) {
                htmlFile = sourceElement.getContainingFile();
            }
            selector = extractTagNameFromText(editor, offset);
            if (selector == null) {
                return null;
            }
        }

        if (htmlFile == null || !htmlFile.getName().endsWith(".html")) {
            return null;
        }
        if (!isComponentTag(selector)) {
            return null;
        }

        PsiElement target = AngularSelectorIndex.resolveSelector(sourceElement.getProject(), selector);
        if (target == null) {
            LOG.debug("[AngularGoto] selector not resolved: " + selector);
            return null;
        }
        return new PsiElement[]{target};
    }

    private static boolean isComponentTag(@Nullable String tagName) {
        return tagName != null && !tagName.isEmpty() && tagName.indexOf('-') > 0;
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
}
