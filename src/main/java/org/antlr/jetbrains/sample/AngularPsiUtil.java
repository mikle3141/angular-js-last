package org.antlr.jetbrains.sample;

import com.gigaide.javascript.psi.js.JsArrayLiteralExpression;
import com.gigaide.javascript.psi.js.JsCallExpression;
import com.gigaide.javascript.psi.js.JsDecorator;
import com.gigaide.javascript.psi.js.JsExpression;
import com.gigaide.javascript.psi.js.JsLiteralExpression;
import com.gigaide.javascript.psi.js.JsObjectLiteralExpression;
import com.gigaide.javascript.psi.js.JsProperty;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * PSI-утилиты для Angular-метаданных в {@code .ts} файлах, разобранных плагином com.gigaide.javascript.
 */
public final class AngularPsiUtil {
    public static final String METADATA_TEMPLATE_URL = "templateUrl";
    public static final String METADATA_STYLE_URL = "styleUrl";
    public static final String METADATA_STYLE_URLS = "styleUrls";

    private AngularPsiUtil() {
    }

    public static @NotNull String unquoteStringLiteral(@NotNull PsiElement element) {
        if (element instanceof JsLiteralExpression literal) {
            String value = literal.asString();
            if (value != null) {
                return value;
            }
        }
        String text = element.getText();
        if (text.length() >= 2) {
            char quote = text.charAt(0);
            if ((quote == '\'' || quote == '"' || quote == '`') && text.charAt(text.length() - 1) == quote) {
                return text.substring(1, text.length() - 1);
            }
        }
        return text;
    }

    public static void collectComponentSelectors(@NotNull PsiFile file,
                                                 @NotNull BiConsumer<String, PsiElement> consumer) {
        for (JsDecorator decorator : PsiTreeUtil.findChildrenOfType(file, JsDecorator.class)) {
            if (!isComponentDecorator(decorator)) {
                continue;
            }
            PsiElement selectorLiteral = findSelectorStringLiteral(decorator);
            if (selectorLiteral == null) {
                continue;
            }
            String selector = unquoteStringLiteral(selectorLiteral);
            if (!selector.isEmpty()) {
                consumer.accept(selector, selectorLiteral);
            }
        }
    }

    public static boolean hasComponentDecorators(@NotNull PsiFile file) {
        for (JsDecorator decorator : PsiTreeUtil.findChildrenOfType(file, JsDecorator.class)) {
            if (isComponentDecorator(decorator)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isComponentDecorator(@NotNull PsiElement element) {
        JsDecorator decorator = element instanceof JsDecorator jsDecorator
                ? jsDecorator
                : PsiTreeUtil.getParentOfType(element, JsDecorator.class);
        if (decorator == null) {
            return false;
        }
        String decoratorName = getDecoratorName(decorator);
        return "Component".equals(decoratorName) || decoratorName.endsWith(".Component");
    }

    @Nullable
    public static PsiElement findSelectorStringLiteral(@NotNull PsiElement componentDecorator) {
        return findComponentMetadataStringLiteral(componentDecorator, "selector");
    }

    @Nullable
    public static PsiElement findComponentMetadataStringLiteral(@NotNull PsiElement componentDecorator,
                                                                @NotNull String propertyName) {
        JsObjectLiteralExpression metadata = getComponentMetadataObject(componentDecorator);
        if (metadata == null) {
            return null;
        }
        JsProperty property = findProperty(metadata, propertyName);
        if (property == null) {
            return null;
        }
        if (METADATA_STYLE_URLS.equals(propertyName)) {
            JsArrayLiteralExpression arrayLiteral = PsiTreeUtil.findChildOfType(property, JsArrayLiteralExpression.class);
            if (arrayLiteral == null) {
                return null;
            }
            return PsiTreeUtil.findChildOfType(arrayLiteral, JsLiteralExpression.class);
        }
        return findStringLiteralValue(property);
    }

    public static void collectComponentResourceLiterals(@NotNull PsiElement componentDecorator,
                                                        @NotNull Consumer<PsiElement> consumer) {
        collectComponentMetadataStringLiterals(componentDecorator, METADATA_TEMPLATE_URL, consumer);
        collectComponentMetadataStringLiterals(componentDecorator, METADATA_STYLE_URL, consumer);
        collectComponentMetadataStringLiterals(componentDecorator, METADATA_STYLE_URLS, consumer);
    }

    public static void collectComponentResourceLiterals(@NotNull PsiFile file,
                                                        @NotNull BiConsumer<PsiElement, String> consumer) {
        for (JsDecorator decorator : PsiTreeUtil.findChildrenOfType(file, JsDecorator.class)) {
            if (!isComponentDecorator(decorator)) {
                continue;
            }
            collectComponentMetadataStringLiterals(decorator, METADATA_TEMPLATE_URL,
                    literal -> consumer.accept(literal, METADATA_TEMPLATE_URL));
            collectComponentMetadataStringLiterals(decorator, METADATA_STYLE_URL,
                    literal -> consumer.accept(literal, METADATA_STYLE_URL));
            collectComponentMetadataStringLiterals(decorator, METADATA_STYLE_URLS,
                    literal -> consumer.accept(literal, METADATA_STYLE_URLS));
        }
    }

    public static boolean isComponentResourceUrlLiteral(@NotNull PsiElement element) {
        return getComponentMetadataPropertyForLiteral(element) != null;
    }

    @Nullable
    public static String getComponentMetadataPropertyForLiteral(@NotNull PsiElement literal) {
        if (!isStringLiteral(literal)) {
            return null;
        }
        JsProperty property = PsiTreeUtil.getParentOfType(literal, JsProperty.class);
        if (property == null || findEnclosingComponentDecorator(literal) == null) {
            return null;
        }
        String name = property.getName();
        if (METADATA_TEMPLATE_URL.equals(name)
                || METADATA_STYLE_URL.equals(name)
                || METADATA_STYLE_URLS.equals(name)) {
            return name;
        }
        return null;
    }

    @Nullable
    public static PsiElement findEnclosingComponentDecorator(@NotNull PsiElement element) {
        PsiElement walk = element;
        while (walk != null) {
            if (walk instanceof JsDecorator decorator && isComponentDecorator(decorator)) {
                return decorator;
            }
            walk = walk.getParent();
        }
        return null;
    }

    public static boolean isStringLiteral(@NotNull PsiElement element) {
        return element instanceof JsLiteralExpression literal && literal.isStringLiteral();
    }

    @Nullable
    private static JsObjectLiteralExpression getComponentMetadataObject(@NotNull PsiElement componentDecorator) {
        JsCallExpression call = getDecoratorCallExpression(componentDecorator);
        if (call == null) {
            return null;
        }
        for (JsExpression argument : call.getArguments()) {
            if (argument instanceof JsObjectLiteralExpression objectLiteral) {
                return objectLiteral;
            }
        }
        return null;
    }

    @Nullable
    private static JsCallExpression getDecoratorCallExpression(@NotNull PsiElement componentDecorator) {
        if (componentDecorator instanceof JsDecorator decorator) {
            return PsiTreeUtil.findChildOfType(decorator, JsCallExpression.class);
        }
        return PsiTreeUtil.getParentOfType(componentDecorator, JsCallExpression.class);
    }

    @NotNull
    private static String getDecoratorName(@NotNull JsDecorator decorator) {
        JsCallExpression call = PsiTreeUtil.findChildOfType(decorator, JsCallExpression.class);
        if (call == null) {
            return decorator.getText();
        }
        JsExpression expression = call.getExpression();
        return expression == null ? call.getText() : expression.getText();
    }

    @Nullable
    private static JsProperty findProperty(@NotNull JsObjectLiteralExpression objectLiteral, @NotNull String propertyName) {
        for (JsProperty property : objectLiteral.getProperties()) {
            if (propertyName.equals(property.getName())) {
                return property;
            }
        }
        return null;
    }

    @Nullable
    private static PsiElement findStringLiteralValue(@NotNull JsProperty property) {
        return PsiTreeUtil.findChildOfType(property, JsLiteralExpression.class);
    }

    private static void collectComponentMetadataStringLiterals(@NotNull PsiElement componentDecorator,
                                                               @NotNull String propertyName,
                                                               @NotNull Consumer<PsiElement> consumer) {
        JsObjectLiteralExpression metadata = getComponentMetadataObject(componentDecorator);
        if (metadata == null) {
            return;
        }
        JsProperty property = findProperty(metadata, propertyName);
        if (property == null) {
            return;
        }
        if (METADATA_STYLE_URLS.equals(propertyName)) {
            JsArrayLiteralExpression arrayLiteral = PsiTreeUtil.findChildOfType(property, JsArrayLiteralExpression.class);
            if (arrayLiteral != null) {
                for (JsLiteralExpression literal : PsiTreeUtil.findChildrenOfType(arrayLiteral, JsLiteralExpression.class)) {
                    if (literal.isStringLiteral()) {
                        consumer.accept(literal);
                    }
                }
            }
            return;
        }
        PsiElement literal = findStringLiteralValue(property);
        if (literal != null) {
            consumer.accept(literal);
        }
    }
}
