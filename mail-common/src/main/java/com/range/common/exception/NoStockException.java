package com.range.common.exception;

/**
 * 无库存异常
 */
public class NoStockException extends RuntimeException {

    private Long skuId;

    //构造器
    public NoStockException(Long skuId) {
        super("商品 ID:" + skuId + "没有足够的库存.");
    }

    public NoStockException(String msg) {
        super(msg);
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

}
