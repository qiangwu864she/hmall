package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticDocumentTest {
    private RestHighLevelClient client;

    @Autowired
    private IItemService itemService;

    //在es中新增文档(类似于一行数据）
    @Test
    void testIndexDocument() throws IOException {
        //0.准备数据
        //0.1根据id查询数据库中的数据
        Item item = itemService.getById(577967L);
        //0.2把数据库中的数据转为文档中的数据
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        itemDoc.setPrice(74800);
        //1.准备Request
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        //2.准备请求参数
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        //3.发送请求
        IndexResponse resp = client.index(request, RequestOptions.DEFAULT);
        System.out.println("resp = " + resp);
    }

    //在es中查询文档测试 2024年6月29日
    @Test
    void testGetDocument() throws IOException {
        //1.准备Request
        GetRequest request = new GetRequest("items","577967");
        //2.发送请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        //3.解析响应结果
        String json = response.getSourceAsString();
        ItemDoc doc = JSONUtil.toBean(json, ItemDoc.class);
        System.out.println("doc = " + doc);
    }

    //在es中删除文档测试 2024年6月29日
    @Test
    void testDeleteDocument() throws IOException {
        //1.准备Request
        DeleteRequest request = new DeleteRequest("items", "577967");
        //2.发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }

    //在es中局部修改文档测试（全量修改和新增文档类似） 2024年6月29日
    @Test
    void testUpdateDocument() throws IOException {
        //1.准备Request
        UpdateRequest request = new UpdateRequest("items", "577967");
        //2.准备请求参数
        request.doc(
                "price",72300
        );
        //3.发送请求
        client.update(request, RequestOptions.DEFAULT);
    }

    //在es中批量处理文档bulk
    @Test
    void testBulkDocument() throws IOException {
        int pageNo = 1, pageSize = 500;
        while(true){
            //0.准备文档数据，从数据库中获取
            Page<Item> page = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(Page.of(pageNo, pageSize));
            List<Item> records = page.getRecords();
            if(records == null || records.isEmpty()){
                return;
            }
            //1.准备Request
            BulkRequest request = new BulkRequest();
            //2.准备请求参数
            for (Item item : records) {
                request.add(new IndexRequest("items")
                        .id(item.getId().toString())
                        .source(JSONUtil.toJsonStr(BeanUtil.copyProperties(item, ItemDoc.class)),XContentType.JSON));
            }
            //3.发送请求
            client.bulk(request, RequestOptions.DEFAULT);
            //4.翻页
            pageNo++;
        }
    }

    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(RestClient. builder(
                HttpHost.create("http://localhost:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
    }
}
