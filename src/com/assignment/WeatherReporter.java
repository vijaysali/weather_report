package com.assignment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.io.SAXReader;

/**
 * Servlet implementation class WeatherReport
 * this servlet will read input from the user  and fetches the results from RSS feed of yahoo
 */
public class WeatherReporter extends HttpServlet {
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 * @param zipcode : WOEID should be given as input to the method
	 * @return: This method will returns XML as response from the yahoo api
	 * 
	 */

	public InputStream retrieve(String zipcode) throws Exception {
		String url = "http://weather.yahooapis.com/forecastrss?w=" + zipcode;
		URLConnection conn = new URL(url).openConnection();
		return conn.getInputStream();
	}
	/**
	 * 
	 * @return : This method will create SAXReader object and returns it to calling function
	 */
	public SAXReader createXmlReader() {
		Map<String, String> uris = new HashMap<String, String>();
		uris.put("y", "http://xml.weather.yahoo.com/ns/rss/1.0");

		DocumentFactory factory = new DocumentFactory();
		factory.setXPathNamespaceURIs(uris);

		SAXReader xmlReader = new SAXReader();
		xmlReader.setDocumentFactory(factory);
		return xmlReader;
	}
	/**
	 * 
	 * @param inputStream :This parameter contains XML output , which contains weather information
	 * @return : This method will return formatted HTML output which servlet will send it to browser
	 * 
	 */

	public String parse(InputStream inputStream) throws Exception {
		SAXReader xmlReader = createXmlReader();
		Document doc = xmlReader.read(inputStream);
		String res = "<table><tr>";
		res = res + "<td><b>City:</b></td><td style='color:white;'>"
				+ doc.valueOf("/rss/channel/y:location/@city")
				+ "</td>";
		res = res + "<td><b>Region:</b></td><td style='color:white;'>" + doc.valueOf("/rss/channel/y:location/@region")
				+ "</td>";
		res = res + "<td><b>Country:</b></td><td style='color:white;'>" + doc.valueOf("/rss/channel/y:location/@country")
				+ "</td></tr>";
		res = res + "<tr><td><b>Conditions:</b></td><td style='color:white;'>" + doc.valueOf("/rss/channel/item/y:condition/@text")
				+ "</td></tr>";
		res = res + "<tr><td><b>Temp:</b></td><td style='color:white;'>" + doc.valueOf("/rss/channel/item/y:condition/@temp")
				+ "F</td>";
		res = res + "<td><b>Wind:</b></td><td style='color:white;'>" + doc.valueOf("/rss/channel/y:wind/@chill")
				+ "</td>";
		res = res + "<td><b>Humidity:</b></td><td style='color:white;'>" + doc.valueOf("/rss/channel/y:atmosphere/@humidity")
				+ "</td>";
		res = res + "</tr></table><img src='"+doc.valueOf("/rss/channel/image/url")+"'/><div id='weburl'>"
				+ doc.valueOf("/rss/channel/item/description") + "</div>";
		return res;
	}
	/**
	 * 
	 * @param url : url object
	 * @return : this method will return the string which contains xml results which are given by url.openconnection method
	 *           [This method will read the contents of URL
	 */
	public static String readUrl(URL url) {
		StringBuffer sb = new StringBuffer();
		try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {

		}
		return sb.toString();
	}

	/**
	 * 
	 * @param str : This parameter is result of readUrl(URL url) method
	 * @param open : this is the <woeid> 
	 * @param close: this is the </woeid>
	 * @return : this method will gives you the WOEID of particular place
	 */
	public static String substringBetween(String str, String open, String close) {
		if (str == null || open == null || close == null) {
			return null;
		}
		int start = str.indexOf(open);
		if (start != -1) {
			int end = str.indexOf(close, start + open.length());
			if (end != -1) {
				return str.substring(start + open.length(), end);
			}
		}
		return null;
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		WeatherReporter tobj = new WeatherReporter();
		InputStream dataIn = null;
		//Reading input from the html page
		String cities = request.getParameter("txtinput");
		/**
		 *  The below lines of code fetches the WOEID from yahoo service.
		 *  Here the contents of URL is read from readUrl() method , 
		 *  which establishes connection and gives XML result in string
		 *  substringBetween(str,start,end) method will extract specified 
		 *  contents which we are sending as parameters to method.
		 */
		String url = "http://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20geo.places%20where%20text%3D%22";
		url += cities + "%22";
		String str = readUrl(new URL(url));
		String inputs = substringBetween(str, "<woeid>", "</woeid>");
		String lat=substringBetween(str,"<latitude>","</latitude>");
		String lon=substringBetween(str,"<longitude>","</longitude>");
		
		//If the values returns null user will gets below message 
		if(inputs==null)
		{
			out.println("No such cities found, Make sure that you enter proper city name.");
		}
		else{
		try {
			/*
			 * retrive() method will take parameter which is WOEID values and gives details information
			 * about the particular city in XML format
			 */
			dataIn = tobj.retrieve(inputs);
			/*
			 * parse() method will parser the XML contents which are passed as argument to method.
			 * It gives HTML formatted result that can be displayed to the browsers. 
			 */
			String results = parse(dataIn);
			//latlong variable contains the HTML tags (hidden tags) which needs to be used in google maps 
			String latlongres="<input type='hidden' id='lat' value='"+lat+"'/>";
			latlongres=latlongres+"<input type='hidden' id='lon' value='"+lon+"'/>";
			//sends results back to browser
			out.print(results+latlongres);
		} catch (Exception e) {
			e.printStackTrace();
		}
		}
	}
}
