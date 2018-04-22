package net.pvytykac.cvnsdb;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Paly
 * @since 2018-04-01
 */
public class IcoContainer {

    private final Map<Long, String> icos;

    public IcoContainer() {
        this(IcoContainer.class.getClassLoader().getResourceAsStream("ico.csv"));
    }

    public IcoContainer(InputStream in) {
        try {
            CSVFormat format = CSVFormat.MYSQL.withDelimiter(',').withFirstRecordAsHeader();
            CSVParser parser = CSVParser.parse(in, Charset.forName("utf-8"), format);

            icos = parser.getRecords().stream()
                    .collect(Collectors.toMap(row -> Long.valueOf(row.get("org_id")), row -> row.get("ico")));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public Stream<Ico> stream() {
        return icos.entrySet().stream()
                .map(entry -> new Ico(entry.getKey(), entry.getValue()));
    }

}
