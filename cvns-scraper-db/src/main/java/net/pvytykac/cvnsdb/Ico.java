package net.pvytykac.cvnsdb;

/**
 * @author Paly
 * @since 2018-04-01
 */
public class Ico {

    private final Long id;
    private final String ico;

    public Ico(Long id, String ico) {
        this.id = id;
        this.ico = ico;
    }

    public Long getId() {
        return id;
    }

    public String getIco() {
        return ico;
    }
}
