package de.tuberlin.cit.project.energy.zabbix.exception;

public class InternalErrorException extends Exception {
    private static final long serialVersionUID = 1L;

    public InternalErrorException() {
        super();
    }

    public InternalErrorException(String message) {
        super(message);
    }
}
