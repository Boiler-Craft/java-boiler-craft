package kr.craft.javaboilercraft.processor.impl.constructor

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import kr.craft.javaboilercraft.processor.CompletionProcessor

/**
 * BuilderCompletionProcessor
 *
 * @author jaypark
 * @version 1.0.0
 * @since 11/23/23
 */
class ConstructorCompletionProcessor : CompletionProcessor() {

    companion object {
        private val SUPPORT_OPTION = "All Args Constructor"
    }

    override fun supportOption() = SUPPORT_OPTION
    override fun applicable(targetElement: PsiElement, targetClass: PsiClass): Boolean {
        val constructors = targetClass.constructors
        constructors.forEach { constructor ->
            if (constructor.parameterList.parameters.size == targetClass.fields.size) {
                return true
            }
        }
        return false
    }

    override fun completionString(targetClass: PsiClass, targetElement: PsiElement): String {
        return targetClass.fields.joinToString(
            prefix = "new ${targetClass.name}(",
            postfix = ");\n",
            separator = ","
        ) { field -> field.name }
    }
}