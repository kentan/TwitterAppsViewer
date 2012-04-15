package com.twitterClientGetter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import twitter4j.IDs;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

@SuppressWarnings("serial")
public class TwitterAppsViewer extends HttpServlet {
	private static String SESSION_REQUEST_TOKEN = "reqestToken";
	private static String SESSION_ACCESS_TOKEN = "accessToken";
	private static String QUERY_MODE = "mode";
	private static String SESSION_AUTH_VERIFIER = "oauth_verifier";
	private static String QUERY_OAUTH = "oauth";
	private static String QUERY_CALLBACK = "callback";

	private static Logger logger;
	static  {
		logger = Logger.getLogger("TwitterAppsViewer");
		logger.setLevel(Level.INFO);
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {


		String queryString = req.getParameter(QUERY_MODE);

		if (QUERY_CALLBACK.equals(queryString)) {
			doCallBack(req, resp);
		} else if (QUERY_OAUTH.equals(queryString)) {
			doOAuth(req, resp);
		} else if (queryString == null || "".equals(queryString)){
			doOAuth(req, resp);
		} else {
			doUnderConstruction(req, resp);
		}

	}

	private void doOAuth(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		Twitter twitter = (new TwitterFactory()).getInstance();

		try {

			RequestToken reqToken = twitter
			.getOAuthRequestToken(req.getRequestURL() + "?" + QUERY_MODE + "=" + QUERY_CALLBACK);
			HttpSession session = req.getSession();
			session.setAttribute(SESSION_REQUEST_TOKEN, reqToken);
			String strUrl = reqToken.getAuthorizationURL();
			strUrl = reqToken.getAuthenticationURL();
			resp.sendRedirect(strUrl);
		} catch (TwitterException e) {
			Logger logger = Logger.getLogger(this.getClass().getName());
			logger.warning(e.getMessage());
		}

	}

	private void doCallBack(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		HttpSession session = req.getSession();
		AccessToken accessToken = null;
		accessToken = (AccessToken)session.getAttribute(SESSION_ACCESS_TOKEN);
		
		// in case of already haveing access token
		if(accessToken != null){
			doWriteHTML(req, resp);
		}else{
			Twitter twitter = (new TwitterFactory()).getInstance();
	
			String verifier = req.getParameter(SESSION_AUTH_VERIFIER);
	
			try {
				accessToken = twitter.getOAuthAccessToken((RequestToken) session
						.getAttribute(SESSION_REQUEST_TOKEN), verifier);
			} catch (TwitterException e) {
				Logger logger = Logger.getLogger(this.getClass().getName());
				logger.warning(e.getMessage());
			}
	
			if (accessToken != null) {
				session.setAttribute(SESSION_ACCESS_TOKEN, accessToken);
				session.removeAttribute(SESSION_REQUEST_TOKEN);
				doWriteHTML(req, resp);
	
			} else {
				resp.setContentType("text/plain");
			}
		}
	}

	private void doWriteHTML(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {


		resp.setContentType("text/html");
		resp.setLocale(Locale.JAPAN);
		resp.setCharacterEncoding("UTF-8");


		writeHtmlBeginning(resp.getWriter());
		writeHtmlHeader(resp.getWriter());
		writeBodyBeginning(resp.getWriter());
		writeBodyPageBeginning(resp.getWriter());
		writeBodyContentHeader(resp.getWriter());
		writeBodyContentBeginning(resp.getWriter());

		// composing HTML from the information from Twitter API  
		writeBodyContentList(req,resp);
	
		
		writeBodyContentEnding(resp.getWriter());
		writeBodyPageEnding(resp.getWriter());
		writeBodyEnding(resp.getWriter());
		writeHtmlEnding(resp.getWriter());

	}
	private void writeBodyContentList(HttpServletRequest req, HttpServletResponse resp)throws IOException
	{
		HttpSession session = req.getSession();
		Twitter twitter = TwitterFactory.getSingleton();

		AccessToken token = (AccessToken) session
				.getAttribute(SESSION_ACCESS_TOKEN);
		if (token == null) {
			logger.warning("access token missing!");
		}

		twitter.setOAuthAccessToken(token);
		try {
			IDs ids = twitter.getFriendsIDs(-1);
			long[] friendIds = ids.getIDs();
			ResponseList<User> userList = twitter.lookupUsers(friendIds);

			for (User user : userList) {
				String name = user.getName();
				URL imageURL = user.getProfileImageURL();

				resp.getWriter().print("<li>");
				resp.getWriter().print("<img src='http://");
				resp.getWriter().print(imageURL.getHost());
				resp.getWriter().print(imageURL.getPath());
				resp.getWriter().print("'/>");

				resp.getWriter().print(name);

				String source = user.getStatus().getSource();
				// composing a <a> tag, since when the source is "web", API return <a> tag.
				if ("web".equals(source)) {
					source = "<a href = http://twitter.com>web</a>";
				}
				resp.getWriter().println(source);
				resp.getWriter().println("</li>");
			}

		} catch (TwitterException e) {
			resp.getWriter().println(e);
		}
	}

	private void doUnderConstruction(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		resp.getWriter().println("under construction");
	}

	private static  String HTML_BEGINNING_TEXT = "<html>";

	private void writeHtmlBeginning(PrintWriter out) {
		out.print(HTML_BEGINNING_TEXT);
	}

	private static  String HTML_ENDING_TEXT = "</html>";

	private void writeHtmlEnding(PrintWriter out) {
		out.print(HTML_ENDING_TEXT);
	}

	private static  String HTML_HEADER_TEXT = "<head>"
			+ "<meta charset='utf-8'>"
			+ "<meta name='viewport' content='width=device-width,initial-scale=1'>"
			+ "<title>Twitter Apps Viewer</title> "
			+ "<link rel='stylesheet' href='http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.css' />"
			+ "<script src='http://code.jquery.com/jquery-1.6.4.min.js'></script>"
			+ "<script src='http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.js'></script>"
			+ "</head> ";

	private void writeHtmlHeader(PrintWriter out) {
		out.print(HTML_HEADER_TEXT);
	}

	private static  String BODY_BEGINNING_TEXT = "<body>";

	private void writeBodyBeginning(PrintWriter out) {
		out.print(BODY_BEGINNING_TEXT);
	}

	private static  String BODY_ENDING_TEXT = "</body>";

	private void writeBodyEnding(PrintWriter out) {
		out.print(BODY_ENDING_TEXT);
	}

	private static  String BODY_CONTENT_HEADER_TEXT = "<div data-role='header' data-theme='f'>"
			+ "<h1>Twitter Apps Viewer</h1>"
			+ "<a href='../../' data-icon='home' data-iconpos='notext' data-direction='reverse' class='ui-btn-right jqm-home'>Home</a>"
			+ "</div>";

	private void writeBodyContentHeader(PrintWriter out) {
		out.print(BODY_CONTENT_HEADER_TEXT);
	}

	private static  String CONTENT_BEGINNING_TEXT = "<div data-role='content'>"
			+ "<ul data-role='listview' data-inset='true'>";

	private void writeBodyContentBeginning(PrintWriter out) {
		out.print(CONTENT_BEGINNING_TEXT);
	}

	private static  String CONTENT_ENDING_TEXT = "</ul></div>";

	private void writeBodyContentEnding(PrintWriter out) {
		out.print(CONTENT_ENDING_TEXT);
	}

	private static  String PAGE_BEGINNING_TEXT = "<div data-role='page' class='type-index'>";

	private void writeBodyPageBeginning(PrintWriter out) {
		out.print(PAGE_BEGINNING_TEXT);
	}

	private static  String PAGE_ENDING_TEXT = "</div>";

	private void writeBodyPageEnding(PrintWriter out) {
		out.print(PAGE_ENDING_TEXT);
	}

}
