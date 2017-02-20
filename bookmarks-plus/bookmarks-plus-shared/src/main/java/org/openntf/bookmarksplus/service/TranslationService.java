package org.openntf.bookmarksplus.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;

import com.darwino.commons.Platform;
import com.darwino.commons.json.JsonObject;
import com.darwino.commons.services.AbstractHttpService;
import com.darwino.commons.services.HttpServiceContext;
import com.darwino.commons.util.StringUtil;
import com.darwino.ibm.watson.LanguageTranslationFactory;
import com.ibm.watson.developer_cloud.http.ServiceCall;
import com.ibm.watson.developer_cloud.language_translation.v2.LanguageTranslation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.Language;
import com.ibm.watson.developer_cloud.language_translation.v2.model.TranslationResult;

public class TranslationService extends AbstractHttpService {
	public static final String PARAM_URL = "url";
	public static final String PARAM_LANG = "lang";
	
	public static final String PROP_URL = "url";
	public static final String PROP_RESULT = "result";
	
	@Override
	protected void doGet(HttpServiceContext context) throws Exception {
		String url = context.getQueryParameterString(PARAM_URL);
		if(StringUtil.isEmpty(url)) {
			throw new IllegalArgumentException("url cannot be empty");
		}
		String langParam = context.getQueryParameterString(PARAM_LANG);
		if(StringUtil.isEmpty(langParam)) {
			throw new IllegalArgumentException("lang cannot be empty");
		}
		Language lang = Language.valueOf(langParam.toUpperCase());
		
		JsonObject result = new JsonObject();
		result.put(PROP_URL, url);
		
		LanguageTranslationFactory fac = Platform.getManagedBean(LanguageTranslationFactory.BEAN_TYPE);
		LanguageTranslation translator = fac.createLanguageTranslation();
		
		// TODO look for lang in source URL
		// TODO break apart and re-join HTML
		List<String> text = getStrings(url);
		ServiceCall<TranslationResult> promise = translator.translate(text, Language.ENGLISH, lang);
		TranslationResult trans = promise.execute();
		
		result.put(PROP_RESULT, trans.getFirstTranslation());
		
		
		context.emitJson(result);
	}
	
	private static List<String> getStrings(String url) throws IOException {
		org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
		return Arrays.asList(doc.text());
	}
}
