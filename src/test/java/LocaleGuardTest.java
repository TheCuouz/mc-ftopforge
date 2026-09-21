import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El candado del punto decimal.
 *
 * <p>Un {@code String.format} sin Locale usa el idioma DE LA MAQUINA: el mismo jar
 * escribe "1,234,567" en un servidor ingles y "1.234.567" en uno espanol, y el /ftop
 * es una lista de dinero. Los plugins hablan ingles entero, asi que el numero tiene
 * que salir siempre igual. Se destapo el 21/9/2026 leyendo la barra de accion que
 * recibe el cliente: HudForge pintaba "tps 20,0".
 *
 * <p>Se arregla poniendo {@code java.util.Locale.ROOT} como primer argumento.
 * Escrito en Java 8 a proposito: este plugin es de 1.8.8 y compila con release 8.
 */
class LocaleGuardTest {

    @Test
    @DisplayName("ningun numero que ve el jugador depende del idioma del sistema")
    void numbersDoNotFollowTheSystemLocale() throws IOException {
        List<String> culpables = new ArrayList<String>();
        recorre(new File("src/main/java"), culpables);
        assertTrue(culpables.isEmpty(),
            "String.format sin Locale (usa java.util.Locale.ROOT) en: " + culpables);
    }

    private void recorre(File dir, List<String> culpables) throws IOException {
        File[] hijos = dir.listFiles();
        if (hijos == null) return;
        for (File f : hijos) {
            if (f.isDirectory()) {
                recorre(f, culpables);
            } else if (f.getName().endsWith(".java")) {
                String codigo = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                if (codigo.contains("String.format(\"")) culpables.add(f.getPath());
            }
        }
    }
}
