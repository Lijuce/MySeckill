package com.enums;

public enum SeckillStatEnum {
    SUCCESS(1, "秒杀成功"),
    END(0, "秒杀结束"),
    REPEAT_KILL(-1, "重复秒杀"),
    INNER_ERROR(-2, "系统异常"),
    DATE_REWRITE(-3, "数据篡改");

    private int state;
    private String info;

    SeckillStatEnum(){

    }

    SeckillStatEnum(int state, String info) {
        this.state = state;
        this.info = info;
    }

    public int getState() {
        return state;
    }

    public String getInfo() {
        return info;
    }
    // 根据索引获取秒杀状态
    public static SeckillStatEnum stateOf(int index){
        for (SeckillStatEnum statEnum: values()){
            if (statEnum.getState() == index)
                return statEnum;
        }
        return null;
    }
}
