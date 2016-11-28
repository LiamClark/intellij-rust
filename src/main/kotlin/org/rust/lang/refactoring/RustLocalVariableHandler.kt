package org.rust.lang.refactoring

import com.intellij.codeInsight.PsiEquivalenceUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pass
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.IntroduceTargetChooser
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser
import org.rust.lang.core.psi.*
import java.util.*

class RustLocalVariableHandler : RefactoringActionHandler {
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
        val offSet = editor?.caretModel?.offset
        val expr = findExpr(file, offSet ?: 0)
        val exprs: List<RustExprElement> = expr?.let(::possibleExpressions) ?: emptyList()

        val passer: Pass<RustExprElement> = pass {
            if (it.isValid) {
                val occurrences = findOccurrences(it)

                OccurrencesChooser.simpleChooser<PsiElement>(editor).showChooser(expr, occurrences, pass { choice ->
                    if (choice == OccurrencesChooser.ReplaceChoice.ALL) {
                        replaceElement(project, editor!!, occurrences)
                    } else {
                        replaceElement(project, editor!!, listOf(it))
                    }
                })
            }
        }

        IntroduceTargetChooser.showChooser(editor!!, exprs, passer) {
            it.text
        }
    }

    fun replaceElement(project: Project, editor: Editor, exprs: List<PsiElement>) {
        //the expr that has been chosen
        val expr = exprs.first()
        val anchor = findAnchor(expr)
        val parent = expr.parent
        if (anchor == expr) {
            inlineLet(project, editor, anchor, { RustElementFactory.createVariableDeclaration(project, "x", it)!! })
        } else if (parent is RustExprStmtElement) {
            inlineLet(project, editor, parent, { RustElementFactory.createVariableDeclarationFromStmt(project, "x", it)!! })
        } else {
            replaceElementForAllExpr(project, editor, exprs)
        }
    }

    fun replaceElementForAllExpr(project: Project, editor: Editor, exprs: List<PsiElement>) {
        val expr = exprs.first()

        createLet(project, expr)?.let {
            val (let, name) = it
            var nameElem: RustPatBindingElement? = null
            WriteCommandAction.runWriteCommandAction(project) {
                val newElement = introduceLet(project, expr, let)
                exprs.forEach { it.replace(name) }

                nameElem = moveEditorToNameElement(editor, newElement)
            }
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
            nameElem?.let { RustInPlaceVariableIntroducer(it, editor, project, "choose a variable", emptyArray()).performInplaceRefactoring(LinkedHashSet(listOf("x"))) }
        }
    }


    fun <T : PsiElement> inlineLet(project: Project, editor: Editor, stmt: T, statementFactory: (T) -> RustStmtElement) {
        var newNameElem: RustPatBindingElement? = null
        WriteCommandAction.runWriteCommandAction(project) {
            val statement = statementFactory.invoke(stmt)
            val newStatement = stmt.replace(statement)

            newNameElem = moveEditorToNameElement(editor, newStatement)
        }

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
        newNameElem?.let { RustInPlaceVariableIntroducer(it, editor, project, "choose a variable", emptyArray()).performInplaceRefactoring(LinkedHashSet(listOf("x"))) }
    }

    fun introduceLet(project: Project, expr: PsiElement, let: RustLetDeclElement): PsiElement? {
        val anchor = findAnchor(expr)
        val context = anchor?.context
        val newline = PsiParserFacade.SERVICE.getInstance(project).createWhiteSpaceFromText(System.lineSeparator())

        return context?.addBefore(let, context.addBefore(newline, anchor))
    }

    /**
     * Creates a let binding for the found expression.
     * Returning handles to the complete let expr and the identifier inside the newly created let binding.
     */
    fun createLet(project: Project, expr: PsiElement): Pair<RustLetDeclElement, PsiElement>? {
        val parent = expr.parent

        val let = if (parent is RustUnaryExprElement && parent.mut != null) {
            RustElementFactory.createMutableVariableDeclaration(project, "x", expr)
        } else {
            RustElementFactory.createVariableDeclaration(project, "x", expr)
        }

        val binding = let?.findBinding()

        if (let != null && binding != null) {
            return Pair(let, binding.identifier)
        } else {
            return null
        }
    }

    fun moveEditorToNameElement(editor: Editor, element: PsiElement?): RustPatBindingElement? {
        val newName = element?.findBinding()

        editor.caretModel.moveToOffset(newName?.identifier?.textRange?.startOffset ?: 0)

        return newName
    }

    fun PsiElement.findBinding() = PsiTreeUtil.findChildOfType(this, RustPatBindingElement::class.java)

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        println("not from the editor.")
    }
}

fun findExpr(file: PsiFile?, offSet: Int) = PsiTreeUtil.getNonStrictParentOfType(file?.findElementAt(offSet), RustExprElement::class.java)


fun findAnchor(expr: PsiElement) = PsiTreeUtil.getNonStrictParentOfType(expr, RustBlockElement::class.java)?.let { findAnchor(expr, it) }

fun findAnchor(expr: PsiElement, block: PsiElement): PsiElement? {
    var anchor = expr
    while (anchor.parent != block) {
        anchor = anchor.parent
    }

    return anchor
}

fun possibleExpressions(expr: RustExprElement) = SyntaxTraverser.psiApi().parents(expr)
    .takeWhile { it !is RustBlockElement }
    .filter { it !is RustPathExprElement }
    .filterIsInstance(RustExprElement::class.java)

fun findBlock(expr: PsiElement) = PsiTreeUtil.getNonStrictParentOfType(expr, RustBlockElement::class.java)

fun findOccurrences(expr: PsiElement): List<PsiElement> {
    val visitor = OccurrenceVisitor(expr)

    val occurrences: List<PsiElement> = findBlock(expr)?.let {
        it.acceptChildren(visitor)
        visitor.foundOccurrences
    } ?: emptyList()

    return occurrences
}

fun <T> pass(pass: (T) -> Unit): Pass<T> {
    return object : Pass<T>() {
        override fun pass(t: T) {
            pass.invoke(t)
        }
    }
}


class OccurrenceVisitor(val element: PsiElement) : PsiRecursiveElementVisitor() {
    val foundOccurrences = ArrayList<PsiElement>()

    override fun visitElement(element: PsiElement?) {
        if (element != null) {
            if (PsiEquivalenceUtil.areElementsEquivalent(this.element, element)) {
                foundOccurrences.add(element)
            } else {
                super.visitElement(element)
            }
        }
    }
}


class RustInPlaceVariableIntroducer(elementToRename: PsiNamedElement, editor: Editor, project: Project, title: String, occurrences: Array<PsiElement>) :
    InplaceVariableIntroducer<PsiElement>(elementToRename, editor, project, title, occurrences, null) {
}

