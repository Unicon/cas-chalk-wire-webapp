package net.unicon.cas.chalkwire.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

class ChalkWireResponseParser {

	private static final Log	logger		= LogFactory.getLog(ChalkWireResponseParser.class);

	private int					errorCode;
	private boolean				isSuccess	= Boolean.FALSE;

	private String				message		= null;
	private String				messageKey	= null;
	private String				token		= null;
	private String				url			= null;

	private String openURL(final String url) throws IOException {
		StringBuffer response = null;

		HttpURLConnection httpConnection = null;
		InputStream input = null;

		try {
			if (!url.startsWith("http"))
				throw new MalformedURLException("getURL(): URL \"" + url + "\" does not use HTTPS.");

			final URL u = new URL(url);
			httpConnection = (HttpURLConnection) u.openConnection();

			httpConnection.setUseCaches(false);
			httpConnection.setDoOutput(true);
			httpConnection.setRequestMethod("GET");

			httpConnection.connect();
			final int responseCode = httpConnection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				input = httpConnection.getInputStream();

				response = new StringBuffer();
				Reader reader = new InputStreamReader(input, "UTF-8");
				reader = new BufferedReader(reader);
				final char[] buffer = new char[1024];
				for (int n = 0; n >= 0;) {
					n = reader.read(buffer, 0, buffer.length);
					if (n > 0)
						response.append(buffer, 0, n);
				}

			}
		} finally {
			if (input != null)
				input.close();

			if (httpConnection != null)
				httpConnection.disconnect();

		}
		if (response != null)
			return response.toString();

		if (logger.isDebugEnabled())
			logger.debug("Server response did not return anything");
		return null;
	}

	private Document parseXml(final String xml) throws IOException {
		try {
			final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			final Document doc = docBuilder.parse(new InputSource(new StringReader(xml)));
			return doc;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public ChalkWireResponseParser(final String urlAddr) throws IOException {

		final String content = openURL(urlAddr);

		if (content == null || content.equals(""))
			message = "No content returned from: " + urlAddr;
		else {

			if (content.startsWith("<response>")) {

				if (logger.isDebugEnabled())
					logger.debug("Xml response received from chalk and wire:" + content);

				final Document doc = parseXml(content);

				NodeList list = doc.getElementsByTagName("errorCode");
				if (list.getLength() == 0)
					list = doc.getElementsByTagName("errorcode");

				Node nd = null;

				if (list.getLength() > 0) {
					nd = list.item(0);
					if (nd != null)
						this.errorCode = Integer.parseInt(nd.getTextContent());
				}

				list = doc.getElementsByTagName("returncode");
				if (list.getLength() == 0)
					list = doc.getElementsByTagName("returnCode");

				if (list.getLength() > 0) {
					nd = list.item(0);
					this.isSuccess = nd.getTextContent().trim().equalsIgnoreCase("true");
				}

				list = doc.getElementsByTagName("message");
				if (list.getLength() > 0) {
					nd = list.item(0);
					if (nd != null)
						this.message = nd.getTextContent().trim();
				}

				list = doc.getElementsByTagName("token");
				if (list.getLength() > 0) {
					nd = list.item(0);
					if (nd != null)
						this.token = nd.getTextContent().trim();
				}

				list = doc.getElementsByTagName("url");
				if (list.getLength() > 0) {
					nd = list.item(0);
					if (nd != null)
						this.url = nd.getTextContent().trim();
				}

				list = doc.getElementsByTagName("messageKey");
				if (list.getLength() > 0) {
					nd = list.item(0);
					if (nd != null)
						this.messageKey = nd.getTextContent().trim();
				}
			} else {
				if (logger.isDebugEnabled())
					logger.debug("Invalid xml doc. Assuming webpage was return for successful login");
				this.isSuccess = true;
			}

		}

	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getMessage() {
		return getMessageKey() + " - " + message;
	}

	private String getMessageKey() {
		return messageKey;
	}

	public String getToken() {
		return token;
	}

	public String getURL() {
		return url;
	}

	public boolean isSuccess() {
		return isSuccess;
	}
}
