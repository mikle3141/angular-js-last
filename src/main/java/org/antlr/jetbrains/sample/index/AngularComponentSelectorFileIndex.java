package org.antlr.jetbrains.sample.index;

import com.gigaide.javascript.fileTypes.TsFileType;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.antlr.jetbrains.sample.AngularIndexScope;
import org.antlr.jetbrains.sample.AngularPsiUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * File-based index: selector string → .ts files that declare {@code @Component({ selector: '...' })}.
 * Надёжнее полного скана через {@link com.intellij.psi.search.FilenameIndex} в runIde.
 */
public final class AngularComponentSelectorFileIndex extends ScalarIndexExtension<String> {
    public static final ID<String, Void> NAME = ID.create("com.gigaide.angular.component.selector");

    private static final DataIndexer<String, Void, FileContent> INDEXER = inputData -> {
        if (AngularIndexScope.isExcludedPath(inputData.getFile().getPath())) {
            return Collections.emptyMap();
        }
        var psiFile = inputData.getPsiFile();
        if (psiFile == null) {
            return Collections.emptyMap();
        }
        Map<String, Void> selectors = new HashMap<>();
        AngularPsiUtil.collectComponentSelectors(psiFile, (selector, ignored) -> selectors.putIfAbsent(selector, null));
        return selectors;
    };

    @Override
    public @NotNull ID<String, Void> getName() {
        return NAME;
    }

    @Override
    public @NotNull DataIndexer<String, Void, FileContent> getIndexer() {
        return INDEXER;
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public @NotNull FileBasedIndex.InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(TsFileType.INSTANCE);
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
