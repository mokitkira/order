package com.kira.order.entity;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @author 
 * 
 */
@Data
public class SubOrder implements Serializable {
    private Integer id;

    private Date crtTime;

    private Integer orderId;

    private String crtUser;

    private String orderNum;

    private static final long serialVersionUID = 1L;
}