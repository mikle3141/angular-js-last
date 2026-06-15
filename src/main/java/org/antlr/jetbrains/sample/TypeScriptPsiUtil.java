package org.antlr.jetbrains.sample;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.jetbrains.sample.parser.TypeScriptLexer;
import org.antlr.jetbrains.sample.psi.IdentifierPSINode;
import org.antlr.jetbrains.sample.psi.TypeScriptPSIFileRoot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_arrayLiteral;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_classDeclaration;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_decoratorCallExpression;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_decoratorMemberExpression;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_exportModuleItems;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_exportStatement;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_functionDeclaration;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_identifier;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_importFrom;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_importStatement;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_propertyAssignment;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_propertyName;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_sourceElement;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_statement;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_variableDeclaration;

/**
 * Общие PSI-утилиты для TypeScript AST, создаваемого ANTLR-парсером плагина.
 */
public final class TypeScriptPsiUtil {
    public static final String METADATA_TEMPLATE_URL = "templateUrl";
    public static final String METADATA_STYLE_URL = "styleUrl";
    public static final String METADATA_STYLE_URLS = "styleUrls";

    private TypeScriptPsiUtil() {
    }

    /** Снимает окружающие кавычки у строкового литерала. */
    public static @NotNull String unquoteStringLiteral(@NotNull PsiElement element) {
        String text = element.getText();
        if (text.length() >= 2) {
            char quote = text.charAt(0);
            if ((quote == '\'' || quote == '"' || quote == '`') && text.charAt(text.length() - 1) == quote) {
                return text.substring(1, text.length() - 1);
            }
        }
        return text;
    }

    public static boolean isInsideImportStatement(@NotNull PsiElement element) {
        return findImportStatementAncestor(element) != null;
    }

    @Nullable
    public static PsiElement findImportStatementAncestor(@NotNull PsiElement element) {
        PsiElement walk = element;
        while (walk != null) {
            if (isRule(walk, RULE_importStatement)) {
                return walk;
            }
            walk = walk.getParent();
        }
        return null;
    }

    /** Возвращает путь модуля из {@code import ... from '...'}. */
    @Nullable
    public static String getImportModulePath(@NotNull PsiElement importStatement) {
        for (PsiElement importFrom : findElementsByRule(importStatement, RULE_importFrom)) {
            PsiElement literal = findFirstStringLiteral(importFrom);
            if (literal != null) {
                return unquoteStringLiteral(literal);
            }
        }
        return null;
    }

    /** Собирает selector'ы из {@code @Component({ selector: '...' })} в файле. */
    public static void collectComponentSelectors(@NotNull PsiFile file,
                                                 @NotNull BiConsumer<String, PsiElement> consumer) {
        for (PsiElement decoratorCall : findElementsByRule(file, RULE_decoratorCallExpression)) {
            if (!isComponentDecorator(decoratorCall)) {
                continue;
            }
            PsiElement selectorLiteral = findSelectorStringLiteral(decoratorCall);
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
        for (PsiElement decoratorCall : findElementsByRule(file, RULE_decoratorCallExpression)) {
            if (isComponentDecorator(decoratorCall)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static PsiElement findExportedIdentifier(@NotNull TypeScriptPSIFileRoot file, @NotNull String name) {
        for (IdentifierPSINode id : PsiTreeUtil.findChildrenOfType(file, IdentifierPSINode.class)) {
            if (name.equals(id.getText()) && isExportedIdentifier(id)) {
                return id;
            }
        }
        return null;
    }

    public static boolean isComponentDecorator(@NotNull PsiElement decoratorCallExpression) {
        if (!isRule(decoratorCallExpression, RULE_decoratorCallExpression)) {
            return false;
        }
        String decoratorName = getDecoratorName(decoratorCallExpression);
        return "Component".equals(decoratorName) || decoratorName.endsWith(".Component");
    }

    @Nullable
    public static PsiElement findSelectorStringLiteral(@NotNull PsiElement componentDecoratorCall) {
        return findComponentMetadataStringLiteral(componentDecoratorCall, "selector");
    }

    /** Строковый литерал {@code templateUrl}, {@code styleUrl} или элемент {@code styleUrls}. */
    @Nullable
    public static PsiElement findComponentMetadataStringLiteral(@NotNull PsiElement componentDecoratorCall,
                                                                @NotNull String propertyName) {
        for (PsiElement assignment : findElementsByRule(componentDecoratorCall, RULE_propertyAssignment)) {
            if (!propertyName.equals(getPropertyAssignmentName(assignment))) {
                continue;
            }
            if (METADATA_STYLE_URLS.equals(propertyName)) {
                for (PsiElement arrayLiteral : findElementsByRule(assignment, RULE_arrayLiteral)) {
                    PsiElement literal = findFirstStringLiteral(arrayLiteral);
                    if (literal != null) {
                        return literal;
                    }
                }
                continue;
            }
            PsiElement literal = findPropertyAssignmentValueLiteral(assignment);
            if (literal != null) {
                return literal;
            }
        }
        return null;
    }

    /** Все строковые литералы метаданных {@code templateUrl}/{@code styleUrl}/{@code styleUrls}. */
    public static void collectComponentResourceLiterals(@NotNull PsiElement componentDecoratorCall,
                                                        @NotNull Consumer<PsiElement> consumer) {
        collectComponentMetadataStringLiterals(componentDecoratorCall, METADATA_TEMPLATE_URL, consumer);
        collectComponentMetadataStringLiterals(componentDecoratorCall, METADATA_STYLE_URL, consumer);
        collectComponentMetadataStringLiterals(componentDecoratorCall, METADATA_STYLE_URLS, consumer);
    }

    public static void collectComponentResourceLiterals(@NotNull PsiFile file,
                                                        @NotNull BiConsumer<PsiElement, String> consumer) {
        for (PsiElement decoratorCall : findElementsByRule(file, RULE_decoratorCallExpression)) {
            if (!isComponentDecorator(decoratorCall)) {
                continue;
            }
            collectComponentMetadataStringLiterals(decoratorCall, METADATA_TEMPLATE_URL,
                    literal -> consumer.accept(literal, METADATA_TEMPLATE_URL));
            collectComponentMetadataStringLiterals(decoratorCall, METADATA_STYLE_URL,
                    literal -> consumer.accept(literal, METADATA_STYLE_URL));
            collectComponentMetadataStringLiterals(decoratorCall, METADATA_STYLE_URLS,
                    literal -> consumer.accept(literal, METADATA_STYLE_URLS));
        }
    }

    public static boolean isComponentResourceUrlLiteral(@NotNull PsiElement element) {
        return getComponentMetadataPropertyForLiteral(element) != null;
    }

    @Nullable
    public static String getComponentMetadataPropertyForLiteral(@NotNull PsiElement literal) {
        if (!isStringLiteralToken(literal)) {
            return null;
        }
        PsiElement assignment = findEnclosingRule(literal, RULE_propertyAssignment);
        if (assignment == null || findEnclosingComponentDecorator(literal) == null) {
            return null;
        }
        String name = getPropertyAssignmentName(assignment);
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
            if (isComponentDecorator(walk)) {
                return walk;
            }
            walk = walk.getParent();
        }
        return null;
    }

    public static boolean isStringLiteral(@NotNull PsiElement element) {
        return isStringLiteralToken(element);
    }

    private static void collectComponentMetadataStringLiterals(@NotNull PsiElement componentDecoratorCall,
                                                               @NotNull String propertyName,
                                                               @NotNull Consumer<PsiElement> consumer) {
        for (PsiElement assignment : findElementsByRule(componentDecoratorCall, RULE_propertyAssignment)) {
            if (!propertyName.equals(getPropertyAssignmentName(assignment))) {
                continue;
            }
            if (METADATA_STYLE_URLS.equals(propertyName)) {
                for (PsiElement arrayLiteral : findElementsByRule(assignment, RULE_arrayLiteral)) {
                    collectStringLiterals(arrayLiteral, consumer);
                }
            }
            else {
                PsiElement literal = findPropertyAssignmentValueLiteral(assignment);
                if (literal != null) {
                    consumer.accept(literal);
                }
            }
        }
    }

    private static void collectStringLiterals(@NotNull PsiElement root, @NotNull Consumer<PsiElement> consumer) {
        if (isStringLiteralToken(root)) {
            consumer.accept(root);
            return;
        }
        for (PsiElement child : root.getChildren()) {
            collectStringLiterals(child, consumer);
        }
    }

    @Nullable
    private static PsiElement findEnclosingRule(@NotNull PsiElement element, int ruleIndex) {
        PsiElement walk = element;
        while (walk != null) {
            if (isRule(walk, ruleIndex)) {
                return walk;
            }
            walk = walk.getParent();
        }
        return null;
    }

    public static boolean isExportedIdentifier(@NotNull IdentifierPSINode id) {
        PsiElement declaration = findNamedDeclaration(id);
        return declaration != null && isExportedDeclaration(declaration);
    }

    @Nullable
    private static PsiElement findNamedDeclaration(@NotNull IdentifierPSINode id) {
        PsiElement walk = id.getParent();
        while (walk != null) {
            if (isNamedDeclarationRule(walk) && isDeclarationNameIdentifier(id, walk)) {
                return walk;
            }
            walk = walk.getParent();
        }
        return null;
    }

    private static boolean isNamedDeclarationRule(@NotNull PsiElement element) {
        return isRule(element, RULE_classDeclaration)
                || isRule(element, RULE_functionDeclaration)
                || isRule(element, RULE_variableDeclaration)
                || isRule(element, RULE_exportModuleItems);
    }

    private static boolean isDeclarationNameIdentifier(@NotNull IdentifierPSINode id, @NotNull PsiElement declaration) {
        if (isRule(declaration, RULE_exportModuleItems)) {
            return true;
        }
        PsiElement parent = id.getParent();
        return parent != null && isRule(parent, RULE_identifier) && parent.getParent() == declaration;
    }

    private static boolean isExportedDeclaration(@NotNull PsiElement declaration) {
        if (isRule(declaration, RULE_exportModuleItems)) {
            return true;
        }
        PsiElement parent = declaration.getParent();
        if (parent != null && isRule(parent, RULE_exportStatement)) {
            return true;
        }
        if (isRule(declaration, RULE_classDeclaration) && containsExportKeyword(declaration)) {
            return true;
        }
        PsiElement walk = declaration.getParent();
        while (walk != null) {
            if (isNamedDeclarationRule(walk)) {
                return false;
            }
            if (isRule(walk, RULE_sourceElement)) {
                return containsExportKeyword(walk);
            }
            if (isRule(walk, RULE_statement) && containsExportKeyword(walk)) {
                return true;
            }
            walk = walk.getParent();
        }
        return false;
    }

    private static boolean containsExportKeyword(@NotNull PsiElement element) {
        for (PsiElement child : element.getChildren()) {
            if (isExportToken(child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExportToken(@NotNull PsiElement element) {
        IElementType type = element.getNode() != null ? element.getNode().getElementType() : null;
        return type instanceof TokenIElementType
                && ((TokenIElementType) type).getANTLRTokenType() == TypeScriptLexer.Export;
    }

    @NotNull
    private static Collection<PsiElement> findElementsByRule(@NotNull PsiElement root, int ruleIndex) {
        List<PsiElement> result = new ArrayList<>();
        root.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (isRule(element, ruleIndex)) {
                    result.add(element);
                }
                super.visitElement(element);
            }
        });
        return result;
    }

    @Nullable
    private static String getPropertyAssignmentName(@NotNull PsiElement propertyAssignment) {
        for (PsiElement child : propertyAssignment.getChildren()) {
            if (!isRule(child, RULE_propertyName)) {
                continue;
            }
            PsiElement stringLiteral = findFirstStringLiteral(child);
            if (stringLiteral != null) {
                return unquoteStringLiteral(stringLiteral);
            }
            IdentifierPSINode identifier = PsiTreeUtil.findChildOfType(child, IdentifierPSINode.class);
            if (identifier != null) {
                return identifier.getText();
            }
            return child.getText();
        }
        return null;
    }

    @Nullable
    private static PsiElement findPropertyAssignmentValueLiteral(@NotNull PsiElement propertyAssignment) {
        boolean skippedPropertyName = false;
        for (PsiElement child : propertyAssignment.getChildren()) {
            if (!skippedPropertyName && isRule(child, RULE_propertyName)) {
                skippedPropertyName = true;
                continue;
            }
            PsiElement literal = findFirstStringLiteral(child);
            if (literal != null) {
                return literal;
            }
        }
        return null;
    }

    @NotNull
    private static String getDecoratorName(@NotNull PsiElement decoratorCallExpression) {
        for (PsiElement memberExpression : findElementsByRule(decoratorCallExpression, RULE_decoratorMemberExpression)) {
            return memberExpression.getText();
        }
        return decoratorCallExpression.getText();
    }

    @Nullable
    public static PsiElement findFirstStringLiteral(@NotNull PsiElement root) {
        if (isStringLiteralToken(root)) {
            return root;
        }
        for (PsiElement child : root.getChildren()) {
            PsiElement literal = findFirstStringLiteral(child);
            if (literal != null) {
                return literal;
            }
        }
        return null;
    }

    private static boolean isStringLiteralToken(@NotNull PsiElement element) {
        IElementType type = element.getNode() != null ? element.getNode().getElementType() : null;
        return type instanceof TokenIElementType
                && ((TokenIElementType) type).getANTLRTokenType() == TypeScriptLexer.StringLiteral;
    }

    private static boolean isRule(@NotNull PsiElement element, int ruleIndex) {
        ASTNode node = element.getNode();
        if (node == null) {
            return false;
        }
        IElementType type = node.getElementType();
        return type instanceof RuleIElementType && ((RuleIElementType) type).getRuleIndex() == ruleIndex;
    }
}
