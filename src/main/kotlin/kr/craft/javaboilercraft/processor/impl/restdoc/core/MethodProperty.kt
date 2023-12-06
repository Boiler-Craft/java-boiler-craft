package kr.craft.javaboilercraft.processor.impl.restdoc.core

import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType

data class MethodProperty(
    val name: String,
    val requestPath: String,
    val httpMethodName: String,
    val pathVariables: List<PsiParameter>,
    val queryParams: List<PsiParameter>,
    val requestBody: PsiParameter?,
    val responseType: PsiType?,
)