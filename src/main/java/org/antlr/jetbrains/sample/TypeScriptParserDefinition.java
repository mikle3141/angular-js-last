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

public class TypeScriptParserDefinition implements ParserDefinition {
    public static final IFileElementType FILE =
        new IFileElementType(TypeScriptLanguage.INSTANCE);

    public static TokenIElementType ID;
    /** Direct child rule of {@code functionDeclaration} that holds the function name. */
    public static RuleIElementType IDENTIFIER;
    /** Direct child rule of {@code variableDeclaration} that holds the variable name. */
    public static RuleIElementType IDENTIFIER_OR_KEYWORD;
    /** Direct child rule of {@code formalParameterArg} that holds the parameter name. */
    public static RuleIElementType ASSIGNABLE;

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

    public static final TokenSet COMMENTS =
        PSIElementTypeFactory.createTokenSet(
            TypeScriptLanguage.INSTANCE,
            TypeScriptLexer.MultiLineComment,
            TypeScriptLexer.SingleLineComment);

    public static final TokenSet WHITESPACE =
        PSIElementTypeFactory.createTokenSet(
            TypeScriptLanguage.INSTANCE,
            TypeScriptLexer.WhiteSpaces,
            TypeScriptLexer.LineTerminator);

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
                return new FunctionSubtree(node, IDENTIFIER);
            case TypeScriptParser.RULE_variableDeclaration:
                return new VardefSubtree(node, IDENTIFIER_OR_KEYWORD);
            case TypeScriptParser.RULE_formalParameterArg:
                return new ArgdefSubtree(node, ASSIGNABLE);
            case TypeScriptParser.RULE_block:
                return new BlockSubtree(node);
            case TypeScriptParser.RULE_arguments:
                return new CallSubtree(node);
            default:
                return new ANTLRPsiNode(node);
        }
    }
}
