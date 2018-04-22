package net.pvytykac.csvnsrzp;

import net.pvytykac.cvnscommons.CsvPrinter;
import net.pvytykac.cvnsdb.IcoContainer;
import net.pvytykac.cvnshttp.HttpFacade;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Paly
 * @since 2018-04-01
 */
public class Main {

    private static final String HOST = "http://www.rzp.cz/cgi-bin/aps_cacheWEB.sh";
    private static final String CHARSET = "ISO-8859-2";

    private static final String SUBJECT_KEY = "Předmět podnikání:";
    private static final String ACTIVITIES_KEY = "Obory činnosti:";
    private static final String TYPE_KEY = "Druh živnosti:";
    private static final String START_KEY = "Vznik oprávnění:";
    private static final String DURATION_KEY = "Doba platnosti oprávnění:";

    public static void main(String[] args) throws Exception {
        IcoContainer icos = new IcoContainer();
        HttpFacade http = new HttpFacade();
        HttpUriRequest searchTemplate = RequestBuilder.get(HOST)
                .addHeader("Accept", " text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                .addHeader("Accept-Encoding", " gzip, deflate")
                .addHeader("Accept-Language", " en-US,en;q=0.9")
                .addHeader("Cache-Control", " no-cache")
                .addHeader("Connection", " keep-alive")
                .addHeader("Host", " www.rzp.cz")
                .addHeader("Pragma", " no-cache")
                .addHeader("Upgrade-Insecure-Requests", " 1")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36")
                .addParameter("VSS_SERV", "ZVWSBJFND")
                .addParameter("Action", "Search")
                .addParameter("PRESVYBER", "0")
                .addParameter("PODLE", "subjekt")
                .addParameter("OBCHJM", "")
                .addParameter("ROLES", "P")
                .addParameter("OKRES", "")
                .addParameter("OBEC", "")
                .addParameter("CASTOBCE", "")
                .addParameter("ULICE", "")
                .addParameter("COR", "")
                .addParameter("COZ", "")
                .addParameter("CDOM", "")
                .addParameter("JMENO", "")
                .addParameter("PRIJMENI", "")
                .addParameter("NAROZENI", "")
                .addParameter("ROLE", "")
                .addParameter("VYPIS", "1")
                .build();

        try (CsvPrinter printer = new CsvPrinter(new File("E:\\rzp.csv"))) {
            printer.printCell("id");
            printer.printCell("ico");
            printer.printCell("zivnost index");
            printer.printCell("predmet podnikani");
            printer.printCell("druh zivnosti");
            printer.printCell("obory cinnosti");
            printer.printCell("vznik opravneni");
            printer.printCell("doba platnosti opravneni");
            printer.printLn();
            printer.flush();

            icos.stream().forEach(identifier -> {
                HttpUriRequest request = RequestBuilder.copy(searchTemplate)
                        .addParameter("ICO", identifier.getIco())
                        .build();

                sleep();
                Optional<Document> document = http.getHtml(request, CHARSET);
                if (document.isPresent()) {
                    Elements el = document.get().select("a.detail1");
                    if (!el.isEmpty()) {
                        AtomicInteger ix = new AtomicInteger();
                        String href = el.first().attr("href");
                        sleep();
                        document = http.getHtml(RequestBuilder.get(HOST + href).build(), CHARSET);
                        if (document.isPresent()) {
                            document.get().select("div.zivnosti ul li.zivnost").forEach(zivnost -> {
                                Elements el2 = zivnost.select("dl > dt, dl > dd");
                                String header = null;
                                List<String> values = new ArrayList<>();
                                String subject = "";
                                List<String> activities = Collections.singletonList("");
                                String type = "";
                                String start = "";
                                String duration = "";

                                for (int i = 0; i < el2.size(); i++) {
                                    Element e = el2.get(i);
                                    if (e.tag().getName().equals("dt")) {
                                        if (header != null) {
                                            switch (header) {
                                                case SUBJECT_KEY:
                                                    subject = values.get(0);
                                                    break;
                                                case ACTIVITIES_KEY:
                                                    activities = values;
                                                    break;
                                                case TYPE_KEY:
                                                    type = values.get(0);
                                                    break;
                                                case START_KEY:
                                                    start = values.get(0);
                                                    break;
                                                case DURATION_KEY:
                                                    duration = values.get(0);
                                                    break;
                                            }
                                            values = new ArrayList<>();
                                        }

                                        header = e.text();
                                    } else if (e.tag().getName().equals("dd")) {
                                        values.add(e.text());
                                    }
                                }

                                int index = ix.incrementAndGet();
                                for (String activity: activities) {
                                    printer.printCell(identifier.getId());
                                    printer.printCell(identifier.getIco());
                                    printer.printCell(index);
                                    printer.printCell(subject);
                                    printer.printCell(type);
                                    printer.printCell(activity);
                                    printer.printCell(start);
                                    printer.printCell(duration);
                                    printer.printLn();
                                }

                                printer.flush();
                            });
                        }
                    }
                }
            });
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(7500L);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
