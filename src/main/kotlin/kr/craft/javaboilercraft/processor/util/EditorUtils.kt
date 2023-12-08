package kr.craft.javaboilercraft.processor.util

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * EditorUtils
 *
 * @author jaypark
 * @version 1.0.0
 * @since 12/1/23
 */
object  EditorUtils {

    const val TEST_FILE_PATH = "/test"
    const val SRC_TEST_FILE_PATH = "/src/test"

    @JvmStatic
    fun getIndent(psiElement: PsiElement): IndentInfo {
        val psiFile = psiElement.containingFile ?: return IndentInfo.default()
        val documentManager = FileDocumentManager.getInstance()
        val document: Document = documentManager.getDocument(psiFile.virtualFile) ?: return IndentInfo.default()
        val lineText = getLineText(document, psiElement.textOffset)

        return IndentInfo(
            "^\\s*".toRegex().find(lineText)?.value?.length ?: 0,
            "^\\t*".toRegex().find(lineText)?.value?.length ?: 0,
        )
    }
    @JvmStatic
    fun getIndent(editor: Editor): IndentInfo {
        val document = editor.document
        val caretModel = editor.caretModel
        val lineText = getLineText(document, caretModel.offset)

        return IndentInfo(
            "^\\s*".toRegex().find(lineText)?.value?.length ?: 0,
            "^\\t*".toRegex().find(lineText)?.value?.length ?: 0,
        )
    }

    private fun getLineText(
        document: Document,
        offset: Int
    ): String {
        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        return lineText
    }

    @JvmStatic
    fun isCurrentCursorInTestScope(filePath: String) =
        filePath.let { it.contains(TEST_FILE_PATH) || it.contains(SRC_TEST_FILE_PATH) }


}

data class IndentInfo
(
    val spaceCount: Int,
    val tabCount: Int
){
    companion object {
        fun default() = IndentInfo(0, 0)
    }

    fun getIndentString() = " ".repeat(spaceCount) + "\t".repeat(tabCount)

}

