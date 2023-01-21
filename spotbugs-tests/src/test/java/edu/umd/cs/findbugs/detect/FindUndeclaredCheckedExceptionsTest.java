package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class FindUndeclaredCheckedExceptionsTest extends AbstractIntegrationTest {
    @Test
    public void testBadUCE() {
        performAnalysis("ucetestfiles/UndeclaredCheckedExceptions.class", "ucetestfiles/GenericClass.class",
                "ucetestfiles/TestClass.class", "ucetestfiles/Thr.class", "ucetestfiles/GenericClass$1.class");

        assertNumOfUCEBugs(3);

        assertUCEBug("undeclaredThrow", "TestClass");
        assertUCEBug("undeclaredThrow", "TestClass");
        assertUCEBug("test2", "UndeclaredCheckedExceptions");

    }

    @Test
    public void testGoodUCE() {
        performAnalysis("ucetestfiles/GoodCheckedExceptions.class");

        assertNumOfUCEBugs(0);
    }

    private void assertNumOfUCEBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("UCE_DO_NOT_THROW_UNDECLARED_CHECKED_EXCEPTION").build();

        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertUCEBug(String method, String className) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("UCE_DO_NOT_THROW_UNDECLARED_CHECKED_EXCEPTION")
                .inClass(className)
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
