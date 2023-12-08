package kr.craft.javaboilercraft.processor.impl.restdoc.core

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameter

data class PsiClassInfo(
    val psiClass: PsiClass,
    val genericTypeParameters: List<Pair<PsiTypeParameter, PsiType>>,
    val pathPrefix: String,
)