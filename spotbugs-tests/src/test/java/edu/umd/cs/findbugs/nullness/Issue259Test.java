package edu.umd.cs.findbugs.nullness;

import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/259">#259</a>.
 */
public class Issue259Test {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void test() {
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("UCE_DO_NOT_THROW_UNDECLARED_CHECKED_EXCEPTION").build();
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue259.class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue259$1.class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue259$X.class"));
        assertThat(bugCollection, containsExactly(1, bugTypeMatcher));
    }
}
