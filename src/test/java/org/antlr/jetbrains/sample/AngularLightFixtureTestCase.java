package org.antlr.jetbrains.sample;

import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Path;

/**
 * Базовый light-тест с фикстурой PSI и каталогом {@code src/test/testdata}.
 * Зависимые плагины подключаются через Gradle ({@code bundledPlugins}, {@code localPlugin}).
 */
@RunWith(JUnit4.class)
abstract class AngularLightFixtureTestCase extends LightPlatformCodeInsightFixture4TestCase {

    @Override
    protected @NotNull String getTestDataPath() {
        return Path.of(System.getProperty("user.dir"), "src", "test", "testdata").toString();
    }

    /** Значение строкового литерала selector'а без окружающих кавычек. */
    protected static @NotNull String selectorLiteralValue(@NotNull PsiElement element) {
        return AngularPsiUtil.unquoteStringLiteral(element);
    }
}
