package org.rust.lang.refactoring

import org.rust.lang.RustTestCaseBase
import org.rust.lang.core.psi.RustExprStmtElement

class IntroduceVariableTest : RustTestCaseBase() {
    override val dataPath = "org/rust/lang/refactoring/fixtures/introduce_variable/"

//    @Ignore
//    fun testVariable() = checkByFile {
//        val rustLocalVariableHandler = RustLocalVariableHandler()
//        openFileInEditor("variable.rs")
//        rustLocalVariableHandler.invoke(myFixture.project, myFixture.editor, myFixture.file, com.intellij.openapi.actionSystem.DataContext.EMPTY_CONTEXT)
//    }


    fun testMultipleOccurrences() = checkByFile {
        val rustLocalVariableHandler = RustLocalVariableHandler()
        openFileInEditor("multiple_occurrences.rs")
        val expr = findExpr(myFixture.file, myFixture.editor?.caretModel?.offset ?: 0)?.parent
        val occurrences = findOccurrences(expr!!)
        rustLocalVariableHandler.replaceElementForAllExpr(myFixture.project, myFixture.editor, occurrences)
    }

    fun testExpression() = checkByFile {
        val rustLocalVariableHandler = RustLocalVariableHandler()
        openFileInEditor("expression.rs")
        val expr = findExpr(myFixture.file, myFixture.editor?.caretModel?.offset ?: 0)
        rustLocalVariableHandler.replaceElementForAllExpr(myFixture.project, myFixture.editor, listOf(expr!!))
    }

    fun testStatement() = checkByFile {
        val rustLocalVariableHandler = RustLocalVariableHandler()
        openFileInEditor("statement.rs")
        val expr = findExpr(myFixture.file, myFixture.editor?.caretModel?.offset ?: 0)
        val exprs = possibleExpressions(expr!!)
        rustLocalVariableHandler.replaceElement(myFixture.project, myFixture.editor, listOf(exprs[2]))
    }

    fun testMatch() = checkByFile {
        val rustLocalVariableHandler = RustLocalVariableHandler()
        openFileInEditor("match.rs")
        val expr = findExpr(myFixture.file, myFixture.editor?.caretModel?.offset ?: 0)
        rustLocalVariableHandler.replaceElement(myFixture.project, myFixture.editor, listOf(expr!!))
    }

    fun testFile() = checkByFile {
        val rustLocalVariableHandler = RustLocalVariableHandler()
        openFileInEditor("file.rs")
        val expr = findExpr(myFixture.file, myFixture.editor?.caretModel?.offset ?: 0)
        rustLocalVariableHandler.replaceElement(myFixture.project, myFixture.editor, listOf((possibleExpressions(expr!!)[1])))
    }

    fun testRefMut() = checkByFile {
        val rustLocalVariableHandler = RustLocalVariableHandler()
        openFileInEditor("ref_mut.rs")
        val expr = findExpr(myFixture.file, myFixture.editor?.caretModel?.offset ?: 0)
        rustLocalVariableHandler.replaceElement(myFixture.project, myFixture.editor, listOf((possibleExpressions(expr!!)[0])))
    }
}
