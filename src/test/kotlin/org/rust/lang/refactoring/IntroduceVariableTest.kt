package org.rust.lang.refactoring

import org.junit.Ignore
import org.rust.lang.RustTestCaseBase
import org.rust.lang.core.psi.RustExprStmtElement

class IntroduceVariableTest : RustTestCaseBase() {
    override val dataPath = "org/rust/lang/refactoring/fixtures/introduce_variable/"

    @Ignore
    fun testVariable() = checkByFile {
        val rustLocalVariableHandler = RustLocalVariableHandler()
        openFileInEditor("variable.rs")
        rustLocalVariableHandler.invoke(myFixture.project, myFixture.editor, myFixture.file, com.intellij.openapi.actionSystem.DataContext.EMPTY_CONTEXT)
    }

    @Ignore
    fun testExpression() = checkByFile {
            val rustLocalVariableHandler = RustLocalVariableHandler()
            openFileInEditor("expression.rs")
            val expr = rustLocalVariableHandler.findExpr(myFixture.file, myFixture.editor?.caretModel?.offset ?: 0)
            rustLocalVariableHandler.replaceElementForExpr(myFixture.project, expr!!)
    }

    fun testStatement() = checkByFile {
        val rustLocalVariableHandler = RustLocalVariableHandler()
        openFileInEditor("statement.rs")
        val expr = rustLocalVariableHandler.findExpr(myFixture.file, myFixture.editor?.caretModel?.offset ?: 0)
        val statement = possibleExpressions(expr!!).last().parent!! as RustExprStmtElement
        rustLocalVariableHandler.replaceElementForStmt(myFixture.project, statement)
    }
}
