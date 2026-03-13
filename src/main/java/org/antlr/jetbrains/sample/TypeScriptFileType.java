package org.antlr.jetbrains.sample;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TypeScriptFileType extends LanguageFileType {
    public static final String FILE_EXTENSION = "ts";
    public static final TypeScriptFileType INSTANCE = new TypeScriptFileType();

    protected TypeScriptFileType() {
        super(TypeScriptLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "TypeScript";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "TypeScript file (ANTLR)";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return FILE_EXTENSION;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return Icons.SAMPLE_ICON;
    }
}
