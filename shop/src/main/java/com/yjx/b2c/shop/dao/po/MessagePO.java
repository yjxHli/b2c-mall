package com.yjx.b2c.shop.dao.po;

import lombok.Data;

import java.util.Date;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */
@Data
public class MessagePO {
    private Integer id;
    private Integer shopId;
    private Integer senderId;
    private String title;
    private String content;
    private Integer msgType;
    private Integer isRead;
    private Date readTime;
    private Date createdAt;
    private Date updatedAt;
}
