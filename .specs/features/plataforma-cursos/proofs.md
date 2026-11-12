# plataforma-cursos proofs

**HEAD**: 192ab90
**Tree**: clean
**Shell**: /bin/sh
**Result**: 69 pass, 0 fail

| Check | Proof | Result |
| --- | --- | --- |
| C1 | `run all / ✓ AUTH-01` | pass |
| C2 | `run smoke / ✓ AUTH-02` | pass |
| C3 | `run all / ✓ AUTH-03` | pass |
| C4 | `run all / ✓ AUTH-04` | pass |
| C5 | `run all / ✓ AUTH-05` | pass |
| C6 | `run smoke / ✓ AUTH-06` | pass |
| C7 | `run smoke / ✓ AUTH-07` | pass |
| C8 | `run all / ✓ AUTH-08` | pass |
| C9 | `run all / ✓ AUTH-09` | pass |
| C10 | `run all / ✓ AUTH-10` | pass |
| C11 | `run all / ✓ AUTH-11` | pass |
| C12 | `run all / ✓ AUTH-12` | pass |
| C13 | `run all / ✓ AUTH-13` | pass |
| C14 | `run smoke / ✓ CURSO-01` | pass |
| C15 | `run smoke / ✓ CURSO-02` | pass |
| C16 | `run all / ✓ CURSO-03` | pass |
| C17 | `run smoke / ✓ CURSO-04` | pass |
| C18 | `run all / ✓ CURSO-05` | pass |
| C19 | `run all / ✓ CURSO-06` | pass |
| C20 | `run all / ✓ CURSO-07` | pass |
| C21 | `run all / ✓ CURSO-08` | pass |
| C22 | `run all / ✓ CURSO-09` | pass |
| C23 | `run all / ✓ CURSO-10` | pass |
| C24 | `run smoke / ✓ CURSO-11` | pass |
| C25 | `run all / ✓ CURSO-12` | pass |
| C26 | `run smoke / ✓ MATR-01` | pass |
| C27 | `run smoke / ✓ MATR-02` | pass |
| C28 | `run all / ✓ MATR-03` | pass |
| C29 | `run smoke / ✓ MATR-04` | pass |
| C30 | `run all / ✓ MATR-05` | pass |
| C31 | `run all / ✓ MATR-06` | pass |
| C32 | `run all / ✓ MATR-07` | pass |
| C33 | `run all / ✓ MATR-08` | pass |
| C34 | `run all / ✓ MATR-09` | pass |
| C35 | `run all / ✓ MATR-10` | pass |
| C36 | `run smoke / ✓ MATR-11` | pass |
| C37 | `run all / ✓ MATR-12` | pass |
| C38 | `run all / ✓ PAG-01` | pass |
| C39 | `run all / ✓ PAG-02` | pass |
| C40 | `run smoke / ✓ PAG-03` | pass |
| C41 | `run smoke / ✓ PAG-04` | pass |
| C42 | `run all / ✓ PAG-05` | pass |
| C43 | `run all / ✓ PAG-06` | pass |
| C44 | `run all / ✓ PAG-07` | pass |
| C45 | `run smoke / ✓ PAG-08` | pass |
| C46 | `run smoke / ✓ DOCK-01` | pass |
| C47 | `run smoke / ✓ DOCK-02` | pass |
| C48 | `run all / ✓ DOCK-03` | pass |
| C49 | `run smoke / ✓ DOCK-04` | pass |
| C50 | `run smoke / ✓ DOCK-05` | pass |
| C51 | `run smoke / ✓ DOCK-06` | pass |
| C52 | `run all / ✓ DOCK-07` | pass |
| C53 | `run all / ✓ DB-01` | pass |
| C54 | `run smoke / ✓ NAV-01` | pass |
| C55 | `run smoke / ✓ NAV-02` | pass |
| C56 | `run smoke / ✓ NAV-03` | pass |
| C57 | `run all / ✓ ARQ-01` | pass |
| C58 | `run all / ✓ ARQ-02` | pass |
| C59 | `run all / ✓ ARQ-03` | pass |
| C60 | `run all / ✓ ARQ-04` | pass |
| C61 | `run all / ✓ ARQ-05` | pass |
| C62 | `run smoke / ✓ ARQ-06` | pass |
| C63 | `run all / ✓ EDGE-1` | pass |
| C64 | `run all / ✓ EDGE-2` | pass |
| C65 | `run all / ✓ EDGE-3` | pass |
| C66 | `run all / ✓ EDGE-4` | pass |
| C67 | `run all / ✓ EDGE-5` | pass |
| Build | `mvn -q -DskipTests package` | pass |
| Build | `docker compose config -q` | pass |

## Output

`Run all` - exit 0

```
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85)
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122)
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385)
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162)
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507)
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495)

SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
```

`Run smoke` - exit 0

```
✓ MATR-01
✓ MATR-02
✓ MATR-04
✓ ARQ-06
✓ PAG-01
✓ PAG-03
✓ PAG-04
✓ PAG-05
✓ PAG-06
✓ PAG-07
✓ PAG-08
✓ DOCK-05
✓ DOCK-06
✓ MATR-11
✓ CURSO-11
```
