package top.shuzz.config;

import cn.hutool.setting.Setting;

import java.util.Optional;

/**
 * @author heng
 * @since 2023/10/17
 */
public class ConfigHelper {

    public static Integer getConfigInteger(final String group, final String key) {
        final Setting setting = new Setting("conf.setting");
        return Optional.of(setting).map(s -> s.get(group, key)).map(Integer::parseInt).orElse(null);
    }


    public static Boolean getConfigBoolean(final String group, final String key) {
        final Setting setting = new Setting("conf.setting");
        return Optional.of(setting).map(s -> s.get(group, key)).map(Boolean::parseBoolean).orElse(null);
    }
}
