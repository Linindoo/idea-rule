package cn.olange.model;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class RegExTableModel extends AbstractTableModel {
    private String[] heards = new String[]{"规则名称", "规则", "示例"};

    private List<RuleModel> ruleModels;

    public RegExTableModel(List<RuleModel> ruleModels) {

        this.ruleModels = ruleModels;
    }

    @Override
    public int getRowCount() {
        return ruleModels.size();
    }

    @Override
    public int getColumnCount() {
        return heards.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return heards[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        RuleModel ruleModel = this.ruleModels.get(rowIndex);
        if (columnIndex == 0) {
            return ruleModel.getTitle();
        } else if (columnIndex == 1) {
            return ruleModel.getRule();
        } else if (columnIndex == 2) {
            return ruleModel.getExamples();
        }
        return "";
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
