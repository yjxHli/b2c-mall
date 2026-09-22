package com.yjx.b2c.product.componet;

import cn.hutool.core.lang.Assert;
import com.yjx.b2c.product.dao.mapper.CategoryMapper;
import com.yjx.b2c.product.dao.mapper.SkuMapper;
import com.yjx.b2c.product.dao.po.CategoryPO;
import com.yjx.b2c.product.dao.po.SkuPO;
import com.yjx.b2c.product.enums.ProductStatusEnum;
import com.yjx.b2c.product.enums.ProductTypeEnum;
import com.yjx.b2c.product.service.SkuLogService;
import com.yjx.b2c.product.web.request.CreateProductRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * @author yjx
 * @date 2026/9/15
 * @description 新建商品模板：流程固定，差异化的库存/合规校验交给子类实现
 */
@Slf4j
public abstract class CreateProductTemplate {

    /** 违禁词，实际项目应放在词库表或配置中心 */
    private static final List<String> FORBIDDEN_WORDS = Arrays.asList("违禁", "赌博", "毒品", "枪支", "色情", "假货", "刷单");

    @Autowired
    protected SkuMapper skuMapper;
    @Autowired
    protected CategoryMapper categoryMapper;
    @Autowired
    protected SkuLogService skuLogService;

    /**
     * 模板方法：参数校验 -> 类目归属校验 -> 价格库存校验 -> 内容合规审核 -> 保存商品信息 -> 上架 -> 后置处理
     */
    public final Integer createProduct(CreateProductRequest req) {
        // 1 参数校验
        validateParam(req);
        // 2 类目归属校验
        validateCategory(req);
        // 3 价格库存校验（含子类库存钩子）
        validatePriceAndStock(req);
        // 4 内容合规审核（含子类合规钩子）
        auditContent(req);
        // 5 保存商品信息
        SkuPO sku = saveProduct(req);
        // 6 上架
        onShelf(sku);
        // 7 后置处理
        afterProcess(sku, req);
        return sku.getId();
    }

    /** 商品类型，SkuService 用它选择模板 */
    public abstract ProductTypeEnum productType();

    /** 差异化校验：库存（实物商品校验库存，虚拟商品库存不限） */
    protected abstract void validateStock(CreateProductRequest req);

    /** 差异化校验：合规（虚拟商品校验交付内容合规，实物商品无额外要求） */
    protected abstract void auditCompliance(CreateProductRequest req);

    private void validateParam(CreateProductRequest req) {
        Assert.notNull(req, "请求参数不能为空");
        Assert.notNull(req.getShopId(), "店铺不能为空");
        Assert.notNull(req.getCategoryId(), "商品类目不能为空");
        Assert.notNull(req.getProductType(), "商品类型不能为空");
        Assert.notBlank(req.getProductName(), "商品名称不能为空");
        Assert.isTrue(req.getProductName().length() <= 200, "商品名称过长");
    }

    private void validateCategory(CreateProductRequest req) {
        CategoryPO category = categoryMapper.findById(req.getCategoryId());
        Assert.notNull(category, "商品类目不存在");
        Assert.equals(category.getStatus(), 1, "商品类目已禁用");
        Assert.equals(categoryMapper.countChildren(category.getId()), 0, "商品类目必须是末级类目");
    }

    private void validatePriceAndStock(CreateProductRequest req) {
        Assert.notNull(req.getPrice(), "商品价格不能为空");
        Assert.isTrue(req.getPrice().compareTo(BigDecimal.ZERO) > 0, "商品价格必须大于0");
        validateStock(req);
    }

    private void auditContent(CreateProductRequest req) {
        Assert.isFalse(containsForbiddenWord(req.getProductName()), "商品名称包含违禁词");
        Assert.isFalse(containsForbiddenWord(req.getProductDesc()), "商品描述包含违禁词");
        auditCompliance(req);
    }

    private SkuPO saveProduct(CreateProductRequest req) {
        SkuPO po = new SkuPO();
        po.setShopId(req.getShopId());
        po.setCategoryId(req.getCategoryId());
        po.setProductName(req.getProductName());
        po.setProductDesc(req.getProductDesc());
        po.setProductType(req.getProductType());
        po.setPrice(req.getPrice());
        po.setStock(req.getStock() == null ? 0 : req.getStock());
        po.setDeliveryContent(req.getDeliveryContent());
        po.setCoverUrl(req.getCoverUrl());
        po.setOperatorId(req.getOperatorId());
        po.setStatus(ProductStatusEnum.DRAFT.getCode());

        Integer row = skuMapper.save(po);
        Assert.equals(row, 1, "保存商品失败");
        log.info("商品已保存, skuId={}, type={}", po.getId(), productType().getDesc());
        return po;
    }

    private void onShelf(SkuPO sku) {
        int row = skuMapper.updateStatus(sku.getId(), ProductStatusEnum.ON_SALE.getCode());
        Assert.equals(row, 1, "商品上架失败");
        sku.setStatus(ProductStatusEnum.ON_SALE.getCode());
        log.info("商品已上架, skuId={}", sku.getId());
    }

    /** 后置处理：默认记录操作日志，子类可按需扩展 */
    protected void afterProcess(SkuPO sku, CreateProductRequest req) {
        skuLogService.saveLog(sku.getId(), sku.getShopId(), sku.getOperatorId(), "CREATE_PRODUCT",
                "新建商品并上架：" + sku.getProductName());
        log.info("商品创建后置处理完成, skuId={}", sku.getId());
    }

    protected boolean containsForbiddenWord(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return FORBIDDEN_WORDS.stream().anyMatch(text::contains);
    }
}
