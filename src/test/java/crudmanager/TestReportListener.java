package crudmanager;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

/** Imprime "✓ displayName" para cada teste aprovado (AD-003). */
public class TestReportListener implements TestExecutionListener {

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult result) {
        if (id.isTest() && result.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
            System.out.println("✓ " + id.getDisplayName());
        }
    }
}
