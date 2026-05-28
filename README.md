# Angular JS — IntelliJ плагин с поддержкой TypeScript через ANTLR

Плагин предоставляет поддержку языка TypeScript в IntelliJ-based IDE на основе ANTLRv4 грамматики. Он предназначен для работы с TypeScript файлами (`.ts`) и интегрируется с Angular HTML шаблонами, обеспечивая подсветку синтаксиса, автоподстановку и навигацию.

## Основные возможности

- **Синтаксическая подсветка** TypeScript кода
- **Анализатор PSI** для TypeScript языка
- **Структурный обзор** (Structure View) TypeScript файлов
- **Поиск объявлений** и ссылок в Angular HTML шаблонах
- **Автоподстановки** для Angular HTML тегов
- **Красные подсветки ошибок** через аннотаторы

## Технологии

- **Java 17**
- **Gradle 8.13** (Kotlin DSL)
- **ANTLR 4.13.1** — генератор лексера и парсера
- **IntelliJ Platform 2025.1.3** (Community Edition)
- **Gradle IntelliJ Platform plugin** — сборка и запуск плагина

## Предварительные требования

- Java 17 или выше
- IntelliJ IDEA с плагином Gradle (или любая IntelliJ-based IDE)

## Установка и первичная настройка

1. Откройте проект в IntelliJ IDEA через `File -> Open`
2. Выберите `build.gradle.kts` и откройте как проект Gradle
3. Дождитесь завершения импорта и загрузки зависимостей

## Основные команды Gradle

### Сборка и запуск

```bash
# Собрать плагин (без создания дистрибутива)
./gradlew assemble

# Собрать полный плагин с созданием ZIP-архива
./gradlew buildPlugin

# Запустить плагин в изолированной среде IDE
./gradlew runIde

# Собрать и установить в локальный Maven репозиторий
./gradlew install
```

### Тестирование

```bash
# Запустить все тесты
./gradlew test

# Запустить конкретный класс тестов
./gradlew test --tests "org.antlr.jetbrains.sample.TypeScriptLexerParserTest"
```

### Очистка

```bash
# Очистить все сгенерированные файлы и артефакты сборки
./gradlew clean
```

### Публикация

```bash
# Опубликовать в локальный Maven репозиторий
./gradlew publishToMavenLocal

# Опубликовать в удаленный репозиторий (release версия)
./gradlew publishAllPublicationsToMavenRepository -Drelease=true

# Опубликовать в удаленный репозиторий (snapshot версия)
./gradlew publishAllPublicationsToMavenRepository
```

Для публикации требуются системные свойства:

- `-Dgradle.wrapperUser` — имя пользователя Maven
- `-Dgradle.wrapperPassword` — пароль Maven  
- `-Dgradle.wrapperOscToken` — OSC токен для аутентификации

## Структура проекта

```
angular-js/
├── src/
│   ├── main/
│   │   ├── antlr/                    # ANTLR грамматики
│   │   │   └── org/antlr/jetbrains/sample/parser/
│   │   │       ├── TypeScriptLexer.g4      # Лексер
│   │   │       └── TypeScriptParser.g4     # Парсер
│   │   ├── gen/                      # Сгенерированные исходники
│   │   │   ├── TypeScriptLexer.java
│   │   │   ├── TypeScriptParser.java
│   │   │   └── ...
│   │   ├── java/                     # Код плагина
│   │   │   └── org/antlr/jetbrains/sample/
│   │   │       ├── parser/           # Генерируемые классы парсера
│   │   │       ├── psi/              # PSI узлы
│   │   │       ├── structview/       # Показ структуры
│   │   │       ├── TypeScriptParserDefinition.java
│   │   │       ├── TypeScriptSyntaxHighlighter.java
│   │   │       ├── TypeScriptFileType.java
│   │   │       ├── AngularHtmlTagReferenceContributor.java
│   │   │       ├── AngularHtmlTagCompletionContributor.java
│   │   │       └── ...
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── plugin.xml        # Конфигурация плагина
│   │       └── org/                  # Ресурсы
│   └── test/
│       └── java/                     # Тесты
│           └── org/antlr/jetbrains/sample/
│               └── TypeScriptLexerParserTest.java
├── libs/                             # Локальные JAR зависимости
│   └── antlr4-intellij-adaptor-0.2.0.jar
├── build.gradle.kts                  # Настройки сборки
├── settings.gradle.kts               # Настройки проекта
└── gradle.properties                 # Свойства проекта
```

## Работа с генераторами ANTLR

Генерация лексера и парсера происходит автоматически перед каждой компиляцией. Генерируемые файлы сохраняются в `src/main/gen/`.

Если нужно обновить генераторы вручную:

```bash
# Генерировать только ANTLR файлы
./gradlew generateGrammarSource

# Генерировать только lexer
./gradlew generateLexer

# Генерировать только parser
./gradlew generateParser
```

После изменения `.g4` файлов запустите:

```bash
./gradlew clean generateGrammarSource compileJava
```

## Ключевые классы плагина

| Класс | Назначение |
|-------|-----------|
| `TypeScriptParserDefinition` | Определяет парсер, лексер и типы PSI элементов |
| `TypeScriptSyntaxHighlighter` | Подсветка синтаксиса TypeScript |
| `TypeScriptFileTypeFactory` | Регистрация расширения `.ts` |
| `TypeScriptExternalAnnotator` | Внешняя проверка ошибок |
| `TypeScriptDecoratorAnnotator` | Подсветка декораторов `@Component` и т.д. |
| `AngularHtmlTagReferenceContributor` | Разрешение ссылок в Angular HTML |
| `AngularHtmlTagCompletionContributor` | Автодополнение Angular тегов |
| `AngularHtmlGotoDeclarationHandler` | Переход к объявлению |

## Отладка

Плагин запускается в изолированной IDE при выполнении `./gradlew runIde`. Все изменения в коде применяются при перезапуске плагина в окне отладки.

## Известные проблемы

- **macOS Dragon Speech Recognition:** Включение Dragon speech recognition вызывает GUI deadlocks при переименовании. Решение: отключить Dragon.

## Ссылки

- [ANTLR IntelliJ Adaptor](https://github.com/antlr/antlr4-intellij-adaptor/)
- [Gradle IntelliJ Plugin](https://github.com/JetBrains/gradle-intellij-plugin)
- [JetBrains Plugins Repository](https://plugins.jetbrains.com/)
- [ANTLR 4 Documentation](https://antlr.org/)
