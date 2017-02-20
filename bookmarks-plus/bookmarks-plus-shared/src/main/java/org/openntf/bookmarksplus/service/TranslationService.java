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
import org.openntf.bookmarksplus.app.AppDatabaseDef;
import org.openntf.bookmarksplus.app.AppManifest;

import com.darwino.commons.Platform;
import com.darwino.commons.json.JsonException;
import com.darwino.commons.json.JsonObject;
import com.darwino.commons.log.Logger;
import com.darwino.commons.services.AbstractHttpService;
import com.darwino.commons.services.HttpServiceContext;
import com.darwino.commons.util.StringUtil;
import com.darwino.ibm.watson.LanguageTranslationFactory;
import com.darwino.jsonstore.Database;
import com.darwino.jsonstore.Document;
import com.darwino.jsonstore.Session;
import com.darwino.jsonstore.Store;
import com.darwino.platform.DarwinoContext;
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
		
		String type = context.getQueryParameterString(PARAM_TYPE);
		String translated = getOrCacheTranslation(url, lang, type);
		
		result.put(PROP_RESULT, translated);
		
		String direct = context.getQueryParameterString(PARAM_DIRECT);
		if("true".equals(direct)) {
			if(TYPE_HTML.equals(type)) {
				context.emitHtml(translated);
			} else {
				context.emitText(translated);
			}
		} else {
			context.emitJson(result);
		}
	}
	
	private String getOrCacheTranslation(String url, Language lang, String type) throws IOException, JsonException {
		DarwinoContext darwinoContext = DarwinoContext.get();
		Session session = darwinoContext.getSession();
		Database database = session.getDatabase(AppDatabaseDef.DATABASE_NAME);
		Store store = database.getStore(Database.STORE_DEFAULT);
		String key = url + lang + type;
		Document doc = null;
		if(!store.documentExists(key)) {
			if(log.isWarnEnabled()) {
				log.warn("{0}: key '{1}' not cached; fetching new", getClass().getSimpleName(), key);
			}
			
			doc = store.newDocument(key);
			
			String translated = null;
			if(StringUtil.equals(type, TYPE_HTML)) {
				translated = getTranslatedHTML(url, lang);
			} else {
				translated = getTranslatedText(url, lang);
			}
			doc.set(PROP_RESULT, translated);
			doc.save();
		} else {
			doc = store.loadDocument(key);
		}
		return doc.getString(PROP_RESULT);
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
		
		String bodyText = doc.body().text();
		StringBuilder result = new StringBuilder();
		// Break it up into 40K chunks
		for(int i = 0; i < bodyText.length(); i += 40 * 1024) {
			int chunkEnd = bodyText.length() > i+40*1024 ? i+40*1024 : bodyText.length()-1;
			String substr = bodyText.substring(i, chunkEnd);
			ServiceCall<TranslationResult> promise = translator.translate(substr, sourceLang, lang);
			TranslationResult trans = promise.execute();
			result.append(trans.getFirstTranslation());
		}
		
		return result.toString();
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
		
		// Do only up to 40K at a time (fudged for overhead)
		int queueSize = 0;
		List<String> textQueue = new ArrayList<>();
		for(String textEntry : allText) {
			queueSize += textEntry.getBytes().length;
			if(queueSize < 40 * 1024) {
				textQueue.add(textEntry);
			} else {
				// Flush the queue
				ServiceCall<TranslationResult> promise = translator.translate(textQueue, sourceLang, lang);
				TranslationResult trans = promise.execute();
				List<Translation> translations = trans.getTranslations();
				for(Translation translation : translations) {
					allTranslated.add(translation.getTranslation());
				}
				
				queueSize = 0;
				textQueue.clear();
			}
		}
		// If there's anything in the queue, flush that
		ServiceCall<TranslationResult> promise = translator.translate(textQueue, sourceLang, lang);
		TranslationResult trans = promise.execute();
		List<Translation> translations = trans.getTranslations();
		for(Translation translation : translations) {
			allTranslated.add(translation.getTranslation());
		}
		
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
			String to = "";
			if(allTranslated.size() > i) {
				to = allTranslated.get(i);
			}
			
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
