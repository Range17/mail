package com.range.mail.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.range.mail.product.entity.CategoryBrandRelationEntity;
import com.range.mail.product.service.CategoryBrandRelationService;
import com.range.mail.product.vo.Catelog2Vo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.util.Times;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.range.common.utils.PageUtils;
import com.range.common.utils.Query;

import com.range.mail.product.dao.CategoryDao;
import com.range.mail.product.entity.CategoryEntity;
import com.range.mail.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    /**
     *  @Cacheable:触发将数据保存到缓存的操作
     *  @CacheEvict：触发将数据从缓存删除的操作
     *  @CachePut：不影响方法执行更新缓存
     *  @Caching：组合以上多个操作
     *  @CacheConfig：在类级别共享缓存的相同配置
     */


    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查询出所有分类
        List<CategoryEntity> categoryEntityList = categoryDao.selectList(null);

        //获取所有分类下的子分类
        List<CategoryEntity> classifyCategoryEntityList = categoryEntityList.stream().filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .map(categoryEntity -> {
                    categoryEntity.setChildren(getChildrenCategoryEntity(categoryEntity, categoryEntityList));
                    return categoryEntity;
                }).sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                }).collect(Collectors.toList());

        return classifyCategoryEntityList;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {

        //TODO 1.检查当前删除的菜单是否被别的地方引用
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCateLogPath(Long catelogId) {
        List<Long> path = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, path);
        //将集合顺序反转了
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[path.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());


    }

    @Cacheable({"category"})
    //代表当前方法的结果需要缓存
    //1、如果缓存有，方法不用调用
    //2、如果缓存中没有，会调用方法，最后将结果放入缓存
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() throws InterruptedException {
        /**
         * 1. SpringBoot2.0之后默认使用 lettuce 作为操作 redis 的客户端，lettuce 使用 Netty 进行网络通信
         * 2. lettuce 的 bug 导致 Netty 堆外内存溢出 -Xmx300m   Netty 如果没有指定对外内存 默认使用 JVM 设置的参数
                *      可以通过 -Dio.netty.maxDirectMemory 设置堆外内存
                * 解决方案：不能仅仅使用 -Dio.netty.maxDirectMemory 去调大堆外内存
                *      1. 升级 lettuce 客户端   2. 切换使用 jedis
         *
         *      RedisTemplate 对 lettuce 与 jedis 均进行了封装 所以直接使用 详情见：RedisAutoConfiguration 类
         */

        /**
         * 1、空结果缓存：解决缓存穿透
         * 2、设置过期时间（加随机值）：解决缓存雪崩
         * 3、加锁：解决缓存击穿
         */

        // 给缓存中放入JSON字符串，取出JSON字符串还需要逆转为能用的对象类型

        //查询缓存
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if(StringUtils.isEmpty(catalogJSON)){
            //缓存中没有，从数据库中查询
            Map<String, List<Catelog2Vo>> catalogJsonFromDB = getCatalogJsonFromWithRedissonLock();

            //将对象转为json放入缓存中
            String s = JSON.toJSONString(catalogJsonFromDB);
            //将过期时间设置为1天
            stringRedisTemplate.opsForValue().set("catalogJSON",s,1, TimeUnit.DAYS);
            return catalogJsonFromDB;
        }

        //转为指定的对象
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON,new TypeReference<Map<String, List<Catelog2Vo>>>(){});
        return result;
    }

    //缓存如何和数据库保持一致
    //使用redisson来实现分布式锁
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromWithRedissonLock() throws InterruptedException {

        RLock lock = redissonClient.getLock("catalogJson-lock");
        lock.lock();
        Map<String, List<Catelog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        } finally  {
           lock.unlock();
        }
        return dataFromDb;
    }

        //实现redis分布式锁
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromWithRedisLock() throws InterruptedException {
        String uuid = UUID.randomUUID().toString();
        Boolean lockResult = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid,300, TimeUnit.DAYS);
        if(lockResult){
            //加锁成功。。执行业务
            Map<String, List<Catelog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally  {
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Collections.singletonList("lock"), uuid);
            }
            return dataFromDb;
        }else{
            //加锁失败
            Thread.sleep(100);
            return getCatalogJsonFromWithRedisLock();
        }
    }

        //从数据库查询并封装分类数据
    public Map<String, List<Catelog2Vo>> getDataFromDb() {

        //首先查询缓存是否存在
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if(!StringUtils.isEmpty(catalogJSON)){
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>(){});
            return result;
        }

        //缓存不存在的情况下查询数据库
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        List<CategoryEntity> level1Categories = getParent_cid(selectList, 0L);

        Map<String, List<Catelog2Vo>> parentCid = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2VOS = null;
            if (!CollectionUtils.isEmpty(categoryEntities)) {
                catelog2VOS = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2VO = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    List<CategoryEntity> level3Catalog = getParent_cid(selectList, l2.getCatId());
                    if (!CollectionUtils.isEmpty(level3Catalog)) {
                        List<Catelog2Vo.Catelog3VO> collect = level3Catalog.stream().map(l3 -> {
                            Catelog2Vo.Catelog3VO catelog3VO = new Catelog2Vo.Catelog3VO(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3VO;
                        }).collect(Collectors.toList());
                        catelog2VO.setCatalog3List(collect);
                    }
                    return catelog2VO;
                }).collect(Collectors.toList());
            }
            return catelog2VOS;
        }));

        //将数据库查询出来的数据insert到redis中
        String cache = JSON.toJSONString(parentCid);
        stringRedisTemplate.opsForValue().set("catalogJSON",cache,1, TimeUnit.DAYS);
        return parentCid;
    }

    private List<Long> findParentPath(Long catelogId, List<Long> path) {
        // 收集当前节点
        path.add(catelogId);
        CategoryEntity categoryEntity = this.getById(catelogId);
        if (categoryEntity.getParentCid() != 0) {
            findParentPath(categoryEntity.getParentCid(), path);
        }
        return path;
    }

    /**
     * 使用递归的方法获取子菜单
     *
     * @param categoryEntity：需要获取子菜单的categoryEntity
     * @param categoryEntityList：全部分类数据
     * @return
     */
    public List<CategoryEntity> getChildrenCategoryEntity(CategoryEntity categoryEntity, List<CategoryEntity> categoryEntityList) {

        List<CategoryEntity> childCategoryEntityList = categoryEntityList.stream().filter(categoryEntity1 -> categoryEntity.getCatId().equals(categoryEntity1.getParentCid()))
                .map(categoryEntity1 -> {
                    categoryEntity1.setChildren(getChildrenCategoryEntity(categoryEntity1, categoryEntityList));
                    return categoryEntity1;
                }).sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                }).collect(Collectors.toList());

        return childCategoryEntityList;
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parentCid) {
        return selectList.stream().filter(o -> o.getParentCid().equals(parentCid)).collect(Collectors.toList());
    }

}