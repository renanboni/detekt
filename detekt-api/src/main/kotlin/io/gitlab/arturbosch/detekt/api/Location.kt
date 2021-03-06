package io.gitlab.arturbosch.detekt.api

import io.gitlab.arturbosch.detekt.api.internal.getTextSafe
import io.gitlab.arturbosch.detekt.api.internal.searchName
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getTextWithLocation
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Specifies a position within a source code fragment.
 */
data class Location(
    val source: SourceLocation,
    val text: TextLocation,
    @Deprecated("Will be removed in the future. Use queries on 'ktElement' instead.")
    val locationString: String,
    val file: String
) : Compactable {

    override fun compact(): String = "$file:$source"

    companion object {
        /**
         * Creates a [Location] from a [PsiElement].
         * If the element can't be determined, the [KtFile] with a character offset can be used.
         */
        fun from(element: PsiElement, offset: Int = 0): Location {
            val start = startLineAndColumn(element, offset)
            val sourceLocation = SourceLocation(start.line, start.column)
            val textLocation = TextLocation(element.startOffset + offset, element.endOffset + offset)
            val fileName = element.originalFilePath()
            val locationText = element.getTextAtLocationSafe()
            return Location(sourceLocation, textLocation, locationText, fileName)
        }

        /**
         * Determines the line and column of a [PsiElement] in the source file.
         */
        @Suppress("TooGenericExceptionCaught")
        fun startLineAndColumn(element: PsiElement, offset: Int = 0): PsiDiagnosticUtils.LineAndColumn {
            return try {
                val range = element.textRange
                DiagnosticUtils.getLineAndColumnInPsiFile(element.containingFile,
                    TextRange(range.startOffset + offset, range.endOffset + offset))
            } catch (e: IndexOutOfBoundsException) {
                // #18 - somehow the TextRange is out of bound on '}' leaf nodes, returning fail safe -1
                PsiDiagnosticUtils.LineAndColumn(-1, -1, null)
            }
        }

        private fun PsiElement.originalFilePath() =
            (containingFile.viewProvider.virtualFile as? LightVirtualFile)?.originalFile?.name
                ?: containingFile.name

        private fun PsiElement.getTextAtLocationSafe() =
            getTextSafe({ searchName() }, { getTextWithLocation() })
    }
}

/**
 * Stores line and column information of a location.
 */
data class SourceLocation(val line: Int, val column: Int) {
    override fun toString(): String = "$line:$column"
}

/**
 * Stores character start and end positions of an text file.
 */
data class TextLocation(val start: Int, val end: Int) {
    override fun toString(): String = "$start:$end"
}
