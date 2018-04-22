package net.pvytykac.cvnshttp;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Paly
 * @since 2018-04-01
 */
public class HttpFacade {

    private static final Logger LOG = LoggerFactory.getLogger(HttpFacade.class);

    private final HttpClient client;

    public HttpFacade() {
        this.client = HttpClientBuilder.create()
                .setRedirectStrategy(new DefaultRedirectStrategy())
                .setDefaultCookieStore(new BasicCookieStore())
                .build();
    }

    public Optional<String> getLocation(String ico, HttpUriRequest request) {
        try {
            return client.execute(request, response -> {
                logIt(ico, response, request);

                if (response.getStatusLine().getStatusCode() == 302) {
                    return Optional.ofNullable(response.getFirstHeader("Location").getValue());
                }

                return Optional.empty();
            });
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public Optional<Document> getHtml(String ico, HttpUriRequest request, String charset) {
        try {
            return client.execute(request, response -> {
                logIt(ico, response, request);

                if (response.getStatusLine().getStatusCode() == 200) {
                    InputStream in = response.getEntity().getContent();
                    return Optional.of(
                        Jsoup.parse(in, charset, request.getURI().getHost())
                    );
                } else {
                    return Optional.empty();
                }
            });
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void logIt(String ico, HttpResponse response, HttpUriRequest request) {
        LOG.info("{}, {} {}, {} {}", ico, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(),
                request.getMethod(), request.getURI());
    }

    public static void main(String[] args) {
        HttpFacade facade = new HttpFacade();
        Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14).filter(page -> page < 15).forEach(page -> {
            facade.getHtml("", RequestBuilder.get("http://cvns.econ.muni.cz").addParameter("s", String.valueOf(page)).build(), "utf-8")
                .ifPresent(document -> {
                    StringBuilder builder = new StringBuilder();

                    document.select("div#aktuality > *").forEach(el -> {
                        if (el.tag().getName().equalsIgnoreCase("h3")) {
                            System.out.println(builder.toString());
                            builder.delete(0, builder.length());
                            System.out.println("=================================");
                            String[] text = el.text().split("–");
                            String date = text[0].trim();
                            String header = text[1].trim();
                            System.out.println(header);
                            System.out.println(date);
                        } else if (el.tag().getName().equalsIgnoreCase("p")) {
                            builder.append(el.html());
                        }
                    });
                });
        });
    }

    public static void mainProjekty(String[] args) {
        HttpFacade facade = new HttpFacade();

        facade.getHtml("", RequestBuilder.get("http://cvns.econ.muni.cz/projekty").build(), "utf-8")
                .ifPresent(document -> {
                    document.select("div#content h4 a").stream()
                        .map(el -> el.attr("href"))
                        .forEach(detailUri -> {
                            String uri = detailUri.startsWith("http") ? detailUri : "http://cvns.econ.muni.cz" + detailUri;
                            facade.getHtml("", RequestBuilder.get(uri).build(), "utf-8")
                                .ifPresent(detail -> {
                                    String name = detail.select("div#nadpis_pruh").text();
                                    String text = detail.select("div.projekt").text();
                                    System.out.println(name);
                                    System.out.println(text);
                                    System.out.println("===================================");
                                });
                        });
                });
    }

    public static void mainLide(String[] args) {
        String[] names = new String[] {
            "Zuzana Arnerová",
            "Marie Hladká",
            "Vladimír Hyánek",
            "Jiří Navrátil",
            "Jakub Pejcal",
            "Miroslav Pospíšil",
            "Zuzana Prouzová",
            "Simona Škarabelová",
            "Kateřina Almani Tůmová",
            "Marek Vyskočil",
            "Jaroslav Benák",
            "Ondřej Císař",
            "Lukáš Fasora",
            "Milan Hrubeš",
            "Táňa Klementová",
            "Denisa Nečasová",
            "Kateřina Ronovská",
            "Tomáš Rosenmayer",
            "Martin Škop"
        };

        HttpFacade facade = new HttpFacade();

        Arrays.stream(names).forEach(n -> {
            String namePath = n.replaceAll(" ", "-")
                    .replaceAll("á", "a")
                    .replaceAll("í", "i")
                    .replaceAll("ř", "r")
                    .replaceAll("š", "s")
                    .replaceAll("Š", "s")
                    .replaceAll("ů", "u")
                    .replaceAll("č", "c")
                    .replaceAll("ň", "n")
                    .toLowerCase();

            HttpUriRequest request = RequestBuilder.get("http://cvns.econ.muni.cz/lide/" + namePath)
                    .build();

            facade.getHtml("a", request,"utf-8").ifPresent(document -> {
                Element element = document.select("div#content div.clovek").first();

                String name = element.select("div.text h3").text();
                String text = element.select("div.text p").stream().map(Element::text).reduce("", (acc, cur) -> acc + cur + "\r\n");
                String foto = "cvns.econ.muni.cz" + element.select("div.foto img").attr("src");

                System.out.println(name);
                System.out.println(foto);
                System.out.println(text.trim());
                System.out.println("=================");
            });
        });
    }
}
