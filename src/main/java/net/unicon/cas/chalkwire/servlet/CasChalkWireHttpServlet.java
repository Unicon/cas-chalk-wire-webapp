/**
Licensed to Jasig under one or more contributor license
agreements. See the NOTICE file distributed with this work
for additional information regarding copyright ownership.
Jasig licenses this file to you under the Apache License,
Version 2.0 (the "License"); you may not use this file
except in compliance with the License. You may obtain a
copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the License for the
specific language governing permissions and limitations
under the License.

*/

package net.unicon.cas.chalkwire.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CasChalkWireHttpServlet extends HttpServlet {

	private static final String	ENCODING_TYPE					= "UTF-8";
	private static final long	serialVersionUID				= -5315331084020083938L;
	private static final String	CHALK_WIRE_PARAM_COURSE_ID		= "course";
	private static final String	CHALK_WIRE_PARAM_TOP_SECTION_ID	= "0";

	private static final Log	logger							= LogFactory.getLog(CasChalkWireHttpServlet.class);

	private Properties			properties;

	public CasChalkWireHttpServlet() throws IOException {
		InputStream stream = null;
		try {
			stream = getClass().getClassLoader().getResourceAsStream("chalkwire.properties");
			properties = new Properties();
			properties.load(stream);
		} finally {
			if (stream != null)
				stream.close();
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		throw new ServletException("POST is not supported by " + getClass().getName());
	}

	private String getChalkWireServerUrl() {
		return properties.getProperty("chalkwire.server.url");
	}

	private String getChalkWireCustomerId() {
		return properties.getProperty("chalkwire.customer.id");
	}

	private String getChalkWireCustomerDomain() {
		return properties.getProperty("chalkwire.customer.domain");
	}

	private String getChalkWireSharedSecretKey() {
		return properties.getProperty("chalkwire.shared.secret.key");
	}

	public static String generateMAC(final String sharedSecret, final String data) throws IOException {
		final Mac mac = setKey(sharedSecret);
		final byte[] signBytes = mac.doFinal(data.getBytes("UTF8"));
		final String signature = encodeBase64(signBytes);
		return signature;
	}

	private static String encodeBase64(final byte[] signBytes) {
		return Base64.encodeBase64URLSafeString(signBytes);
	}

	private static Mac setKey(final String sharedSecret) throws IOException {
		try {
			final Mac mac = Mac.getInstance("HmacSHA1");
			final byte[] keyBytes = sharedSecret.getBytes("UTF8");
			final SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
			mac.init(signingKey);
			return mac;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			if (req.getRemoteUser() == null)
				throw new ServletException("User is not authenticated. Check the CAS client log files for details");

			String userId = req.getUserPrincipal().getName();

			if (logger.isDebugEnabled())
				logger.debug("Received login request from user " + userId);

			final String URL = buildSingleSignOnTokenRequestUrl(userId);

			if (logger.isDebugEnabled()) {
				logger.debug("Requesing security token from ePortfolio Connect Server");
				logger.debug("Requesting url:" + URL);
			}

			/*
			 * Send the single sign-on request url to server and parse the response.
			 */
			ChalkWireResponseParser parser = new ChalkWireResponseParser(URL);

			if (logger.isDebugEnabled())
				logger.debug("Response is success:" + parser.isSuccess());

			if (parser.isSuccess()) {

				if (logger.isDebugEnabled())
					logger.debug("url: " + parser.getURL());

				if (logger.isDebugEnabled())
					logger.debug("token: " + parser.getToken());

				String finalURL = buildFinalSingleSignOnUrl(userId, parser);

				if (logger.isDebugEnabled())
					logger.debug("Single sign-on URL:" + finalURL);

				parser = new ChalkWireResponseParser(finalURL);

				if (!parser.isSuccess())
					throw new ServletException(parser.getMessage());

				resp.sendRedirect(finalURL);

			} else
				throw new ServletException(parser.getMessage());

		} catch (ServletException e) {
			logger.error(e.getMessage(), e);

			String casServerLoginUrl = getServletContext().getInitParameter("casServerLoginUrl");

			String service = req.getRequestURL().toString();
			casServerLoginUrl += "login?renew=true&service=" + URLEncoder.encode(service, ENCODING_TYPE);

			RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/WEB-INF/view/jsp/chalkWireError.jsp");
			req.setAttribute("exception", e);

			if (logger.isDebugEnabled())
				logger.debug("Constructed CAS login url:" + casServerLoginUrl);

			req.setAttribute("loginUrl", casServerLoginUrl);
			dispatcher.forward(req, resp);

		}

	}

	private String buildFinalSingleSignOnUrl(String userId, final ChalkWireResponseParser parser) throws UnsupportedEncodingException {

		StringBuilder bldr = new StringBuilder(parser.getURL());

		bldr.append("?act=single_signon" + "&studentId=");
		bldr.append(URLEncoder.encode(userId, ENCODING_TYPE));
		bldr.append("&tocSecId=");
		bldr.append(URLEncoder.encode(CHALK_WIRE_PARAM_TOP_SECTION_ID, ENCODING_TYPE));
		bldr.append("&courseId=");
		bldr.append(URLEncoder.encode(CHALK_WIRE_PARAM_COURSE_ID, ENCODING_TYPE));
		bldr.append("&cus=");
		bldr.append(URLEncoder.encode(getChalkWireCustomerId(), ENCODING_TYPE));
		bldr.append("&token=");
		bldr.append(parser.getToken());

		return bldr.toString();
	}

	/** 
	 * We need to get a security token from the CWConnect server. We'll make the request
	 * in the form of: customerId/mac/guid/timestamp/studentId/firstname/lastname/email/courseId/tocSectionId
	 */
	private String buildSingleSignOnTokenRequestUrl(String userId) throws IOException {

		final long time = System.currentTimeMillis();
		final String timestamp = String.valueOf(time);

		final String userGUID = UUID.randomUUID().toString();

		final String givenName = userId;
		final String familyName = userId;

		final String getEmailAddress = userId + "@" + getChalkWireCustomerDomain();

		final String s = timestamp + userGUID + getChalkWireSharedSecretKey();
		String mac = generateMAC(getChalkWireSharedSecretKey(), s);

		StringBuilder bldr = new StringBuilder(getChalkWireServerUrl());
		bldr.append("/epcs/sign-on/");
		bldr.append(URLEncoder.encode(getChalkWireCustomerId(), ENCODING_TYPE));
		bldr.append("/");
		bldr.append(URLEncoder.encode(mac, ENCODING_TYPE));
		bldr.append("/");
		bldr.append(URLEncoder.encode(userGUID, ENCODING_TYPE));
		bldr.append("/");
		bldr.append(URLEncoder.encode(timestamp, ENCODING_TYPE));
		bldr.append("/");
		bldr.append(URLEncoder.encode(userId, ENCODING_TYPE));
		bldr.append("/");
		bldr.append(URLEncoder.encode(givenName, ENCODING_TYPE));
		bldr.append("/");
		bldr.append(URLEncoder.encode(familyName, ENCODING_TYPE));
		bldr.append("/");
		bldr.append(URLEncoder.encode(getEmailAddress, ENCODING_TYPE));
		bldr.append("/");
		bldr.append(URLEncoder.encode(CHALK_WIRE_PARAM_COURSE_ID, ENCODING_TYPE));
		bldr.append("/");
		bldr.append(URLEncoder.encode(CHALK_WIRE_PARAM_TOP_SECTION_ID, ENCODING_TYPE));

		return bldr.toString();

	}
}
