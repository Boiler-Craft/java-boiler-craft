package kr.samjo.javabuilderfill

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import kr.samjo.javabuilderfill.processor.CompletionProcessor
import kr.samjo.javabuilderfill.processor.impl.BuilderCompletionProcessor
import kr.samjo.javabuilderfill.processor.impl.ConstructorCompletionProcessor


/**
 * BuilderFillCompletion
 *
 * @author jaypark
 * @version 1.0.0
 * @since 11/22/23
 */
class BuilderFillCompletion : CompletionContributor() {

    private val completionProcessors: List<CompletionProcessor> = listOf(
        BuilderCompletionProcessor(),
        ConstructorCompletionProcessor()
    )

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val psiElement = getTargetElement(parameters) ?: return
        if (parameters.offset > 0 && isThereADotRightBeforeCurrenCursor(parameters)) {
            val findClass = findTargetClass(parameters, psiElement) ?: return

            findClass.let {
                completionProcessors.forEach { processor ->
                    if (processor.applicable(it)) {
                        processor.process(it, psiElement, result)
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