package kr.craft.javaboilercraft

import com.intellij.codeInsight.completion.*
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import kr.craft.javaboilercraft.processor.CompletionProcessor
import kr.craft.javaboilercraft.processor.impl.builder.BuilderCompletionProcessor
import kr.craft.javaboilercraft.processor.impl.constructor.ConstructorCompletionProcessor
import kr.craft.javaboilercraft.processor.impl.restdoc.MockMvcTestCompletionProcessor
import kr.craft.javaboilercraft.processor.impl.restdoc.core.MethodPropertiesPsiConverter
import kr.craft.javaboilercraft.processor.impl.restdoc.core.MockMvcTestBoilerplateGenerator


/**
 * JavaBoilerCraftCompletion
 *
 * @author jaypark
 * @version 1.0.0
 * @since 11/22/23
 */
class JavaBoilerCraftCompletion(
    private val completionProcessors: List<CompletionProcessor> = listOf(
        BuilderCompletionProcessor(),
        ConstructorCompletionProcessor(),
        MockMvcTestCompletionProcessor(),
    ),
) : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val psiElement = getTargetElement(parameters) ?: return
        if (parameters.offset > 0 && isThereADotRightBeforeCurrenCursor(parameters)) {
            val findClass = findTargetClass(parameters, psiElement) ?: return

            findClass.let { targetClass ->
                completionProcessors.forEach { processor ->
                    if (processor.applicable(psiElement, targetClass)) {
                        println("processor: ${processor.supportOption()}")
                        processor.process(targetClass, psiElement, result)
                    }
                }
            }
        }
    }

    private fun findTargetClass(
        parameters: CompletionParameters, psiElement: PsiElement,
    ) = psiElement.let {
        val project = parameters.originalFile.project
        val psiClasses = PsiShortNamesCache.getInstance(project)
            .getClassesByName(it.text, GlobalSearchScope.allScope(project))
        psiClasses.firstOrNull()
    }

    private fun getTargetElement(
        parameters: CompletionParameters,
    ) = parameters.originalFile.findElementAt(parameters.offset - 2)

    private fun isThereADotRightBeforeCurrenCursor(parameters: CompletionParameters) =
        parameters.editor.document.charsSequence[parameters.offset - 1] == '.'
}