package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

//@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticSearchTest {
    private RestHighLevelClient client;

    @Test
    void testMatchAll() throws IOException {
        //1.创建Request对象
        SearchRequest searchRequest = new SearchRequest("items");

        //2.配置Request参数
        searchRequest.source()
                .query(QueryBuilders.matchAllQuery());
        //3.发送请求
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("response = " + response);

        //4.解析结果
        parseSearchResult(response);
    }

    @Test
    void testMatchSearch() throws IOException {
        //1.创建Request对象
        SearchRequest searchRequest = new SearchRequest("items");

        //2.组织DSL查询参数
        searchRequest.source()
                .query(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("name","脱脂牛奶"))
                        .filter(QueryBuilders.termQuery("brand","德亚"))
                        .filter(QueryBuilders.rangeQuery("price").lt(10000))
                );
        //3.发送请求
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("response = " + response);

        //4.解析结果
        parseSearchResult(response);
    }

    @Test
    void testSortAndPage() throws IOException {
        //0.模拟前端传递的分页参数
        int pageNo = 1 , pageSize = 5;

        //1.创建Request对象
        SearchRequest searchRequest = new SearchRequest("items");

        //2.组织DSL查询参数
        //2.1 query条件
        searchRequest.source().query(QueryBuilders.matchAllQuery());
        //2.2分页
        searchRequest.source().from((pageNo - 1) * pageSize).size(pageSize);
        //2.3排序
        searchRequest.source()
                .sort("sold", SortOrder.DESC)
                .sort("price", SortOrder.ASC);
        //3.发送请求
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("response = " + response);

        //4.解析结果
        parseSearchResult(response);
    }

    @Test
    void testHighlight() throws IOException {
        //1.创建Request对象
        SearchRequest searchRequest = new SearchRequest("items");

        //2.组织DSL查询参数
        //2.1 query条件
        searchRequest.source().query(QueryBuilders.matchQuery("name","脱脂牛奶"));
        //2.2 高亮条件
        searchRequest.source().highlighter(SearchSourceBuilder.highlight()
                .field("name")
                .preTags("<em>") //高亮默认的标签就是<em>，所以这里也可以选择忽略 ---2024年7月7日 11:17
                .postTags("</em>")
        );
        //3.发送请求
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("response = " + response);

        //4.解析结果
        parseSearchResult(response);
    }

    private static void parseSearchResult(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        //4.1 总条数
        long value = searchHits.getTotalHits().value;
        System.out.println("value = " + value);
        //4.2 命中的数据
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            //4.2.1获取source结果
            String json = hit.getSourceAsString();
            //4.2.2转为Itemdoc
            ItemDoc doc = JSONUtil.toBean(json, ItemDoc.class);
            //4.3处理高亮结果--2024年7月7日
            Map<String, HighlightField> hfs = hit.getHighlightFields();
            if (hfs != null && !hfs.isEmpty()) {
                //4.3.1根据高亮字段名获取高亮结果
                HighlightField hf = hfs.get("name");
                //4.3.2获取高亮结果，覆盖非高亮结果
                String hfName = hf.getFragments()[0].string();
                doc.setName(hfName);
            }
            System.out.println("doc = " + doc);
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
