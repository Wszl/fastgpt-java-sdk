package org.xdove.thridpart.fastgpt;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.xdove.thridpart.fastgpt.entity.ChatMessage;
import org.xdove.thridpart.fastgpt.entity.PushData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ServiceRequestsTest {

    private ServiceRequests serviceRequests;

    @Before
    public void setUp() {
        Config config = new Config(
                System.getenv("KEY"),
                System.getenv("CHAT_KEY")
        );
        this.serviceRequests = new ServiceRequests(config);
        System.out.println(config.getCharset());
    }

    @Test
    public void chatCompletions() {
        String chatId = System.getenv("CHAT_ID");
        List<ChatMessage> msg = Arrays.asList(new ChatMessage("hi!"));
        Map<String, Object> ret = this.serviceRequests.chatCompletions(chatId, true, null, msg);
        System.out.println(ret);
    }

    @Test
    public void chatCompletionsStream() throws IOException {
        String chatId = System.getenv("CHAT_ID");
        List<ChatMessage> msg = Arrays.asList(new ChatMessage("hi!"));
        InputStream is = this.serviceRequests.chatCompletionsStream(chatId, true, null, msg);
        System.out.println(IOUtils.readLines(is, Charset.defaultCharset()));
        is.close();
    }

    @Test
    public void createDataset() {
        String name = "api_test";
        Map<String, Object> ret = this.serviceRequests.createDataset(null, null, name, null, null, null, null);
        System.out.println(ret);
    }

    @Test
    public void createTextCollection() {
        String name = "test";
        String text = "text_text";
        String datasetId = System.getenv("DATASET_ID");
        String trainingType = "chunk";
        int chunkSize = 500;
        Map<String, Object> ret = this.serviceRequests.createTextCollection(text, datasetId, null, name, null, trainingType, chunkSize, null, null);
        System.out.println(ret);
    }

    @Test
    public void pushData() {
        String collectionId = System.getenv("COLLECTION_ID");
        String trainingType = "chunk";
        List<PushData> data = Collections.singletonList(new PushData("hi!", "hello world!", null));
        Map<String, Object> ret = this.serviceRequests.pushData(collectionId, trainingType, null, data);
        System.out.println(ret);
    }

    @Test
    public void getDataset() {
        String datasetId = System.getenv("DATASET_ID");
        Map<String, Object> ret = this.serviceRequests.getDataset(datasetId);
        System.out.println(ret);
    }

    @Test
    public void getCollection() {
        String collectionId = System.getenv("COLLECTION_ID");
        Map<String, Object> ret = this.serviceRequests.getCollection(collectionId);
        System.out.println(ret);
    }

    @Test
    public void listDataset() {
        Map<String, Object> ret = this.serviceRequests.listDataset(null);
        System.out.println(ret);
    }

    @Test
    public void deleteDataset() {
        String datasetId = System.getenv("DATASET_ID");
        Map<String, Object> ret = this.serviceRequests.deleteDataset(datasetId);
        System.out.println(ret);
    }

    @Test
    public void createCollection() {
        String name = "test_createCollection";
        String datasetId = System.getenv("DATASET_ID");
        String type = "folder";
        Map<String, Object> ret = this.serviceRequests.createCollection(datasetId, null, name, type, null);
        System.out.println(ret);
    }

    @Test
    public void createLinkCollection() {
        String link = "https://doc.fastgpt.in/docs/course/quick-start/";
        String datasetId = System.getenv("DATASET_ID");
        String trainingType = "chunk";
        int chunkSize = 500;
        Map<String, Object> ret = this.serviceRequests.createLinkCollection(link, datasetId, null, null, trainingType, chunkSize, null, null);
        System.out.println(ret);
    }

    @Test
    public void listCollection() {
        String datasetId = System.getenv("DATASET_ID");
        int pageNum = 1;
        int pageSize = 10;
        String searchText = "hi";
        Map<String, Object> ret = this.serviceRequests.listCollection(datasetId, pageNum, pageSize, null, searchText);
        System.out.println(ret);
    }

    @Test
    public void updateCollection() {
        String collectionId = System.getenv("COLLECTION_ID");
        String name = "new_collection";
        Map<String, Object> ret = this.serviceRequests.updateCollection(collectionId, null, name);
        System.out.println(ret);
    }

    @Test
    public void deleteCollection() {
        String datasetId = System.getenv("COLLECTION_ID");
        String name = "new_collection";
        Map<String, Object> ret = this.serviceRequests.updateCollection(datasetId, null, name);
        System.out.println(ret);
    }

    @Test
    public void listData() {
        String collectionId = System.getenv("COLLECTION_ID");
        int pageNum = 1;
        int pageSize = 10;
        String searchText = "hi";
        Map<String, Object> ret = this.serviceRequests.listData(collectionId, pageNum, pageSize, searchText);
        System.out.println(ret);
    }

    @Test
    public void getData() {
        String dataId = System.getenv("DATA_ID");
        Map<String, Object> ret = this.serviceRequests.getData(dataId);
        System.out.println(ret);
    }

    @Test
    public void updateData() {
        String dataId = System.getenv("DATA_ID");
        String q = "new_q";
        String a = "new_a";
        Map<String, Object> ret = this.serviceRequests.updateData(dataId, q, a, null);
        System.out.println(ret);
    }

    @Test
    public void deleteData() {
        String dataId = System.getenv("DATA_ID");
        Map<String, Object> ret = this.serviceRequests.deleteData(dataId);
        System.out.println(ret);
    }

    @Test
    public void searchTestDataset() {
        String datasetId = System.getenv("DATASET_ID");
        String searchText = "hi";
        int limit = 100;
        int similarity = 0;
        String searchMode = "embedding";
        boolean usingReRank = false;
        Map<String, Object> ret = this.serviceRequests.searchTestDataset(datasetId, searchText, limit, similarity, searchMode, usingReRank);
        System.out.println(ret);
    }


}