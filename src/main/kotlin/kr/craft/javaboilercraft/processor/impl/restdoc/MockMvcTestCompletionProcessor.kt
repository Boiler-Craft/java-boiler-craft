package kr.craft.javaboilercraft.processor.impl.restdoc

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import kr.craft.javaboilercraft.processor.CompletionProcessor
import kr.craft.javaboilercraft.processor.impl.restdoc.core.MethodPropertiesPsiConverter
import kr.craft.javaboilercraft.processor.impl.restdoc.core.MockMvcTestBoilerplateGenerator
import kr.craft.javaboilercraft.processor.util.EditorUtils
import kr.craft.javaboilercraft.processor.util.EditorUtils.isCurrentCursorInTestScope

/**
 * MockMvcTestCompletionProcessor
 *
 * @author jaypark
 * @version 1.0.0
 * @since 11/30/23
 */
class MockMvcTestCompletionProcessor: CompletionProcessor() {

    companion object {
        private const val SUPPORT_OPTION = "MockMvc Test for RestDocs"
        private const val CONTROLLER = "Controller"
    }

    override fun supportOption() = SUPPORT_OPTION

    override fun applicable(targetElement: PsiElement, targetClass: PsiClass): Boolean {
        val filePath = targetElement.containingFile.virtualFile.canonicalPath ?: return false
        return targetClass.annotations.any { annotation ->
            annotation.qualifiedName?.contains(CONTROLLER) == true && isCurrentCursorInTestScope(filePath)
        }
    }

    override fun completionString(targetClass: PsiClass, targetElement: PsiElement): String {
        val result = targetClass.allMethods.mapNotNull { method ->
            MethodPropertiesPsiConverter.convert(targetClass, method)?.let {
                MockMvcTestBoilerplateGenerator.generateBoilerplate(it, EditorUtils.getIndent(targetElement).getIndentString())
            }
        }
        return result.joinToString("\n")
    }
}