package com.gigaide.angular;

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

/** Общая логика HTML-навигации: теги компонентов и templateUrl. */
public final class AngularHtmlNavigationUtil {
    private AngularHtmlNavigationUtil() {
    }

    public static boolean isComponentTag(@Nullable String tagName) {
        return tagName != null && !tagName.isEmpty() && tagName.indexOf('-') > 0;
    }

    /** Имя Angular-тега под кареткой, или {@code null} если курсор не на имени компонентного тега. */
    @Nullable
    public static String findComponentTagNameAt(@NotNull PsiElement sourceElement,
                                                int offset,
                                                @NotNull Editor editor) {
        String tagName = findTagNameTokenText(sourceElement, offset, editor);
        return isComponentTag(tagName) ? tagName : null;
    }

    @Nullable
    private static String findTagNameTokenText(@NotNull PsiElement sourceElement, int offset, @NotNull Editor editor) {
        PsiElement tagNameElement = findTagNameElement(sourceElement, offset, editor);
        if (tagNameElement != null) {
            return tagNameElement.getText();
        }
        return extractTagNameFromText(editor, offset);
    }

    @Nullable
    private static PsiElement findTagNameElement(@NotNull PsiElement sourceElement, int offset, @NotNull Editor editor) {
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
            PsiElement element = file.findElementAt(candidate);
            if (isTagNameToken(element)) {
                return element;
            }
        }
        return null;
    }

    private static boolean isTagNameToken(@Nullable PsiElement element) {
        if (element == null || element.getNode() == null) {
            return false;
        }
        IElementType type = element.getNode().getElementType();
        if (type != XmlTokenType.XML_NAME && type != XmlTokenType.XML_TAG_NAME) {
            return false;
        }
        XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, false);
        return tag != null;
    }

    @Nullable
    private static String extractTagNameFromText(@NotNull Editor editor, int offset) {
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
