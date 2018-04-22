package net.pvytykac.cvnsmvcr;

import com.google.common.collect.ImmutableList;
import net.pvytykac.cvnscommons.CsvPrinter;
import net.pvytykac.cvnsdb.Ico;
import net.pvytykac.cvnsdb.IcoContainer;
import net.pvytykac.cvnshttp.HttpFacade;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Paly
 * @since 2018-04-02
 */
public class Main {

    public static final String HOST = "http://aplikace.mvcr.cz/";

    public static void main(String[] args) throws Exception {
        HttpFacade facade = new HttpFacade();
        IcoContainer icos = new IcoContainer();
        HttpUriRequest searchRequest = RequestBuilder.get(HOST + "seznam-verejnych-sbirek/").build();

        try (CsvPrinter printer = new CsvPrinter(new File("E:\\mcvr.csv"))) {

            printer.printCell("id");
            printer.printCell("ico");
            printer.printCell("Označení sbírky");
            printer.printCell("Územní rozsah sbírky");
            printer.printCell("Doba konání sbírky");
            printer.printCell("Způsob provádění sbírky");
            printer.printCell("Účel(y)");
            printer.printLn();
            printer.flush();

            icos.stream().forEach(ico -> {
                Optional<Document> document = facade.getHtml(ico.getIco(), searchRequest, "utf-8");
                document.ifPresent(doc -> {
                    getDetailsHrefs(ico.getIco(), facade, doc).forEach(detailRequest -> {
                        facade.getHtml(ico.getIco(), detailRequest, "utf-8")
                                .ifPresent(detailDoc -> processDetail(ico, detailDoc, printer));
                    });
                });
            });
        }
    }

    private static Stream<HttpUriRequest> getDetailsHrefs(String ico, HttpFacade facade, Document document) {
        List<NameValuePair> parameters = ImmutableList.of(
            new BasicNameValuePair("__EVENTTARGET", select(document, "input[name='__EVENTTARGET']")),
            new BasicNameValuePair("__EVENTARGUMENT", select(document, "input[name='__EVENTARGUMENT']")),
            new BasicNameValuePair("__VIEWSTATE", select(document, "input[name='__VIEWSTATE']")),
            new BasicNameValuePair("__VIEWSTATEGENERATOR", select(document, "input[name='__VIEWSTATEGENERATOR']")),
            new BasicNameValuePair("__EVENTVALIDATION", select(document, "input[name='__EVENTVALIDATION']")),
            new BasicNameValuePair("ctl00$Application$txtCorpIc", ico),
            new BasicNameValuePair("ctl00$Application$txtCorporation", ""),
            new BasicNameValuePair("ctl00$Application$txtSbirka", ""),
            new BasicNameValuePair("ctl00$Application$txtPurpose", ""),
            new BasicNameValuePair("ctl00$Application$lstKraj", ""),
            new BasicNameValuePair("ctl00$Application$rbtStatus", "proceed"),
            new BasicNameValuePair("ctl00$Application$BtnSearch", "Vyhledat")
        );
        HttpUriRequest request = RequestBuilder.post(HOST + "seznam-verejnych-sbirek/default.aspx")
            .setEntity(EntityBuilder.create()
                    .setParameters(parameters)
                    .build()
            ).build();

        Optional<String> location = facade.getLocation(ico, request);

        if (location.isPresent()) {
            return facade.getHtml(ico, RequestBuilder.get(HOST + location.get()).build(), "utf-8")
                    .map(doc -> doc.select("tr.searchResults a").stream().map(el -> HOST + "seznam-verejnych-sbirek/" + el.attr("href")))
                    .orElse(Stream.empty())
                    .map(uri -> RequestBuilder.get(uri).build());
        } else {
            return Stream.empty();
        }
    }

    private static void processDetail(Ico ico, Document document, CsvPrinter printer) {
        Map<String, String> map = document.select("table#formularTable tr").stream().map(row -> {
            Elements cells = row.getElementsByTag("td");
            String header = cells.get(0).select("span").first().text();
            String value = cells.get(1).select("span").stream()
                    .flatMap(el -> el.getAllElements().stream())
                    .map(el -> {
                        return el.html().replaceAll("<br>", " | ").replaceAll("&nbsp;", " ");
                    })
                    .reduce("", (acc, cur) -> acc + cur + " ");
            return new AbstractMap.SimpleEntry<>(header, value.trim());
        }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        printer.printCell(ico.getId());
        printer.printCell(ico.getIco());
        printer.printCell(map.get("Označení sbírky:"));
        printer.printCell(map.get("Územní rozsah sbírky"));
        printer.printCell(map.get("Doba konání sbírky"));
        printer.printCell(map.get("Způsob provádění sbírky"));
        printer.printCell(map.get("Účel(y)"));
        printer.printLn();
        printer.flush();
    }

    private static String select(Document document, String selector) {
        return Optional.ofNullable(document.select(selector).first())
                .map(el -> el.attr("value")).orElse("");
    }

}
