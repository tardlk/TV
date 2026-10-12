package com.github.catvod.spider;

import com.github.catvod.crawler.Spider;
import java.util.List;

public final class Smoke extends Spider {
    @Override public String homeContent(boolean filter) {
        return "{\"class\":[{\"type_id\":\"1\",\"type_name\":\"JAR smoke\"}],\"list\":[{\"vod_id\":\"jar\",\"vod_name\":\"JAR smoke\"}]}";
    }
    @Override public String playerContent(String flag, String id, List<String> vipFlags) {
        return "{\"parse\":0,\"url\":\"jar-fixture\",\"site\":\"" + siteKey + "\"}";
    }
}
