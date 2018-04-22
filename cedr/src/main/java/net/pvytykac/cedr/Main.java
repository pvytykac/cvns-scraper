package net.pvytykac.cedr;

import java.io.File;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;

import net.pvytykac.cvnscommons.CsvPrinter;
import net.pvytykac.cvnsdb.IcoContainer;
import net.pvytykac.cvnshttp.HttpFacade;

public class Main {

	private static final String CHARSET = "utf-8";

	private static final HttpUriRequest SEARCH_FORM_REQUEST = RequestBuilder
			.get("http://cedr.mfcr.cz/Cedr3InternetV419/CommonPages/ConditionPage.aspx?queryType=1")
			.build();

	private static final HttpUriRequest SEARCH_POST_REQUEST_TEMPLATE = RequestBuilder
			.post("http://cedr.mfcr.cz/Cedr3InternetV419/CommonPages/ConditionPage.aspx?queryType=1")
			.addParameter("__EVENTTARGET", "")
			.addParameter("__EVENTARGUMENT", "")
			.addParameter("__LASTFOCUS", "")
			.addParameter("__VIEWSTATE", "")
			.addParameter("__ASDEVENTTARGET", "")
			.addParameter("__ASDEVENTARGUMENT", "")
			.addParameter("ctl00$ContentPlaceHolder1$_nameOfTakerTextBox", "")
			.addParameter("ctl00$ContentPlaceHolder1$_showExtendedResultCheckBox", "on")
			.addParameter("ctl00$ContentPlaceHolder1$_maxRecordsDropDownList", "1000")
			.addParameter("ctl00$ContentPlaceHolder1$_pageRecordsDropDownList", "1000000")
			.addParameter("ctl00$ContentPlaceHolder1$_findButton", "Hledat")
			.build();

	private static final HttpUriRequest TRANSACTION_DETAIL_TEMPLATE = RequestBuilder
			.post()
			.addParameter("__EVENTTARGET", "")
			.addParameter("__EVENTARGUMENT", "")
			.addParameter("__VIEWSTATE", "")
			.addParameter("__ASDEVENTTARGET", "")
			.addParameter("__ASDEVENTARGUMENT", "")
			.addParameter("ctl00$ContentPlaceHolder1$_commonBox1$_showExtendedResultCheckBox", "on")
			.addParameter("ctl00$ContentPlaceHolder1$_gridView$ctl02$_editButton.x", "9")
			.addParameter("ctl00$ContentPlaceHolder1$_gridView$ctl02$_editButton.y", "6")
			.build();

	public static void main(String[] args) throws Exception {
		HttpFacade http = new HttpFacade();
		IcoContainer icos = new IcoContainer();

		try (CsvPrinter printer = new CsvPrinter(new File("E:\\cedr.csv"))) {
			icos.stream().skip(1).findFirst().ifPresent(ico -> {
				http.getHtml(ico.getIco(), SEARCH_FORM_REQUEST, CHARSET).ifPresent(searchForm -> {
					String appUniqueNumber = searchForm.select("input[name='_APPUNIQUENUMBER']").val();
					String eventValidation = searchForm.select("input[name='__EVENTVALIDATION']").val();

					HttpUriRequest post = RequestBuilder.copy(SEARCH_POST_REQUEST_TEMPLATE)
							.addParameter("_APPUNIQUENUMBER", appUniqueNumber)
							.addParameter("__EVENTVALIDATION", eventValidation)
							.addParameter("ctl00$ContentPlaceHolder1$_icTextBox", ico.getIco()).build();

					http.getLocation(ico.getIco(), post).ifPresent(location -> {
						HttpUriRequest get = RequestBuilder.get("http://cedr.mfcr.cz" + location).build();

						http.getHtml(ico.getIco(), get, CHARSET).ifPresent(detail -> {
							String one = detail.select("input[name='_APPUNIQUENUMBER']").val();
							String two = detail.select("input[name='__EVENTVALIDATION']").val();

							detail.select("table#ctl00_ContentPlaceHolder1__gridView tr").stream()
									.skip(1)
									.forEach(row -> {
										row.select("td").stream().skip(1).findFirst().ifPresent(id -> {
											HttpUriRequest findDetailPost = RequestBuilder.copy(TRANSACTION_DETAIL_TEMPLATE)
													.addParameter("_APPUNIQUENUMBER", one)
													.addParameter("__EVENTVALIDATION", two)
													.build();

											http.getLocation(ico.getIco(), findDetailPost).ifPresent(dl -> {
												http.getHtml(ico.getIco(), RequestBuilder.get(dl).build(), CHARSET).ifPresent(info -> {
													info.select("input").stream().findFirst().ifPresent(System.out::println);
												});
											});
										});
									});
						});
					});
				});
			});
		}
	}

}
