package org.antlr.jetbrains.sample;

import com.gigaide.javascript.TsLanguage;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Посвечивает специфические ангулярные конструкции внутри интерпретируемых строк
 * (шаблонные литералы в backtick-кавычках) тем же цветом что и метаданные (декораторы).
 *
 * Ищутся следующие конструкции:
 * "{{", "}}", "@if", "@else", "@for", "@switch", "@case", "@default",
 * "@defer", "@placeholder", "@loading@, "@error"
 */
public class AngularTemplateAnnotator implements Annotator {

    private static final Pattern ANGULAR_PATTERN = Pattern.compile(
            "\\{\\{|}}|" +
                    "@if\\b|@else\\b|@for\\b|@switch\\b|@case\\b|@default\\b|" +
                    "@defer\\b|@placeholder\\b|@loading\\b|@error\\b"
    );

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        PsiFile file = element.getContainingFile();
        if (file == null || !file.getLanguage().is(TsLanguage.INSTANCE)) {
            return;
        }
        // Чтобы не выполняь работу по сто раз, можно ограничиться корнем
        if (element != file) {
            return;
        }

        CharSequence text = file.getViewProvider().getContents();
        int length = text.length();
        int index = 0;

        while (index < length) {
            int backtickStart = findNextBacktick(text, index, length);
            if (backtickStart == -1) {
                break;
            }
            int backtickEnd = findClosingBacktick(text, backtickStart + 1, length);
            if (backtickEnd == -1) {
                break;
            }

            highlightAngularInTemplate(text, backtickStart + 1, backtickEnd, holder);
            index = backtickEnd + 1;
        }
    }

    private static int findNextBacktick(CharSequence text, int from, int length) {
        for (int i = from; i < length; i++) {
            char c = text.charAt(i);
            if (c == '`') {
                if (i == 0 || text.charAt(i - 1) != '\\') {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findClosingBacktick(CharSequence text, int from, int length) {
        for (int i = from; i < length; ++i) {
            char c = text.charAt(i);
            if (c == '\\') {
                // пропускаем экранированный символ
                if (i + 1 < length) {
                    i++;
                }
                continue;
            }
            if (c == '`') {
                return i;
            }
        }
        return -1;
    }

    private static void highlightAngularInTemplate(CharSequence text,
                                                   int startOffset,
                                                   int endOffset,
                                                   AnnotationHolder holder) {
        if (startOffset >= endOffset) {
            return;
        }
        CharSequence slice = text.subSequence(startOffset, endOffset);
        Matcher m = ANGULAR_PATTERN.matcher(slice);
        while (m.find()) {
            int start = startOffset + m.start();
            int end = startOffset + m.end();
            TextRange range = new TextRange(start, end);
            Annotation ann = holder.createInfoAnnotation(range, null);
            ann.setTextAttributes(AngularHighlighterKeys.DECORATOR);
        }
    }
}
