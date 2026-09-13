package com.example.middemo.web;

/**
 * HTTP 路径的唯一出处。
 *
 * <p>控制器用它声明路由、也用它拼 {@code Location} 响应头；测试用同一批常量构造请求。
 * 这样"改一个路径"只改这里一处，不必在注解、Location 拼接、以及 50 多处测试里各改一遍。
 *
 * <p>路径模板里的 {@code {id}} 由 {@link #withId(String, Object)} 填充。
 */
public final class ApiPaths {

    public static final String USERS = "/api/users";
    public static final String USER_BY_ID = USERS + "/{id}";
    public static final String USER_STATUS = USER_BY_ID + "/status";

    public static final String PRODUCTS = "/api/products";
    public static final String PRODUCT_BY_ID = PRODUCTS + "/{id}";
    public static final String PRODUCT_STOCK = PRODUCT_BY_ID + "/stock";
    public static final String PRODUCT_STATUS = PRODUCT_BY_ID + "/status";

    public static final String ORDERS = "/api/orders";
    public static final String ORDER_BY_ID = ORDERS + "/{id}";
    public static final String ORDER_STATUS = ORDER_BY_ID + "/status";

    public static final String DASHBOARD_SUMMARY = "/api/dashboard/summary";
    public static final String DASHBOARD_RECENT_ORDERS = "/api/dashboard/recent-orders";

    /** 把模板里的 {@code {id}} 换成真实值。 */
    public static String withId(String template, Object id) {
        return template.replace("{id}", String.valueOf(id));
    }

    private ApiPaths() {
    }
}
