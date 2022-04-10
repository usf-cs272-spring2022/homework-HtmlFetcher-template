package edu.usfca.cs272;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/**
 * Tests the {@link HtmlFetcher} class.
 *
 * @author CS 272 Software Development (University of San Francisco)
 * @version Spring 2022
 */
@TestMethodOrder(MethodName.class)
public class HtmlFetcherTest {
	// ███████╗████████╗ ██████╗ ██████╗
	// ██╔════╝╚══██╔══╝██╔═══██╗██╔══██╗
	// ███████╗   ██║   ██║   ██║██████╔╝
	// ╚════██║   ██║   ██║   ██║██╔═══╝
	// ███████║   ██║   ╚██████╔╝██║
	// ╚══════╝   ╚═╝    ╚═════╝ ╚═╝

	/*
	 * TODO ...and read this! Please do not spam web servers by rapidly re-running
	 * all of these tests over and over again. You risk being blocked by the web
	 * server if you make making too many requests in too short of a time period!
	 *
	 * Focus on one test or one group of tests at a time instead. If you do that,
	 * you will not have anything to worry about!
	 */

	/**
	 * Tests the {@link HtmlFetcher#isHtml(Map)} method.
	 *
	 * @see HtmlFetcher#isHtml(Map)
	 */
	@Nested
	@TestMethodOrder(OrderAnnotation.class)
	public class A_HtmlTypeTests {
		/**
		 * Tests the {@link HtmlFetcher#isHtml(Map)} method for URLs that do not
		 * point to valid HTML webpages.
		 *
		 * @param link the link to test
		 * @throws IOException from {@link URL#openConnection()}
		 *
		 * @see HtmlFetcher#isHtml(Map)
		 */
		@ParameterizedTest
		@ValueSource(strings = {
				"input/simple/no_extension",
				"input/simple/double_extension.html.txt",
				"input/style.css",
				"input/guten/1661-h/images/cover.jpg" })
		@Order(1)
		public void testNotHtml(String link) throws IOException {
			URL base = new URL(REMOTE);
			URL url = new URL(base, link);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				Map<String, List<String>> headers = getHeaders(url);
				Supplier<String> debug = () -> "\nURL:\n" + url + "\n\nHeaders:\n" + headers + "\n\n";
				Assertions.assertFalse(HtmlFetcher.isHtml(headers), debug);
			});
		}

		/**
		 * Tests the {@link HtmlFetcher#isHtml(Map)} method for URLs that do point
		 * to valid HTML webpages.
		 *
		 * @param link the link to test
		 * @throws IOException from {@link URL#openConnection()}
		 *
		 * @see HtmlFetcher#isHtml(Map)
		 */
		@ParameterizedTest
		@ValueSource(strings = {
				"input/simple/",
				"input/simple/empty.html",
				"input/birds/falcon.html",
				"input/simple/wrong_extension.html#file=hello.jpg",
				"404.html"})
		@Order(2)
		public void testIsHtml(String link) throws IOException {
			URL base = new URL(REMOTE);
			URL url = new URL(base, link);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				Map<String, List<String>> headers = getHeaders(url);
				Supplier<String> debug = () -> "\nURL:\n" + url + "\n\nHeaders:\n" + headers + "\n\n";
				Assertions.assertTrue(HtmlFetcher.isHtml(headers), debug);
			});
		}
	}

	/**
	 * Tests the status code methods.
	 *
	 * @see HtmlFetcher#getStatusCode(Map)
	 */
	@Nested
	@TestMethodOrder(OrderAnnotation.class)
	public class B_StatusCodeTests {
		/**
		 * Tests that the status code is 200.
		 *
		 * @param link the link to fetch
		 * @throws IOException from {@link #testStatusCode(String, String, int)}
		 * @see HtmlFetcher#getStatusCode(Map)
		 */
		@ParameterizedTest
		@ValueSource(strings = {
				"input/simple/no_extension",
				"input/simple/double_extension.html.txt",
				"input/birds/yellowthroat.html" })
		@Order(1)
		public void test200(String link) throws IOException {
			testStatusCode(REMOTE, link, 200);
		}

		/**
		 * Tests that the status code is 404.
		 *
		 * @throws IOException from {@link #testStatusCode(String, String, int)}
		 * @see HtmlFetcher#getStatusCode(Map)
		 */
		@Test
		@Order(2)
		public void test404() throws IOException {
			String link = "redirect/nowhere";
			testStatusCode(STARGATE, link, 404);
		}

		/**
		 * Tests that the status code is 410.
		 *
		 * @throws IOException from {@link #testStatusCode(String, String, int)}
		 * @see HtmlFetcher#getStatusCode(Map)
		 */
		@Test
		@Order(3)
		public void test410() throws IOException {
			String link = "redirect/gone";
			testStatusCode(STARGATE, link, 410);
		}
	}

	/**
	 * Tests the redirect status code methods.
	 *
	 * @see HtmlFetcher#isRedirect(Map)
	 */
	@Nested
	@TestMethodOrder(OrderAnnotation.class)
	public class C_RedirectCodeTests {
		/**
		 * Tests that the status code is a redirect.
		 *
		 * @param link the link to fetch
		 * @throws IOException if unable to get headers
		 *
		 * @see HtmlFetcher#isRedirect(Map)
		 */
		@ParameterizedTest
		@ValueSource(strings = {
				"redirect/loop1",
				"redirect/loop2",
				"redirect/one",
				"redirect/two" })
		@Order(4)
		public void testRedirect(String link) throws IOException {
			URL base = new URL(STARGATE);
			URL url = new URL(base, link);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				Map<String, List<String>> headers = getHeaders(url);
				Supplier<String> debug = () -> "\nURL:\n" + url + "\n\nHeaders:\n" + headers + "\n\n";
				Assertions.assertTrue(HtmlFetcher.isRedirect(headers), debug);
			});
		}

		/**
		 * Tests that the status code is not a redirect.
		 *
		 * @param link the link to fetch
		 * @throws IOException if unable to get headers
		 *
		 * @see HtmlFetcher#isRedirect(Map)
		 */
		@ParameterizedTest
		@ValueSource(strings = {
				"simple/no_extension",
				"redirect/nowhere",
				"redirect/gone" })
		@Order(5)
		public void testNotRedirect(String link) throws IOException {
			URL base = new URL(STARGATE);
			URL url = new URL(base, link);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				Map<String, List<String>> headers = getHeaders(url);
				Supplier<String> debug = () -> "\nURL:\n" + url + "\n\nHeaders:\n" + headers + "\n\n";
				Assertions.assertFalse(HtmlFetcher.isRedirect(headers), debug);
			});
		}
	}

	/**
	 * Tests fetching HTML for troublesome links.
	 *
	 * @see HtmlFetcher#fetch(String)
	 * @see HtmlFetcher#fetch(URL)
	 */
	@Nested
	@TestMethodOrder(OrderAnnotation.class)
	public class D_FetchHtmlTests {
		/**
		 * Test that attempting to fetch pages that do not have valid HTML results
		 * in a null value.
		 *
		 * @param link the link to fetch
		 * @throws MalformedURLException if unable to create URLs
		 */
		@ParameterizedTest
		@ValueSource(strings = {
				"simple/no_extension",
				"simple/double_extension.html.txt",
				"nowhere" })
		@Order(1)
		public void testNotValidHtml(String link) throws MalformedURLException {
			URL base = new URL(REMOTE);
			URL url = new URL(base, link);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				String html = HtmlFetcher.fetch(url);
				Supplier<String> debug = () -> {
					var headers = getHeaders(url);
					return "\nURL:\n" + url + "\n\nHeaders:\n" + headers + "\n\n";
				};
				Assertions.assertNull(html, debug);
			});
		}

		/**
		 * Tests the HTML returned for a valid page.
		 *
		 * @throws IOException if unable to read html file
		 */
		@Test
		@Order(2)
		public void testHtmlYellow() throws IOException {
			String link = "input/birds/yellowthroat.html";
			URL base = new URL(REMOTE);
			URL url = new URL(base, link);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				String html = HtmlFetcher.fetch(url);
				testSameHtml(url, "yellowthroat.html", html);
			});
		}

		/**
		 * Tests the HTML returned for a valid page.
		 *
		 * @throws IOException if unable to read html file
		 */
		@Test
		@Order(3)
		public void testHtmlJava() throws IOException {
			String link = "docs/api/allclasses-index.html";
			URL base = new URL(REMOTE);
			URL url = new URL(base, link);

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				String html = HtmlFetcher.fetch(url);
				testSameHtml(url, "allclasses-index.html", html);
			});
		}
	}

	/**
	 * Tests fetching HTML for redirects.
	 *
	 * @see HtmlFetcher#fetch(String, int)
	 * @see HtmlFetcher#fetch(URL, int)
	 */
	@Nested
	@TestMethodOrder(OrderAnnotation.class)
	public class E_FetchRedirectTests {

		/**
		 * Tests that null is returned when a link does not resolve within a
		 * specific number of redirects.
		 *
		 * @param redirects the number of redirects to try
		 * @throws MalformedURLException if unable to create URLs
		 */
		@ParameterizedTest
		@ValueSource(ints = { -1, 0, 1, 2 })
		@Order(1)
		public void testUnsuccessfulRedirect(int redirects) throws MalformedURLException {
			URL base = new URL(STARGATE);
			URL url = new URL(base, "redirect/one");

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				String html = HtmlFetcher.fetch(url, redirects);
				Supplier<String> debug = () -> {
					var headers = getHeaders(url);
					return "\nURL:\n" + url + "\n\nHeaders:\n" + headers + "\n\n";
				};
				Assertions.assertNull(html, debug);
			});
		}

		/**
		 * Tests that proper HTML is returned when a link DOES resolve within a
		 * specific number of redirects.
		 *
		 * @param redirects the number of redirects to try
		 * @throws MalformedURLException if unable to create URLs
		 * @throws IOException if unable to read html file
		 */
		@ParameterizedTest
		@ValueSource(ints = { 3, 4 })
		@Order(2)
		public void testSuccessfulRedirect(int redirects) throws MalformedURLException, IOException {
			URL base = new URL(STARGATE);
			URL url = new URL(base, "redirect/one");

			Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
				String html = HtmlFetcher.fetch(url, redirects);
				testSameHtml(url, "hello.html", html);
			});
		}
	}

	/**
	 * Tests that certain classes or packages do not appear in the implementation
	 * code. Attempts to fool this test will be considered cheating.
	 */
	@Tag("approach")
	@Nested
	public class F_ApproachTests {
		/**
		 * Tests that certain classes or packages do not appear in the
		 * implementation code. Attempts to fool this test will be considered
		 * cheating.
		 *
		 * @throws IOException if unable to read source code
		 */
		@Test
		public void testClasses() throws IOException {
			Path base = Path.of("src", "main", "java", "edu", "usfca", "cs272");
			Path path = base.resolve(HtmlFetcher.class.getSimpleName() + ".java");
			String source = Files.readString(path, UTF_8);

			Assertions.assertAll(
					() -> Assertions.assertFalse(source.contains("import java.net.*;"),
							"Modify your code to use more specific import statements."),
					() -> Assertions.assertFalse(
							source.contains("import java.net.URLConnection;"),
							"You may not use this class."),
					() -> Assertions.assertFalse(
							source.contains("import java.net.HttpURLConnection;"),
							"You may not use this class."));
		}

		/**
		 * Causes this group of tests to fail if the other non-approach tests are
		 * not yet passing.
		 */
		@Test
		public void testOthersPassing() {
			var request = LauncherDiscoveryRequestBuilder.request()
					.selectors(DiscoverySelectors.selectClass(HtmlFetcherTest.class))
					.filters(TagFilter.excludeTags("approach")).build();

			var launcher = LauncherFactory.create();
			var listener = new SummaryGeneratingListener();

			Logger logger = Logger.getLogger("org.junit.platform.launcher");
			logger.setLevel(Level.SEVERE);

			launcher.registerTestExecutionListeners(listener);
			launcher.execute(request);

			Assertions.assertEquals(0, listener.getSummary().getTotalFailureCount(),
					"Must pass other tests to earn credit for approach group!");
		}
	}

	/**
	 * Tests if the status code returned is as expected.
	 *
	 * @param absolute the base URL in absolute form
	 * @param relative the URL to fetch from the base in relative form
	 * @param code the expected status code
	 * @throws IOException from {@link URL#openConnection()}
	 *
	 * @see HtmlFetcher#getStatusCode(Map)
	 */
	public static void testStatusCode(String absolute, String relative, int code) throws IOException {
		URL base = new URL(absolute);
		URL url = new URL(base, relative);

		Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
			Map<String, List<String>> headers = getHeaders(url);
			int actual = HtmlFetcher.getStatusCode(headers);
			Supplier<String> debug = () -> "\nURL:\n" + url + "\n\nHeaders:\n" + headers + "\n\n";
			Assertions.assertEquals(code, actual, debug);
		});
	}

	/**
	 * Tests whether the HTML file given by the name matches the fetched HTML.
	 * Standardizes how newlines are handled to avoid issues with different
	 * operating systems.
	 *
	 * @param url the url that was fetched
	 * @param name the filename to use for expected output
	 * @param html the actual html returned after cleaning
	 * @throws IOException if unable to read the expected file
	 */
	public static void testSameHtml(URL url, String name, String html) throws IOException {
		Path file = Path.of("src", "test", "resources", name);
		String expected = String.join(System.lineSeparator(), Files.readAllLines(file, UTF_8)).stripTrailing();
		String actual = html.lines().collect(Collectors.joining(System.lineSeparator())).stripTrailing();
		Supplier<String> debug = () -> "\nURL:\n" + url + "\n\nExpected:\n" + file + "\n\n";
		Assertions.assertEquals(expected, actual, debug);
	}

	/**
	 * Gets just the headers from a URL using {@link URL#openConnection()} and
	 * {@link URLConnection#getHeaderFields()}, and attempts to close the created
	 * URL connection. Will cause an assertion error if fails for any reason;
	 *
	 * <p>
	 * Using the built-in {@link URL#openConnection()} method is not allowed for
	 * homework or project implementations.
	 *
	 * @param url the url to fetch headers for
	 * @return the headers returned for the url
	 * @throws AssertionError if unable to get headers successfully
	 */
	public static Map<String, List<String>> getHeaders(URL url) throws AssertionError {
		Map<String, List<String>> headers = null;
		URLConnection connection = null;

		try {
			HttpURLConnection.setFollowRedirects(false);
			connection = url.openConnection();
			headers = connection.getHeaderFields();
		}
		catch(Exception e) {
			Assertions.fail("Unable to get header fields from: " + url.toString(), e);
		}
		finally {
			try {
				// attempt to close input stream
				connection.getInputStream().close();
			}
			catch (NullPointerException | IOException ignored) {
				// sometimes the stream is already closed (e.g. 404 status code)
				// ignore failure to close in those cases
			}
		}

		return headers;
	}

	/** How long to wait for individual tests to complete. */
	public static final Duration TIMEOUT = Duration.ofSeconds(45);

	/** Primary location of the remote tests. */
	public static final String REMOTE = "https://usf-cs272-spring2022.github.io/project-web/";

	/** Location of remote tests hosted by stargate. */
	public static final String STARGATE = "https://www.cs.usfca.edu/~cs272/";
}
