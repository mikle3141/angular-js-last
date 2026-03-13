package org.antlr.jetbrains.sample;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.lexer.ANTLRLexerAdaptor;
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.jetbrains.sample.parser.TypeScriptLexer;
import org.antlr.jetbrains.sample.parser.TypeScriptParser;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class TypeScriptSyntaxHighlighter extends SyntaxHighlighterBase {
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];
    public static final TextAttributesKey ID =
        createTextAttributesKey("TS_ID", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey KEYWORD =
        createTextAttributesKey("TS_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey STRING =
        createTextAttributesKey("TS_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey LINE_COMMENT =
        createTextAttributesKey("TS_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BLOCK_COMMENT =
        createTextAttributesKey("TS_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    public static final TextAttributesKey NUMBER =
        createTextAttributesKey("TS_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    /** Декораторы (@Identifier) — по умолчанию синий (как METADATA). */
    public static final TextAttributesKey DECORATOR =
        createTextAttributesKey("TS_DECORATOR", DefaultLanguageHighlighterColors.METADATA);

    static {
        PSIElementTypeFactory.defineLanguageIElementTypes(TypeScriptLanguage.INSTANCE,
            TypeScriptParser.tokenNames,
            TypeScriptParser.ruleNames);
    }

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        TypeScriptLexer lexer = new TypeScriptLexer(null);
        return new ANTLRLexerAdaptor(TypeScriptLanguage.INSTANCE, lexer);
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (!(tokenType instanceof TokenIElementType)) return EMPTY_KEYS;
        int ttype = ((TokenIElementType) tokenType).getANTLRTokenType();
        TextAttributesKey attrKey;
        switch (ttype) {
            case TypeScriptLexer.Identifier:
                attrKey = ID;
                break;
            case TypeScriptLexer.StringLiteral:
                attrKey = STRING;
                break;
            case TypeScriptLexer.MultiLineComment:
                attrKey = BLOCK_COMMENT;
                break;
            case TypeScriptLexer.SingleLineComment:
                attrKey = LINE_COMMENT;
                break;
            case TypeScriptLexer.DecimalLiteral:
            case TypeScriptLexer.HexIntegerLiteral:
            case TypeScriptLexer.OctalIntegerLiteral2:
            case TypeScriptLexer.BinaryIntegerLiteral:
                attrKey = NUMBER;
                break;
            case TypeScriptLexer.Break:
            case TypeScriptLexer.Do:
            case TypeScriptLexer.Instanceof:
            case TypeScriptLexer.Typeof:
            case TypeScriptLexer.Case:
            case TypeScriptLexer.Else:
            case TypeScriptLexer.New:
            case TypeScriptLexer.Var:
            case TypeScriptLexer.Catch:
            case TypeScriptLexer.Finally:
            case TypeScriptLexer.Return:
            case TypeScriptLexer.Void:
            case TypeScriptLexer.Continue:
            case TypeScriptLexer.For:
            case TypeScriptLexer.Switch:
            case TypeScriptLexer.While:
            case TypeScriptLexer.Debugger:
            case TypeScriptLexer.Function_:
            case TypeScriptLexer.This:
            case TypeScriptLexer.With:
            case TypeScriptLexer.Default:
            case TypeScriptLexer.If:
            case TypeScriptLexer.Throw:
            case TypeScriptLexer.Delete:
            case TypeScriptLexer.In:
            case TypeScriptLexer.Try:
            case TypeScriptLexer.As:
            case TypeScriptLexer.From:
            case TypeScriptLexer.ReadOnly:
            case TypeScriptLexer.Async:
            case TypeScriptLexer.Await:
            case TypeScriptLexer.Yield:
            case TypeScriptLexer.YieldStar:
            case TypeScriptLexer.Class:
            case TypeScriptLexer.Enum:
            case TypeScriptLexer.Extends:
            case TypeScriptLexer.Super:
            case TypeScriptLexer.Const:
            case TypeScriptLexer.Export:
            case TypeScriptLexer.Import:
            case TypeScriptLexer.Implements:
            case TypeScriptLexer.Let:
            case TypeScriptLexer.Private:
            case TypeScriptLexer.Public:
            case TypeScriptLexer.Interface:
            case TypeScriptLexer.Package:
            case TypeScriptLexer.Protected:
            case TypeScriptLexer.Static:
            case TypeScriptLexer.Any:
            case TypeScriptLexer.Number:
            case TypeScriptLexer.Never:
            case TypeScriptLexer.Boolean:
            case TypeScriptLexer.String:
            case TypeScriptLexer.Unique:
            case TypeScriptLexer.Symbol:
            case TypeScriptLexer.Undefined:
            case TypeScriptLexer.Object:
            case TypeScriptLexer.Of:
            case TypeScriptLexer.KeyOf:
            case TypeScriptLexer.TypeAlias:
            case TypeScriptLexer.Constructor:
            case TypeScriptLexer.Namespace:
            case TypeScriptLexer.Require:
            case TypeScriptLexer.Module:
            case TypeScriptLexer.Declare:
            case TypeScriptLexer.Abstract:
            case TypeScriptLexer.Is:
            case TypeScriptLexer.NullLiteral:
            case TypeScriptLexer.BooleanLiteral:
                attrKey = KEYWORD;
                break;
            case TypeScriptLexer.At:
                attrKey = DECORATOR;
                break;
            default:
                return EMPTY_KEYS;
        }
        return new TextAttributesKey[]{attrKey};
    }
}
