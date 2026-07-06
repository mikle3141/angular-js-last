# Angular JS IntelliJ Plugin

Angular navigation and metadata support for TypeScript projects. The plugin depends on
GigaIDE JS/TS (`com.gigaide.javascript`) for TypeScript PSI and adds:

- HTML tag → `@Component({ selector })` navigation, completion, and go-to-declaration
- Reverse navigation from HTML/CSS/SCSS resources to `templateUrl` / `styleUrls` in `.ts`
- Highlighting for `@Component` decorators and Angular control-flow syntax in template literals
- References from `templateUrl` / `styleUrl` / `styleUrls` string literals to resource files

## Prerequisites

- IntelliJ IDEA Community 2025.1.3+ (see `gradle.properties`)
- Built `com.gigaide.javascript` plugin ZIP (see `jsdecorPluginZip` in `gradle.properties`)
- `com.gigaide.pro` JAR (see `gigaideProJar` in `gradle.properties`)

## Build and run

```bash
./gradlew buildPlugin
./gradlew runIde
./gradlew test
```

## Project layout

| Path | Role |
|------|------|
| `src/main/java/.../AngularPsiUtil.java` | Walks jsdecor PSI for `@Component` metadata |
| `src/main/java/.../AngularSelectorIndex.java` | Project-wide selector index |
| `src/main/java/.../AngularHtml*.java` | HTML/XML cross-language navigation |
| `src/test/testdata/` | Light test fixtures |
