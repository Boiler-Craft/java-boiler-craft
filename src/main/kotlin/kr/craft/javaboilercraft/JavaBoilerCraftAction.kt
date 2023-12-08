package kr.craft.javaboilercraft

import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.PsiMethodMember
import com.intellij.ide.util.MemberChooser
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.ui.components.JBList
import kr.craft.javaboilercraft.processor.impl.restdoc.core.MethodPropertiesPsiConverter
import kr.craft.javaboilercraft.processor.impl.restdoc.core.MockMvcTestBoilerplateGenerator
import kr.craft.javaboilercraft.processor.util.EditorUtils.getIndent
import kr.craft.javaboilercraft.processor.util.EditorUtils.isCurrentCursorInTestScope
import javax.swing.ListSelectionModel

/**
 * JavaBoilerCraftAction
 *
 * @author jaypark
 * @version 1.0.0
 * @since 12/8/23
 */
class JavaBoilerCraftAction : AnAction() {


    /**
     * Updates the state of the action. The action is visible only if the current cursor is in test scope.
     */
    override fun update(e: AnActionEvent) {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val editor: Editor? = e.getData(CommonDataKeys.EDITOR)
        var visible = false
        if (psiFile != null && editor != null) {
            psiFile.containingFile.virtualFile.canonicalPath?.let { filePath ->
                visible = isCurrentCursorInTestScope(filePath)
            }
        }
        e.presentation.isEnabledAndVisible = visible
    }

    override fun actionPerformed(e: AnActionEvent) {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (psiFile == null || editor == null) return

        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val document = editor.document

        val restControllerClass = getRestControllerClass(project) ?: return
        val restControllerClasses = getRestControllerClasses(restControllerClass, project)

        val restControllerClassNames = JBList(restControllerClasses.map { it.name })
        if (restControllerClassNames.isEmpty) return
        restControllerClassNames.selectionMode = ListSelectionModel.SINGLE_SELECTION

        showPopUpAndGetSelectedClass(restControllerClassNames, editor) { selectedClass ->
            val selectedControllerClass = restControllerClasses.find { it.name == selectedClass } ?: return@showPopUpAndGetSelectedClass
            val selectedMethods = showMemberChooserAndGetSelectedMethods(selectedControllerClass, project) ?: return@showPopUpAndGetSelectedClass
            generateBoilerplateAndInsertIntoDocument(selectedControllerClass, selectedMethods, editor, document, project)
        }
    }

    /**
     * Gets the PsiClass for the @RestController annotation.
     */
    private fun getRestControllerClass(project: Project): PsiClass? {
        return JavaPsiFacade.getInstance(project)
            .findClass("org.springframework.web.bind.annotation.RestController", GlobalSearchScope.allScope(project))
    }

    /**
     * Gets all classes in the project that have the @RestController annotation.
     */
    private fun getRestControllerClasses(restControllerClass: PsiClass, project: Project): MutableList<PsiClass> {
        val restControllerClasses = mutableListOf<PsiClass>()
        runReadAction {
            AnnotatedElementsSearch.searchPsiClasses(restControllerClass, GlobalSearchScope.allScope(project)).forEach { psiClass ->
                restControllerClasses.add(psiClass)
            }
        }
        return restControllerClasses
    }

    /**
     * Shows a popup to select a controller class and calls the provided callback with the selected class.
     */
    private fun showPopUpAndGetSelectedClass(
        classList: JBList<String?>,
        editor: Editor,
        onItemSelected: (String) -> Unit,
    ) {
        PopupChooserBuilder(classList)
            .setTitle("Select Controller Class to Generate MockMvc Test")
            .setItemChoosenCallback {
                val selectedClass = classList.selectedValue ?: ""
                onItemSelected(selectedClass)
            }
            .createPopup()
            .showInBestPositionFor(editor)
    }


    /**
     * Shows a MemberChooser to select methods and returns the selected methods.
     */
    private fun showMemberChooserAndGetSelectedMethods(selectedControllerClass: PsiClass, project: Project): List<PsiMethodMember>? {
        val methods = selectedControllerClass.methods.map { PsiMethodMember(it) }
        val chooser = MemberChooser<ClassMember>(methods.toTypedArray(), false, true, project)
        chooser.title = "Select Methods to Generate MockMvc Test"
        chooser.show()
        return chooser.selectedElements?.map { it as PsiMethodMember }
    }

    /**
     * Generates boilerplate code for MockMvc tests and inserts it into the document.
     */
    private fun generateBoilerplateAndInsertIntoDocument(
        selectedControllerClass: PsiClass,
        selectedMethods: List<PsiMethodMember>,
        editor: Editor,
        document: Document,
        project: Project
    ) {
        val boilerplate = selectedMethods.mapNotNull { methodMember ->
            val method = methodMember.element
            MethodPropertiesPsiConverter.convert(selectedControllerClass, method)?.let {
                MockMvcTestBoilerplateGenerator.generateBoilerplate(it, getIndent(editor).getIndentString())
            }
        }.joinToString("\n")

        WriteCommandAction.writeCommandAction(project)
            .run<RuntimeException> {
                document.insertString(editor.caretModel.offset, boilerplate)
            }
    }
}