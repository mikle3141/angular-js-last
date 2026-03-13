package org.antlr.jetbrains.sample.structview;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import org.antlr.jetbrains.sample.psi.TypeScriptPSIFileRoot;
import org.jetbrains.annotations.NotNull;

public class TypeScriptStructureViewModel
    extends StructureViewModelBase
    implements StructureViewModel.ElementInfoProvider {

    public TypeScriptStructureViewModel(TypeScriptPSIFileRoot root) {
        super(root, new TypeScriptStructureViewRootElement(root));
    }

    @NotNull
    @Override
    public Sorter[] getSorters() {
        return new Sorter[]{Sorter.ALPHA_SORTER};
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        return !isAlwaysShowsPlus(element);
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return element.getValue() instanceof TypeScriptPSIFileRoot;
    }
}
