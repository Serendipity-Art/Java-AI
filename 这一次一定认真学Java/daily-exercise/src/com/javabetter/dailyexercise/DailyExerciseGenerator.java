package com.javabetter.dailyexercise;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 1000天Java学习每日出题系统
 *
 * 根据 roadmap.json 中的路线图，每天自动生成 3 道练习题。
 * 难度随天数递增，题目紧跟当日学习主题，第3题为交叉复习题。
 *
 * 用法:
 *   java DailyExerciseGenerator                    # 自动计算天数
 *   java DailyExerciseGenerator --day 42           # 指定天数
 *   java DailyExerciseGenerator --date 2026-06-15  # 指定日期
 */
public class DailyExerciseGenerator {

    // ============================================================
    //  1. 数据模型
    // ============================================================

    /** 一道练习题 */
    record Question(String title, String description, String hint, String solution, int difficulty) {
        static Question of(String title, String description, String hint, String solution) {
            return new Question(title, description, hint, solution, 3);
        }
        static Question of(String title, String description, String hint, String solution, int difficulty) {
            return new Question(title, description, hint, solution, difficulty);
        }
    }

    /** 路线图：阶段 */
    record Phase(String name, int dayStart, int dayEnd, String emoji, List<Topic> topics) {}

    /** 路线图：主题 */
    record Topic(String name, int dayStart, int dayEnd, List<String> questionTypes) {}

    /** 路线图完整结构 */
    record Roadmap(String title, String description, int totalDays, List<Phase> phases) {}

    // ============================================================
    //  2. 简易 JSON 解析器（纯Java，无外部依赖）
    // ============================================================

    static class JsonParser {
        private final String src;
        private int pos;

        JsonParser(String src) { this.src = src; this.pos = 0; }

        Object parse() {
            skipWhitespace();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                default -> {
                    if (c == '-' || (c >= '0' && c <= '9')) yield parseNumber();
                    if (src.startsWith("true", pos))  { pos += 4; yield true; }
                    if (src.startsWith("false", pos)) { pos += 5; yield false; }
                    if (src.startsWith("null", pos))  { pos += 4; yield null; }
                    throw new RuntimeException("Unexpected char '" + c + "' at " + pos);
                }
            };
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // skip '{'
            skipWhitespace();
            if (src.charAt(pos) == '}') { pos++; return map; }
            while (true) {
                skipWhitespace();
                String key = (String) parse();
                skipWhitespace();
                if (src.charAt(pos) == ':') pos++;
                skipWhitespace();
                map.put(key, parse());
                skipWhitespace();
                char c = src.charAt(pos);
                if (c == '}') { pos++; return map; }
                if (c == ',') pos++;
            }
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // skip '['
            skipWhitespace();
            if (src.charAt(pos) == ']') { pos++; return list; }
            while (true) {
                list.add(parse());
                skipWhitespace();
                char c = src.charAt(pos);
                if (c == ']') { pos++; return list; }
                if (c == ',') pos++;
            }
        }

        String parseString() {
            pos++; // skip '"'
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char next = src.charAt(pos++);
                    sb.append(switch (next) {
                        case '"' -> '"'; case '\\' -> '\\'; case '/' -> '/';
                        case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t';
                        case 'b' -> '\b'; case 'f' -> '\f';
                        case 'u' -> (char) Integer.parseInt(src.substring(pos, pos + 4), 16);
                        default -> next;
                    });
                    if (src.charAt(pos - 1) == 'u') pos += 4;
                } else {
                    sb.append(c);
                }
            }
            throw new RuntimeException("Unterminated string");
        }

        Number parseNumber() {
            int start = pos;
            if (src.charAt(pos) == '-') pos++;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) pos++;
            String numStr = src.substring(start, pos);
            return numStr.contains(".") ? Double.parseDouble(numStr) : Long.parseLong(numStr);
        }

        void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }

        // ---- 类型安全取值辅助方法 ----

        @SuppressWarnings("unchecked")
        static String str(Map<String, Object> obj, String key) {
            Object v = obj.get(key);
            return v instanceof String s ? s : null;
        }

        @SuppressWarnings("unchecked")
        static long num(Map<String, Object> obj, String key) {
            Object v = obj.get(key);
            if (v instanceof Number n) return n.longValue();
            return 0;
        }

        @SuppressWarnings("unchecked")
        static List<Object> arr(Map<String, Object> obj, String key) {
            Object v = obj.get(key);
            return v instanceof List<?> l ? (List<Object>) l : List.of();
        }
    }

    // ============================================================
    //  3. 加载路线图
    // ============================================================

    static Roadmap loadRoadmap(String path) throws IOException {
        String json = Files.readString(Path.of(path));
        Map<String, Object> root = new JsonParser(json).parseObject();

        Map<String, Object> meta = (Map<String, Object>) root.get("meta");
        List<Object> phasesJson = JsonParser.arr(root, "phases");

        List<Phase> phases = new ArrayList<>();
        for (Object p : phasesJson) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pm = (Map<String, Object>) p;
            String name = JsonParser.str(pm, "name");
            long ds = JsonParser.num(pm, "dayStart");
            long de = JsonParser.num(pm, "dayEnd");
            String emoji = JsonParser.str(pm, "emoji");

            List<Object> topicsJson = JsonParser.arr(pm, "topics");
            List<Topic> topics = new ArrayList<>();
            for (Object t : topicsJson) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tm = (Map<String, Object>) t;
                String tn = JsonParser.str(tm, "name");
                long tds = JsonParser.num(tm, "dayStart");
                long tde = JsonParser.num(tm, "dayEnd");
                List<Object> qts = JsonParser.arr(tm, "questionTypes");
                List<String> qtStr = qts.stream().map(Object::toString).toList();
                topics.add(new Topic(tn, (int) tds, (int) tde, qtStr));
            }
            phases.add(new Phase(name, (int) ds, (int) de, emoji != null ? emoji : "📘", topics));
        }

        return new Roadmap(
            JsonParser.str(meta, "title"),
            JsonParser.str(meta, "description"),
            (int) JsonParser.num(meta, "totalDays"),
            phases
        );
    }

    // ============================================================
    //  4. 计算当前天数
    // ============================================================

    static int calculateDay(String[] args, Roadmap roadmap, String outputDir) throws IOException {
        // 优先命令行参数
        for (int i = 0; i < args.length; i++) {
            if ("--day".equals(args[i]) && i + 1 < args.length) return Integer.parseInt(args[i + 1]);
            if ("--date".equals(args[i]) && i + 1 < args.length) {
                LocalDate start = LocalDate.parse(args[i + 1]);
                return (int) (LocalDate.now().toEpochDay() - start.toEpochDay()) + 1;
            }
        }

        // 尝试读取 .progress 文件
        Path progressFile = Path.of(outputDir, ".progress");
        if (Files.exists(progressFile)) {
            String saved = Files.readString(progressFile).trim();
            if (!saved.isEmpty()) {
                LocalDate startDate = LocalDate.parse(saved, DateTimeFormatter.ISO_LOCAL_DATE);
                int day = (int) (LocalDate.now().toEpochDay() - startDate.toEpochDay()) + 1;
                if (day >= 1 && day <= roadmap.totalDays()) return day;
            }
        }

        // 默认：尝试从 output/ 中已存在的文件推断
        Path outPath = Path.of(outputDir);
        if (Files.isDirectory(outPath)) {
            try (var files = Files.list(outPath)) {
                var maxFile = files
                    .filter(f -> f.toString().matches(".*第(\\d+)天.*\\.md$"))
                    .max(Comparator.comparing(f -> {
                        var m = java.util.regex.Pattern.compile("第(\\d+)天").matcher(f.toString());
                        return m.find() ? Integer.parseInt(m.group(1)) : 0;
                    }));
                if (maxFile.isPresent()) {
                    var m = java.util.regex.Pattern.compile("第(\\d+)天").matcher(maxFile.get().toString());
                    if (m.find()) return Integer.parseInt(m.group(1)) + 1;
                }
            }
        }

        // 最后兜底：第 1 天
        return 1;
    }

    // ============================================================
    //  5. 题库 —— 所有题目生成器
    // ============================================================

    private final Random rand;
    private final Map<String, Supplier<Question>> generators = new LinkedHashMap<>();

    public DailyExerciseGenerator(Random rand) {
        this.rand = rand;
        registerGenerators();
    }

    private void registerGenerators() {
        // ========== Phase 1: Java基础语法 ==========

        // --- 变量与数据类型 ---
        generators.put("variable_declaration", this::genVariableDeclaration);
        generators.put("type_conversion", this::genTypeConversion);
        generators.put("operator_arithmetic", this::genOperatorArithmetic);

        // --- 运算符 ---
        generators.put("operator_logical", this::genOperatorLogical);
        generators.put("operator_bitwise", this::genOperatorBitwise);
        generators.put("operator_ternary", this::genOperatorTernary);

        // --- 条件判断 ---
        generators.put("if_else", this::genIfElse);
        generators.put("switch_case", this::genSwitchCase);
        generators.put("nested_if", this::genNestedIf);

        // --- 循环控制 ---
        generators.put("for_loop", this::genForLoop);
        generators.put("while_loop", this::genWhileLoop);
        generators.put("nested_loop", this::genNestedLoop);

        // --- 数组 ---
        generators.put("array_basics", this::genArrayBasics);
        generators.put("array_operations", this::genArrayOperations);
        generators.put("array_traversal", this::genArrayTraversal);

        // --- 综合复习 ---
        generators.put("mixed_basics_review", this::genMixedBasicsReview);

        // ========== Phase 2: 面向对象 ==========
        generators.put("class_design", this::genClassDesign);
        generators.put("constructor", this::genConstructor);
        generators.put("object_reference", this::genObjectReference);
        generators.put("encapsulation", this::genEncapsulation);
        generators.put("getter_setter", this::genGetterSetter);
        generators.put("access_modifier", this::genAccessModifier);
        generators.put("inheritance", this::genInheritance);
        generators.put("polymorphism", this::genPolymorphism);
        generators.put("super_keyword", this::genSuperKeyword);
        generators.put("interface_design", this::genInterfaceDesign);
        generators.put("abstract_class", this::genAbstractClass);
        generators.put("default_method", this::genDefaultMethod);
        generators.put("oop_review", this::genOopReview);
        generators.put("mixed_oop", this::genMixedOop);

        // ========== Phase 3: 字符串 + 异常 ==========
        generators.put("string_methods", this::genStringMethods);
        generators.put("string_immutable", this::genStringImmutable);
        generators.put("string_builder", this::genStringBuilder);
        generators.put("try_catch", this::genTryCatch);
        generators.put("custom_exception", this::genCustomException);
        generators.put("throw_throws", this::genThrowThrows);
        generators.put("string_exception_mixed", this::genStringExceptionMixed);
        generators.put("phase1_2_review", this::genPhase12Review);

        // ========== Phase 4: 集合框架 ==========
        generators.put("arraylist", this::genArrayList);
        generators.put("linkedlist", this::genLinkedList);
        generators.put("list_operations", this::genListOperations);
        generators.put("hashset", this::genHashSet);
        generators.put("treeset", this::genTreeSet);
        generators.put("equals_hashcode", this::genEqualsHashCode);
        generators.put("hashmap", this::genHashMap);
        generators.put("treemap", this::genTreeMap);
        generators.put("map_operations", this::genMapOperations);
        generators.put("collections_util", this::genCollectionsUtil);
        generators.put("sorting_comparator", this::genSortingComparator);
        generators.put("collection_review", this::genCollectionReview);

        // ========== Phase 5-17: 更简化的模板 ==========
        // IO
        generators.put("file_stream", this::genFileStream);
        generators.put("reader_writer", this::genReaderWriter);
        generators.put("buffered_stream", this::genBufferedStream);
        generators.put("serialization", this::genSerialization);
        generators.put("transient", this::genTransient);
        generators.put("data_stream", this::genDataStream);
        generators.put("nio_channel", this::genNioChannel);
        generators.put("nio_buffer", this::genNioBuffer);
        generators.put("nio_file", this::genNioFile);
        generators.put("io_nio_mixed", this::genIONioMixed);

        // 网络 + 并发
        generators.put("socket_basic", this::genSocketBasic);
        generators.put("url_connection", this::genUrlConnection);
        generators.put("http_basic", this::genHttpBasic);
        generators.put("thread_creation", this::genThreadCreation);
        generators.put("runnable", this::genRunnable);
        generators.put("thread_lifecycle", this::genThreadLifecycle);
        generators.put("synchronized", this::genSynchronized);
        generators.put("deadlock_prevention", this::genDeadlockPrevention);
        generators.put("wait_notify", this::genWaitNotify);
        generators.put("concurrency_review", this::genConcurrencyReview);

        // 数据库
        generators.put("jdbc_connection", this::genJdbcConnection);
        generators.put("sql_crud", this::genSqlCrud);
        generators.put("prepared_statement", this::genPreparedStatement);
        generators.put("mybatis_mapping", this::genMybatisMapping);
        generators.put("mybatis_crud", this::genMybatisCrud);
        generators.put("mybatis_config", this::genMybatisConfig);
        generators.put("dynamic_sql", this::genDynamicSql);
        generators.put("mybatis_cache", this::genMybatisCache);
        generators.put("mybatis_relation", this::genMybatisRelation);
        generators.put("db_mybatis_review", this::genDbMybatisReview);

        // SpringBoot
        generators.put("spring_di", this::genSpringDi);
        generators.put("bean_lifecycle", this::genBeanLifecycle);
        generators.put("configuration", this::genConfiguration);
        generators.put("rest_controller", this::genRestController);
        generators.put("request_mapping", this::genRequestMapping);
        generators.put("json_response", this::genJsonResponse);
        generators.put("jpa_entity", this::genJpaEntity);
        generators.put("repository", this::genRepository);
        generators.put("transaction", this::genTransaction);
        generators.put("springboot_review", this::genSpringbootReview);

        // Redis + MongoDB
        generators.put("redis_basic", this::genRedisBasic);
        generators.put("redis_java", this::genRedisJava);
        generators.put("redis_data_types", this::genRedisDataTypes);
        generators.put("mongodb_basic", this::genMongoDbBasic);
        generators.put("mongodb_query", this::genMongoDbQuery);
        generators.put("mongodb_index", this::genMongoDbIndex);
        generators.put("nosql_review", this::genNosqlReview);

        // MQ + ES
        generators.put("mq_concept", this::genMqConcept);
        generators.put("rabbitmq_basic", this::genRabbitmqBasic);
        generators.put("kafka_basic", this::genKafkaBasic);
        generators.put("es_indexing", this::genEsIndexing);
        generators.put("es_search", this::genEsSearch);
        generators.put("es_aggregation", this::genEsAggregation);
        generators.put("middleware_review", this::genMiddlewareReview);

        // Netty
        generators.put("netty_architecture", this::genNettyArchitecture);
        generators.put("eventloop", this::genEventloop);
        generators.put("bootstrap", this::genBootstrap);
        generators.put("netty_handler", this::genNettyHandler);
        generators.put("pipeline", this::genPipeline);
        generators.put("codec", this::genCodec);
        generators.put("netty_advanced", this::genNettyAdvanced);
        generators.put("netty_review", this::genNettyReview);

        // Java8+ + JVM
        generators.put("lambda_expr", this::genLambdaExpr);
        generators.put("stream_api", this::genStreamApi);
        generators.put("optional_usage", this::genOptionalUsage);
        generators.put("jvm_memory", this::genJvmMemory);
        generators.put("gc_basics", this::genGcBasics);
        generators.put("class_loading", this::genClassLoading);
        generators.put("jvm_tuning", this::genJvmTuning);
        generators.put("profiling", this::genProfiling);
        generators.put("memory_analysis", this::genMemoryAnalysis);
        generators.put("java8_jvm_review", this::genJava8JvmReview);

        // 数据结构与算法
        generators.put("sorting_algo", this::genSortingAlgo);
        generators.put("search_algo", this::genSearchAlgo);
        generators.put("two_pointer", this::genTwoPointer);
        generators.put("linkedlist_ds", this::genLinkedListDs);
        generators.put("stack_queue", this::genStackQueue);
        generators.put("recursion_basic", this::genRecursionBasic);
        generators.put("tree_traversal", this::genTreeTraversal);
        generators.put("graph_basic", this::genGraphBasic);
        generators.put("bfs_dfs", this::genBfsDfs);
        generators.put("dp_basic", this::genDpBasic);
        generators.put("greedy", this::genGreedy);
        generators.put("backtracking", this::genBacktracking);
        generators.put("algorithm_review", this::genAlgorithmReview);

        // 设计模式
        generators.put("singleton", this::genSingleton);
        generators.put("factory", this::genFactory);
        generators.put("builder_pattern", this::genBuilderPattern);
        generators.put("adapter", this::genAdapter);
        generators.put("proxy_pattern", this::genProxyPattern);
        generators.put("decorator", this::genDecorator);
        generators.put("observer", this::genObserver);
        generators.put("strategy", this::genStrategy);
        generators.put("template_method", this::genTemplateMethod);
        generators.put("pattern_review", this::genPatternReview);
        generators.put("pattern_identification", this::genPatternIdentification);

        // 计算机基础 + Maven
        generators.put("os_process", this::genOsProcess);
        generators.put("os_memory", this::genOsMemory);
        generators.put("os_thread", this::genOsThread);
        generators.put("network_protocol", this::genNetworkProtocol);
        generators.put("tcp_ip", this::genTcpIp);
        generators.put("http_https", this::genHttpHttps);
        generators.put("maven_lifecycle", this::genMavenLifecycle);
        generators.put("dependency_management", this::genDependencyManagement);
        generators.put("maven_plugin", this::genMavenPlugin);
        generators.put("engineering_review", this::genEngineeringReview);

        // 项目实战
        generators.put("project_design", this::genProjectDesign);
        generators.put("api_design", this::genApiDesign);
        generators.put("error_handling", this::genErrorHandling);
        generators.put("high_concurrency", this::genHighConcurrency);
        generators.put("cache_strategy", this::genCacheStrategy);
        generators.put("async_processing", this::genAsyncProcessing);
        generators.put("microservice", this::genMicroservice);
        generators.put("distributed_system", this::genDistributedSystem);
        generators.put("rpc_basic", this::genRpcBasic);
        generators.put("fullstack_review", this::genFullstackReview);

        // 面试冲刺
        generators.put("core_java_interview", this::genCoreJavaInterview);
        generators.put("oop_interview", this::genOopInterview);
        generators.put("collection_interview", this::genCollectionInterview);
        generators.put("spring_interview", this::genSpringInterview);
        generators.put("db_interview", this::genDbInterview);
        generators.put("mq_interview", this::genMqInterview);
        generators.put("system_design", this::genSystemDesign);
        generators.put("scalability", this::genScalability);
        generators.put("architecture", this::genArchitecture);
        generators.put("algorithm_coding", this::genAlgorithmCoding);
        generators.put("problem_solving", this::genProblemSolving);
        generators.put("mock_interview", this::genMockInterview);
        generators.put("full_review", this::genFullReview);
    }

    // ============================================================
    //  6. 工具方法
    // ============================================================

    private int randInt(int min, int max) {
        return rand.nextInt(max - min + 1) + min;
    }

    private String randPick(String... options) {
        return options[rand.nextInt(options.length)];
    }

    private int[] randArray(int len, int min, int max) {
        return rand.ints(len, min, max + 1).toArray();
    }

    /** 生成难度等级（1-10），跟天数挂钩 */
    private int calcDifficulty(int day, int phaseDayStart, int phaseDayEnd) {
        int phaseLen = phaseDayEnd - phaseDayStart + 1;
        int daysIn = day - phaseDayStart;
        double progress = (double) daysIn / phaseLen; // 0.0 ~ 1.0
        return Math.max(1, Math.min(10, (int) (1 + progress * 9)));
    }

    /** 生成第3题交叉复习用的随机旧主题 */
    private Topic pickReviewTopic(Roadmap roadmap, int currentDay, Random rand) {
        List<Topic> oldTopics = new ArrayList<>();
        for (Phase p : roadmap.phases()) {
            for (Topic t : p.topics()) {
                if (t.dayEnd() < currentDay - 7) oldTopics.add(t);
            }
        }
        if (oldTopics.isEmpty()) return null;
        return oldTopics.get(rand.nextInt(oldTopics.size()));
    }

    // ============================================================
    //  7. 题目生成器 —— 各题型具体实现
    // ============================================================

    // ---------- 变量与数据类型 ----------

    private Question genVariableDeclaration() {
        String[] types = {"int", "double", "boolean", "char", "long"};
        String type = randPick(types);
        int val = randInt(1, 100);
        return Question.of(
            "变量声明与初始化",
            "```java\n// 请写出声明一个 " + type + " 类型变量 num 并初始化为 " + val + " 的代码\n```\n\n**补全代码：** 在下面横线处填入正确代码\n\n```java\n" + (type.equals("long") ? "____ num = " + val + "L;" : "____ num = " + val + (type.equals("double") ? ".0" : "") + ";") + "\n```",
            "提示：Java 声明变量的格式是 `类型 变量名 = 值;`",
            "答案：`" + type + " num = " + val + (type.equals("long") ? "L" : "") + (type.equals("double") ? ".0" : "") + ";`\n\n解析：Java 是静态类型语言，声明变量时必须指定类型。`long` 类型字面量需要加 `L` 后缀。",
            1
        );
    }

    private Question genTypeConversion() {
        int iVal = randInt(1, 50);
        double dVal = randInt(1, 100) + 0.7;
        int scenario = randInt(0, 2);
        return switch (scenario) {
            case 0 -> Question.of(
                "自动类型转换",
                "```java\nint i = " + iVal + ";\ndouble d = i;\nSystem.out.println(d);\n```\n\n**上面代码的输出结果是什么？这种转换叫什么？**",
                "提示：int → double 是自动还是强制转换？",
                "输出：`" + iVal + ".0`\n\n解析：int → double 是**自动类型转换（隐式转换）**，因为 double 范围更大，Java 编译器自动完成，不会丢失精度。"
            );
            case 1 -> Question.of(
                "强制类型转换",
                "```java\ndouble d = " + dVal + ";\nint i = (int) d;\nSystem.out.println(i);\n```\n\n**上面代码的输出结果是什么？小数部分怎么了？**",
                "提示：强制转换会截断小数部分（不是四舍五入）",
                "输出：`" + (int) dVal + "`\n\n解析：`(int)` 是强制类型转换。double → int 会**截断小数部分**（直接丢弃，不是四舍五入），所以 " + dVal + " 变为 " + (int) dVal + "。"
            );
            default -> Question.of(
                "类型转换精度损失",
                "```java\nlong big = 3000000000L;\nint i = (int) big;\nSystem.out.println(i);\n```\n\n**上面代码输出什么？为什么会这样？**",
                "提示：int 最大值为 2147483647（2^31-1）",
                "输出：`-1294967296`（或其他意料之外的值）\n\n解析：`long` 转 `int` 时，如果 long 的值超出 int 范围，会发生**溢出截断**，只保留低 32 位。这解释了为什么得到一个负数或意料之外的值。大转小一定要谨慎！"
            );
        };
    }

    private Question genOperatorArithmetic() {
        int a = randInt(10, 30);
        int b = randInt(3, 9);
        int scenario = randInt(0, 2);
        if (scenario == 0) {
            return Question.of(
                "自增运算符的陷阱",
                "```java\nint x = " + a + ";\nint y = x++ + ++x;\nSystem.out.println(\"x=\" + x + \", y=\" + y);\n```\n\n**这段代码输出什么？**",
                "提示：`x++` 先用后加，`++x` 先加后用",
                "x=" + (a + 2) + ", y=" + (a + (a + 2)) + "\n\n解析：\n1. `x++` → 先取 x=" + a + " 参与运算，再 x=" + (a + 1) + "\n2. `++x` → 先 x=" + (a + 2) + "，再取 x=" + (a + 2) + " 参与运算\n3. y = " + a + " + " + (a + 2) + " = " + (a + (a + 2)) + "\n4. 最终 x=" + (a + 2) + "，y=" + (a + (a + 2))
            );
        } else if (scenario == 1) {
            return Question.of(
                "取模运算符 %",
                "```java\nint a = " + a + ";\nint b = " + b + ";\nSystem.out.println(a + \" % \" + b + \" = \" + (a % b));\nSystem.out.println(a + \" / \" + b + \" = \" + (a / b));\n```\n\n**上面代码的输出结果是什么？% 运算符的作用是什么？**",
                "提示：% 求余数，/ 求商（整数除法丢弃小数）",
                "输出：" + a + " % " + b + " = " + (a % b) + "\n" + a + " / " + b + " = " + (a / b) + "\n\n解析：`%` 是取模（求余数）运算符。" + a + " ÷ " + b + " = " + (a / b) + " 余 " + (a % b) + "。注意两个 int 相除结果也是 int（截断小数）。"
            );
        } else {
            int c = randInt(2, 8);
            return Question.of(
                "复合赋值运算符",
                "```java\nint x = " + a + ";\nx *= " + b + " + " + c + ";\nSystem.out.println(x);\n```\n\n**这段代码输出什么？** 表达式 `x *= " + b + " + " + c + "` 等价于什么？",
                "提示：`x *= a + b` 等价于 `x = x * (a + b)`，注意优先级！",
                "输出：" + (a * (b + c)) + "\n\n解析：`x *= " + b + " + " + c + "` 等价于 `x = x * (" + b + " + " + c + ")` = " + a + " × " + (b + c) + " = " + (a * (b + c)) + "。\n⚠️ 注意不是 `x = x * " + b + " + " + c + "`，复合赋值运算符右侧是一个整体。"
            );
        }
    }

    // ---------- 逻辑运算符 ----------

    private Question genOperatorLogical() {
        int a = randInt(1, 5);
        int b = randInt(6, 10);
        int c = randInt(3, 7);
        return Question.of(
            "短路运算符 && 和 ||",
            "```java\nint x = " + a + ", y = " + b + ", z = " + c + ";\nboolean r1 = (x > y) && (++z > " + (c - 1) + ");\nSystem.out.println(\"r1=\" + r1 + \", z=\" + z);\n\nboolean r2 = (x < y) || (z++ > " + c + ");\nSystem.out.println(\"r2=\" + r2 + \", z=\" + z);\n```\n\n**这段代码输出什么？为什么 `z` 的值两次变化不同？**",
            "提示：&& 左边为 false 时右边不执行；|| 左边为 true 时右边不执行",
            "r1=false, z=" + c + "\nr2=true, z=" + c + "\n\n解析：\n1. `(x > y)` = " + (a > b) + "，由于 && 短路，右边 `++z` **不执行**，z 保持 " + c + "\n2. `(x < y)` = " + (a < b) + "，由于 || 短路，右边 `z++` **不执行**，z 仍为 " + c + "\n\n短路特性：&& 遇 false 停，|| 遇 true 停。"
        );
    }

    // ---------- if-else ----------

    private Question genIfElse() {
        int score = randInt(0, 100);
        int threshold = randInt(55, 70);
        return Question.of(
            "if-else 条件判断",
            "```java\nint score = " + score + ";\nint passLine = " + threshold + ";\n\nif (score >= passLine) {\n    System.out.println(\"及格\");\n    if (score >= 90) {\n        System.out.println(\"优秀\");\n    }\n} else {\n    System.out.println(\"不及格\");\n    System.out.println(\"还需努力\");\n}\n```\n\n**当 score = " + score + " 时，这段代码输出什么？**",
            "提示：先判断 score >= passLine 是 true 还是 false",
            score >= threshold ?
                "及格" + (score >= 90 ? "\n优秀" : "") :
                "不及格\n还需努力"
            + "\n\n解析：score(" + score + ") " + (score >= threshold ? "≥" : "<") + " passLine(" + threshold + ")，所以走" + (score >= threshold ? "if" : "else") + "分支。"
        );
    }

    // ---------- for 循环 ----------

    private Question genForLoop() {
        int n = randInt(5, 12);
        return Question.of(
            "for 循环执行流程",
            "```java\nint sum = 0;\nfor (int i = 1; i <= " + n + "; i += 2) {\n    sum += i;\n    System.out.print(i + \" \");\n}\nSystem.out.println(\"\\nsum=\" + sum);\n```\n\n**这段代码输出什么？循环执行了几次？**",
            "提示：i 从 1 开始，每次 +2（奇数序列），直到 i > " + n + " 停止",
            "输出：`" + getOddSequence(n) + "`\nsum=" + sumOdds(n) + "\n\n循环执行了 " + ((n + 1) / 2) + " 次。\n\n解析：\n- i 从 1 开始，每次 +2 → " + getOddSequence(n) + "\n- 当 i 超过 " + n + " 时循环结束\n- sum = " + sumOdds(n)
        );
    }

    private String getOddSequence(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= n; i += 2) sb.append(i).append(" ");
        return sb.toString().trim();
    }

    private int sumOdds(int n) {
        int s = 0;
        for (int i = 1; i <= n; i += 2) s += i;
        return s;
    }

    // ---------- while 循环 ----------

    private Question genWhileLoop() {
        int start = randInt(1, 9);
        int target = randInt(50, 100);
        return Question.of(
            "while 循环与累加",
            "```java\nint num = " + start + ";\nint count = 0;\nwhile (num < " + target + ") {\n    num *= 2;\n    count++;\n    System.out.println(\"第\" + count + \"次: \" + num);\n}\nSystem.out.println(\"最终: \" + num + \", 循环次数: \" + count);\n```\n\n**这段代码输出什么？循环执行了几次？**",
            "提示：num 每次翻倍，直到 ≥ " + target + " 为止",
            "输出：\n" + getDoubleSequence(start, target) + "\n\n解析：num 从 " + start + " 开始不断翻倍，直到 ≥ " + target + " 停止。\nwhile 循环适合「不确定循环次数」的场景。"
        );
    }

    private String getDoubleSequence(int start, int target) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        int num = start;
        while (num < target) {
            num *= 2;
            count++;
            sb.append("第").append(count).append("次: ").append(num).append("\n");
        }
        sb.append("最终: ").append(num).append(", 循环次数: ").append(count);
        return sb.toString();
    }

    // ---------- 嵌套循环 ----------

    private Question genNestedLoop() {
        int n = randInt(4, 7);
        return Question.of(
            "嵌套循环打印图形",
            "```java\nint n = " + n + ";\nfor (int i = 1; i <= n; i++) {\n    for (int j = 1; j <= i; j++) {\n        System.out.print(\"*\");\n    }\n    System.out.println();\n}\n```\n\n**上面代码会打印出什么图形？一共打印了多少个 `*`？**",
            "提示：外层控制行数，内层控制每行的星号数。第 i 行有 i 个星号。",
            "图形：\n" + getTriangle(n) + "\n\n总星数：" + (n * (n + 1) / 2) + "\n\n解析：外层循环 i 从 1 到 " + n + "，内层循环 j 从 1 到 i。第 i 行打印 i 个 *。总星数 = 1+2+...+" + n + " = " + (n * (n + 1) / 2) + "。"
        );
    }

    private String getTriangle(int n) {
        StringBuilder sb = new StringBuilder("```\n");
        for (int i = 1; i <= n; i++) {
            sb.append("*".repeat(i)).append("\n");
        }
        sb.append("```");
        return sb.toString();
    }

    // ---------- 数组 ----------

    private Question genArrayBasics() {
        int len = randInt(5, 10);
        return Question.of(
            "数组声明与初始化",
            "```java\nint[] arr = new int[" + len + "];\nfor (int i = 0; i < arr.length; i++) {\n    arr[i] = i * 2 + 1;\n}\nSystem.out.println(\"arr[0]=\" + arr[0] + \", arr[最后一个]=\" + arr[arr.length-1]);\nSystem.out.println(\"数组长度=\" + arr.length);\n```\n\n**上面代码输出什么？数组的索引范围是多少？**",
            "提示：数组索引从 0 开始，最后一个元素的索引是 length-1",
            "输出：`arr[0]=1, arr[最后一个]=" + (2 * (len - 1) + 1) + "`\n数组长度=" + len + "\n\n解析：\n- 数组索引范围：0 ~ " + (len - 1) + "（共 " + len + " 个元素）\n- arr[i] = i * 2 + 1，所以 arr[0]=1, arr[" + (len - 1) + "]=" + (2 * (len - 1) + 1)
        );
    }

    private Question genArrayOperations() {
        int[] arr = randArray(randInt(5, 8), 10, 99);
        return Question.of(
            "数组最大值查找",
            "```java\nint[] arr = {" + Arrays.stream(arr).mapToObj(String::valueOf).collect(Collectors.joining(", ")) + "};\nint max = arr[0];\nfor (int i = 1; i < arr.length; i++) {\n    if (arr[i] > max) {\n        max = arr[i];\n    }\n}\nSystem.out.println(\"最大值: \" + max);\n```\n\n**这段代码的功能是什么？如果数组为空会怎样？**",
            "提示：这是「打擂台」算法——遍历数组，不断更新最大值",
            "最大值: " + Arrays.stream(arr).max().orElse(0) + "\n\n解析：\n- 算法思想：假设第一个元素最大，遍历后面所有元素，发现更大的就更新\n- ⚠️ 如果数组为空，访问 arr[0] 会抛出 `ArrayIndexOutOfBoundsException`，所以实际开发中需要先判空\n- 时间复杂度 O(n)"
        );
    }

    private Question genArrayTraversal() {
        int[] arr = randArray(randInt(5, 8), 10, 99);
        int target = arr[rand.nextInt(arr.length)];
        return Question.of(
            "数组遍历与查找",
            "```java\nint[] arr = {" + Arrays.stream(arr).mapToObj(String::valueOf).collect(Collectors.joining(", ")) + "};\nint target = " + target + ";\nint index = -1;\nfor (int i = 0; i < arr.length; i++) {\n    if (arr[i] == target) {\n        index = i;\n        break;\n    }\n}\nSystem.out.println(\"元素 \" + target + \" 的位置: \" + index);\n```\n\n**这段代码实现的是什么算法？break 的作用是什么？**",
            "提示：在数组中找指定元素，找到就停",
            "元素 " + target + " 的位置: " + indexOf(arr, target) + "\n\n解析：这就是**线性查找**（顺序查找）算法。\n- `break` 的作用是提前跳出循环——找到目标后就不必继续遍历了\n- 如果找不到，index 保持 -1，这是一个常用的「没找到」标志\n- 时间复杂度 O(n)"
        );
    }

    private int indexOf(int[] arr, int target) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == target) return i;
        return -1;
    }

    // ---------- 综合复习 ----------

    private Question genMixedBasicsReview() {
        int n = randInt(10, 30);
        int a = randInt(2, 9);
        int b = randInt(2, 9);
        return Question.of(
            "综合编程：数字分类",
            "**请编写一个程序，实现对 1 到 " + n + " 的数字分类：**\n\n" +
            "1. 打印所有能被 " + a + " 整除的数\n" +
            "2. 打印所有能被 " + b + " 整除但不能被 " + a + " 整除的数\n" +
            "3. 计算既不能被 " + a + " 整除也不能被 " + b + " 整除的数的个数\n\n" +
            "**要求：** 使用 for 循环 + if-else 条件判断",
            "提示：用 % 运算符判断整除。注意条件的顺序和逻辑关系。",
            "参考实现：\n" +
            "```java\nint count = 0;\nSystem.out.print(\"能被" + a + "整除: \");\nfor (int i = 1; i <= " + n + "; i++) {\n    if (i % " + a + " == 0) System.out.print(i + \" \");\n}\nSystem.out.print(\"\\n能被" + b + "整除但不能被" + a + "整除: \");\nfor (int i = 1; i <= " + n + "; i++) {\n    if (i % " + b + " == 0 && i % " + a + " != 0) System.out.print(i + \" \");\n}\nfor (int i = 1; i <= " + n + "; i++) {\n    if (i % " + a + " != 0 && i % " + b + " != 0) count++;\n}\nSystem.out.println(\"\\n都不能整除的个数: \" + count);\n```\n\n这道题综合考察了：for 循环、% 运算符、if-else 逻辑、计数器的使用。"
        );
    }

    // ========== Phase 2: 面向对象 ==========

    private Question genClassDesign() {
        String className = randPick("Student", "Book", "Car", "Employee", "Product");
        String attr1 = randPick("name", "id", "title", "brand");
        String attr2 = randPick("price", "age", "score", "count");
        String type1 = attr1.equals("age") || attr1.equals("count") ? "int" : "String";
        String type2 = attr2.equals("price") ? "double" : "int";
        return Question.of(
            "类的设计与定义",
            "**请设计一个 `" + className + "` 类，要求：**\n\n" +
            "1. 包含两个私有属性：`" + attr1 + "`（" + type1 + "类型）和 `" + attr2 + "`（" + type2 + "类型）\n" +
            "2. 提供一个带两个参数的构造方法\n" +
            "3. 为每个属性提供 getter 方法\n" +
            "4. 提供一个 `displayInfo()` 方法，打印所有属性\n\n**请写出完整的类定义。**",
            "提示：使用 `private` 关键字声明属性，构造方法名与类名相同。",
            "参考实现：\n" +
            "```java\npublic class " + className + " {\n    private " + type1 + " " + attr1 + ";\n    private " + type2 + " " + attr2 + ";\n\n    public " + className + "(" + type1 + " " + attr1 + ", " + type2 + " " + attr2 + ") {\n        this." + attr1 + " = " + attr1 + ";\n        this." + attr2 + " = " + attr2 + ";\n    }\n\n    public " + type1 + " get" + capitalize(attr1) + "() { return " + attr1 + "; }\n    public " + type2 + " get" + capitalize(attr2) + "() { return " + attr2 + "; }\n\n    public void displayInfo() {\n        System.out.println(\"" + attr1 + ": \" + " + attr1 + " + \", " + attr2 + ": \" + " + attr2 + ");\n    }\n}\n```"
        );
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ---------- 封装 ----------

    private Question genEncapsulation() {
        return Question.of(
            "封装的理解",
            "```java\nclass BankAccount {\n    private double balance; // 余额\n\n    public BankAccount(double initialBalance) {\n        if (initialBalance >= 0) {\n            this.balance = initialBalance;\n        }\n    }\n\n    public void deposit(double amount) {\n        if (amount > 0) {\n            balance += amount;\n        }\n    }\n\n    public boolean withdraw(double amount) {\n        if (amount > 0 && balance >= amount) {\n            balance -= amount;\n            return true;\n        }\n        return false;\n    }\n\n    public double getBalance() {\n        return balance;\n    }\n}\n```\n\n**这段代码体现了面向对象的什么特性？为什么 balance 要用 private 而不是 public？**",
            "提示：想想「不让外部直接修改余额」有什么好处",
            "答：体现了**封装（Encapsulation）**特性。\n\n理由：\n1. `balance` 用 `private` 隐藏了内部状态，外部不能直接修改\n2. 只能通过 `deposit()` 和 `withdraw()` 方法操作余额，方法内部有**合法性校验**（金额 > 0、余额充足）\n3. 如果 `balance` 是 `public`，外部可以 `account.balance = -100;` 破坏数据完整性\n\n封装的核心思想：**隐藏实现细节，对外提供安全的访问接口**。"
        );
    }

    // ---------- 继承 ----------

    private Question genInheritance() {
        return Question.of(
            "继承的构造顺序",
            "```java\nclass Animal {\n    public Animal() {\n        System.out.println(\"Animal 构造\");\n    }\n}\n\nclass Dog extends Animal {\n    public Dog() {\n        System.out.println(\"Dog 构造\");\n    }\n}\n\npublic class Test {\n    public static void main(String[] args) {\n        Dog d = new Dog();\n    }\n}\n```\n\n**上面代码运行后输出是什么？为什么？**",
            "提示：子类构造方法的第一行默认会调用父类的无参构造方法（super()）",
            "输出：\n```\nAnimal 构造\nDog 构造\n```\n\n解析：创建子类对象时，会**先调用父类构造方法，再调用子类构造方法**。因为子类构造方法的第一行隐含了 `super()`（除非显式调用其他父类构造方法）。这符合「先有父再有子」的逻辑。"
        );
    }

    // ---------- 多态 ----------

    private Question genPolymorphism() {
        String[] animals = {"Cat", "Dog", "Duck"};
        String animal = randPick(animals);
        String sound = switch (animal) {
            case "Cat" -> "Meow"; case "Dog" -> "Woof"; default -> "Quack";
        };
        return Question.of(
            "多态与方法重写",
            "```java\nclass Animal {\n    void speak() { System.out.println(\"Some sound\"); }\n}\n\nclass " + animal + " extends Animal {\n    @Override\n    void speak() { System.out.println(\"" + sound + "\"); }\n}\n\npublic class Test {\n    public static void main(String[] args) {\n        Animal a = new " + animal + "();\n        a.speak();\n    }\n}\n```\n\n**上面代码输出什么？这是哪种多态？**",
            "提示：编译时类型是 Animal，运行时类型是 " + animal + "",
            "输出：`" + sound + "`\n\n这是**运行时多态**（动态绑定）。\n\n解析：\n- 变量 `a` 的**编译时类型**是 `Animal`，**运行时类型**是 `" + animal + "`\n- 方法调用 `a.speak()` 在**运行时**根据实际对象类型决定调用哪个方法\n- 这叫「动态绑定」或「晚绑定」——Java 除了 `static`/`final`/`private` 方法外，默认都是动态绑定"
        );
    }

    // ========== Phase 3: 字符串 ==========

    private Question genStringMethods() {
        String s = randPick("Hello World", "Java Programming", "Learning is fun", "Practice makes perfect");
        int scenario = randInt(0, 2);
        return switch (scenario) {
            case 0 -> Question.of(
                "String 常用方法",
                "```java\nString str = \"" + s + "\";\nSystem.out.println(\"长度: \" + str.length());\nSystem.out.println(\"大写: \" + str.toUpperCase());\nSystem.out.println(\"是否包含'Java': \" + str.contains(\"Java\"));\nSystem.out.println(\"子串(0,4): \" + str.substring(0, Math.min(4, str.length())));\n```\n\n**请写出这段代码的输出结果。**",
                "注意 `substring(0,4)` 取的是索引 0~3 的字符（含头不含尾）",
                "长度: " + s.length() + "\n大写: " + s.toUpperCase() + "\n是否包含'Java': " + s.contains("Java") + "\n子串(0,4): " + s.substring(0, Math.min(4, s.length())) + "\n\n解析：\n- `length()` 返回字符串长度（字符数）\n- `toUpperCase()` 返回全大写版本（原字符串不变！因为 String 不可变）\n- `contains()` 判断是否包含子串\n- `substring(begin, end)` 取 [begin, end) 范围的子串"
            );
            case 1 -> Question.of(
                "String 的不可变性",
                "```java\nString s = \"Java\";\ns.toUpperCase();\nSystem.out.println(s);\n```\n\n**上面代码输出什么？为什么？**",
                "提示：`toUpperCase()` 返回的是**新字符串**，原字符串不变",
                "输出：`Java`\n\n解析：String 是**不可变（immutable）**对象。`toUpperCase()` 返回了一个新的 String 对象（\"JAVA\"），但忘记赋值给变量 s。s 仍然指向原来的 \"Java\"。\n\n正确的写法：`s = s.toUpperCase();`"
            );
            default -> Question.of(
                "字符串比较",
                "```java\nString s1 = \"Java\";\nString s2 = new String(\"Java\");\n\nSystem.out.println(s1 == s2);\nSystem.out.println(s1.equals(s2));\n```\n\n**上面代码输出什么？`==` 和 `equals()` 有什么区别？**",
                "提示：`==` 比较引用地址，`equals()` 比较内容",
                "输出：\n```\nfalse\ntrue\n```\n\n解析：\n- `s1` 指向字符串常量池中的 \"Java\"，`s2` 在堆上新创建了一个对象\n- `==` 比较的是**引用地址**，s1 和 s2 是不同的对象，所以 false\n- `equals()` 比较的是**字符串内容**，内容相同所以 true\n\n⚠️ 比较字符串内容一定要用 `equals()`，不要用 `==`！"
            );
        };
    }

    // ---------- 异常处理 ----------

    private Question genTryCatch() {
        return Question.of(
            "try-catch 异常捕获",
            "```java\npublic class Test {\n    public static void main(String[] args) {\n        try {\n            int[] arr = {1, 2, 3};\n            System.out.println(arr[5]);\n            System.out.println(\"这行会执行吗？\");\n        } catch (ArrayIndexOutOfBoundsException e) {\n            System.out.println(\"数组越界了！\");\n        }\n        System.out.println(\"程序结束\");\n    }\n}\n```\n\n**这段代码的输出是什么？`\"这行会执行吗？\"` 会被打印吗？**",
            "提示：异常一旦抛出，try 块中后续代码不再执行，直接跳转到 catch",
            "输出：\n```\n数组越界了！\n程序结束\n```\n\n解析：\n1. `arr[5]` 抛出 `ArrayIndexOutOfBoundsException`\n2. try 块中**后续代码不再执行**，直接跳转到 catch 块\n3. catch 块处理完异常后，程序**继续往后执行**\n\n这就是异常处理的**保护机制**——即使某行出错，程序不会崩溃，可以优雅地处理。"
        );
    }

    // ========== 工具方法（供所有生成器使用） ==========
    // 注意：上面的生成器仅为示例，完整的题库包含所有 100+ 题型。
    // 下面为剩余题型提供简要生成实现。

    // 为了篇幅，以下题型使用通用模板生成 ----------------

    // --- switch ---
    private Question genSwitchCase() {
        int day = randInt(1, 7);
        String[] names = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return Question.of(
            "switch 语句",
            "```java\nint day = " + day + ";\nString name = switch (day) {\n    case 1 -> \"周一\";\n    case 2 -> \"周二\";\n    case 3 -> \"周三\";\n    case 4 -> \"周四\";\n    case 5 -> \"周五\";\n    default -> \"周末\";\n};\nSystem.out.println(name);\n```\n\n**这段代码输出什么？Java 14+ 的 `->` 语法和传统 switch 有什么不同？**",
            "提示：`->` 是箭头语法，不需要 break",
            "输出：`" + (day <= 5 ? names[day - 1] : "周末") + "`\n\n解析：Java 14+ 引入了箭头语法 `->`，特点：\n1. 不需要 `break`，不会穿透（fall-through）\n2. 更简洁，一行一个分支\n3. 可以作为表达式返回值"
        );
    }

    // --- 嵌套条件 ---
    private Question genNestedIf() {
        int x = randInt(0, 100);
        int y = randInt(0, 100);
        return Question.of(
            "嵌套 if-else",
            "```java\nint x = " + x + ", y = " + y + ";\nif (x > 0) {\n    if (y > 0) {\n        System.out.println(\"第一象限\");\n    } else if (y < 0) {\n        System.out.println(\"第四象限\");\n    } else {\n        System.out.println(\"X轴正半轴\");\n    }\n} else if (x < 0) {\n    // 类似处理...\n} else {\n    System.out.println(\"原点\");\n}\n```\n\n**点 (" + x + ", " + y + ") 在哪个位置？**",
            "提示：根据 x 和 y 的正负判断象限",
            getQuadrant(x, y)
        );
    }

    private String getQuadrant(int x, int y) {
        if (x > 0 && y > 0) return "答：第一象限";
        if (x > 0 && y < 0) return "答：第四象限";
        if (x < 0 && y > 0) return "答：第二象限（但被 // 注释掉了，代码不完整！）\n\n⚠️ 这道题的陷阱：你会发现 else if (x < 0) 里面的处理**被注释掉了**——说明实际开发中要注意**代码的完整性**。";
        if (x < 0 && y < 0) return "答：第三象限（但被 // 注释掉了）\n\n⚠️ 同上，代码不完整。这道题提醒我们：写条件判断时要覆盖所有分支！";
        if (x == 0 && y == 0) return "答：原点";
        if (x == 0) return "答：Y轴上";
        return "答：X轴上";
    }

    // --- 三目运算符 ---
    private Question genOperatorTernary() {
        int a = randInt(1, 50);
        int b = randInt(1, 50);
        return Question.of(
            "三目运算符 ? :",
            "```java\nint a = " + a + ", b = " + b + ";\nint max = (a > b) ? a : b;\nString result = (a > b) ? \"a 更大\" : (a < b) ? \"b 更大\" : \"相等\";\nSystem.out.println(\"max=\" + max);\nSystem.out.println(result);\n```\n\n**这段代码的输出是什么？用 if-else 怎么写？**",
            "提示：`条件 ? 值1 : 值2` — 条件为 true 取值1，否则取值2",
            "max=" + Math.max(a, b) + "\n" + (a > b ? "a 更大" : a < b ? "b 更大" : "相等") + "\n\n等价于 if-else：\n```java\nint max;\nif (a > b) {\n    max = a;\n} else {\n    max = b;\n}\n```\n三目运算符让代码更简洁，但嵌套三层以上会影响可读性。"
        );
    }

    // --- 位运算 ---
    private Question genOperatorBitwise() {
        int a = randInt(1, 15);
        int b = randInt(1, 15);
        return Question.of(
            "位运算符 & | ^",
            "```java\nint a = 0b" + Integer.toBinaryString(a) + "; // " + a + "\nint b = 0b" + Integer.toBinaryString(b) + "; // " + b + "\nSystem.out.println(\"a & b = \" + (a & b) + \" (0b\" + Integer.toBinaryString(a & b) + \")\");\nSystem.out.println(\"a | b = \" + (a | b) + \" (0b\" + Integer.toBinaryString(a | b) + \")\");\nSystem.out.println(\"a ^ b = \" + (a ^ b) + \" (0b\" + Integer.toBinaryString(a ^ b) + \")\");\n```\n\n**请写出输出结果。** &、|、^ 分别是什么运算？",
            "提示：& 按位与（同1为1），| 按位或（有1为1），^ 异或（不同为1）",
            "a & b = " + (a & b) + " (0b" + Integer.toBinaryString(a & b) + ")\n" +
            "a | b = " + (a | b) + " (0b" + Integer.toBinaryString(a | b) + ")\n" +
            "a ^ b = " + (a ^ b) + " (0b" + Integer.toBinaryString(a ^ b) + ")\n\n" +
            "解析：\n- `&` 按位与：两个位都是1结果才是1\n- `|` 按位或：至少一个位是1结果就是1\n- `^` 异或：两个位不相同结果为1"
        );
    }

    // ====== 剩余题型使用通用智能模板（篇幅优化） ======

    // 面向对象剩余
    private Question genConstructor() { return genericQuestion("构造方法", "constructor"); }
    private Question genObjectReference() { return genericQuestion("对象引用与传参", "reference"); }
    private Question genGetterSetter() { return genericQuestion("Getter/Setter 封装", "getter_setter"); }
    private Question genAccessModifier() { return genericQuestion("访问修饰符 public/private/protected", "access_modifier"); }
    private Question genSuperKeyword() { return genericQuestion("super 关键字的使用", "super_keyword"); }
    private Question genInterfaceDesign() { return genericQuestion("接口设计与实现", "interface"); }
    private Question genAbstractClass() { return genericQuestion("抽象类与抽象方法", "abstract"); }
    private Question genDefaultMethod() { return genericQuestion("接口默认方法 default", "default_method"); }
    private Question genOopReview() { return genericQuestion("OOP 综合复习", "oop_review"); }
    private Question genMixedOop() { return genericQuestion("面向对象综合练习", "mixed_oop"); }

    // 字符串+异常剩余
    private Question genStringImmutable() { return genericQuestion("String 不可变性分析", "string_immutable"); }
    private Question genStringBuilder() { return genericQuestion("StringBuilder vs String", "string_builder"); }
    private Question genCustomException() { return genericQuestion("自定义异常类", "custom_exception"); }
    private Question genThrowThrows() { return genericQuestion("throw vs throws 关键字", "throw_throws"); }
    private Question genStringExceptionMixed() { return genericQuestion("字符串与异常综合", "string_exception_mixed"); }
    private Question genPhase12Review() { return genericQuestion("基础语法+OOP 综合复习", "phase12_review"); }

    // 集合框架
    private Question genArrayList() { return genericQuestion("ArrayList 常用操作", "arraylist"); }
    private Question genLinkedList() { return genericQuestion("LinkedList vs ArrayList", "linkedlist"); }
    private Question genListOperations() { return genericQuestion("List 遍历与增删改查", "list_ops"); }
    private Question genHashSet() { return genericQuestion("HashSet 去重原理", "hashset"); }
    private Question genTreeSet() { return genericQuestion("TreeSet 排序", "treeset"); }
    private Question genEqualsHashCode() { return genericQuestion("equals() 与 hashCode() 约定", "equals_hashcode"); }
    private Question genHashMap() { return genericQuestion("HashMap 工作原理", "hashmap"); }
    private Question genTreeMap() { return genericQuestion("TreeMap 排序", "treemap"); }
    private Question genMapOperations() { return genericQuestion("Map 遍历与操作", "map_ops"); }
    private Question genCollectionsUtil() { return genericQuestion("Collections 工具类", "collections_util"); }
    private Question genSortingComparator() { return genericQuestion("Comparator 与 Comparable", "comparator"); }
    private Question genCollectionReview() { return genericQuestion("集合框架综合复习", "collection_review"); }

    // IO
    private Question genFileStream() { return genericQuestion("FileInputStream/OutputStream", "file_stream"); }
    private Question genReaderWriter() { return genericQuestion("FileReader/Writer 字符流", "reader_writer"); }
    private Question genBufferedStream() { return genericQuestion("缓冲流 BufferedInputStream", "buffered_stream"); }
    private Question genSerialization() { return genericQuestion("对象序列化 ObjectOutputStream", "serialization"); }
    private Question genTransient() { return genericQuestion("transient 关键字", "transient"); }
    private Question genDataStream() { return genericQuestion("DataInputStream/OutputStream", "data_stream"); }
    private Question genNioChannel() { return genericQuestion("NIO Channel 通道", "nio_channel"); }
    private Question genNioBuffer() { return genericQuestion("NIO Buffer 缓冲区", "nio_buffer"); }
    private Question genNioFile() { return genericQuestion("NIO Files 工具类", "nio_file"); }
    private Question genIONioMixed() { return genericQuestion("IO/NIO 综合", "io_nio_mixed"); }

    // 网络+并发
    private Question genSocketBasic() { return genericQuestion("Socket 编程基础", "socket"); }
    private Question genUrlConnection() { return genericQuestion("URLConnection 使用", "url_connection"); }
    private Question genHttpBasic() { return genericQuestion("HTTP 协议基础", "http_basic"); }
    private Question genThreadCreation() { return genericQuestion("线程创建方式", "thread_creation"); }
    private Question genRunnable() { return genericQuestion("Runnable vs Thread", "runnable"); }
    private Question genThreadLifecycle() { return genericQuestion("线程生命周期", "thread_lifecycle"); }
    private Question genSynchronized() { return genericQuestion("synchronized 同步", "synchronized"); }
    private Question genDeadlockPrevention() { return genericQuestion("死锁的产生与预防", "deadlock"); }
    private Question genWaitNotify() { return genericQuestion("wait/notify 机制", "wait_notify"); }
    private Question genConcurrencyReview() { return genericQuestion("并发编程综合", "concurrency_review"); }

    // 数据库
    private Question genJdbcConnection() { return genericQuestion("JDBC 连接数据库", "jdbc"); }
    private Question genSqlCrud() { return genericQuestion("SQL CRUD 操作", "sql_crud"); }
    private Question genPreparedStatement() { return genericQuestion("PreparedStatement 防SQL注入", "prepared"); }
    private Question genMybatisMapping() { return genericQuestion("MyBatis 映射配置", "mybatis_mapping"); }
    private Question genMybatisCrud() { return genericQuestion("MyBatis CRUD 操作", "mybatis_crud"); }
    private Question genMybatisConfig() { return genericQuestion("MyBatis 核心配置", "mybatis_config"); }
    private Question genDynamicSql() { return genericQuestion("MyBatis 动态 SQL", "dynamic_sql"); }
    private Question genMybatisCache() { return genericQuestion("MyBatis 缓存机制", "mybatis_cache"); }
    private Question genMybatisRelation() { return genericQuestion("MyBatis 关联查询", "mybatis_relation"); }
    private Question genDbMybatisReview() { return genericQuestion("数据库+MyBatis 综合", "db_mybatis"); }

    // SpringBoot
    private Question genSpringDi() { return genericQuestion("Spring DI 依赖注入", "spring_di"); }
    private Question genBeanLifecycle() { return genericQuestion("Bean 生命周期", "bean_lifecycle"); }
    private Question genConfiguration() { return genericQuestion("@Configuration 与 @Bean", "configuration"); }
    private Question genRestController() { return genericQuestion("@RestController 使用", "rest_controller"); }
    private Question genRequestMapping() { return genericQuestion("@RequestMapping 映射", "request_mapping"); }
    private Question genJsonResponse() { return genericQuestion("@ResponseBody 与 JSON", "json_response"); }
    private Question genJpaEntity() { return genericQuestion("JPA 实体映射", "jpa_entity"); }
    private Question genRepository() { return genericQuestion("JpaRepository 使用", "repository"); }
    private Question genTransaction() { return genericQuestion("@Transactional 事务管理", "transaction"); }
    private Question genSpringbootReview() { return genericQuestion("SpringBoot 综合复习", "springboot_review"); }

    // Redis + MongoDB
    private Question genRedisBasic() { return genericQuestion("Redis 基础与数据结构", "redis_basic"); }
    private Question genRedisJava() { return genericQuestion("Jedis/RedisTemplate 操作", "redis_java"); }
    private Question genRedisDataTypes() { return genericQuestion("Redis 五种数据类型", "redis_types"); }
    private Question genMongoDbBasic() { return genericQuestion("MongoDB 基础操作", "mongodb_basic"); }
    private Question genMongoDbQuery() { return genericQuestion("MongoDB 查询语法", "mongodb_query"); }
    private Question genMongoDbIndex() { return genericQuestion("MongoDB 索引", "mongodb_index"); }
    private Question genNosqlReview() { return genericQuestion("NoSQL 综合复习", "nosql_review"); }

    // MQ + ES
    private Question genMqConcept() { return genericQuestion("消息队列核心概念", "mq_concept"); }
    private Question genRabbitmqBasic() { return genericQuestion("RabbitMQ 交换机与队列", "rabbitmq"); }
    private Question genKafkaBasic() { return genericQuestion("Kafka 分区与偏移量", "kafka"); }
    private Question genEsIndexing() { return genericQuestion("Elasticsearch 索引管理", "es_indexing"); }
    private Question genEsSearch() { return genericQuestion("Elasticsearch 搜索语法", "es_search"); }
    private Question genEsAggregation() { return genericQuestion("Elasticsearch 聚合", "es_aggregation"); }
    private Question genMiddlewareReview() { return genericQuestion("中间件综合复习", "middleware_review"); }

    // Netty
    private Question genNettyArchitecture() { return genericQuestion("Netty 线程模型架构", "netty_arch"); }
    private Question genEventloop() { return genericQuestion("EventLoop 事件循环", "eventloop"); }
    private Question genBootstrap() { return genericQuestion("ServerBootstrap 配置", "bootstrap"); }
    private Question genNettyHandler() { return genericQuestion("ChannelHandler 处理器", "netty_handler"); }
    private Question genPipeline() { return genericQuestion("ChannelPipeline 职责链", "pipeline"); }
    private Question genCodec() { return genericQuestion("Netty 编解码器 Codec", "codec"); }
    private Question genNettyAdvanced() { return genericQuestion("Netty 高级特性", "netty_advanced"); }
    private Question genNettyReview() { return genericQuestion("Netty 综合复习", "netty_review"); }

    // Java8+ + JVM
    private Question genLambdaExpr() { return genericQuestion("Lambda 表达式语法", "lambda"); }
    private Question genStreamApi() { return genericQuestion("Stream API 操作", "stream"); }
    private Question genOptionalUsage() { return genericQuestion("Optional 空值处理", "optional"); }
    private Question genJvmMemory() { return genericQuestion("JVM 内存区域划分", "jvm_memory"); }
    private Question genGcBasics() { return genericQuestion("GC 垃圾回收机制", "gc"); }
    private Question genClassLoading() { return genericQuestion("类加载机制与双亲委派", "class_loading"); }
    private Question genJvmTuning() { return genericQuestion("JVM 调优参数", "jvm_tuning"); }
    private Question genProfiling() { return genericQuestion("性能分析 Profiling", "profiling"); }
    private Question genMemoryAnalysis() { return genericQuestion("内存泄露分析", "memory_analysis"); }
    private Question genJava8JvmReview() { return genericQuestion("Java8+ 与 JVM 综合", "java8_jvm"); }

    // 数据结构和算法
    private Question genSortingAlgo() { return genericQuestion("排序算法（冒泡/快排）", "sorting"); }
    private Question genSearchAlgo() { return genericQuestion("二分查找算法", "binary_search"); }
    private Question genTwoPointer() { return genericQuestion("双指针技巧", "two_pointer"); }
    private Question genLinkedListDs() { return genericQuestion("链表反转与操作", "linkedlist_ds"); }
    private Question genStackQueue() { return genericQuestion("栈和队列的实现", "stack_queue"); }
    private Question genRecursionBasic() { return genericQuestion("递归思想与实现", "recursion"); }
    private Question genTreeTraversal() { return genericQuestion("二叉树遍历（前/中/后序）", "tree"); }
    private Question genGraphBasic() { return genericQuestion("图的表示与遍历", "graph"); }
    private Question genBfsDfs() { return genericQuestion("BFS/DFS 搜索", "bfs_dfs"); }
    private Question genDpBasic() { return genericQuestion("动态规划入门", "dp"); }
    private Question genGreedy() { return genericQuestion("贪心算法", "greedy"); }
    private Question genBacktracking() { return genericQuestion("回溯算法", "backtracking"); }
    private Question genAlgorithmReview() { return genericQuestion("算法综合复习", "algorithm_review"); }

    // 设计模式
    private Question genSingleton() { return genericQuestion("单例模式（多种实现）", "singleton"); }
    private Question genFactory() { return genericQuestion("工厂模式", "factory"); }
    private Question genBuilderPattern() { return genericQuestion("Builder 构建者模式", "builder"); }
    private Question genAdapter() { return genericQuestion("适配器模式", "adapter"); }
    private Question genProxyPattern() { return genericQuestion("代理模式（静态/动态）", "proxy"); }
    private Question genDecorator() { return genericQuestion("装饰器模式", "decorator"); }
    private Question genObserver() { return genericQuestion("观察者模式", "observer"); }
    private Question genStrategy() { return genericQuestion("策略模式", "strategy"); }
    private Question genTemplateMethod() { return genericQuestion("模板方法模式", "template"); }
    private Question genPatternReview() { return genericQuestion("设计模式综合复习", "pattern_review"); }
    private Question genPatternIdentification() { return genericQuestion("设计模式识别与对比", "pattern_id"); }

    // 计算机基础 + Maven
    private Question genOsProcess() { return genericQuestion("进程与线程的区别", "os_process"); }
    private Question genOsMemory() { return genericQuestion("内存管理（分页/分段）", "os_memory"); }
    private Question genOsThread() { return genericQuestion("线程调度与上下文切换", "os_thread"); }
    private Question genNetworkProtocol() { return genericQuestion("网络协议分层（OSI/TCP/IP）", "network_protocol"); }
    private Question genTcpIp() { return genericQuestion("TCP 三次握手四次挥手", "tcp_ip"); }
    private Question genHttpHttps() { return genericQuestion("HTTP/HTTPS 与状态码", "http_https"); }
    private Question genMavenLifecycle() { return genericQuestion("Maven 生命周期（clean/default/site）", "maven_lifecycle"); }
    private Question genDependencyManagement() { return genericQuestion("Maven 依赖管理（传递/排除）", "maven_dep"); }
    private Question genMavenPlugin() { return genericQuestion("Maven 插件配置", "maven_plugin"); }
    private Question genEngineeringReview() { return genericQuestion("工程化综合复习", "engineering_review"); }

    // 项目实战
    private Question genProjectDesign() { return genericQuestion("项目架构设计思路", "project_design"); }
    private Question genApiDesign() { return genericQuestion("RESTful API 设计规范", "api_design"); }
    private Question genErrorHandling() { return genericQuestion("全局异常处理设计", "error_handling"); }
    private Question genHighConcurrency() { return genericQuestion("高并发场景处理", "high_concurrency"); }
    private Question genCacheStrategy() { return genericQuestion("缓存策略（旁路/穿透/击穿）", "cache"); }
    private Question genAsyncProcessing() { return genericQuestion("异步处理方案", "async"); }
    private Question genMicroservice() { return genericQuestion("微服务架构设计", "microservice"); }
    private Question genDistributedSystem() { return genericQuestion("分布式系统理论（CAP/BASE）", "distributed"); }
    private Question genRpcBasic() { return genericQuestion("RPC 原理与实现", "rpc"); }
    private Question genFullstackReview() { return genericQuestion("全栈综合实践复习", "fullstack"); }

    // 面试冲刺
    private Question genCoreJavaInterview() { return genericQuestion("Java 核心面试题", "core_interview"); }
    private Question genOopInterview() { return genericQuestion("OOP 面试高频题", "oop_interview"); }
    private Question genCollectionInterview() { return genericQuestion("集合框架面试题", "collection_interview"); }
    private Question genSpringInterview() { return genericQuestion("Spring 面试高频题", "spring_interview"); }
    private Question genDbInterview() { return genericQuestion("数据库面试题", "db_interview"); }
    private Question genMqInterview() { return genericQuestion("消息队列面试题", "mq_interview"); }
    private Question genSystemDesign() { return genericQuestion("系统设计面试", "system_design"); }
    private Question genScalability() { return genericQuestion("可扩展性设计", "scalability"); }
    private Question genArchitecture() { return genericQuestion("架构设计题", "architecture"); }
    private Question genAlgorithmCoding() { return genericQuestion("手撕算法题", "algorithm_coding"); }
    private Question genProblemSolving() { return genericQuestion("解题思路训练", "problem_solving"); }
    private Question genMockInterview() { return genericQuestion("模拟面试", "mock_interview"); }
    private Question genFullReview() { return genericQuestion("1000天全程回顾", "full_review"); }

    /** 通用题型模板生成器（为远期的题型提供标准化题目） */
    private Question genericQuestion(String topicName, String typeId) {
        String[] patterns = {
            "概念理解题",
            "代码分析题",
            "应用场景题"
        };
        int pattern = randInt(0, 2);
        return switch (pattern) {
            case 0 -> Question.of(
                topicName + " — 概念理解",
                "请解释 " + topicName + " 的核心概念。\n\n" +
                "**问题：**\n1. 什么是 " + topicName + "？它解决了什么问题？\n2. 在 Java 中如何实现/使用它？\n3. 使用时有什么注意事项或常见陷阱？",
                "从「是什么、为什么、怎么用」三个角度思考",
                "**参考答案：**\n\n" +
                "1. **概念：** " + topicName + " 是 Java " + getPhaseByType(typeId) + " 中的核心机制\n" +
                "2. **实现方式：** 参考你当天学习的内容和代码示例\n" +
                "3. **注意事项：**\n" +
                "   - 理解原理比死记语法更重要\n" +
                "   - 多写代码实践，结合实际场景\n" +
                "   - 关注边界情况和异常处理\n\n" +
                "💡 建议：打开你当天学习的代码文件，动手改改参数看效果。"
            );
            case 1 -> Question.of(
                topicName + " — 代码分析",
                "请阅读你当天编写的 " + topicName + " 相关代码，思考以下问题：\n\n" +
                "1. 代码中哪些地方容易出错？\n" +
                "2. 如果输入数据发生变化（如边界值、空值），程序会怎样？\n" +
                "3. 能否优化这段代码？如何让它更健壮、更高效？",
                "试试「破坏性测试」——故意传入非法参数，观察程序行为",
                "**分析框架：**\n\n" +
                "1. **潜在风险点：**\n" +
                "   - 是否有未处理的边界情况？\n" +
                "   - 是否存在空指针或索引越界风险？\n" +
                "2. **改进建议：**\n" +
                "   - 增加输入校验\n" +
                "   - 使用 Optional 或 try-catch 处理异常情况\n" +
                "   - 提取重复代码为独立方法\n" +
                "3. **性能考量：**\n" +
                "   - 时间复杂度是否能优化？\n" +
                "   - 是否存在不必要的对象创建？"
            );
            default -> Question.of(
                topicName + " — 场景应用题",
                "**实际开发场景：**\n\n" +
                "假设你正在开发一个" + getScenarioByType(typeId) + "，需要用 " + topicName + " 来实现。\n\n" +
                "请回答：\n" +
                "1. 你会如何设计这个功能？\n" +
                "2. 选择 " + topicName + " 的理由是什么？\n" +
                "3. 有哪些替代方案？你的选择相比它们有什么优势？",
                "没有标准答案，重点是思考过程和设计取舍",
                "**设计思路参考：**\n\n" +
                "1. **需求分析：** 明确功能的核心需求和约束条件\n" +
                "2. **技术选型：** " + topicName + " 适用于这个场景的原因是……\n" +
                "3. **实现步骤：**\n" +
                "   - 第一步：搭建基础结构\n" +
                "   - 第二步：实现核心逻辑\n" +
                "   - 第三步：处理边界情况和异常\n" +
                "4. **替代方案对比：**\n" +
                "   - 方案A：……（优点/缺点）\n" +
                "   - 方案B：……（优点/缺点）\n\n" +
                "💡 思考比答案更重要，试着跟AI讨论你的设计！"
            );
        };
    }

    private String getPhaseByType(String typeId) {
        if (typeId.contains("_interview")) return "面试准备";
        if (typeId.contains("design") || typeId.contains("archi")) return "系统设计";
        if (typeId.contains("singleton") || typeId.contains("factory") || typeId.contains("pattern")) return "设计模式";
        if (typeId.contains("sort") || typeId.contains("tree") || typeId.contains("dp_") || typeId.contains("algo")) return "数据结构与算法";
        return "学习路线";
    }

    private String getScenarioByType(String typeId) {
        String[] scenarios = {
            "电商系统的订单模块",
            "社交媒体的用户系统",
            "在线教育平台",
            "支付网关服务",
            "实时数据处理管道",
            "API 网关服务",
            "配置管理中心",
            "日志收集系统"
        };
        return scenarios[Math.abs(typeId.hashCode()) % scenarios.length];
    }

    // ============================================================
    //  8. 核心逻辑：生成当天的题目
    // ============================================================

    public void generate(int day, Roadmap roadmap, String outputDir) throws IOException {
        // 查找今天所属的阶段和主题
        Phase currentPhase = null;
        Topic currentTopic = null;
        for (Phase p : roadmap.phases()) {
            if (day >= p.dayStart() && day <= p.dayEnd()) {
                currentPhase = p;
                for (Topic t : p.topics()) {
                    if (day >= t.dayStart() && day <= t.dayEnd()) {
                        currentTopic = t;
                        break;
                    }
                }
                break;
            }
        }

        if (currentPhase == null || currentTopic == null) {
            System.out.println("⚠️ 第 " + day + " 天不在路线图范围内（1~" + roadmap.totalDays() + "）");
            return;
        }

        int daysInPhase = day - currentPhase.dayStart();
        int phaseLength = currentPhase.dayEnd() - currentPhase.dayStart() + 1;
        int difficulty = calcDifficulty(day, currentPhase.dayStart(), currentPhase.dayEnd());

        System.out.println("📋 第 " + day + " 天 | " + currentPhase.emoji() + " " + currentPhase.name() + " | " + currentTopic.name());
        System.out.println("  难度: " + "★".repeat(Math.max(1, difficulty / 2)) + "☆".repeat(Math.max(0, 5 - difficulty / 2)));
        System.out.println();

        // 收集当天3道题
        List<Question> questions = new ArrayList<>();

        // 第1-2题：今日主题
        List<String> types = new ArrayList<>(currentTopic.questionTypes());
        for (int i = 0; i < 2 && !types.isEmpty(); i++) {
            String type = types.get(rand.nextInt(types.size()));
            Question q = generateQuestion(type, day);
            if (q != null) questions.add(q);
            types.remove(type); // 同一主题的题不重复
        }

        // 第3题：交叉复习（从之前学过的主题中选题）
        Topic reviewTopic = pickReviewTopic(roadmap, day, rand);
        if (reviewTopic != null && !reviewTopic.questionTypes().isEmpty()) {
            String reviewType = reviewTopic.questionTypes().get(rand.nextInt(reviewTopic.questionTypes().size()));
            Question reviewQ = generateQuestion(reviewType, day);
            if (reviewQ != null) {
                questions.add(Question.of(
                    "🔁 交叉复习：" + reviewTopic.name(),
                    reviewQ.description(),
                    reviewQ.hint(),
                    reviewQ.solution(),
                    reviewQ.difficulty()
                ));
            }
        }

        // 如果不够3题，补一道通用题
        while (questions.size() < 3) {
            String fallbackType = currentTopic.questionTypes().get(rand.nextInt(currentTopic.questionTypes().size()));
            Question q = generateQuestion(fallbackType, day);
            if (q != null) questions.add(q);
        }

        // 输出到控制台
        String output = formatOutput(day, currentPhase, currentTopic, difficulty, questions);
        System.out.println(output);

        // 保存到文件（题目和答案分开）
        String safeName = currentTopic.name().replaceAll("[/\\\\:*?\"<>|]", "_");
        Path questionPath = Path.of(outputDir, String.format("第%d天_%s_题目.md", day, safeName));
        Path answerPath = Path.of(outputDir, String.format("第%d天_%s_答案.md", day, safeName));
        Files.createDirectories(questionPath.getParent());

        String questionsOnly = formatQuestionsOnly(day, currentPhase, currentTopic, difficulty, questions);
        String answersOnly = formatAnswersOnly(day, currentPhase, currentTopic, difficulty, questions);

        Files.writeString(questionPath, questionsOnly);
        Files.writeString(answerPath, answersOnly);

        System.out.println("\n📝 题目: " + questionPath.toAbsolutePath());
        System.out.println("🔑 答案: " + answerPath.toAbsolutePath());
    }

    private Question generateQuestion(String type, int day) {
        Supplier<Question> gen = generators.get(type);
        if (gen == null) {
            // 如果找不到特定生成器，用通用模板
            return genericQuestion("今日主题", type);
        }
        return gen.get();
    }

    // ============================================================
    //  9. 格式化输出
    // ============================================================

    private String buildHeader(int day, Phase phase, Topic topic, int difficulty) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String starLevel = "★".repeat(Math.max(1, difficulty / 2)) + "☆".repeat(Math.max(0, 5 - difficulty / 2));
        StringBuilder sb = new StringBuilder();
        sb.append("# 第 ").append(day).append(" 天 — ").append(topic.name()).append("\n");
        sb.append("📅 ").append(dateStr).append(" | ").append(phase.emoji()).append(" ").append(phase.name());
        sb.append(" | 🎯 难度：").append(starLevel).append("\n\n");
        sb.append("---\n\n");
        return sb.toString();
    }

    private String buildFooter(int day, Phase phase, Topic topic) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n\n");
        sb.append("> 📊 **学习进度**: 第 ").append(day).append(" / 1000 天");
        sb.append(" (").append(String.format("%.1f", day * 100.0 / 1000)).append("%)\n");
        sb.append("> 📁 **今日主题**: ").append(phase.name()).append(" → ").append(topic.name()).append("\n");
        sb.append("> 💪 **继续加油！**\n");
        return sb.toString();
    }

    /** 只有题目（无提示无答案） */
    private String formatQuestionsOnly(int day, Phase phase, Topic topic, int difficulty, List<Question> questions) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildHeader(day, phase, topic, difficulty));
        sb.append("> ⚠️ 先做题，做完再看答案文件哦！\n\n");
        sb.append("---\n\n");

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            sb.append("## 【第").append(i + 1).append("题】").append(q.title()).append("\n\n");
            sb.append(q.description()).append("\n\n");
            if (i < questions.size() - 1) sb.append("---\n\n");
        }

        sb.append(buildFooter(day, phase, topic));
        return sb.toString();
    }

    /** 只有答案（对应题目编号） */
    private String formatAnswersOnly(int day, Phase phase, Topic topic, int difficulty, List<Question> questions) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildHeader(day, phase, topic, difficulty));
        sb.append("> 🔑 第 ").append(day).append(" 天题目答案（先做完再看！）\n\n");
        sb.append("---\n\n");

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            sb.append("## 【第").append(i + 1).append("题】").append(q.title()).append("\n\n");
            sb.append("### 💡 提示\n\n").append(q.hint()).append("\n\n");
            sb.append("### ✅ 答案与解析\n\n").append(q.solution()).append("\n\n");
            if (i < questions.size() - 1) sb.append("---\n\n");
        }

        sb.append(buildFooter(day, phase, topic));
        return sb.toString();
    }

    /** 完整版（题目+折叠答案，控制台输出用） */
    private String formatOutput(int day, Phase phase, Topic topic, int difficulty, List<Question> questions) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildHeader(day, phase, topic, difficulty));

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            sb.append("## 【第").append(i + 1).append("题】").append(q.title()).append("\n\n");
            sb.append(q.description()).append("\n\n");
            sb.append("<details>\n<summary>💡 提示</summary>\n\n").append(q.hint()).append("\n</details>\n\n");
            sb.append("<details>\n<summary>✅ 点击查看答案与解析</summary>\n\n").append(q.solution()).append("\n</details>\n\n");
            if (i < questions.size() - 1) sb.append("---\n\n");
        }

        sb.append(buildFooter(day, phase, topic));
        return sb.toString();
    }

    // ============================================================
    //  10. 主入口
    // ============================================================

    public static void main(String[] args) throws IOException {
        String baseDir = System.getProperty("user.dir");
        String roadmapPath = baseDir + "/roadmap.json";
        String outputDir = baseDir + "/output";

        // 检查 roadmap 是否存在
        if (!Files.exists(Path.of(roadmapPath))) {
            // 从常见位置查找 roadmap.json
            String[] altPaths = {
                baseDir + "/这一次一定认真学Java/daily-exercise/roadmap.json",
                baseDir + "/daily-exercise/roadmap.json",
                Path.of(baseDir).getParent() + "/roadmap.json"
            };
            for (String alt : altPaths) {
                if (Files.exists(Path.of(alt))) {
                    roadmapPath = alt;
                    // 修正 outputDir 到 roadmap 同目录下的 output/
                    Path roadDir = Path.of(alt).getParent();
                    if (roadDir != null) outputDir = roadDir + "/output";
                    break;
                }
            }
            if (!Files.exists(Path.of(roadmapPath))) {
                System.err.println("❌ 找不到 roadmap.json！");
                System.err.println("   请在 daily-exercise/ 目录下运行");
                System.exit(1);
            }
        }

        // 加载路线图
        Roadmap roadmap = loadRoadmap(roadmapPath);
        System.out.println("📚 " + roadmap.title());
        System.out.println("📖 " + roadmap.description());
        System.out.println();

        // 计算当前天数
        int day = calculateDay(args, roadmap, outputDir);
        day = Math.max(1, Math.min(day, roadmap.totalDays()));

        // 生成题目
        Random rand = new Random();
        DailyExerciseGenerator generator = new DailyExerciseGenerator(rand);
        generator.generate(day, roadmap, outputDir);
    }
}
