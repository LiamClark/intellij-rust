package org.rust.lang.refactoring

import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.rust.lang.RustTestCaseBase
import org.rust.lang.core.psi.impl.RustFile

class RustNameSuggestionsKtTest : RustTestCaseBase() {
    override val dataPath = "org/rust/lang/refactoring/fixtures/introduce_variable/"

    fun testArgumentNames() = doTest("""
        fn foo(a: i32, veryCoolVariableName: i32) {
            a + b
        }

        fn bar() {
            foo(4, 10/*caret*/ + 2)
        }
    """) {
        val refactoring = RustIntroduceVariableRefactoring(myFixture.project, myFixture.editor, myFixture.file as RustFile)
        val expr = refactoring.possibleTargets().first()

        assertThat(expr.nameForArgument()).isEqualTo("veryCoolVariableName")
        assertThat(expr.suggestNames()).containsExactly("name", "variable_name", "cool_variable_name", "very_cool_variable_name")
    }

    fun testNonDirectArgumentNames() = doTest("""
        fn foo(a: i32, veryCoolVariableName: i32) {
            a + b
        }

        fn bar() {
            foo(4, 1/*caret*/0 + 2)
        }
    """) {
        val refactoring = RustIntroduceVariableRefactoring(myFixture.project, myFixture.editor, myFixture.file as RustFile)
        val expr = refactoring.possibleTargets().first()

        assertThat(expr.suggestNames()).containsExactly("i")
    }


    fun testFunctionNames() = doTest("""
        fn foo(a: i32, veryCoolVariableName: i32) -> i32 {
            a + b
        }

        fn bar() {
            f/*caret*/oo(4, 10 + 2)
        }
    """) {
        val refactoring = RustIntroduceVariableRefactoring(myFixture.project, myFixture.editor, myFixture.file as RustFile)
        val expr = refactoring.possibleTargets().first()

        val names = expr.suggestNames()
        assertThat(names).containsExactly("i", "foo")
    }

    fun testStringNew() = doTest("""
        fn read_file() -> Result<String, Error> {
            let file = File::open("res/input.txt")?;

            file.read_to_string(&mut String:/*caret*/:new())?;
    }""") {
        val refactoring = RustIntroduceVariableRefactoring(myFixture.project, myFixture.editor, myFixture.file as RustFile)
        val expr = refactoring.possibleTargets().first()

        assertThat(expr.suggestNames()).containsExactly("string", "new")
    }

    fun testLocalNames() = doTest("""
        fn foo() {
            let a = 5;
            let b = String::new();
            5/*caret*/+ 10;
        }
    """) {
        val refactoring = RustIntroduceVariableRefactoring(myFixture.project, myFixture.editor, myFixture.file as RustFile)
        val expr = refactoring.possibleTargets().first()

        assertThat(findNamesInLocalScope(expr)).containsExactly("a", "b")
    }

    fun testFunctionCallAsArgument() = doTest("""
        fn foo(board_size: i32) {}

        fn bar() {
            foo(Default::de/*caret*/fault());
        }
    """) {
        val refactoring = RustIntroduceVariableRefactoring(myFixture.project, myFixture.editor, myFixture.file as RustFile)
        val expr = refactoring.possibleTargets().first()

        assertThat(expr.suggestNames()).containsExactly("size", "board_size")
    }

    fun testStructLiteral() = doTest("""
        struct Foo {
            bar: i32,
            baz: i32,
        }

        impl Foo {
            fn new() -> Foo {
                Foo{bar: 5, baz: 1/*caret*/0}
            }
        }
        """) {
        val refactoring = RustIntroduceVariableRefactoring(myFixture.project, myFixture.editor, myFixture.file as RustFile)
        val expr = refactoring.possibleTargets().first()

        assertThat(expr.suggestNames()).containsExactly("i", "baz")
    }

    fun testGenericPath() = doTest("""
        struct Foo<T> {
            t: T,
        }

        impl <T> Foo<T> {
            fn new(t: T) -> Foo<T> {
                Foo {t: t}
            }
        }

        fn bar() {
            Foo:/*caret*/:<i32>::new(10)
        }
        """) {
        val refactoring = RustIntroduceVariableRefactoring(myFixture.project, myFixture.editor, myFixture.file as RustFile)
        val expr = refactoring.possibleTargets().first()

        assertThat(expr.suggestNames()).containsExactly("f", "foo", "new")
    }


    private fun doTest(@Language("Rust") before: String, action: () -> Unit) {
        InlineFile(before).withCaret()
        openFileInEditor("main.rs")
        action()
    }
}
