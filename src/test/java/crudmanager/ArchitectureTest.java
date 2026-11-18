package crudmanager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final Path ROOT = projectRoot();
    private static final Path MAIN = ROOT.resolve("src/main/java/crudmanager");

    private static Path projectRoot() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("pom.xml"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("pom.xml não encontrado a partir de " + System.getProperty("user.dir"));
        }
        return dir;
    }

    private static List<Path> javaFiles(Path dir) {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(".java")).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("ARQ-01 pacotes das camadas existem e contêm classes")
    void packagesExist() {
        for (String pkg : List.of("model", "exception", "dao", "dao/jdbc", "service", "service/impl", "security",
                "controller", "config")) {
            Path dir = MAIN.resolve(pkg);
            assertTrue(Files.isDirectory(dir), "pacote ausente: " + pkg);
            assertFalse(javaFiles(dir).isEmpty(), "pacote sem classes: " + pkg);
        }
    }

    @Test
    @DisplayName("ARQ-02 model, exception, dao (interfaces), service e security não importam jakarta.servlet, java.sql nem javax.sql")
    void businessLayersAreFrameworkFree() {
        List<Path> files = new ArrayList<>();
        for (String pkg : List.of("model", "exception", "service", "security")) {
            files.addAll(javaFiles(MAIN.resolve(pkg)));
        }
        try (Stream<Path> s = Files.list(MAIN.resolve("dao"))) {
            s.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertFalse(files.isEmpty(), "a varredura não encontrou fontes");
        List<String> violations = new ArrayList<>();
        for (Path f : files) {
            for (String line : read(f).split("\n")) {
                String t = line.trim();
                if (t.startsWith("import ") && (t.contains(" jakarta.servlet") || t.contains(" java.sql")
                        || t.contains(" javax.sql") || t.contains(" static jakarta.servlet")
                        || t.contains(" static java.sql") || t.contains(" static javax.sql"))) {
                    violations.add(MAIN.relativize(f) + ": " + t);
                }
            }
        }
        assertTrue(violations.isEmpty(), "imports proibidos: " + violations);
    }

    @Test
    @DisplayName("ARQ-05 controller não importa java.sql nem o pacote dao")
    void controllerHasNoPersistence() {
        List<Path> files = javaFiles(MAIN.resolve("controller"));
        assertFalse(files.isEmpty(), "a varredura não encontrou fontes");
        List<String> violations = new ArrayList<>();
        for (Path f : files) {
            for (String line : read(f).split("\n")) {
                String t = line.trim();
                if (t.startsWith("import ") && (t.contains("java.sql") || t.contains("javax.sql")
                        || t.contains("crudmanager.dao"))) {
                    violations.add(f.getFileName() + ": " + t);
                }
            }
        }
        assertTrue(violations.isEmpty(), "imports proibidos: " + violations);
    }

    @Test
    @DisplayName("ARQ-07 service não depende de controller nem da implementação JDBC")
    void serviceDependsOnlyOnAbstractions() {
        List<Path> files = javaFiles(MAIN.resolve("service"));
        assertFalse(files.isEmpty(), "a varredura não encontrou fontes");
        List<String> violations = new ArrayList<>();
        for (Path f : files) {
            for (String line : read(f).split("\n")) {
                String t = line.trim();
                if (t.startsWith("import ") && (t.contains("crudmanager.controller")
                        || t.contains("crudmanager.dao.jdbc") || t.contains("crudmanager.config"))) {
                    violations.add(MAIN.relativize(f) + ": " + t);
                }
            }
        }
        assertTrue(violations.isEmpty(), "imports proibidos: " + violations);
    }

    @Test
    @DisplayName("DOCK-07 pom.xml usa o driver PostgreSQL e não há MySQL no pom nem nos fontes")
    void postgresOnly() {
        String pom = read(ROOT.resolve("pom.xml"));
        assertTrue(pom.contains("<groupId>org.postgresql</groupId>")
                && pom.contains("<artifactId>postgresql</artifactId>"), "dependência org.postgresql ausente");
        assertFalse(pom.toLowerCase().contains("mysql"), "pom.xml menciona mysql");
        List<Path> files = javaFiles(ROOT.resolve("src/main/java"));
        assertFalse(files.isEmpty(), "a varredura não encontrou fontes");
        for (Path f : files) {
            assertFalse(read(f).toLowerCase().contains("mysql"), "mysql em " + ROOT.relativize(f));
        }
    }
}
