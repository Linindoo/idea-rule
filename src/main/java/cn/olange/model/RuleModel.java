package cn.olange.model;

public class RuleModel {
    private String title;
    private String rule;
    private String examples;
    private boolean selfBuild;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getExamples() {
        return examples;
    }

    public void setExamples(String examples) {
        this.examples = examples;
    }

    public boolean isSelfBuild() {
        return selfBuild;
    }

    public void setSelfBuild(boolean selfBuild) {
        this.selfBuild = selfBuild;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RuleModel) {
            return isEq(((RuleModel) obj).getTitle(), this.title) && isEq(((RuleModel) obj).rule, this.rule) && isEq(((RuleModel) obj).examples, this.examples);
        }
        return false;
    }

    private boolean isEq(Object value1, Object value2) {
        if (value1 != null) {
            return value1.equals(value2);
        } else if (value2 != null) {
            return false;
        } else {
            return true;
        }
    }
}
