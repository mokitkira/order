package com.kira.order.entity;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @author 
 * 
 */
@Data
public class Order implements Serializable {
    private Integer id;

    private String orderNum;

    private Date crtTime;

    private Date updTime;

    private String crtUser;

    private static final long serialVersionUID = 1L;
}