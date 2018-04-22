package net.pvytykac.cvnsmkcr;

import net.pvytykac.cvnscommons.CsvPrinter;
import net.pvytykac.cvnsdb.IcoContainer;
import net.pvytykac.cvnshttp.HttpFacade;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Paly
 * @since 2018-04-15
 */
public class Main {

    private static final String HOST = "http://www3.mkcr.cz";

    public static void main(String[] args) throws Exception {
        HttpFacade facade = new HttpFacade();
        IcoContainer icos = new IcoContainer(Main.class.getClassLoader().getResourceAsStream("5000-ico-list-nab.csv"));
        HttpUriRequest searchRequest = RequestBuilder.get(HOST + "/cns_internet/CNS/Podminky.aspx").build();

        try (CsvPrinter printer = new CsvPrinter(new File("E:\\mkcr.csv"))) {

            printer.printCell("Název subjektu");
            printer.printCell("IČO");
            printer.printCell("Ulice a číslo");
            printer.printCell("Obec");
            printer.printCell("PSČ");
            printer.printCell("Datum evidence");
            printer.printCell("Předmět obecně prospěšné, podnikatelské a jiné výdělečné činnosti");
            printer.printCell("Zřizovatel");
            printer.printCell("Zrušení evidence");
            printer.printCell("Likvidace");
            printer.printCell("Insolvenční řízení");
            printer.printCell("Zánik");
            printer.printCell("Právní nástupce");
            printer.printLn();
            printer.flush();

            icos.stream().forEach(composite -> {
                String ico = composite.getIco();
                Optional<Document> document = facade.getHtml(ico, searchRequest, "windows-1250");
                document.ifPresent(doc -> {
                    getDetailsHref(ico, facade, doc).ifPresent(detailRequest -> {
                        facade.getHtml(ico, detailRequest, "windows-1250")
                                .ifPresent(detailDoc -> processDetail(detailDoc, printer));
                    });
                });
            });
        }
    }

    private static Optional<HttpUriRequest> getDetailsHref(String ico, HttpFacade facade, Document document) {
        String viewState = document.select("input[name='__VIEWSTATE']").attr("value");
        String viewStateGenerator = document.select("input[name='__VIEWSTATEGENERATOR']").attr("value");
        String eventValidation = document.select("input[name='__EVENTVALIDATION']").attr("value");

        if (StringUtils.isNotBlank(viewState)) {
            HttpUriRequest request = RequestBuilder.post(HOST + "/cns_internet/CNS/Podminky.aspx")
                    .addParameter("tb_nazev", "")
                    .addParameter("tb_ico", ico)
                    .addParameter("tb_prijmeni", "")
                    .addParameter("ddl_obce", "")
                    .addParameter("tb_zrizovatel", "")
                    .addParameter("btn_Vyhledat", "Vyhledat")
                    .addParameter("__VIEWSTATE", viewState)
                    .addParameter("__VIEWSTATEGENERATOR", viewStateGenerator)
                    .addParameter("__EVENTVALIDATION", eventValidation)
                    .build();

            return facade.getLocation(ico, request).map(location -> {
                return facade.getHtml(ico, RequestBuilder.get(HOST + location).build(), "windows-1250").map(doc -> {
                    Elements hrefs = doc.select("table#ASDGridSeznam1 tr.GridCell a");

                    if (!hrefs.isEmpty()) {
                        return RequestBuilder.get(HOST + "/cns_internet/CNS/" + hrefs.first().attr("href")).build();
                    } else {
                        return null;
                    }
                }).orElse(null);
            });
        } else {
            return Optional.empty();
        }
    }

    private static void processDetail(Document document, CsvPrinter printer) {
        Elements elements = document.select("table#Table3 tr td");

        Map<String, String> entries = new HashMap<>();
        String name = null;
        for (int i = 0; i < elements.size(); i++) {
            Element td = elements.get(i);

            if ("Sídlo:".equals(td.text())) {
                i += 1;
                td = elements.get(i);
            } else if ("Statutární orgán:".equals(td.text())) {
                String function = elements.get(++i).text();
                String fullName = elements.get(++i).text();
                String since = elements.get(++i).text().substring(4);
                String dob = elements.get(++i).text();
                entries.put("Statutární orgán", String.format("%s, %s, %s (%s)", function, since, fullName, dob));
                continue;
            }

            if (name == null) {
                if (StringUtils.isNotBlank(td.text())) {
                    name = td.text();
                } else {
                    ++i;
                }
            } else {
                entries.put(name, td.text());
                name = null;
            }
        }

        printer.printCell(entries.getOrDefault("Název:", ""));
        printer.printCell(entries.getOrDefault("IČO:", ""));
        printer.printCell(entries.getOrDefault("Ulice a číslo:", ""));
        printer.printCell(entries.getOrDefault("Obec:", ""));
        printer.printCell(entries.getOrDefault("PSČ:", ""));
        printer.printCell(entries.getOrDefault("Datum evidence:", ""));
        printer.printCell(entries.getOrDefault("Předmět obecně prospěšné, podnikatelské a jiné výdělečné činnosti:", ""));
        printer.printCell(entries.getOrDefault("Zřizovatel:", ""));
        printer.printCell(entries.getOrDefault("Zrušení evidence:", ""));
        printer.printCell(entries.getOrDefault("Likvidace:", ""));
        printer.printCell(entries.getOrDefault("Insolvenční řízení:", ""));
        printer.printCell(entries.getOrDefault("Zánik:", ""));
        printer.printCell(entries.getOrDefault("Právní nástupce:", ""));
        printer.printLn();
        printer.flush();
    }

}
