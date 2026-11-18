package crudmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaginationTest {

    @Test
    @DisplayName("PAG-01 tamanho de página é 10")
    void pageSizeIsTen() {
        assertEquals(10, Pagination.PAGE_SIZE);
    }

    @Test
    @DisplayName("PAG-02 offset da página N é (N-1)*10")
    void offsetOfPage() {
        assertEquals(0, Pagination.offset(1));
        assertEquals(10, Pagination.offset(2));
        assertEquals(20, Pagination.offset(3));
    }

    @Test
    @DisplayName("PAG-02 parse de ?page=N numérico válido devolve N")
    void parseValid() {
        assertEquals(1, Pagination.parse("1"));
        assertEquals(3, Pagination.parse("3"));
        assertEquals(99, Pagination.parse("99"));
    }

    @Test
    @DisplayName("PAG-05 page ausente (null ou vazio) vira página 1")
    void parseAbsent() {
        assertEquals(1, Pagination.parse(null));
        assertEquals(1, Pagination.parse(""));
        assertEquals(1, Pagination.parse("  "));
    }

    @Test
    @DisplayName("PAG-05 page não numérico vira página 1")
    void parseNotNumeric() {
        assertEquals(1, Pagination.parse("abc"));
        assertEquals(1, Pagination.parse("2.5"));
        assertEquals(1, Pagination.parse("99999999999999999999"));
    }

    @Test
    @DisplayName("PAG-05 page menor que 1 (zero ou negativo) vira página 1")
    void parseBelowOne() {
        assertEquals(1, Pagination.parse("0"));
        assertEquals(1, Pagination.parse("-4"));
    }

    @Test
    @DisplayName("PAG-06 page acima da última vira a última página (25 itens, página 99 = 3)")
    void clampAboveLast() {
        assertEquals(3, Pagination.clamp(99, 25));
        assertEquals(2, Pagination.clamp(3, 20));
    }

    @Test
    @DisplayName("PAG-02 page dentro do intervalo permanece (25 itens, página 3)")
    void clampWithinRange() {
        assertEquals(1, Pagination.clamp(1, 25));
        assertEquals(3, Pagination.clamp(3, 25));
    }

    @Test
    @DisplayName("PAG-06 sem itens a página é 1")
    void clampEmpty() {
        assertEquals(1, Pagination.clamp(5, 0));
    }
}
