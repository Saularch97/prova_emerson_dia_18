package crudmanager.exception;

/** Registro com dados relacionados (violação de chave estrangeira). */
public class RelatedDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RelatedDataException(String message) {
        super(message);
    }
}
