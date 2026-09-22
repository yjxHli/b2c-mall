package com.yjx.b2c.product.dao.po;

import lombok.Data;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 商品类目
 */
@Data
public class CategoryPO {
    private Integer id;
    private Integer parentId;
    private String categoryName;
    private Integer level;
    private Integer status;
}
