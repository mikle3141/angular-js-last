package org.antlr.jetbrains.sample.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_classDeclaration;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_exportModuleItems;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_exportStatement;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_functionDeclaration;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_importStatement;

/**
 * Reference for an identifier that appears in an import statement.
 * Resolves to the corresponding export (class of function) in the imported module.
 */
public class ImportRef extends SampleElementRef {
    private static final Pattern IMPORT_FROM_PATTERN = Pattern.compile("\\bfrom\\s+['\"]([^'\"]+)['\"]");

    public ImportRef(@NotNull IdentifierPSINode element) {
        super(element);
    }

    @Override
    public boolean isDefSubtree(PsiElement def) {
        // Accept identifier nodes that are the exported name (under classDeclaration or functionDeclaration or in export list)
        if (def instanceof IdentifierPSINode) {
            return isExportedIdentifier((IdentifierPSINode) def);
        }
        return false;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        PsiElement importStatement = findImportStatementAncestor(myElement);
        if (importStatement == null) {
            return null;
        }
        String importText = importStatement.getText();
        Matcher matcher = IMPORT_FROM_PATTERN.matcher(importText);
        if (!matcher.find()) {
            return null;
        }
        String modulePath = matcher.group(1);
        String importedName = myElement.getName();
        if (importedName == null || importedName.isEmpty()) {
            return null;
        }

        PsiFile currentFile = myElement.getContainingFile();
        VirtualFile currentVf = currentFile != null ? currentFile.getVirtualFile() : null;
        if (currentVf == null) {
            return null;
        }
        VirtualFile baseDir = currentVf.getParent();
        if (baseDir == null || !baseDir.isDirectory()) {
            return null;
        }

        VirtualFile targetFile = resolveRelativeModule(baseDir, modulePath);
        if (targetFile == null) {
            return null;
        }

        PsiFile targetPsi = PsiManager.getInstance(myElement.getProject()).findFile(targetFile);
        if (!(targetPsi instanceof TypeScriptPSIFileRoot)) {
            return null;
        }

        return findExportedName((TypeScriptPSIFileRoot) targetPsi, importedName);
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        return myElement.setName(newElementName);
    }

    @Override
    public boolean isReferenceTo(PsiElement def) {
        String refName = myElement.getName();
        if (refName == null) {
            return false;
        }
        if (def instanceof IdentifierPSINode) {
            return refName.equals(((IdentifierPSINode) def).getText());
        }
        return super.isReferenceTo(def);
    }

    @Nullable
    private static PsiElement findImportStatementAncestor(@NotNull PsiElement element) {
        PsiElement p = element.getParent();
        while (p != null) {
            ASTNode node = p.getNode();
            if (node != null) {
                IElementType type = node.getElementType();
                if (type instanceof RuleIElementType) {
                    if (((RuleIElementType) type).getRuleIndex() == RULE_importStatement) {
                        return p;
                    }
                }
            }
            p = p.getParent();
        }
        return null;
    }

    @Nullable
    private static VirtualFile resolveRelativeModule(@NotNull VirtualFile baseDir, @NotNull String modulePath) {
        Path basePath = Paths.get(baseDir.getPath());
        Path resolved = basePath.resolve(modulePath).normalize();
        // Use forward slashes so LocalFileSystem.findFileSystem.findFileByPath works on all platforms
        String pathStr = resolved.toString().replace('\\', '/');

        LocalFileSystem fs = LocalFileSystem.getInstance();
        // If path already has .ts/.tsx, try as-is first
        VirtualFile vf = fs.findFileByPath(pathStr);
        if (vf != null && !vf.isDirectory()) {
            return vf;
        }
        if (!pathStr.endsWith(".ts") && !pathStr.endsWith(".tsx")) {
            vf = fs.findFileByPath(pathStr + ".ts");
            if (vf != null && !vf.isDirectory()) {
                return vf;
            }
            vf = fs.findFileByPath(pathStr + ".tsx");
            if (vf != null && !vf.isDirectory()) {
                return vf;
            }
        }
        vf = fs.findFileByPath(pathStr);
        if (vf != null && vf.isDirectory()) {
            VirtualFile index = vf.findChild("index.ts");
            if (index != null) {
                return index;
            }
            index = vf.findChild("index.tsx");
            if (index != null) {
                return index;
            }
        }
        // Fallback: resolve via NIO path (e.g. when path has backslashes or different VFS)
        VirtualFile byNio = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(resolved);
        if (byNio != null && !byNio.isDirectory()) {
            return byNio;
        }
        return null;
    }

    @Nullable
    private static PsiElement findExportedName(@NotNull TypeScriptPSIFileRoot file, @NotNull String name) {
        for (IdentifierPSINode id : PsiTreeUtil.findChildrenOfType(file, IdentifierPSINode.class)) {
            if (!name.equals(id.getText())) {
                continue;
            }
            if (isExportedIdentifier(id)) {
                return id;
            }
        }
        return null;
    }

    private static boolean isExportedIdentifier(@NotNull IdentifierPSINode id) {
        boolean underExport = false;
        PsiElement walk = id.getParent();
        while (walk != null) {
            ASTNode node = walk.getNode();
            if (node != null && node.getElementType() instanceof RuleIElementType) {
                int rule = ((RuleIElementType) node.getElementType()).getRuleIndex();
                if (rule == RULE_exportStatement) {
                    underExport = true;
                    break;
                }
            }
            walk = walk.getParent();
        }
        if (!underExport) {
            return false;
        }
        // Walk up: exported name can be under classDeclaration, functionDeclaration, or exportModuleItems
        // (identifier token's parent is often RULE_identifier, not classDeclaration directly)
        walk = id.getParent();
        while (walk != null) {
            ASTNode node = walk.getNode();
            if (node != null && node.getElementType() instanceof RuleIElementType) {
                int rule = ((RuleIElementType) node .getElementType()).getRuleIndex();
                if (rule == RULE_classDeclaration || rule == RULE_functionDeclaration) {
                    return true;
                }
                if (rule == RULE_exportModuleItems) {
                    return true;
                }
                if (rule == RULE_exportStatement) {
                    return true;
                }
            }
            walk = walk.getParent();
        }
        return false;
    }

    private static boolean isInExportModuleItems(PsiElement element) {
        PsiElement walk = element;
        while (walk != null) {
            ASTNode node = walk.getNode();
            if (node != null && node.getElementType() instanceof RuleIElementType) {
                int rule = ((RuleIElementType) node.getElementType()).getRuleIndex();
                if (rule == RULE_exportModuleItems) {
                    return true;
                }
                if (rule == RULE_exportStatement) {
                    return false;
                }
            }
            walk = walk.getParent();
        }
        return false;
    }
}
