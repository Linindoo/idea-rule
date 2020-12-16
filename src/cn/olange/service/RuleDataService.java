package cn.olange.service;

import cn.olange.model.AsyncResult;
import cn.olange.model.Config;
import cn.olange.model.Handler;
import cn.olange.setting.RulePersistentConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleDataService {
	public static void filterRule(String keyword, Handler<AsyncResult> handler) {
		Config config = RulePersistentConfig.getInstance().getState();
		JsonArray regExpArray = config.getRegExpArray();
		if (StringUtils.isEmpty(keyword)) {
			handler.handle(new AsyncResult(true, regExpArray));
			return;
		}
		JsonArray result = new JsonArray();
		Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
		for (int i = 0; i < regExpArray.size(); i++) {
			JsonObject rule = regExpArray.get(i).getAsJsonObject();
			String title = rule.get("title").getAsString();
			if (pattern.matcher(title).find()) {
				result.add(rule);
			}
		}
		handler.handle(new AsyncResult(true, result));
	}

	public static JsonArray getRegArray(String content) {
		JsonArray data = new JsonArray();
		if (StringUtils.isNotEmpty(content)) {
			Pattern compile = Pattern.compile("\\{[\r|\n]+([\\d\\D]*?)[\r|\n]+ *?}");
			Matcher matcher = compile.matcher(content);
			Pattern contentPattern = Pattern.compile("title: \'([\\d\\D]*?)\',[\\d\\D]*?rule: ([\\d\\D]*?),[\r|\n]+[\\d\\D]*?examples: \\[([\\d\\D]*?)]");
			while (matcher.find()) {
				String group = matcher.group(1);
				Matcher matcher1 = contentPattern.matcher(group);
				if (matcher1.find()) {
					String title = matcher1.group(1);
					String rule = matcher1.group(2);
					String examples = matcher1.group(3);
					JsonObject ruleObj = new JsonObject();
					ruleObj.addProperty("title", title);
					ruleObj.addProperty("rule", rule);
					ruleObj.addProperty("examples", examples);
					data.add(ruleObj);
				}
			}
		}
		return data;
	}
}
