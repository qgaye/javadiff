package me.qgaye;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.util.*;

public class JsonDiffTool {

    public static void diff(JsonNode node1, JsonNode node2, JsonPath path, DiffInfo diffInfo, DiffConfig diffConfig) {
        if (isIgnorePath(path, diffConfig)) {
            return;
        }
        if (isEmptyValue(node1, node2, diffConfig)) {
            return;
        }
        if (node1 == null && node2 == null) {
            return;
        }
        if (node1 == null || node2 == null) {
            diffInfo.logError(path, node1, node2);
            return;
        }
        if (node1.isValueNode() && node2.isValueNode()) {
            if (!node1.equals(node2)) {
                diffInfo.logError(path, node1, node2);
            }
            return;
        }
        if (node1.isValueNode() || node2.isValueNode()) {
            diffInfo.logError(path, node1, node2);
            return;
        }
        if (node1.isArray() && node2.isArray()) {
            int size = Math.max(node1.size(), node2.size());
            for (int i = 0; i < size; i++) {
                path.add(i);
                diff(node1.get(i), node2.get(i), path, diffInfo, diffConfig);
                path.remove();
            }
            return;
        }
        if (node1.isArray() || node2.isArray()) {
            diffInfo.logError(path, node1, node2);
            return;
        }
        Set<String> fields = new HashSet<>();
        Iterator<String> iter1 = node1.fieldNames();
        while (iter1.hasNext()) {
            fields.add(iter1.next());
        }
        Iterator<String> iter2 = node2.fieldNames();
        while (iter2.hasNext()) {
            fields.add(iter2.next());
        }
        for (String field : fields) {
            path.add(field);
            diff(node1.get(field), node2.get(field), path, diffInfo, diffConfig);
            path.remove();
        }
    }

    private static boolean isIgnorePath(JsonPath path, DiffConfig diffConfig) {
        if (diffConfig.ignorePathTrie.hit(path)) {
            return true;
        }
        return false;
    }

    private static boolean isEmptyValue(JsonNode node1, JsonNode node2, DiffConfig diffConfig) {
        if (!diffConfig.ignoreEmptyValue) {
            return false;
        }
        boolean isEmptyNode1 = isEmptyValue(node1);
        boolean isEmptyNode2 = isEmptyValue(node2);
        return isEmptyNode1 && isEmptyNode2;
    }

    private static boolean isEmptyValue(JsonNode node) {
        // {}、null、""、0、[]、false
        // 均视作空值，视作相等
        if (node == null) {
            return true;
        }
        if (node.isNull()) {
            return true;
        }
        if (node.isValueNode()) {
            String text = node.asText();
            if (text.equals("null")
                    || text.equals("")
                    || text.equals("0")
                    || text.equals("0.0")
                    || text.equals("false")) {
                return true;
            }
        }
        if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
            if(node.size() == 0) {
                return true;
            }
        }
        if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
            if (node.size() == 0) {
                return true;
            }
        }
        return false;
    }

    public static void prettyOutput(DiffInfo diffInfo) {
        if (diffInfo.errorFields.isEmpty()) {
            System.out.println("GOOD😄");
            return;
        }
        System.out.println("FAIL😭");
        for (int i = 0; i < diffInfo.errorFields.size(); i++) {
            DiffFieldInfo errorField = diffInfo.errorFields.get(i);
            System.out.println("Path[" + i + "]: " + errorField.path);
            System.out.println("    Node1: " + errorField.node1);
            System.out.println("    Node2: " + errorField.node2);
        }
    }

    public static class JsonPath {
        public StringBuilder fullPath;
        public List<PathNode> keys;

        public JsonPath() {
            fullPath = new StringBuilder();
            keys = new ArrayList<>();
        }

        public void add(String field) {
            PathNode pathNode = new PathNode(field);
            if (fullPath.length() == 0) {
                fullPath.append(pathNode.key);
            } else {
                fullPath.append(".").append(pathNode.key);
            }
            keys.add(pathNode);
        }

        public void add(Integer index) {
            PathNode pathNode = new PathNode(index);
            fullPath.append(pathNode.key);
            keys.add(pathNode);
        }

        public void remove() {
            PathNode pathNode = keys.remove(keys.size() - 1);
            if (pathNode.type.equals(KeyNodeEnum.ARRAY)) {
                fullPath.delete(fullPath.length() - pathNode.key.length(), fullPath.length());
            } else if (fullPath.length() == pathNode.key.length()) {
                fullPath.delete(0, fullPath.length());
            } else {
                fullPath.delete(fullPath.length() - pathNode.key.length() - 1, fullPath.length());
            }
        }

        public static class PathNode {
            public Integer index;
            public String field;
            public String key;
            public KeyNodeEnum type;

            public PathNode(Integer index) {
                this.index = index;
                this.type = KeyNodeEnum.ARRAY;
                this.key = "[" + index + "]";
            }

            public PathNode(String field) {
                this.field = field;
                this.type = KeyNodeEnum.OBJECT;
                this.key = field;
            }
        }
    }

    public static class DiffInfo {
        public List<DiffFieldInfo> errorFields;

        public DiffInfo() {
            errorFields = new ArrayList<>();
        }

        public void logError(JsonPath path, JsonNode node1, JsonNode node2) {
            String s1 = JacksonUtils.serialize(node1);
            String s2 = JacksonUtils.serialize(node2);
            errorFields.add(new DiffFieldInfo(path.fullPath.toString(), s1, s2));
        }

    }

    public static class DiffFieldInfo { public String path;
        public String node1;
        public String node2;

        public DiffFieldInfo(String path, String node1, String node2) {
            this.path = path;
            this.node1 = node1;
            this.node2 = node2;
        }
    }

    public static class DiffConfig {
        // 不进行比较的路径
        public List<String> ignorePath;
        // 不进行比较的路径的前缀树
        public Trie ignorePathTrie;
        // 忽略null和空字符串的比较
        public Boolean ignoreEmptyValue;
    }

    public enum KeyNodeEnum {
        OBJECT(1),
        ARRAY(2),
        ;
        public final Integer code;

        KeyNodeEnum(Integer code) {
            this.code = code;
        }
    }

    public static class TrieNode {
        Map<String, TrieNode> children;
        Map<String, KeyNode> keyNodes;
        boolean isEnd;

        public TrieNode() {
            children = new HashMap<>();
            keyNodes = new HashMap<>();
            isEnd = false;
        }

        public static class KeyNode {
            public String key;
            public KeyNodeEnum type;

            public KeyNode(String key, KeyNodeEnum type) {
                this.key = key;
                this.type = type;
            }

        }
    }

    private static String getNodeUniqueId(String s, KeyNodeEnum type) {
        return s + "-" + type.code;
    }

    private static String getNodeUniqueId(Integer i, KeyNodeEnum type) {
        return i + "-" + type.code;
    }

    public static class Trie {
        public TrieNode root;

        public Trie() {
            this.root = new TrieNode();
        }

        public Trie(List<String> list) {
            this();
            for (String s : list) {
                this.insert(s);
            }
        }

        public void insert(String path) {
            String[] split = path.split("\\.");
            List<TrieNode.KeyNode> list = new ArrayList<>();
            for (String s : split) {
                int i = s.indexOf("[");
                if (i > -1) {
                    String pre = s.substring(0, i);
                    String arr = s.substring(i);
                    String arrNum = arr.substring(1, arr.length() - 1);
                    if (!pre.isEmpty()) {
                        list.add(new TrieNode.KeyNode(pre, KeyNodeEnum.OBJECT));
                    }
                    list.add(new TrieNode.KeyNode(arrNum, KeyNodeEnum.ARRAY));
                } else {
                    list.add(new TrieNode.KeyNode(s, KeyNodeEnum.OBJECT));
                }
            }
            TrieNode current = root;
            for (TrieNode.KeyNode keyNode : list) {
                String nodeUniqueId = getNodeUniqueId(keyNode.key, keyNode.type);
                TrieNode trieNode = current.children.get(nodeUniqueId);
                if (trieNode == null) {
                    trieNode = new TrieNode();
                    current.children.put(nodeUniqueId, trieNode);
                    current.keyNodes.put(nodeUniqueId, keyNode);
                }
                current = trieNode;
            }
            current.isEnd = true;
        }

        public boolean hit(JsonPath path) {
            if (root.children.isEmpty() || path.keys.isEmpty()) {
                return false;
            }
            TrieNode current = root;
            for (JsonPath.PathNode pathNode : path.keys) {
                String nodeUniqueId;
                String fullUniqueId;
                if (pathNode.type.equals(KeyNodeEnum.ARRAY)) {
                    nodeUniqueId = getNodeUniqueId(pathNode.index, pathNode.type);
                    fullUniqueId = getNodeUniqueId("*", pathNode.type);
                } else {
                    nodeUniqueId = getNodeUniqueId(pathNode.field, pathNode.type);
                    fullUniqueId = getNodeUniqueId("*", pathNode.type);
                }
                TrieNode trieNode = current.children.get(nodeUniqueId);
                if (trieNode == null) {
                    trieNode = current.children.get(fullUniqueId);
                    if (trieNode == null) {
                        return false;
                    }
                }
                current = trieNode;
            }
            return current.isEnd;
        }
    }

}