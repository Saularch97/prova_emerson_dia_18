package crudmanager.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageTest {

    @Test
    @DisplayName("PAG-08 página vazia (0 itens) não tem páginas, anterior nem próxima")
    void emptyPage() {
        Page<String> p = Page.of(List.of(), 1, 10, 0);
        assertEquals(0, p.getTotalPages());
        assertFalse(p.hasPrevious());
        assertFalse(p.hasNext());
        assertTrue(p.getItems().isEmpty());
    }

    @Test
    @DisplayName("PAG-08 uma única página (10 itens) não tem anterior nem próxima")
    void singlePage() {
        Page<Integer> p = Page.of(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 1, 10, 10);
        assertEquals(1, p.getTotalPages());
        assertFalse(p.hasPrevious());
        assertFalse(p.hasNext());
    }

    @Test
    @DisplayName("PAG-04 primeira de várias páginas: anterior desabilitada, próxima habilitada")
    void firstOfMany() {
        Page<Integer> p = Page.of(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 1, 10, 25);
        assertEquals(3, p.getTotalPages());
        assertFalse(p.hasPrevious());
        assertTrue(p.hasNext());
    }

    @Test
    @DisplayName("PAG-03 página do meio tem anterior e próxima")
    void middleOfMany() {
        Page<Integer> p = Page.of(List.of(11, 12), 2, 10, 25);
        assertTrue(p.hasPrevious());
        assertTrue(p.hasNext());
        assertEquals(2, p.getPage());
        assertEquals(10, p.getPageSize());
        assertEquals(25, p.getTotalItems());
    }

    @Test
    @DisplayName("PAG-04 última página parcial (25 itens, página 3): próximo desabilitado")
    void lastPartialPage() {
        Page<Integer> p = Page.of(List.of(21, 22, 23, 24, 25), 3, 10, 25);
        assertEquals(3, p.getTotalPages());
        assertTrue(p.hasPrevious());
        assertFalse(p.hasNext());
        assertEquals(5, p.getItems().size());
    }

    @Test
    @DisplayName("PAG-03 totalPages arredonda para cima (11 itens = 2 páginas, 20 itens = 2 páginas)")
    void totalPagesRoundsUp() {
        assertEquals(2, Page.of(List.of(), 1, 10, 11).getTotalPages());
        assertEquals(2, Page.of(List.of(), 1, 10, 20).getTotalPages());
    }

    @Test
    @DisplayName("PAG-01 Page é imutável: a lista de itens não aceita alteração nem reflete a original")
    void immutable() {
        List<String> source = new ArrayList<>(List.of("a"));
        Page<String> p = Page.of(source, 1, 10, 1);
        source.add("b");
        assertEquals(List.of("a"), p.getItems());
        assertThrows(UnsupportedOperationException.class, () -> p.getItems().add("c"));
    }
}
