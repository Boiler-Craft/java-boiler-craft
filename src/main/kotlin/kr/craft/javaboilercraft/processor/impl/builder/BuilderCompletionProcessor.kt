package kr.craft.javaboilercraft.processor.impl.builder

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import kr.craft.javaboilercraft.processor.CompletionProcessor
import kr.craft.javaboilercraft.processor.util.EditorUtils.getIndent

/**
 * BuilderCompletionProcessor
 *
 * @author jaypark
 * @version 1.0.0
 * @since 11/23/23
 */
class BuilderCompletionProcessor : CompletionProcessor() {

    companion object {
        private const val SUPPORT_OPTION = "Builder"
        private const val PACKAGE_NAME = "lombok"
    }

    override fun supportOption() = SUPPORT_OPTION
    override fun applicable(targetElement: PsiElement, targetClass: PsiClass) = (targetClass.getAnnotation("$PACKAGE_NAME.$SUPPORT_OPTION") != null)

    override fun completionString(targetClass: PsiClass, targetElement: PsiElement): String {
        val indent = getIndent(targetElement).getIndentString()
        return targetClass.fields.joinToString(
            prefix = "${targetClass.name}.builder()\n",
            postfix = "\n${indent} \t.build();\n",
            separator = "\n"
        ) { field -> "${indent}\t.${field.name}(${field.name})" }
    }

}