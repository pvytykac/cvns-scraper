package net.pvytykac.cvnscommons;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;

/**
 * @author Paly
 * @since 2018-04-02
 */

public class CsvPrinter implements Closeable {

    private final CSVPrinter printer;

    public CsvPrinter(File file) {
        try {
            Appendable appendable = new FileWriter(file);
            this.printer = new CSVPrinter(appendable, CSVFormat.RFC4180);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public void flush() {
        doIo(printer::flush);
    }

    public void printCell(Object value) {
        doIo(() -> printer.print(value));
    }

    public void printLn() {
        doIo(printer::println);
    }

    public void printRecord(Object... values) {
        doIo(() -> printer.printRecord(values));
    }

    @Override
    public void close() throws IOException {
        printer.close();
    }

    private void doIo(IoOperation consumer) {
        try {
            consumer.doIo();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private interface IoOperation {
        void doIo() throws IOException;
    }
}