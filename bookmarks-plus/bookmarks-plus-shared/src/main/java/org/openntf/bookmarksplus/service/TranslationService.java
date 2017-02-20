package org.openntf.bookmarksplus.service;

import com.darwino.commons.json.JsonObject;
import com.darwino.commons.services.AbstractHttpService;
import com.darwino.commons.services.HttpServiceContext;
import com.darwino.commons.util.StringUtil;

public class TranslationService extends AbstractHttpService {
	public static final String PARAM_URL = "url";
	
	@Override
	protected void doGet(HttpServiceContext context) throws Exception {
		String url = context.getQueryParameterString(PARAM_URL);
		if(StringUtil.isEmpty(url)) {
			throw new IllegalArgumentException("url cannot be empty");
		}
		
		JsonObject result = new JsonObject();
		result.put("url", url);
		
		context.emitJson(result);
	}
}
