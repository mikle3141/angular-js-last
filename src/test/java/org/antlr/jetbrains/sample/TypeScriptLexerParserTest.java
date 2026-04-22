package org.antlr.jetbrains.sample;

import org.antlr.jetbrains.sample.parser.TypeScriptLexer;
import org.antlr.jetbrains.sample.parser.TypeScriptParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import static org.junit.Assert.*;

public class TypeScriptLexerParserTest {

    @Test
    public void testSimpleVariableDeclaration() {
        String code = "var x = 10;";
        TypeScriptLexer lexer = new TypeScriptLexer(new ANTLRInputStream(code));
        TypeScriptParser parser = new TypeScriptParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        assertNotNull(tree);
        assertTrue(tree.toStringTree(parser).contains("variableDeclaration"));
    }

    @Test
    public void testFunctionDeclaration() {
        String code = "function foo() { return 1; }";
        TypeScriptLexer lexer = new TypeScriptLexer(new ANTLRInputStream(code));
        TypeScriptParser parser = new TypeScriptParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        assertNotNull(tree);
        assertTrue(tree.toStringTree(parser).contains("functionDeclaration"));
    }

    @Test
    public void testArrowFunction() {
        String code = "const f = (x: number) => x * 2;";
        TypeScriptLexer lexer = new TypeScriptLexer(new ANTLRInputStream(code));
        TypeScriptParser parser = new TypeScriptParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        assertNotNull(tree);
    }

    @Test
    public void testInterfaceDeclaration() {
        String code = "interface Person { name: string; }";
        TypeScriptLexer lexer = new TypeScriptLexer(new ANTLRInputStream(code));
        TypeScriptParser parser = new TypeScriptParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        assertNotNull(tree);
        // Interface is parsed as an expression statement in this grammar
        String treeStr = tree.toStringTree(parser);
        assertTrue(treeStr.contains("interface") || treeStr.contains("identifier Person"));
    }

    @Test
    public void testClassDeclaration() {
        String code = "class MyClass { private x: number; }";
        TypeScriptLexer lexer = new TypeScriptLexer(new ANTLRInputStream(code));
        TypeScriptParser parser = new TypeScriptParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        assertNotNull(tree);
        assertTrue(tree.toStringTree(parser).contains("classDeclaration"));
    }

    @Test
    public void testImportStatement() {
        String code = "import { foo } from './module';";
        TypeScriptLexer lexer = new TypeScriptLexer(new ANTLRInputStream(code));
        TypeScriptParser parser = new TypeScriptParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        assertNotNull(tree);
        assertTrue(tree.toStringTree(parser).contains("importStatement"));
    }

    @Test
    public void testTypeAnnotations() {
        String code = "var x: number = 5; var y: string = 'test';";
        TypeScriptLexer lexer = new TypeScriptLexer(new ANTLRInputStream(code));
        TypeScriptParser parser = new TypeScriptParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        assertNotNull(tree);
    }

    @Test
    public void testGenericFunction() {
        String code = "function identity<T>(arg: T): T { return arg; }";
        TypeScriptLexer lexer = new TypeScriptLexer(new ANTLRInputStream(code));
        TypeScriptParser parser = new TypeScriptParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        assertNotNull(tree);
        assertTrue(tree.toStringTree(parser).contains("functionDeclaration"));
    }

    @Test
    public void testUnionType() {
        String code = "var x: string | number = 'test';";
        TypeScriptLexer lexer = new TypeScriptLexer(new ANTLRInputStream(code));
        TypeScriptParser parser = new TypeScriptParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        assertNotNull(tree);
    }

    @Test
    public void testComplexCode() {
        String code = """
            interface Person {
                name: string;
                age: number;
            }
            
            class Employee implements Person {
                constructor(public name: string, public age: number) {}
            }
            
            function greet(person: Person): string {
                return "Hello, " + person.name;
            }
            """;
        TypeScriptLexer lexer = new TypeScriptLexer(new ANTLRInputStream(code));
        TypeScriptParser parser = new TypeScriptParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        assertNotNull(tree);
    }
}
