package cn.olange.utils;

public class RuleUtil {

	public static String convertRule(String rule) {
		if (rule == null) {
			return "";
		}
		int first = rule.indexOf("/");
		first = Math.max(first + 1, 0);
		int lastIndexOf = rule.lastIndexOf("/");
		lastIndexOf = Math.min(lastIndexOf, rule.length() -1);
		return rule.substring(first, lastIndexOf).replace("[\\]", "");
	}
}
