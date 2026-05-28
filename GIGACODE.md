# Angular JS - IntelliJ Plugin with ANTLR TypeScript Support

## Project Overview

This is an **IntelliJ platform plugin** that provides TypeScript language support using an ANTLRv4 grammar. The plugin is designed for IntelliJ-based IDEs (Community and Ultimate editions) and integrates with Angular HTML templates.

### Key Technologies

- **Build System:** Gradle (Kotlin DSL) with Gradle IntelliJ Platform plugin
- **Language:** Java 17
- **Parser Generator:** ANTLR 4.13.1
- **IntelliJ Platform Version:** 2025.1.3 (IC - Community Edition)
- **Plugin ID:** `com.gigaide.angular.ts`
- **Target IDE Range:** 251 to 253.* (2025.x series)

### Architecture

The plugin uses the ANTLR IntelliJ Adaptor library to bridge ANTLR-generated parsers with IntelliJ's PSI (Program Structure Interface) system:

- **Lexer/Parsers:** Generated from ANTLR grammar files (`TypeScriptLexer.g4`, `TypeScriptParser.g4`)
- **PSI Nodes:** Extend `ANTLRPsiNode` for automatic `PsiNameIdentifierOwner` support
- **Highlighting:** `TypeScriptSyntaxHighlighter` uses `PSIElementTypeFactory` for token-based highlighting
- **Error Detection:** `TypeScriptExternalAnnotator` and `TypeScriptDecoratorAnnotator` provide syntax validation

### File Types

- **TypeScript Files:** `.ts` extension handled by `TypeScriptFileType`
- **Angular HTML Templates:** Enhanced with Angular-specific completions and reference handlers

## Building and Running

### Prerequisites

- Java 17 or later
- Gradle 8.13 (automatically downloaded via wrapper)

### Key Gradle Tasks

```bash
# Build the plugin
./gradlew assemble

# Build the plugin and create distribution ZIP
./gradlew buildPlugin

# Run the plugin in a sandboxed IntelliJ instance
./gradlew runIde

# Run tests
./gradlew test

# Clean build artifacts
./gradlew clean
```

### Publishing

The plugin can be published to Maven repositories:

```bash
# Publish to local Maven repository
./gradlew publishToMavenLocal

# Publish to remote repository (release or snapshot based on -Drelease flag)
./gradlew publishAllPublicationsToMavenRepository
```

### System Properties for Publishing

Publishing requires the following system properties:

- `gradle.wrapperOscToken` - OSC token for authentication
- `gradle.wrapperUser` - Maven username
- `gradle.wrapperPassword` - Maven password

Example:
```bash
./gradlew buildPlugin -Drelease=true -Dgradle.wrapperUser=xxx -Dgradle.wrapperPassword=xxx -Dgradle.wrapperOscToken=yyy
```

## Project Structure

```
angular-js/
├── src/
│   ├── main/
│   │   ├── antlr/              # ANTLR grammar files
│   │   │   └── org/antlr/jetbrains/sample/parser/
│   │   │       ├── TypeScriptLexer.g4
│   │   │       └── TypeScriptParser.g4
│   │   ├── java/               # Java source code
│   │   │   └── org/antlr/jetbrains/sample/
│   │   │       ├── parser/     # Generated parser classes
│   │   │       ├── psi/        # PSI-related classes
│   │   │       ├── structview/ # Structure view providers
│   │   │       ├── AngularHtmlGotoDeclarationHandler.java
│   │   │       ├── TypeScriptParserDefinition.java
│   │   │       ├── TypeScriptSyntaxHighlighter.java
│   │   │       └── ...
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── plugin.xml      # Plugin configuration
│   │       └── org/
│   └── test/
│       └── java/
│           └── org/antlr/jetbrains/sample/
│               └── TypeScriptLexerParserTest.java
├── libs/                       # Local JAR dependencies
│   └── antlr4-intellij-adaptor-0.2.0.jar
├── build.gradle.kts           # Build configuration
├── settings.gradle.kts        # Settings configuration
└── gradle.properties          # Project properties
```

## Development Conventions

### Code Style

- Java 17 language features
- Standard IntelliJ plugin conventions
- PSI-based AST traversal
- ANTLR grammar format with `$antlr-format` directives

### Key Classes

| Class | Purpose |
|-------|---------|
| `TypeScriptParserDefinition` | Defines the parser, lexer, and PSI element types |
| `TypeScriptSyntaxHighlighter` | Provides syntax highlighting for TypeScript |
| `TypeScriptFileTypeFactory` | Registers `.ts` file extension |
| `TypeScriptExternalAnnotator` | External annotation (code analysis) |
| `TypeScriptDecoratorAnnotator` | Decorator highlighting |
| `AngularHtmlTagReferenceContributor` | HTML tag reference resolution |
| `AngularHtmlTagCompletionContributor` | HTML tag completion |
| `AngularHtmlGotoDeclarationHandler` | Go to declaration handler |

### Testing

Tests use JUnit 4 and validate:
- Lexer tokenization
- Parser rule matching
- Various TypeScript constructs (variables, functions, classes, interfaces, etc.)

Run tests with:
```bash
./gradlew test
```

### Plugin Configuration

Configuration is in `src/main/resources/META-INF/plugin.xml`:
- Declares language support for `ANTLRTypeScript`
- Registers syntax highlighter, parser definition, AST factory
- Configures Angular-specific extensions (completions, references, goto-declaration)
- Declares incompatible plugin: `com.gigaide.javascript`

### Maven Publishing

The plugin is configured to publish to SberOSC repositories:
- Release: `https://nexus-ci.delta.sbrf.ru/repository/maven-lib-release`
- Snapshot: `https://nexus-ci.delta.sbrf.ru/repository/maven-lib-dev`

## Known Issues

- **macOS Dragon Speech Recognition:** Turning on Dragon speech recognition causes GUI deadlocks when renaming elements. Workaround: Turn off Dragon.

## References

- [ANTLR IntelliJ Adaptor](https://github.com/antlr/antlr4-intellij-adaptor/)
- [Gradle IntelliJ Plugin](https://github.com/JetBrains/gradle-intellij-plugin)
- [JetBrains Plugins Repository](https://plugins.jetbrains.com/)
