package com.hgz.community.dao.elasticsearch;


import com.hgz.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {
}
