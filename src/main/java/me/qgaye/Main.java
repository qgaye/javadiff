package me.qgaye;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        String s1 = "{\"a\": {\"bb\":1}}";
        String s2 = "{\"a\": {\"bb\":1}, \"c\":{}}";
        JsonNode jsonNode1 = JacksonUtils.read(s1);
        JsonNode jsonNode2 = JacksonUtils.read(s2);
        JsonDiffTool.DiffConfig diffConfig = new JsonDiffTool.DiffConfig();
        diffConfig.ignorePath = new ArrayList<>();
        diffConfig.ignorePathTrie = new JsonDiffTool.Trie(diffConfig.ignorePath);
        diffConfig.ignoreEmptyValue = false;
        JsonDiffTool.DiffInfo diffInfo = new JsonDiffTool.DiffInfo();
        JsonDiffTool.diff(jsonNode1, jsonNode2, new JsonDiffTool.JsonPath(), diffInfo, diffConfig);
        JsonDiffTool.prettyOutput(diffInfo);
    }
}