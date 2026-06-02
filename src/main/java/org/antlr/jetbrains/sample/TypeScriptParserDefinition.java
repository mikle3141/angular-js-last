package org.antlr.jetbrains.sample;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.adaptor.lexer.ANTLRLexerAdaptor;
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.intellij.adaptor.parser.ANTLRParserAdaptor;
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode;
import org.antlr.jetbrains.sample.parser.TypeScriptLexer;
import org.antlr.jetbrains.sample.parser.TypeScriptParser;
import org.antlr.jetbrains.sample.psi.ArgdefSubtree;
import org.antlr.jetbrains.sample.psi.BlockSubtree;
import org.antlr.jetbrains.sample.psi.CallSubtree;
import org.antlr.jetbrains.sample.psi.FunctionSubtree;
import org.antlr.jetbrains.sample.psi.TypeScriptPSIFileRoot;
import org.antlr.jetbrains.sample.psi.VardefSubtree;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Определение парсера TypeScript для платформы IntelliJ.
 * <p>
 * Связывает сгенерированные ANTLR-лексер и парсер с PSI-деревом:
 * лексер, парсер, типы токенов/правил и фабрика PSI-узлов.
 * Листовые узлы (идентификаторы) создаются в {@link TypeScriptASTFactory},
 * составные — в {@link #createElement(ASTNode)}.
 */
public class TypeScriptParserDefinition implements ParserDefinition {

    /** Тип корневого узла файла. */
    public static final IFileElementType FILE =
        new IFileElementType(TypeScriptLanguage.INSTANCE);

    /** Тип токена идентификатора (используется при переименовании и т.п.). */
    public static TokenIElementType ID;

    /** Прямой дочерний узел {@code functionDeclaration}, содержащий имя функции. */
    public static RuleIElementType IDENTIFIER;

    /** Прямой дочерний узел {@code variableDeclaration}, содержащий имя переменной. */
    public static RuleIElementType IDENTIFIER_OR_KEYWORD;

    /** Прямой дочерний узел {@code formalParameterArg}, содержащий имя параметра. */
    public static RuleIElementType ASSIGNABLE;

    /** Регистрация типов токенов и правил грамматики для языка ANTLRTypeScript. */
    static {
        PSIElementTypeFactory.defineLanguageIElementTypes(TypeScriptLanguage.INSTANCE,
            TypeScriptParser.tokenNames,
            TypeScriptParser.ruleNames);
        List<TokenIElementType> tokenIElementTypes =
            PSIElementTypeFactory.getTokenIElementTypes(TypeScriptLanguage.INSTANCE);
        ID = tokenIElementTypes.get(TypeScriptLexer.Identifier);
        List<RuleIElementType> ruleIElementTypes =
            PSIElementTypeFactory.getRuleIElementTypes(TypeScriptLanguage.INSTANCE);
        IDENTIFIER = ruleIElementTypes.get(TypeScriptParser.RULE_identifier);
        IDENTIFIER_OR_KEYWORD = ruleIElementTypes.get(TypeScriptParser.RULE_identifierOrKeyWord);
        ASSIGNABLE = ruleIElementTypes.get(TypeScriptParser.RULE_assignable);
    }

    /** Многострочные и однострочные комментарии — пропускаются при построении PSI. */
    public static final TokenSet COMMENTS =
        PSIElementTypeFactory.createTokenSet(
            TypeScriptLanguage.INSTANCE,
            TypeScriptLexer.MultiLineComment,
            TypeScriptLexer.SingleLineComment);

    /** Пробелы и переводы строк — пропускаются при построении PSI. */
    public static final TokenSet WHITESPACE =
        PSIElementTypeFactory.createTokenSet(
            TypeScriptLanguage.INSTANCE,
            TypeScriptLexer.WhiteSpaces,
            TypeScriptLexer.LineTerminator);

    /** Строковые литералы (для подсветки и навигации по строкам). */
    public static final TokenSet STRING =
        PSIElementTypeFactory.createTokenSet(
            TypeScriptLanguage.INSTANCE,
            TypeScriptLexer.StringLiteral);

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        TypeScriptLexer lexer = new TypeScriptLexer(null);
        return new ANTLRLexerAdaptor(TypeScriptLanguage.INSTANCE, lexer);
    }

    @NotNull
    @Override
    public PsiParser createParser(final Project project) {
        final TypeScriptParser parser = new TypeScriptParser(null);
        return new ANTLRParserAdaptor(TypeScriptLanguage.INSTANCE, parser) {
            @Override
            protected ParseTree parse(Parser parser, IElementType root) {
                // Для файла — правило program; для переименования отдельного ID — identifier.
                if (root instanceof IFileElementType) {
                    return ((TypeScriptParser) parser).program();
                }
                return ((TypeScriptParser) parser).identifier();
            }
        };
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return WHITESPACE;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return STRING;
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new TypeScriptPSIFileRoot(viewProvider);
    }

    /**
     * Преобразует внутренний AST-узел в PSI-элемент.
     * <p>
     * Для ключевых правил грамматики создаются специализированные поддеревья,
     * необходимые для навигации, поиска использований и переименования.
     * Листья-идентификаторы создаются в {@link TypeScriptASTFactory}.
     */
    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        IElementType elType = node.getElementType();
        if (elType instanceof TokenIElementType) {
            return new ANTLRPsiNode(node);
        }
        if (!(elType instanceof RuleIElementType)) {
            return new ANTLRPsiNode(node);
        }
        switch (((RuleIElementType) elType).getRuleIndex()) {
            case TypeScriptParser.RULE_functionDeclaration:
                // Определение функции: PsiNameIdentifierOwner + область видимости параметров.
                return new FunctionSubtree(node, IDENTIFIER);
            case TypeScriptParser.RULE_variableDeclaration:
                // Объявление переменной (var/let/const).
                return new VardefSubtree(node, IDENTIFIER_OR_KEYWORD);
            case TypeScriptParser.RULE_formalParameterArg:
                // Параметр функции.
                return new ArgdefSubtree(node, ASSIGNABLE);
            case TypeScriptParser.RULE_block:
                // Блок { ... }: область видимости локальных переменных.
                return new BlockSubtree(node);
            case TypeScriptParser.RULE_arguments:
                // Вызов функции: foo(...).
                return new CallSubtree(node);
            default:
                return new ANTLRPsiNode(node);
        }
    }
}
