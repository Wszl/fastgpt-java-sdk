package org.xdove.thridpart.fastgpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import lombok.NonNull;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.async.methods.*;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.apache.hc.core5.io.CloseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdove.thridpart.fastgpt.entity.ChatMessage;
import org.xdove.thridpart.fastgpt.entity.PushData;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;

public class ServiceRequests {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequests.class);
    private final CloseableHttpClient client;

    private final CloseableHttpAsyncClient asyncClient;
    private final Config config;
    private final RequestConfig requestConfig;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    
    /** 对话接口 */
    public static final String PATH_CHAT_COMPLETIONS = "/api/v1/chat/completions";
    /** 创建一个知识库  */
    public static final String PATH_DATESET_CREATE = "/api/core/dataset/create";
    /** 获取知识库详情  */
    public static final String PATH_DATESET_GET = "/api/core/dataset/detail";
    /** 获取知识库列表  */
    public static final String PATH_DATESET_LIST = "/api/core/dataset/list";
    /** 删除知识库  */
    public static final String PATH_DATESET_DELETE = "/api/core/dataset/delete";
    /** 创建一个空的集合   */
    public static final String PATH_COLLECTION_CREATE = "/api/core/dataset/collection/create";
    /** 创建一个纯文本集合   */
    public static final String PATH_TEXT_COLLECTION_CREATE = "/api/core/dataset/collection/create/text";
    /** 创建一个链接集合   */
    public static final String PATH_LINK_COLLECTION_CREATE = "/api/core/dataset/collection/create/link";
    /** 获取集合详情   */
    public static final String PATH_COLLECTION_GET = "/api/core/dataset/collection/detail";
    /** 获取集合列表   */
    public static final String PATH_COLLECTION_LIST = "/api/core/dataset/collection/list";
    /** 修改集合信息   */
    public static final String PATH_COLLECTION_PUT = "/api/core/dataset/collection/update";
    /** 删除一个集合   */
    public static final String PATH_COLLECTION_DELETE = "/api/core/dataset/collection/delete";
    /** 为集合批量添加添加数据    */
    public static final String PATH_PUSH_DATA = "/api/core/dataset/data/pushData";
    /** 获取集合的数据列表    */
    public static final String PATH_DATA_LIST = "/api/core/dataset/data/list";
    /** 获取单条数据详情    */
    public static final String PATH_DATA_GET = "/api/core/dataset/data/detail";
    /** 修改单条数据    */
    public static final String PATH_DATA_UPDATE = "/api/core/dataset/data/update";
    /** 删除单条数据    */
    public static final String PATH_DATA_DELETE = "/api/core/dataset/data/delete";
    /** 搜索测试    */
    public static final String PATH_DATASET_SEARCH_TEST = "/api/core/dataset/searchTest";



    public ServiceRequests(Config config) {
        this(HttpClientBuilder.create().build(),
                HttpAsyncClients.createDefault(),
             RequestConfig.DEFAULT, config);
    }

    public ServiceRequests(CloseableHttpClient client, CloseableHttpAsyncClient asyncClient, RequestConfig requestConfig, Config config) {
        this.client = client;
        this.asyncClient = asyncClient;
        this.config = config;
        this.requestConfig = requestConfig;
    }

    public void destroy() {
        client.close(CloseMode.GRACEFUL);
    }

    /**
     * 对话
     * @param chatId 为 undefined 时（不传入），不使用 FastGpt 提供的上下文功能，完全通过传入的 messages 构建上下文。 不会将你的记录存储到数据库中，你也无法在记录汇总中查阅到。
     *               为非空字符串时，意味着使用 chatId 进行对话，自动从 FastGpt 数据库取历史记录，并使用 messages 数组最后一个内容作为用户问题。请自行确保 chatId 唯一，长度小于250，通常可以是自己系统的对话框ID。
     * @param detail 是否返回中间值（模块状态，响应的完整结果等），stream模式下会通过event进行区分，非stream模式结果保存在responseData中。
     * @param variables 模块变量，一个对象，会替换模块中，输入框内容里的{{key}}
     * @param messages  结构与 GPT接口 chat模式一致。
     * @return
     */
    public Map<String, Object> chatCompletions(String chatId, boolean detail, Map<String, String> variables,
                                               List<ChatMessage> messages) {
        if(log.isDebugEnabled()) {
            log.debug("request chatCompletions chatId={}, stream={}, detail={}, variables={}, messages={}",
                    chatId, false, detail, variables, messages);
        }
        HashMap<String, Object> param = new HashMap<>();
        param.put("chatId", chatId);
        param.put("stream", false);
        param.put("detail", detail);
        param.put("variables", variables);
        param.put("messages", messages);

        try {
            String e = this.postRequest(PATH_CHAT_COMPLETIONS, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 流式对话
     * @param chatId 为 undefined 时（不传入），不使用 FastGpt 提供的上下文功能，完全通过传入的 messages 构建上下文。 不会将你的记录存储到数据库中，你也无法在记录汇总中查阅到。
     *               为非空字符串时，意味着使用 chatId 进行对话，自动从 FastGpt 数据库取历史记录，并使用 messages 数组最后一个内容作为用户问题。请自行确保 chatId 唯一，长度小于250，通常可以是自己系统的对话框ID。
     * @param detail 是否返回中间值（模块状态，响应的完整结果等），stream模式下会通过event进行区分，非stream模式结果保存在responseData中。
     * @param variables 模块变量，一个对象，会替换模块中，输入框内容里的{{key}}
     * @param messages  结构与 GPT接口 chat模式一致。
     */
    public InputStream chatCompletionsStream(String chatId, boolean detail, Map<String, String> variables,
                                List<ChatMessage> messages) {
        if(log.isDebugEnabled()) {
            log.debug("request chatCompletionsStream chatId={}, stream={}, detail={}, variables={}, messages={}",
                    chatId, true, detail, variables, messages);
        }
        HashMap<String, Object> param = new HashMap<>();
        param.put("chatId", chatId);
        param.put("stream", true);
        param.put("detail", detail);
        param.put("variables", variables);
        param.put("messages", messages);

        try {
            return this.postSSERequest(PATH_CHAT_COMPLETIONS, param);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建一个知识库
     * @param parentId 父级ID，用于构建目录结构。通常可以为 null 或者直接不传。
     * @param type dataset或者folder，代表普通知识库和文件夹。不传则代表创建普通知识库。
     * @param name 知识库名（必填）
     * @param intro 介绍（可选）
     * @param avatar 头像地址（可选）
     * @param vectorModel 向量模型（建议传空，用系统默认的）
     * @param agentModel 文本处理模型（建议传空，用系统默认的）
     * @return
     */
    public Map<String, Object> createDataset(Long parentId, String type, @NonNull String name, String intro,
                                             String avatar, String vectorModel, String agentModel) {
        if(log.isDebugEnabled()) {
            log.debug("request createDataset parentId={}, type={}, name={}, intro={}, avatar={}, vectorModel={}, agentModel={} ",
                    parentId, type, name, intro, avatar, vectorModel, agentModel);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("parentId", parentId);
        param.put("type", type);
        param.put("name", name);
        param.put("intro", intro);
        param.put("avatar", avatar);
        param.put("vectorModel", vectorModel);
        param.put("agentModel", agentModel);

        try {
            String e = this.postRequest(PATH_DATESET_CREATE, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取知识库列表
     * @param parentId 父级ID，不传或为空，代表获取根目录下的知识库
     * @return
     */
    public Map<String, Object> listDataset(String parentId) {
        if(log.isDebugEnabled()) {
            log.debug("request listDataset parentId={}", parentId);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("parentId", parentId);

        try {
            String e = this.getRequest(PATH_DATESET_LIST, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取知识库详情
     * @param datasetId 知识库id
     * @return
     */
    public Map<String, Object> getDataset(@NonNull String datasetId) {
        if(log.isDebugEnabled()) {
            log.debug("request getDataset datasetId={}", datasetId);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("id", datasetId);

        try {
            String e = this.getRequest(PATH_DATESET_GET, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除知识库
     * @param datasetId 知识库id
     * @return
     */
    public Map<String, Object> deleteDataset(@NonNull String datasetId) {
        if(log.isDebugEnabled()) {
            log.debug("request deleteDataset datasetId={}", datasetId);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("id", datasetId);

        try {
            String e = this.deleteRequest(PATH_DATESET_DELETE, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建一个空的集合
     * @param datasetId 知识库的ID(必填)
     * @param parentId 父级ID，不填则默认为根目录
     * @param name 集合名称（必填）
     * @param type folder：文件夹 virtual：虚拟集合(手动集合)
     * @param metadata 元数据（暂时没啥用）
     * @return
     */
    public Map<String, Object> createCollection(@NonNull String datasetId, String parentId, @NonNull String name, @NonNull String type,
                                                String metadata) {
        if(log.isDebugEnabled()) {
            log.debug("request createCollection datasetId={}, parentId={}, name={}, type={}, metadata={}", datasetId, parentId, name, type, metadata);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("datasetId", datasetId);
        param.put("parentId", parentId);
        param.put("name", name);
        param.put("type", type);
        param.put("metadata", metadata);

        try {
            String e = this.postRequest(PATH_COLLECTION_CREATE, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建一个纯文本集合
     * @param text 原文本
     * @param datasetId 知识库的ID(必填)
     * @param parentId 父级ID，不填则默认为根目录
     * @param name 集合名称（必填）
     * @param metadata 元数据（暂时没啥用）
     * @param trainingType （必填）  chunk: 按文本长度进行分割 qa: QA拆分
     * @param chunkSize 每个 chunk 的长度（可选）. chunk模式:100~3000; qa模式: 4000~模型最大token（16k模型通常建议不超过10000）
     * @param chunkSplitter 自定义最高优先分割符号（可选）
     * @param qaPrompt qa拆分自定义提示词（可选）
     * @return
     */
    public Map<String, Object> createTextCollection(@NonNull String text, @NonNull String datasetId, String parentId, @NonNull String name,
                                                    String metadata, @NonNull String trainingType, int chunkSize, String chunkSplitter,
                                                    String qaPrompt) {
        if(log.isDebugEnabled()) {
            log.debug("request createTextCollection text={}, datasetId={}, parentId={}, name={}, metadata={}, trainingType={}," +
                    "chunkSize={}, chunkSplitter={}, qaPrompt={}", text, datasetId, parentId, name, metadata, trainingType,
                    chunkSize, chunkSplitter, qaPrompt);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("text", text);
        param.put("datasetId", datasetId);
        param.put("parentId", parentId);
        param.put("name", name);
        param.put("metadata", metadata);
        param.put("trainingType", trainingType);
        param.put("chunkSize", chunkSize);
        param.put("chunkSplitter", chunkSplitter);
        param.put("qaPrompt", qaPrompt);

        try {
            String e = this.postRequest(PATH_TEXT_COLLECTION_CREATE, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建一个链接集合
     * @param link 网络链接
     * @param datasetId 知识库的ID(必填)
     * @param parentId 父级ID，不填则默认为根目录
     * @param metadata metadata.webPageSelector: 网页选择器，用于指定网页中的哪个元素作为文本(可选)
     * @param trainingType （必填）  chunk: 按文本长度进行分割 qa: QA拆分
     * @param chunkSize 每个 chunk 的长度（可选）. chunk模式:100~3000; qa模式: 4000~模型最大token（16k模型通常建议不超过10000）
     * @param chunkSplitter 自定义最高优先分割符号（可选）
     * @param qaPrompt qa拆分自定义提示词（可选）
     * @return
     */
    public Map<String, Object> createLinkCollection(@NonNull String link, @NonNull String datasetId, String parentId,
                                                    String metadata, @NonNull String trainingType, int chunkSize, String chunkSplitter,
                                                    String qaPrompt) {
        if(log.isDebugEnabled()) {
            log.debug("request createLinkCollection link={}, datasetId={}, parentId={}, metadata={}, trainingType={}," +
                            "chunkSize={}, chunkSplitter={}, qaPrompt={}", link, datasetId, parentId, metadata, trainingType,
                    chunkSize, chunkSplitter, qaPrompt);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("link", link);
        param.put("datasetId", datasetId);
        param.put("parentId", parentId);
        param.put("metadata", metadata);
        param.put("trainingType", trainingType);
        param.put("chunkSize", chunkSize);
        param.put("chunkSplitter", chunkSplitter);
        param.put("qaPrompt", qaPrompt);

        try {
            String e = this.postRequest(PATH_LINK_COLLECTION_CREATE, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取集合详情
     * @param collectionId 集合id
     * @return
     */
    public Map<String, Object> getCollection(@NonNull String collectionId) {
        if(log.isDebugEnabled()) {
            log.debug("request getCollection collectionId={}", collectionId);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("id", collectionId);

        try {
            String e = this.getRequest(PATH_COLLECTION_GET, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取集合列表
     * @param datasetId 知识库的ID(必填)
     * @param pageNum 页码（选填）
     * @param pageSize 每页数量，最大30（选填）
     * @param parentId 父级Id（选填）
     * @param searchText 模糊搜索文本（选填）
     * @return
     */
    public Map<String, Object> listCollection(@NonNull String datasetId, int pageNum, int pageSize, String parentId, String searchText) {
        if(log.isDebugEnabled()) {
            log.debug("request listCollection datasetId={}, pageNum={}, pageSize={}, parentId={}, searchText={}",
                    datasetId, pageNum, pageSize, parentId, searchText);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("datasetId", datasetId);
        param.put("pageNum", pageNum);
        param.put("pageSize", pageSize);
        param.put("parentId", parentId);
        param.put("searchText", searchText);

        try {
            String e = this.getRequest(PATH_COLLECTION_LIST, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 修改集合信息
     * @param collectionId 集合的ID
     * @param parentId 修改父级ID（可选）
     * @param name 修改集合名称（可选）
     * @return
     */
    public Map<String, Object> updateCollection(@NonNull String collectionId, String parentId, String name) {
        if(log.isDebugEnabled()) {
            log.debug("request updateCollection collectionId={}, parentId={}, name={}", collectionId, parentId, name);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("id", collectionId);
        param.put("parentId", parentId);
        param.put("name", name);

        try {
            String e = this.putRequest(PATH_COLLECTION_PUT, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除一个集合
     * @param collectionId 集合的ID
     * @return
     */
    public Map<String, Object> deleteCollection(@NonNull String collectionId) {
        if(log.isDebugEnabled()) {
            log.debug("request deleteCollection collectionId={}", collectionId);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("id", collectionId);

        try {
            String e = this.deleteRequest(PATH_COLLECTION_DELETE, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 为集合批量添加添加数据
     * @param collectionId 集合ID（必填）
     * @param trainingType （必填） chunk: 按文本长度进行分割  qa: QA拆分
     * @param prompt 自定义 QA 拆分提示词，需严格按照模板，建议不要传入。（选填）
     * @param data （具体数据）  q: 主要数据（必填） a: 辅助数据（选填） indexes: 自定义索引（选填），不传入则默认使用q和a构建索引。也可以传入
     * @return
     */
    public Map<String, Object> pushData(@NonNull String collectionId, @NonNull String trainingType, String prompt,
                                        @NonNull List<PushData> data) {
        if(log.isDebugEnabled()) {
            log.debug("request pushData collectionId={}, trainingType={},  prompt={}, data={}",
                    collectionId, trainingType, prompt, data);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("collectionId", collectionId);
        param.put("trainingType", trainingType);
        param.put("prompt", prompt);
        param.put("data", data);

        try {
            String e = this.postRequest(PATH_PUSH_DATA, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取集合的数据列表
     * @param collectionId 集合的ID（必填）
     * @param pageNum 页码（选填）
     * @param pageSize 每页数量，最大30（选填）
     * @param searchText 模糊搜索文本（选填）
     * @return
     */
    public Map<String, Object> listData(@NonNull String collectionId, int pageNum, int pageSize, String searchText) {
        if(log.isDebugEnabled()) {
            log.debug("request listData collectionId={}, pageNum={}, pageSize={}, searchText={}",
                    collectionId, pageNum, pageSize, searchText);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("collectionId", collectionId);
        param.put("pageNum", pageNum);
        param.put("pageSize", pageSize);
        param.put("searchText", searchText);

        try {
            String e = this.postRequest(PATH_DATA_LIST, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取单条数据详情
     * @param dataId 数据的id
     * @return
     */
    public Map<String, Object> getData(@NonNull String dataId) {
        if(log.isDebugEnabled()) {
            log.debug("request getData dataId={}", dataId);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("id", dataId);

        try {
            String e = this.getRequest(PATH_DATA_GET, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 修改单条数据
     * @param dataId 数据的id
     * @param q 主要数据（选填）
     * @param a 辅助数据（选填）
     * @param indexes 自定义索引（选填），类型参考为集合批量添加添加数据，建议直接不传。更新q,a后，如果有默认索引，则会直接更新默认索引。
     * @return
     */
    public Map<String, Object> updateData(@NonNull String dataId, String q, String a, String indexes) {
        if(log.isDebugEnabled()) {
            log.debug("request updateData dataId={}, q={}, a={}, indexes={}", dataId, q, a, indexes);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("id", dataId);
        param.put("q", q);
        param.put("a", a);
        param.put("indexes", indexes);

        try {
            String e = this.putRequest(PATH_DATA_UPDATE, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除单条数据
     * @param dataId 数据的id
     * @return
     */
    public Map<String, Object> deleteData(@NonNull String dataId) {
        if(log.isDebugEnabled()) {
            log.debug("request deleteData dataId={}", dataId);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("id", dataId);

        try {
            String e = this.deleteRequest(PATH_DATA_DELETE, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 搜索测试
     * @param datasetId 知识库ID
     * @param text 需要测试的文本
     * @param limit 最大 tokens 数量
     * @param similarity 最低相关度（0~1，可选）
     * @param searchMode 搜索模式：embedding | fullTextRecall | mixedRecall
     * @param usingReRank 使用重排
     * @return
     */
    public Map<String, Object> searchTestDataset(String datasetId, String text, int limit, int similarity, String searchMode, boolean usingReRank) {
        if(log.isDebugEnabled()) {
            log.debug("request searchTestDataset datasetId={}, text={}, limit={}, similarity={}, searchMode={}, usingReRank={}",
                    datasetId, text, limit, similarity, searchMode, usingReRank);
        }

        HashMap<String, Object> param = new HashMap<>();
        param.put("datasetId", datasetId);
        param.put("text", text);
        param.put("limit", limit);
        param.put("similarity", similarity);
        param.put("searchMode", searchMode);
        param.put("usingReRank", usingReRank);

        try {
            String e = this.postRequest(PATH_DATASET_SEARCH_TEST, param);
            return str2Map(e);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    private String combPath(String path) {
        return this.config.getApiUrl() + path;
    }

    private HttpEntity combBody(Map<String, Object> p) throws UnsupportedEncodingException, JsonProcessingException {
        p.entrySet().removeIf((e) -> Objects.isNull(e.getValue()));
        return new StringEntity(jsonMapper.writeValueAsString(p), Charset.forName(config.getCharset()));
    }

    private String postRequest(String path, Map<String, Object> p) {
        String ret;
        try {
            String url = this.combPath(path);
            HttpEntity body = this.combBody(p);
            HttpPost post = new HttpPost(url);
            post.setEntity(body);
            ret = handleResp(doRequest(path, post));
        } catch (Exception e) {
            log.info("path=[{}], params=[{}] error.", path, p, e);
            throw new RuntimeException(e);
        }

        return ret;
    }

    private InputStream postSSERequest(String path, Map<String, Object> p) {

        try {
            String url = this.combPath(path);
            HttpEntity body = this.combBody(p);
            HttpPost post = new HttpPost(url);
            post.setEntity(body);
            return handleStream(doRequest(path, post));
        } catch (Exception e) {
            log.info("path=[{}], params=[{}] error.", path, p, e);
            throw new RuntimeException(e);
        }

    }


    private CloseableHttpResponse doRequest(String path, HttpUriRequestBase method) throws IOException, ParseException {
        CloseableHttpResponse response = null;

        String ret;
        try {
            method.setConfig(this.requestConfig);
            method.addHeader("Content-Type","application/json");
            if(Objects.nonNull(this.config.getKey())) {
                if (Objects.equals(path,PATH_CHAT_COMPLETIONS)) {
                    method.setHeader("Authorization", "Bearer " + this.config.getChatKey());
                } else {
                    method.setHeader("Authorization", "Bearer " + this.config.getKey());
                }
            }

            if(log.isDebugEnabled()) {
                log.debug("{} request path=[{}], headers=[{}]]", method.getMethod(), method.getRequestUri(), method.getHeaders());
            }

            response = this.client.execute(method);
        } catch (Exception e) {
            log.info("path=[{}], params=[{}] error.", path, readContent(method.getEntity(), config.getCharset()), e);
            throw new RuntimeException(e);
        }

        return response;
    }

    private String handleResp(CloseableHttpResponse response)  {
        String respContent = null;
        try {
            respContent = this.readContent(response.getEntity(),
                    Objects.isNull(response.getEntity().getContentEncoding())?this.config.getCharset():response.getEntity().getContentEncoding());
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if(Objects.nonNull(response)) {
                    EntityUtils.consume(response.getEntity());
                }
            } catch (IOException e) {
                log.error("error={}", e.getLocalizedMessage(), e);
            }

        }
        if(log.isDebugEnabled()) {
            log.debug(" response status=[{}] content=[{}]", response.getCode(), respContent);
        }

        return respContent;
    }

    private InputStream handleStream(CloseableHttpResponse response) throws IOException {
        return response.getEntity().getContent();
    }


    private String combParams(Map<String, Object> p) {
        p.entrySet().removeIf((e) -> Objects.isNull(e.getValue()));
        StringBuilder sb = new StringBuilder("?");
        p.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        return sb.substring(0, sb.length() - 1);
    }

    private String getRequest(String path, Map<String, Object> p) {
        String ret;
        try {
            String url = this.combPath(path);
            String params = this.combParams(p);
            String rurl = url + params;
            HttpGet get = new HttpGet(rurl);
            ret = handleResp(doRequest(path, get));
        } catch (Exception e) {
            log.info("path=[{}], params=[{}] error.", path, p, e);
            throw new RuntimeException(e);
        }

        return ret;
    }

    private String deleteRequest(String path, Map<String, Object> p) {
        String ret;
        try {
            String url = this.combPath(path);
            String params = this.combParams(p);
            String rurl = url + params;
            HttpDelete delete = new HttpDelete(rurl);
            ret = handleResp(doRequest(path, delete));
        } catch (Exception e) {
            log.info("path=[{}], params=[{}] error.", path, p, e);
            throw new RuntimeException(e);
        }

        return ret;
    }

    private String putRequest(String path, Map<String, Object> p) {
        String ret;
        try {
            String url = this.combPath(path);
            HttpEntity body = this.combBody(p);
            HttpPut put = new HttpPut(url);
            put.setEntity(body);
            ret = handleResp(doRequest(path, put));
        } catch (Exception e) {
            log.info("path=[{}], params=[{}] error.", path, p, e);
            throw new RuntimeException(e);
        }

        return ret;
    }

    private String readContent(HttpEntity e, String charset) throws IOException, ParseException {
        return EntityUtils.toString(e, Objects.isNull(charset)?this.config.getCharset():charset);
    }

    private String parseString(Object o) {
        return Objects.isNull(o)?null:o.toString();
    }

    private Map<String, Object> str2Map(String s) {
        try {
            return this.jsonMapper.readValue(s, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}