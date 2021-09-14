package cn.olange.setting;

import cn.olange.model.Config;
import cn.olange.model.RuleModel;
import cn.olange.service.RuleDataService;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

@State(name = "RulePersistentConfig", storages = {@Storage(value = "any-rule-config.xml")})
public class RulePersistentConfig implements PersistentStateComponent<Config> {

    private Config config = new Config();
    @Nullable
    @Override
    public Config getState() {
        if (config.getRegExpList() == null || config.getRegExpList().size() == 0) {
            try {
                InputStream resourceAsStream = RuleDataService.class.getResourceAsStream("/data/rule.js");
                InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream, "utf-8");
                String content = FileUtil.loadTextAndClose(inputStreamReader);
                List<RuleModel> ruleModels = RuleDataService.getRegArray(content);
                config.setRegExpList(ruleModels);
            } catch (Exception e) {
            }
        }
        return this.config;
    }

    @Override
    public void loadState(@NotNull Config config) {
        this.config = config;
    }

    @Nullable
    public static RulePersistentConfig getInstance() {
        return ServiceManager.getService(RulePersistentConfig.class);
    }
}
