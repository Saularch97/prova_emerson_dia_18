package crudmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.launcher.TestIdentifier;

class TestReportListenerTest {

    private static TestIdentifier test(String displayName) {
        TestDescriptor d = new AbstractTestDescriptor(UniqueId.forEngine("e").append("test", displayName), displayName) {
            @Override
            public Type getType() {
                return Type.TEST;
            }
        };
        return TestIdentifier.from(d);
    }

    private static String capture(TestExecutionResult result) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            new TestReportListener().executionFinished(test("X-01 exemplo"), result);
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("AD-003 listener imprime teste aprovado")
    void printsPassedTest() {
        assertEquals("✓ X-01 exemplo" + System.lineSeparator(), capture(TestExecutionResult.successful()));
    }

    @Test
    @DisplayName("AD-003 listener não imprime teste falho")
    void doesNotPrintFailedTest() {
        assertEquals("", capture(TestExecutionResult.failed(new AssertionError("falhou"))));
    }
}
