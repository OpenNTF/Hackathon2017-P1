package org.openntf.bookmarksplus.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.darwino.commons.Platform;
import com.darwino.commons.json.JsonObject;
import com.darwino.commons.log.Logger;
import com.darwino.commons.services.AbstractHttpService;
import com.darwino.commons.services.HttpServiceContext;
import com.darwino.commons.util.StringUtil;
import com.darwino.ibm.watson.LanguageTranslationFactory;
import com.ibm.watson.developer_cloud.http.ServiceCall;
import com.ibm.watson.developer_cloud.language_translation.v2.LanguageTranslation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.Language;
import com.ibm.watson.developer_cloud.language_translation.v2.model.Translation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.TranslationResult;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TranslationService extends AbstractHttpService {
	
	private static final Logger log = Platform.logService().getLogMgr(TranslationService.class.getPackage().getName());
	static {
		log.setLogLevel(Logger.LOG_WARN_LEVEL);
	}
	
	public static final String PARAM_URL = "url";
	public static final String PARAM_LANG = "lang";
	public static final String PARAM_TYPE = "type";
	public static final String PARAM_DIRECT = "direct";
	public static final String TYPE_HTML = "html";
	
	public static final String PROP_URL = "url";
	public static final String PROP_RESULT = "result";
	
	@Override
	protected void doGet(HttpServiceContext context) throws Exception {
		if(log.isWarnEnabled()) {
			log.warn("{0}: New request", getClass().getSimpleName());
		}
		
		String url = context.getQueryParameterString(PARAM_URL);
		if(StringUtil.isEmpty(url)) {
			throw new IllegalArgumentException("url cannot be empty");
		}
		String langParam = context.getQueryParameterString(PARAM_LANG);
		if(StringUtil.isEmpty(langParam)) {
			throw new IllegalArgumentException("lang cannot be empty");
		}
		Language lang = Language.valueOf(langParam.toUpperCase());
		
		if(log.isWarnEnabled()) {
			log.warn("{0}: Handling request for url='{1}'", getClass().getSimpleName(), url);
		}
		
		JsonObject result = new JsonObject();
		result.put(PROP_URL, url);
		
		String translated = null;
		String type = context.getQueryParameterString(PARAM_TYPE);
		if(StringUtil.equals(type, TYPE_HTML)) {
			translated = getTranslatedHTML(url, lang);
		} else {
			translated = getTranslatedText(url, lang);
		}
		
		result.put(PROP_RESULT, translated);
		
		String direct = context.getQueryParameterString(PARAM_DIRECT);
		if("true".equals(direct)) {
			context.emitHtml(translated);
		} else {
			context.emitJson(result);
		}
	}
	
	private String getTranslatedText(String url, Language lang) throws IOException {
		if(log.isTraceEnabled()) {
			log.trace("{0}: Going to get translated HTML for URL '{1}', lang '{2}'", getClass().getSimpleName(), url, lang);
		}
		
		LanguageTranslationFactory fac = Platform.getManagedBean(LanguageTranslationFactory.BEAN_TYPE);
		LanguageTranslation translator = fac.createLanguageTranslation();
		
		OkHttpClient http = new OkHttpClient();
		Request req = new Request.Builder()
				.url(url)
				.build();
		Response res = http.newCall(req).execute();

		Language sourceLang = Language.ENGLISH;
		
		// Look for a language header
		String headerLang = res.header("Content-Language");
		if(log.isDebugEnabled()) {
			log.debug("{0}: Found Content-Language header: {1}", getClass().getName(), headerLang);
		}
		if(StringUtil.isNotEmpty(headerLang)) {
			sourceLang = getLangForName(headerLang);
		}
		
		String html = res.body().string();
		org.jsoup.nodes.Document doc = Jsoup.parse(html);
		
		// Check for an HTML lang
		String htmlLang = doc.select("html").first().attr("lang");
		if(log.isDebugEnabled()) {
			log.debug("{0}: Found HTML lang: {1}", getClass().getName(), htmlLang);
		}
		if(StringUtil.isNotEmpty(htmlLang)) {
			sourceLang = getLangForName(htmlLang);
		}
		
		List<String> text = Arrays.asList(doc.body().text());
		ServiceCall<TranslationResult> promise = translator.translate(text, sourceLang, lang);
		TranslationResult trans = promise.execute();
		
		return trans.getFirstTranslation();
	}
	
	private String getTranslatedHTML(String url, Language lang) throws IOException {
		if(log.isTraceEnabled()) {
			log.trace("{0}: Going to get translated HTML for URL '{1}', lang '{2}'", getClass().getSimpleName(), url, lang);
		}
		
		LanguageTranslationFactory fac = Platform.getManagedBean(LanguageTranslationFactory.BEAN_TYPE);
		LanguageTranslation translator = fac.createLanguageTranslation();
		
		OkHttpClient http = new OkHttpClient();
		Request req = new Request.Builder()
				.url(url)
				.build();
		Response res = http.newCall(req).execute();

		Language sourceLang = Language.ENGLISH;
		
		// Look for a language header
		String headerLang = res.header("Content-Language");
		if(log.isDebugEnabled()) {
			log.debug("{0}: Found Content-Language header: {1}", getClass().getName(), headerLang);
		}
		if(StringUtil.isNotEmpty(headerLang)) {
			sourceLang = getLangForName(headerLang);
		}
		
		String html = res.body().string();
		org.jsoup.nodes.Document doc = Jsoup.parse(html);
		
		// Check for an HTML lang
		String htmlLang = doc.select("html").first().attr("lang");
		if(log.isDebugEnabled()) {
			log.debug("{0}: Found HTML lang: {1}", getClass().getName(), htmlLang);
		}
		if(StringUtil.isNotEmpty(htmlLang)) {
			sourceLang = getLangForName(htmlLang);
		}
		
		// TODO break apart and re-join HTML
		
		// Build a Map of textual nodes
		Map<String, Set<TextNode>> textMap = new LinkedHashMap<>();
		
		searchForText(doc, textMap);
		
		List<String> allText = new ArrayList<>(textMap.keySet());
		System.out.println("Source size " + allText.size() + ": " + allText);
		List<String> allTranslated = new ArrayList<>(allText.size());
		

		ServiceCall<TranslationResult> promise = translator.translate(allText, sourceLang, lang);
		TranslationResult trans = promise.execute();
		List<Translation> translations = trans.getTranslations();
		for(Translation translation : translations) {
			allTranslated.add(translation.getTranslation());
		}
		
		// Do 20 strings at a time
//		for(int i = 0; i < allText.size(); i += 20) {
//			int lastIndex = allText.size() > i + 20 ? i + 20 : allText.size();
//			System.out.println("Going to build a block from index " + i + " to index " + lastIndex);
//			List<String> block = allText.subList(i, lastIndex);
//
//			ServiceCall<TranslationResult> promise = translator.translate(block, sourceLang, lang);
//			TranslationResult trans = promise.execute();
//			List<Translation> translations = trans.getTranslations();
//			System.out.println("Got result size " + translations.size());
//			
//			for(Translation translation : translations) {
//				allTranslated.add(translation.getTranslation());
//			}
//		}
//		System.out.println("Translated size " + allTranslated.size() + ": " + allTranslated);
		
		// Try to add a base href
		Elements baseElements = doc.getElementsByTag("base");
		Element base = null;
		if(baseElements.isEmpty()) {
			base = doc.head().appendElement("base");
		} else {
			base = baseElements.first();
		}
		String href = base.attr("href");
		if(StringUtil.isEmpty(href)) {
			base.attr("href", url);
		}
		
		
		// Now rebuild the result
		for(int i = 0; i < allText.size(); i++) {
			String from = allText.get(i);
			String to = allTranslated.get(i);
			
			if(log.isDebugEnabled()) {
				log.debug("Translated '{0}' to '{1}'", from, to);
			}
			
			for(TextNode textNode : textMap.get(from)) {
				textNode.text(to);
			}
		}
		
		return doc.html();
	}
	
	private static void searchForText(Element node, Map<String, Set<TextNode>> textMap) {
		for(TextNode textNode : node.textNodes()) {
			String text = textNode.text();
			if(StringUtil.isEmpty(text) || (text != null && Pattern.matches("^[\\s| ]+$", text))) {
				continue;
			}
			if(!textMap.containsKey(text)) {
				textMap.put(text, new HashSet<TextNode>());
				textMap.get(text).add(textNode);
			}
		}
		for(Element child : node.children()) {
			searchForText(child, textMap);
		}
	}
	
	private Language getLangForName(String name) {
		if(StringUtil.isEmpty(name)) {
			return Language.ENGLISH;
		}
		if(name.startsWith("ar")) {
			return Language.ARABIC;
		} else if(name.startsWith("fr")) {
			return Language.FRENCH;
		} else if(name.startsWith("es")) {
			return Language.SPANISH;
		} else if(name.startsWith("pt")) {
			return Language.PORTUGUESE;
		} else if(name.startsWith("it")) {
			return Language.ITALIAN;
		} else {
			return Language.ENGLISH;
		}
	}
}
