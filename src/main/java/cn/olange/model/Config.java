package cn.olange.model;


import java.util.List;

public class Config {
    private String updateUrl ="https://raw.githubusercontent.com/any86/any-rule/v0.3.7/packages/www/src/RULES.js";
    private List<RuleModel> regExpList;

    public String getUpdateUrl() {
        return updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }

    public List<RuleModel> getRegExpList() {
        return regExpList;
    }

    public void setRegExpList(List<RuleModel> regExpList) {
        this.regExpList = regExpList;
    }
}
