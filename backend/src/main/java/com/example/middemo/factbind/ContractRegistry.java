package com.example.middemo.factbind;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 契约的内存形态：启动时加载一次，失败即终止。
 *
 * <p>它只做"把契约（YAML）读成事实"，不认识 Spring、也不认识业务——所以生产代码和测试可以共用同一个类。
 *
 * <p>YAML 用 Spring Boot 本来就带在 classpath 上的 SnakeYAML 解析（它读 {@code application.yml} 用的就是它），
 * 因此这里没有引入任何新依赖；解析出的 Map 再经 Jackson 转成 {@link JsonNode}，下面的遍历逻辑与格式无关。
 */
public class ContractRegistry {

    private static final Logger log = LoggerFactory.getLogger(ContractRegistry.class);
    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    private final Map<String, ResolvedOperation> bySymbol = new LinkedHashMap<>();
    private final Map<String, Integer> errorStatuses = new LinkedHashMap<>();

    public ContractRegistry(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            String location
    ) {
        Resource resource = resourceLoader.getResource(location);
        JsonNode root = readTree(objectMapper, resource);
        JsonNode paths = root.path("paths");
        if (!paths.isObject()) {
            throw FactBindException.contractLoad("missing or invalid 'paths' object");
        }

        loadErrorCatalog(root.path("x-factbind-errors"));

        Iterator<Map.Entry<String, JsonNode>> pathEntries = paths.fields();
        while (pathEntries.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathEntries.next();
            collect(pathEntry.getKey(), pathEntry.getValue());
        }
        log.info("FactBind loaded {} operation(s) from {}", bySymbol.size(), resource);
    }

    /** 拿到一条事实本身。 */
    public ResolvedOperation operation(String symbol) {
        ResolvedOperation op = bySymbol.get(symbol);
        if (op == null) {
            throw FactBindException.unknownSymbol(symbol);
        }
        return op;
    }

    /** 按契约拼出路径。 */
    public String path(String symbol, Map<String, ?> params) {
        ResolvedOperation op = operation(symbol);
        String result = op.path();
        for (String name : op.pathParams()) {
            Object value = params.get(name);
            if (value == null) {
                throw FactBindException.missingParameter(symbol, name);
            }
            result = result.replace("{" + name + "}", String.valueOf(value));
        }
        return result;
    }

    /** 错误码 → HTTP 状态码。这件事只写在契约里，异常类里不再出现状态码。 */
    public int errorStatus(String code) {
        Integer status = errorStatuses.get(code);
        if (status == null) {
            throw FactBindException.unknownErrorCode(code);
        }
        return status;
    }

    private void loadErrorCatalog(JsonNode catalog) {
        Iterator<Map.Entry<String, JsonNode>> codes = catalog.fields();
        while (codes.hasNext()) {
            Map.Entry<String, JsonNode> entry = codes.next();
            JsonNode status = entry.getValue().get("status");
            if (status == null || !status.canConvertToInt()) {
                throw FactBindException.contractLoad(
                        "error code '" + entry.getKey() + "' has no integer 'status'");
            }
            errorStatuses.put(entry.getKey(), status.asInt());
        }
        if (errorStatuses.isEmpty()) {
            throw FactBindException.contractLoad("missing or empty 'x-factbind-errors'");
        }
    }

    private void collect(String pathTemplate, JsonNode operations) {
        Iterator<Map.Entry<String, JsonNode>> it = operations.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> opEntry = it.next();
            String method = opEntry.getKey();
            if (!HTTP_METHODS.contains(method)) {
                continue;
            }
            JsonNode operation = opEntry.getValue();
            String symbol = operation.path("operationId").asText(null);
            if (symbol == null || symbol.isBlank()) {
                throw FactBindException.contractLoad(
                        method.toUpperCase() + " " + pathTemplate + " has no operationId");
            }
            if (bySymbol.containsKey(symbol)) {
                throw FactBindException.contractLoad("duplicate operationId '" + symbol + "'");
            }
            bySymbol.put(symbol, new ResolvedOperation(
                    symbol, method.toUpperCase(), pathTemplate, parameters(operation)));
        }
    }

    private List<ResolvedParameter> parameters(JsonNode operation) {
        List<ResolvedParameter> declared = new ArrayList<>();
        for (JsonNode parameter : operation.path("parameters")) {
            String name = parameter.path("name").asText(null);
            String in = parameter.path("in").asText(null);
            if (name == null || in == null) {
                throw FactBindException.contractLoad("a parameter is missing 'name' or 'in'");
            }
            JsonNode schema = parameter.path("schema");
            String defaultValue = schema.hasNonNull("default") ? schema.get("default").asText() : null;
            declared.add(new ResolvedParameter(
                    name, in, parameter.path("required").asBoolean(false), defaultValue));
        }
        return declared;
    }

    private JsonNode readTree(ObjectMapper objectMapper, Resource resource) {
        Object parsed;
        try (InputStream in = resource.getInputStream()) {
            // SafeConstructor：YAML 里的自定义标签不会去实例化任意类。契约是本地文件，但没有理由留这个口子。
            parsed = new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
        } catch (IOException e) {
            throw FactBindException.contractLoad("cannot read " + resource + ": " + e.getMessage());
        } catch (YAMLException e) {
            throw FactBindException.contractLoad("cannot parse " + resource + ": " + e.getMessage());
        }
        if (parsed == null) {
            throw FactBindException.contractLoad("contract is empty: " + resource);
        }
        return objectMapper.valueToTree(parsed);
    }
}
