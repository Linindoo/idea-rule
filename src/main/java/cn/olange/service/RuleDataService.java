package cn.olange.service;

import cn.olange.model.AsyncResult;
import cn.olange.model.Config;
import cn.olange.model.Handler;
import cn.olange.model.RuleModel;
import cn.olange.setting.RulePersistentConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleDataService {
	public static void filterRule(String keyword, Handler<AsyncResult<List<RuleModel>>> handler) {
		Config config = RulePersistentConfig.getInstance().getState();
		List<RuleModel> regExpList = config.getRegExpList();
		if (StringUtils.isEmpty(keyword)) {
			handler.handle(new AsyncResult(true, regExpList));
			return;
		}
		List<RuleModel> matchResult = new ArrayList<>();
		Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
		for (int i = 0; i < regExpList.size(); i++) {
			RuleModel ruleModel = regExpList.get(i);
			String title = ruleModel.getTitle();
			if (pattern.matcher(title).find()) {
				matchResult.add(ruleModel);
			}
		}
		handler.handle(new AsyncResult<>(true, matchResult));
	}

	public static List<RuleModel> getRegArray(String content) {
		List<RuleModel> ruleModels = new ArrayList<>();
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
					RuleModel ruleObj = new RuleModel();
					ruleObj.setTitle(title);
					ruleObj.setRule(rule);
					ruleObj.setExamples(examples);
					ruleObj.setSelfBuild(false);
					ruleModels.add(ruleObj);
				}
			}
		}
		return ruleModels;
	}
}
